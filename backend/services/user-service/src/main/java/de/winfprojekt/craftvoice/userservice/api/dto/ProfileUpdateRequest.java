package de.winfprojekt.craftvoice.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileUpdateRequest {

    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String profilePictureUrl;
}
