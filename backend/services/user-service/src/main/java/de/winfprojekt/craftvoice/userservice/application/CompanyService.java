package de.winfprojekt.craftvoice.userservice.application;

import de.winfprojekt.craftvoice.userservice.UserEntity;
import de.winfprojekt.craftvoice.userservice.application.port.AuditLogger;
import de.winfprojekt.craftvoice.userservice.api.dto.CompanyUpdateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.TravelConfigDto;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class CompanyService {

    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Inject
    public CompanyService(UserRepository userRepository, AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public void updateCompanyData(Long userId, CompanyUpdateRequest data) {
        UserEntity user = userRepository.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (data.companyName != null) user.companyName = data.companyName;
        if (data.vatId != null) user.vatId = data.vatId;
        if (data.tradeRegisterNumber != null) user.tradeRegisterNumber = data.tradeRegisterNumber;

        if (data.street != null) user.street = data.street;
        if (data.houseNumber != null) user.houseNumber = data.houseNumber;
        if (data.zipCode != null) user.zipCode = data.zipCode;
        if (data.city != null) user.city = data.city;
        if (data.state != null) user.state = data.state;
        if (data.country != null) user.country = data.country;

        if (data.companyEmail != null) user.companyEmail = data.companyEmail;
        if (data.companyPhoneNumber != null) user.companyPhoneNumber = data.companyPhoneNumber;
        if (data.website != null) user.website = data.website;
        if (data.industry != null) user.industry = data.industry;

        if (data.iban != null) user.iban = data.iban;
        if (data.bic != null) user.bic = data.bic;
        if (data.bankName != null) user.bankName = data.bankName;
        if (data.accountHolder != null) user.accountHolder = data.accountHolder;

        if (data.taxNumber != null) user.taxNumber = data.taxNumber;
        if (data.legalForm != null) user.legalForm = data.legalForm;

        if (data.employeeCount != null) user.employeeCount = data.employeeCount;
        if (data.customerCount != null) user.customerCount = data.customerCount;
        if (data.hourlyRate != null) user.hourlyRate = data.hourlyRate;
        if (data.priceListUrl != null) user.priceListUrl = data.priceListUrl;

        if (data.travelModel != null) user.travelModel = data.travelModel;
        if (data.travelFlatRate != null) user.travelFlatRate = data.travelFlatRate;
        if (data.travelKmRate != null) user.travelKmRate = data.travelKmRate;

        auditLogger.log(
                user.id,
                "COMPANY_DATA_UPDATE",
                "User updated company metadata"
        );
    }

    @Transactional
    public void updateTravelConfig(Long userId, TravelConfigDto data) {
        UserEntity user = userRepository.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (data.isModellProvided()) {
            user.travelModel = data.getModell();
        }
        if (data.getPauschale() != null) {
            user.travelFlatRate = data.getPauschale();
        }
        if (data.getKmSatz() != null) {
            user.travelKmRate = data.getKmSatz();
        }

        TravelConfigDto.AddressDto address = data.getAdresse();
        if (address != null) {
            if (address.isStrasseProvided()) user.street = address.getStrasse();
            if (address.isHausnummerProvided()) user.houseNumber = address.getHausnummer();
            if (address.isPlzProvided()) user.zipCode = address.getPlz();
            if (address.isOrtProvided()) user.city = address.getOrt();
            if (address.isBundeslandProvided()) user.state = address.getBundesland();
            if (address.isLandProvided()) user.country = address.getLand();
        }

        auditLogger.log(
                user.id,
                "TRAVEL_CONFIG_UPDATE",
                "User updated travel configuration and address details"
        );
    }
}
