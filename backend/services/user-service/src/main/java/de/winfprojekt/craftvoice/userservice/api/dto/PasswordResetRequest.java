package de.winfprojekt.craftvoice.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordResetRequest {

    @NotBlank
    @Email
    public String email;
}
