package de.winfprojekt.craftvoice.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerCreateRequest {

    @NotBlank
    @Email
    public String email;

    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String companyName;
    public String street;
    public String houseNumber;
    public String zipCode;
    public String city;
    public String state;
    public String country;
}
