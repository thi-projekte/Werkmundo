package de.winfprojekt.craftvoice.userservice.api;

import de.winfprojekt.craftvoice.userservice.UserEntity;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerResponse;
import de.winfprojekt.craftvoice.userservice.api.dto.ProfileUpdateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.UserResponse;

import java.util.Set;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(UserEntity user) {
        UserResponse response = new UserResponse();
        response.id = user.id;
        response.email = user.email;
        response.firstName = user.firstName;
        response.lastName = user.lastName;
        response.phoneNumber = user.phoneNumber;
        response.profilePictureUrl = user.profilePictureUrl;
        response.status = user.status;
        response.roles = user.roles == null ? null : Set.copyOf(user.roles);

        response.companyName = user.companyName;
        response.vatId = user.vatId;
        response.tradeRegisterNumber = user.tradeRegisterNumber;

        response.street = user.street;
        response.houseNumber = user.houseNumber;
        response.zipCode = user.zipCode;
        response.city = user.city;
        response.state = user.state;
        response.country = user.country;

        response.companyEmail = user.companyEmail;
        response.companyPhoneNumber = user.companyPhoneNumber;
        response.website = user.website;
        response.industry = user.industry;

        response.iban = user.iban;
        response.bic = user.bic;
        response.bankName = user.bankName;
        response.accountHolder = user.accountHolder;

        response.taxNumber = user.taxNumber;
        response.legalForm = user.legalForm;

        response.employeeCount = user.employeeCount;
        response.customerCount = user.customerCount;
        response.hourlyRate = user.hourlyRate;
        response.priceListUrl = user.priceListUrl;

        response.travelModel = user.travelModel;
        response.travelFlatRate = user.travelFlatRate;
        response.travelKmRate = user.travelKmRate;

        response.toneOfVoice = user.toneOfVoice;
        response.detailLevel = user.detailLevel;
        response.agbNotes = user.agbNotes;
        response.paymentTerms = user.paymentTerms;
        return response;
    }

    public static UserEntity toProfileUpdateEntity(ProfileUpdateRequest request) {
        UserEntity update = new UserEntity();
        update.firstName = request.firstName;
        update.lastName = request.lastName;
        update.phoneNumber = request.phoneNumber;
        update.profilePictureUrl = request.profilePictureUrl;
        return update;
    }

    public static CustomerResponse toCustomerResponse(UserEntity customer) {
        CustomerResponse response = new CustomerResponse();
        response.id = customer.id;
        response.email = customer.email;
        response.firstName = customer.firstName;
        response.lastName = customer.lastName;
        response.phoneNumber = customer.phoneNumber;
        response.profilePictureUrl = customer.profilePictureUrl;
        response.companyName = customer.companyName;
        response.street = customer.street;
        response.houseNumber = customer.houseNumber;
        response.zipCode = customer.zipCode;
        response.city = customer.city;
        response.state = customer.state;
        response.country = customer.country;
        return response;
    }

}
