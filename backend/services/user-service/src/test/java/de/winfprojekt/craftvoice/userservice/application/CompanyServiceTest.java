package de.winfprojekt.craftvoice.userservice.application;

import de.winfprojekt.craftvoice.userservice.UserEntity;
import de.winfprojekt.craftvoice.userservice.application.port.AuditLogger;
import de.winfprojekt.craftvoice.userservice.api.dto.CompanyUpdateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.TravelConfigDto;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class CompanyServiceTest {

    @Test
    @TestTransaction
    void updatesCompanyFieldsOfTheRepositoryUserAndLeavesNullFieldsUnchanged() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        UserEntity storedUser = new UserEntity();
        storedUser.id = 17L;
        storedUser.companyName = "Old name";
        storedUser.city = "Berlin";
        when(userRepository.findById(17L)).thenReturn(storedUser);
        CompanyService service = new CompanyService(userRepository, auditLogger);

        CompanyUpdateRequest data = new CompanyUpdateRequest();
        data.companyName = "New name";
        data.vatId = "DE123";
        data.hourlyRate = 85.0;
        data.travelFlatRate = new BigDecimal("12.50");

        service.updateCompanyData(17L, data);

        assertEquals(17L, storedUser.id);
        assertEquals("New name", storedUser.companyName);
        assertEquals("DE123", storedUser.vatId);
        assertEquals("Berlin", storedUser.city);
        assertEquals(85.0, storedUser.hourlyRate);
        assertEquals(new BigDecimal("12.50"), storedUser.travelFlatRate);
        verify(userRepository).findById(17L);
        verify(auditLogger).log(17L, "COMPANY_DATA_UPDATE", "User updated company metadata");
    }

    @Test
    void rejectsAnUnknownUser() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(userRepository.findById(404L)).thenReturn(null);
        CompanyService service = new CompanyService(userRepository, auditLogger);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.updateCompanyData(404L, new CompanyUpdateRequest())
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(404L);
    }

    @Test
    @TestTransaction
    void updatesTravelConfigWithTypedAmountsAndNestedAddress() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        UserEntity storedUser = new UserEntity();
        storedUser.id = 17L;
        storedUser.street = "Old street";
        storedUser.travelKmRate = new BigDecimal("0.50");
        when(userRepository.findById(17L)).thenReturn(storedUser);
        CompanyService service = new CompanyService(userRepository, auditLogger);

        TravelConfigDto dto = new TravelConfigDto();
        dto.setModell("PAUSCHALE_PLUS_KM");
        dto.setPauschale(new BigDecimal("12.34"));
        dto.setKmSatz(new BigDecimal("0.75"));
        TravelConfigDto.AddressDto address = new TravelConfigDto.AddressDto();
        address.setStrasse("New street");
        address.setHausnummer("10a");
        address.setPlz("12345");
        address.setOrt("Berlin");
        address.setBundesland("Berlin");
        address.setLand("Deutschland");
        dto.setAdresse(address);

        service.updateTravelConfig(17L, dto);

        assertEquals("PAUSCHALE_PLUS_KM", storedUser.travelModel);
        assertEquals(new BigDecimal("12.34"), storedUser.travelFlatRate);
        assertEquals(new BigDecimal("0.75"), storedUser.travelKmRate);
        assertEquals("New street", storedUser.street);
        assertEquals("10a", storedUser.houseNumber);
        assertEquals("12345", storedUser.zipCode);
        assertEquals("Berlin", storedUser.city);
        assertEquals("Berlin", storedUser.state);
        assertEquals("Deutschland", storedUser.country);
        verify(userRepository).findById(17L);
        verify(auditLogger).log(
                17L,
                "TRAVEL_CONFIG_UPDATE",
                "User updated travel configuration and address details"
        );
    }

    @Test
    @TestTransaction
    void preservesOmittedAndNullMoneyValuesButAllowsExplicitNullAddressFields() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        UserEntity storedUser = new UserEntity();
        storedUser.id = 17L;
        storedUser.travelModel = "NUR_KM";
        storedUser.travelFlatRate = new BigDecimal("9.00");
        storedUser.street = "Old street";
        storedUser.city = "Berlin";
        when(userRepository.findById(17L)).thenReturn(storedUser);
        CompanyService service = new CompanyService(userRepository, auditLogger);

        TravelConfigDto dto = new TravelConfigDto();
        dto.setPauschale(null);
        TravelConfigDto.AddressDto address = new TravelConfigDto.AddressDto();
        address.setStrasse(null);
        dto.setAdresse(address);

        service.updateTravelConfig(17L, dto);

        assertEquals("NUR_KM", storedUser.travelModel);
        assertEquals(new BigDecimal("9.00"), storedUser.travelFlatRate);
        org.junit.jupiter.api.Assertions.assertNull(storedUser.street);
        assertEquals("Berlin", storedUser.city);
    }

    @Test
    void rejectsTravelConfigUpdateForAnUnknownUser() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(userRepository.findById(404L)).thenReturn(null);
        CompanyService service = new CompanyService(userRepository, auditLogger);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.updateTravelConfig(404L, new TravelConfigDto())
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(404L);
    }
}
