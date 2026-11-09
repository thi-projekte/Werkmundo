package de.winfprojekt.craftvoice.userservice.infrastructure;

import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilePictureStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesDecodedPngWithGeneratedName() throws IOException {
        Path upload = createPng("upload.png");
        ProfilePictureStorage storage = new ProfilePictureStorage(tempDir.resolve("store"), 1024);

        String fileName = storage.store(17L, upload);

        assertTrue(fileName.matches("profile_17_[0-9a-f-]{36}\\.png"));
        assertNotEquals(upload.getFileName().toString(), fileName);
        assertTrue(Files.isRegularFile(tempDir.resolve("store").resolve("profiles").resolve(fileName)));
    }

    @Test
    void storesWebpFromItsRiffImageContent() throws IOException {
        byte[] webp = Base64.getDecoder().decode(
                "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEAAUAmJaQAA3AA/v89"
        );
        Path upload = Files.write(tempDir.resolve("upload.dat"), webp);
        ProfilePictureStorage storage = new ProfilePictureStorage(tempDir.resolve("store"), 1024);

        String fileName = storage.store(17L, upload);

        assertTrue(fileName.matches("profile_17_[0-9a-f-]{36}\\.webp"));
        assertTrue(Files.isRegularFile(tempDir.resolve("store").resolve("profiles").resolve(fileName)));
    }

    @Test
    void rejectsUnsupportedContentEvenWhenClientUsesImageExtension() throws IOException {
        Path upload = Files.writeString(tempDir.resolve("fake.jpg"), "not an image");
        ProfilePictureStorage storage = new ProfilePictureStorage(tempDir.resolve("store"), 1024);

        assertThrows(NotSupportedException.class, () -> storage.store(17L, upload));
    }

    @Test
    void rejectsPngContentThatIsNotDecodable() throws IOException {
        Path upload = Files.write(
                tempDir.resolve("broken.png"),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}
        );
        ProfilePictureStorage storage = new ProfilePictureStorage(tempDir.resolve("store"), 1024);

        assertThrows(BadRequestException.class, () -> storage.store(17L, upload));
    }

    @Test
    void rejectsFilesAboveConfiguredLimitWithHttp413() throws IOException {
        Path upload = Files.write(tempDir.resolve("large.bin"), new byte[65]);
        ProfilePictureStorage storage = new ProfilePictureStorage(tempDir.resolve("store"), 64);

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> storage.store(17L, upload)
        );

        assertEquals(413, exception.getResponse().getStatus());
    }

    private Path createPng(String name) throws IOException {
        Path file = tempDir.resolve(name);
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", file.toFile());
        return file;
    }
}
