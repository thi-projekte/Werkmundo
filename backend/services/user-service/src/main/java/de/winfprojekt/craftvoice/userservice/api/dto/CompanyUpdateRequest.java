package de.winfprojekt.craftvoice.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyUpdateRequest {

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
}
