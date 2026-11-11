package de.winfprojekt.craftvoice.userservice.application.port;

public interface IdentityProvider {

    String createUser(String email, String firstName, String lastName, String password);

    void sendVerificationEmail(String userId);

    void assignRealmRole(String userId, String roleName);

    void sendPasswordResetEmail(String email);

    void deleteUser(String userId);

    void updateName(String userId, String firstName, String lastName);
}
