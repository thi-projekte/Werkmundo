package de.winfprojekt.craftvoice.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerUpdateRequest {

    @Email
    @Size(min = 1)
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
