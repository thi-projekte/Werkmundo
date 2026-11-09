package de.winfprojekt.craftvoice.userservice.application.port;

import java.nio.file.Path;

public interface ProfilePictureStore {

    String store(Long userId, Path uploadedFile);
}
