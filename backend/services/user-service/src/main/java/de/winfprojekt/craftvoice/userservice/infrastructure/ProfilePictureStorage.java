package de.winfprojekt.craftvoice.userservice.infrastructure;

import de.winfprojekt.craftvoice.userservice.application.port.ProfilePictureStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class ProfilePictureStorage implements ProfilePictureStore {

    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final int MAX_IMAGE_DIMENSION = 10_000;

    private final Path storageDirectory;
    private final long maxFileSize;

    @Inject
    public ProfilePictureStorage(
            @ConfigProperty(
                    name = "userservice.profile-picture.storage-path",
                    defaultValue = "${java.io.tmpdir}/craftvoice"
            ) String storagePath,
            @ConfigProperty(name = "userservice.profile-picture.max-size-bytes", defaultValue = "5242880")
            long maxFileSize
    ) {
        this(Paths.get(storagePath), maxFileSize);
    }

    ProfilePictureStorage(Path storageDirectory, long maxFileSize) {
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Profile picture maximum size must be positive");
        }
        this.storageDirectory = storageDirectory.toAbsolutePath().normalize().resolve("profiles");
        this.maxFileSize = maxFileSize;
    }

    @Override
    public String store(Long userId, Path uploadedFile) {
        if (userId == null || uploadedFile == null || !Files.isRegularFile(uploadedFile)) {
            throw new BadRequestException("Invalid profile picture upload");
        }

        final long fileSize;
        try {
            fileSize = Files.size(uploadedFile);
        } catch (IOException e) {
            throw new BadRequestException("Could not read profile picture upload", e);
        }
        if (fileSize == 0) {
            throw new BadRequestException("Profile picture upload is empty");
        }
        if (fileSize > maxFileSize) {
            throw new WebApplicationException(
                    "Profile picture exceeds the maximum size of " + maxFileSize + " bytes",
                    Response.Status.REQUEST_ENTITY_TOO_LARGE
            );
        }

        final byte[] content;
        try {
            content = Files.readAllBytes(uploadedFile);
        } catch (IOException e) {
            throw new BadRequestException("Could not read profile picture upload", e);
        }
        if (content.length > maxFileSize) {
            throw new WebApplicationException(
                    "Profile picture exceeds the maximum size of " + maxFileSize + " bytes",
                    Response.Status.REQUEST_ENTITY_TOO_LARGE
            );
        }

        String extension = detectImageExtension(uploadedFile, content);
        String fileName = "profile_" + userId + "_" + UUID.randomUUID() + extension;
        Path destination = storageDirectory.resolve(fileName).normalize();
        if (!destination.getParent().equals(storageDirectory)) {
            throw new BadRequestException("Invalid profile picture path");
        }

        try {
            Files.createDirectories(storageDirectory);
            Files.write(
                    destination,
                    content,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
            return fileName;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw new WebApplicationException("Could not save profile picture", e);
        }
    }

    private String detectImageExtension(Path file, byte[] content) {
        if (isWebp(content)) {
            return ".webp";
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input != null) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                while (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(input, true, true);
                        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                        if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
                            continue;
                        }

                        int width = reader.getWidth(0);
                        int height = reader.getHeight(0);
                        if (!isAcceptableDimensions(width, height)) {
                            throw new BadRequestException("Profile picture dimensions are not supported");
                        }
                        if (reader.read(0) == null) {
                            throw new BadRequestException("Profile picture content is invalid");
                        }
                        return format.equals("png") ? ".png" : ".jpg";
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (BadRequestException | NotSupportedException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("Profile picture content is invalid", e);
        }

        throw new NotSupportedException("Only JPEG, PNG, and WebP images are supported");
    }

    private boolean isWebp(byte[] content) {
        if (content.length < 20
                || !matches(content, 0, "RIFF")
                || !matches(content, 8, "WEBP")
                || readUnsignedIntLittleEndian(content, 4) + 8 != content.length) {
            return false;
        }

        int offset = 12;
        while (offset + 8 <= content.length) {
            long chunkSize = readUnsignedIntLittleEndian(content, offset + 4);
            int dataStart = offset + 8;
            long nextOffset = dataStart + chunkSize + (chunkSize & 1);
            if (nextOffset > content.length) {
                return false;
            }

            if (matches(content, offset, "VP8 ") && chunkSize > 10) {
                return content[dataStart + 3] == (byte) 0x9d
                        && content[dataStart + 4] == 0x01
                        && content[dataStart + 5] == 0x2a
                        && isAcceptableDimensions(
                                littleEndianShort(content, dataStart + 6) & 0x3fff,
                                littleEndianShort(content, dataStart + 8) & 0x3fff
                        );
            }

            if (matches(content, offset, "VP8L") && chunkSize > 5) {
                int imageHeader = dataStart;
                if (content[imageHeader] != 0x2f) {
                    return false;
                }
                int width = 1 + (content[imageHeader + 1] & 0xff)
                        + ((content[imageHeader + 2] & 0x3f) << 8);
                int height = 1 + ((content[imageHeader + 2] & 0xc0) >>> 6)
                        + ((content[imageHeader + 3] & 0xff) << 2)
                        + ((content[imageHeader + 4] & 0x0f) << 10);
                return isAcceptableDimensions(width, height);
            }

            offset = Math.toIntExact(nextOffset);
        }
        return false;
    }

    private boolean isAcceptableDimensions(int width, int height) {
        return width > 0
                && height > 0
                && width <= MAX_IMAGE_DIMENSION
                && height <= MAX_IMAGE_DIMENSION
                && (long) width * height <= MAX_IMAGE_PIXELS;
    }

    private static int littleEndianShort(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static long readUnsignedIntLittleEndian(byte[] data, int offset) {
        return Integer.toUnsignedLong(ByteBuffer.wrap(data, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt());
    }

    private static boolean matches(byte[] data, int offset, String expected) {
        for (int i = 0; i < expected.length(); i++) {
            if (data[offset + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
