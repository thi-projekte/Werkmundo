package de.winfprojekt.craftvoice.userservice.api.dto;

import de.winfprojekt.craftvoice.userservice.UserRole;
import de.winfprojekt.craftvoice.userservice.UserStatus;

import java.math.BigDecimal;
import java.util.Set;

public class UserResponse {

    public Long id;
    public String email;
    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String profilePictureUrl;
    public UserStatus status;
    public Set<UserRole> roles;

    public String companyName;
    public String vatId;
    public String tradeRegisterNumber;

    public String street;
    public String houseNumber;
    public String zipCode;
    public String city;
    public String state;
    public String country;

    public String companyEmail;
    public String companyPhoneNumber;
    public String website;
    public String industry;

    public String iban;
    public String bic;
    public String bankName;
    public String accountHolder;

    public String taxNumber;
    public String legalForm;

    public Integer employeeCount;
    public Integer customerCount;
    public Double hourlyRate;
    public String priceListUrl;

    public String travelModel;
    public BigDecimal travelFlatRate;
    public BigDecimal travelKmRate;

    public String toneOfVoice;
    public String detailLevel;
    public String agbNotes;
    public String paymentTerms;
}
