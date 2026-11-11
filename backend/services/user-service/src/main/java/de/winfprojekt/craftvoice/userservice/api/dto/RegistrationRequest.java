package de.winfprojekt.craftvoice.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistrationRequest {

    @NotBlank
    @Email
    public String email;

    @NotBlank
    public String password;

    @NotBlank
    public String firstName;

    @NotBlank
    public String lastName;
}
