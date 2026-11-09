package de.winfprojekt.craftvoice.userservice;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.ws.rs.ForbiddenException;
import de.winfprojekt.craftvoice.userservice.application.CompanyService;
import de.winfprojekt.craftvoice.userservice.application.ProfileService;
import de.winfprojekt.craftvoice.userservice.application.RegistrationService;
import de.winfprojekt.craftvoice.userservice.api.dto.CompanyUpdateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerCreateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerUpdateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.TravelConfigDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Mock-Tests für den UserResource-Controller.
 * Diese Tests benötigen KEINE Datenbank und KEIN Keycloak.
 */
@QuarkusTest
public class UserResourceTest {

    @InjectMock
    UserService userService;

    @InjectMock
    RegistrationService registrationService;

    @InjectMock
    ProfileService profileService;

    @InjectMock
    CompanyService companyService;

    @InjectMock
    CurrentUserService currentUserService;

    @Test
    public void testRegisterEndpoint() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"test@example.com\",\"password\":\"password123\","
                        + "\"firstName\":\"Max\",\"lastName\":\"Mustermann\","
                        + "\"id\":99,\"keycloakId\":\"attacker-id\","
                        + "\"status\":\"ACTIVE\",\"roles\":[\"OWNER\",\"ACCOUNTANT\"],"
                        + "\"ownerId\":123}")
                .when().post("/api/users/register")
                .then()
                .statusCode(201);

        Mockito.verify(registrationService).register(
                Mockito.argThat(user -> user.id == null
                        && user.keycloakId == null
                        && user.ownerId == null
                        && user.status == UserStatus.PENDING
                        && user.roles.isEmpty()
                        && "test@example.com".equals(user.email)
                        && "Max".equals(user.firstName)
                        && "Mustermann".equals(user.lastName)),
                Mockito.eq("password123")
        );
    }

    @Test
    public void testRegisterRejectsMissingRequiredFields() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"test@example.com\",\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}")
                .when().post("/api/users/register")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(registrationService);
    }

    @Test
    public void testRegisterRejectsMalformedEmail() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"not-an-email\",\"password\":\"password123\","
                        + "\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}")
                .when().post("/api/users/register")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(registrationService);
    }

    @Test
    public void testPasswordResetRequiresValidEmailAndKeepsExistingSuccessResponse() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"user@example.com\"}")
                .when().post("/api/users/password-reset/initiate")
                .then()
                .statusCode(200)
                .body(is("If the email exists, a reset instruction has been sent via Keycloak"));

        Mockito.verify(registrationService).initiatePasswordReset("user@example.com");
    }

    @Test
    public void testPasswordResetRejectsMissingOrMalformedEmail() {
        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/users/password-reset/initiate")
                .then()
                .statusCode(400);

        given()
                .contentType("application/json")
                .body("{\"email\":\"not-an-email\"}")
                .when().post("/api/users/password-reset/initiate")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(registrationService);
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testMeEndpointWithMockedUser() {
        UserEntity mockUser = new UserEntity();
        mockUser.id = 17L;
        mockUser.email = "test@example.com";
        mockUser.firstName = "Mock";
        mockUser.lastName = "User";
        mockUser.status = UserStatus.ACTIVE;
        mockUser.roles = Set.of(UserRole.OWNER);
        mockUser.iban = "DE123";
        mockUser.keycloakId = "internal-keycloak-id";
        mockUser.ownerId = 99L;
        mockUser.createdAt = java.time.LocalDateTime.now();
        mockUser.updatedAt = mockUser.createdAt;

        Mockito.when(currentUserService.syncUserWithDatabase()).thenReturn(mockUser);

        String response = given()
                .when().get("/api/users/me")
                .then()
                .statusCode(200)
                .body("email", is("test@example.com"))
                .body("id", is(17))
                .body("roles[0]", is("OWNER"))
                .body("iban", is("DE123"))
                .extract().asString();

        assertFalse(response.contains("\"keycloakId\""));
        assertFalse(response.contains("\"ownerId\""));
        assertFalse(response.contains("\"createdAt\""));
        assertFalse(response.contains("\"updatedAt\""));
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testUpdateProfileUsesAuthenticatedUserId() {
        UserEntity currentUser = new UserEntity();
        currentUser.id = 17L;
        UserEntity updatedUser = new UserEntity();
        updatedUser.id = 17L;
        updatedUser.firstName = "Ada";
        updatedUser.keycloakId = "internal-keycloak-id";
        updatedUser.ownerId = 99L;

        Mockito.when(currentUserService.syncUserWithDatabase()).thenReturn(currentUser);
        Mockito.when(profileService.updateProfile(Mockito.eq(17L), Mockito.any())).thenReturn(updatedUser);

        String response = given()
                .contentType("application/json")
                .body("{\"id\":99,\"keycloakId\":\"attacker-keycloak-id\","
                        + "\"ownerId\":999,\"roles\":[\"OWNER\"],"
                        + "\"firstName\":\"Ada\",\"phoneNumber\":\"123\"}")
                .when().put("/api/users/profile")
                .then()
                .statusCode(200)
                .body("firstName", is("Ada"))
                .extract().asString();

        assertFalse(response.contains("\"keycloakId\""));
        assertFalse(response.contains("\"ownerId\""));
        Mockito.verify(profileService).updateProfile(
                Mockito.eq(17L),
                Mockito.argThat(data -> data.id == null
                        && data.keycloakId == null
                        && data.ownerId == null
                        && data.roles.isEmpty()
                        && "Ada".equals(data.firstName)
                        && "123".equals(data.phoneNumber))
        );
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testUploadProfilePictureUsesAuthenticatedUserId() {
        UserEntity currentUser = new UserEntity();
        currentUser.id = 17L;
        Mockito.when(currentUserService.syncUserWithDatabase()).thenReturn(currentUser);
        Mockito.when(profileService.uploadProfilePicture(Mockito.eq(17L), Mockito.any()))
                .thenReturn("/api/users/profile-picture/profile_17_test.png");

        given()
                .multiPart("file", "avatar.png", "image-content".getBytes(), "image/png")
                .when().post("/api/users/profile-picture")
                .then()
                .statusCode(200)
                .body("url", is("/api/users/profile-picture/profile_17_test.png"));

        Mockito.verify(profileService).uploadProfilePicture(Mockito.eq(17L), Mockito.any());
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testUpdateCompanyUsesAuthenticatedUserId() {
        UserEntity currentUser = new UserEntity();
        currentUser.id = 17L;

        Mockito.when(currentUserService.syncUserWithDatabase()).thenReturn(currentUser);

        given()
                .contentType("application/json")
                .body("{\"id\":99,\"keycloakId\":\"attacker-keycloak-id\","
                        + "\"ownerId\":999,\"status\":\"DELETED\","
                        + "\"roles\":[\"OWNER\"],\"companyName\":\"Example GmbH\","
                        + "\"iban\":\"DE123\",\"taxNumber\":\"123/456\"}")
                .when().put("/api/users/company")
                .then()
                .statusCode(200);

        Mockito.verify(companyService).updateCompanyData(
                Mockito.eq(17L),
                Mockito.argThat(data -> "Example GmbH".equals(data.companyName)
                        && "DE123".equals(data.iban)
                        && "123/456".equals(data.taxNumber))
        );
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"CUSTOMER"})
    public void testUpdateCompanyForbiddenForNonOwner() {
        given()
                .contentType("application/json")
                .body("{\"companyName\":\"Not allowed\"}")
                .when().put("/api/users/company")
                .then()
                .statusCode(403);

        Mockito.verifyNoInteractions(companyService);
    }

    @Test
    @TestSecurity(user = "pendingUser", roles = {"OWNER"})
    public void testPendingOwnerCannotUseOwnerEndpoint() {
        Mockito.when(currentUserService.syncUserWithDatabase())
                .thenThrow(new ForbiddenException("Email address has not been verified"));

        given()
                .contentType("application/json")
                .body("{\"companyName\":\"Not allowed\"}")
                .when().put("/api/users/company")
                .then()
                .statusCode(403);

        Mockito.verifyNoInteractions(companyService);
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testUpdateTravelConfigUsesAuthenticatedUserAndCompatibleJsonFields() {
        UserEntity currentUser = new UserEntity();
        currentUser.id = 17L;
        Mockito.when(currentUserService.syncUserWithDatabase()).thenReturn(currentUser);

        given()
                .contentType("application/json")
                .body("{\"modell\":\"PAUSCHALE_PLUS_KM\",\"pauschale\":12.34,\"kmSatz\":0.75,"
                        + "\"adresse\":{\"strasse\":\"Hauptstrasse\",\"hausnummer\":\"5\","
                        + "\"plz\":\"12345\",\"ort\":\"Berlin\",\"bundesland\":\"Berlin\","
                        + "\"land\":\"Deutschland\"},\"id\":99}")
                .when().put("/api/users/profile/travel-config-detailed")
                .then()
                .statusCode(200);

        Mockito.verify(companyService).updateTravelConfig(
                Mockito.eq(17L),
                Mockito.argThat(dto -> "PAUSCHALE_PLUS_KM".equals(dto.getModell())
                        && new java.math.BigDecimal("12.34").equals(dto.getPauschale())
                        && new java.math.BigDecimal("0.75").equals(dto.getKmSatz())
                        && "Hauptstrasse".equals(dto.getAdresse().getStrasse())
                        && "5".equals(dto.getAdresse().getHausnummer())
                        && "12345".equals(dto.getAdresse().getPlz())
                        && "Berlin".equals(dto.getAdresse().getOrt())
                        && "Berlin".equals(dto.getAdresse().getBundesland())
                        && "Deutschland".equals(dto.getAdresse().getLand()))
        );
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testUpdateTravelConfigRejectsInvalidMoneyValue() {
        given()
                .contentType("application/json")
                .body("{\"pauschale\":\"not-a-number\"}")
                .when().put("/api/users/profile/travel-config-detailed")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(companyService);
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"CUSTOMER"})
    public void testUpdateTravelConfigForbiddenForCustomer() {
        given()
                .contentType("application/json")
                .body("{\"modell\":\"NUR_KM\"}")
                .when().put("/api/users/profile/travel-config-detailed")
                .then()
                .statusCode(403);

        Mockito.verifyNoInteractions(companyService);
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testCreateCustomer() {
        UserEntity customer = new UserEntity();
        customer.id = 23L;
        customer.email = "customer@example.com";
        customer.firstName = "Kunde";
        customer.companyName = "Kunden GmbH";
        customer.roles.add(UserRole.CUSTOMER);
        customer.status = UserStatus.ACTIVE;
        customer.keycloakId = "internal-keycloak-id";
        customer.ownerId = 99L;
        customer.iban = "sensitive-iban";

        Mockito.when(userService.createCustomer(Mockito.any(CustomerCreateRequest.class)))
                .thenReturn(customer);

        given()
                .contentType("application/json")
                .body("{\"email\":\"customer@example.com\",\"firstName\":\"Kunde\","
                        + "\"companyName\":\"Kunden GmbH\",\"street\":\"Musterstrasse\","
                        + "\"id\":99,\"keycloakId\":\"attacker-id\",\"status\":\"ACTIVE\","
                        + "\"roles\":[\"OWNER\"],\"ownerId\":123,\"iban\":\"sensitive\"}")
                .when().post("/api/users/customers")
                .then()
                .statusCode(201)
                .body("id", is(23))
                .body("email", is("customer@example.com"))
                .body("companyName", is("Kunden GmbH"));

        Mockito.verify(userService).createCustomer(Mockito.argThat(request ->
                "customer@example.com".equals(request.email)
                        && "Kunde".equals(request.firstName)
                        && "Kunden GmbH".equals(request.companyName)
                        && "Musterstrasse".equals(request.street)
        ));
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testCustomerResponsesDoNotExposeInternalOrSensitiveFields() {
        UserEntity customer = new UserEntity();
        customer.id = 23L;
        customer.email = "customer@example.com";
        customer.firstName = "Kunde";
        customer.companyName = "Kunden GmbH";
        customer.roles.add(UserRole.CUSTOMER);
        customer.status = UserStatus.ACTIVE;
        customer.keycloakId = "internal-keycloak-id";
        customer.ownerId = 99L;
        customer.iban = "sensitive-iban";
        customer.taxNumber = "sensitive-tax-number";

        Mockito.when(userService.listCustomers()).thenReturn(java.util.List.of(customer));
        Mockito.when(userService.getCustomerById(23L)).thenReturn(customer);

        String listResponse = given()
                .when().get("/api/users/customers")
                .then()
                .statusCode(200)
                .body("[0].id", is(23))
                .body("[0].email", is("customer@example.com"))
                .extract().asString();
        String detailResponse = given()
                .when().get("/api/users/customers/23")
                .then()
                .statusCode(200)
                .body("companyName", is("Kunden GmbH"))
                .extract().asString();

        for (String response : java.util.List.of(listResponse, detailResponse)) {
            assertFalse(response.contains("\"keycloakId\""));
            assertFalse(response.contains("\"ownerId\""));
            assertFalse(response.contains("\"roles\""));
            assertFalse(response.contains("\"status\""));
            assertFalse(response.contains("\"iban\""));
            assertFalse(response.contains("\"taxNumber\""));
        }
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testUpdateCustomerIgnoresInternalFieldsAndReturnsSafeResponse() {
        UserEntity updated = new UserEntity();
        updated.id = 23L;
        updated.email = "customer@example.com";
        updated.firstName = "Ada";
        updated.roles.add(UserRole.CUSTOMER);
        updated.status = UserStatus.ACTIVE;
        updated.keycloakId = "internal-keycloak-id";
        updated.ownerId = 99L;
        updated.iban = "sensitive-iban";
        Mockito.when(userService.updateCustomer(
                Mockito.eq(23L),
                Mockito.any(CustomerUpdateRequest.class)
        )).thenReturn(updated);

        String response = given()
                .contentType("application/json")
                .body("{\"firstName\":\"Ada\",\"id\":88,\"keycloakId\":\"attacker-id\","
                        + "\"ownerId\":999,\"status\":\"DELETED\","
                        + "\"roles\":[\"OWNER\"],\"iban\":\"attacker-iban\"}")
                .when().put("/api/users/customers/23")
                .then()
                .statusCode(200)
                .body("firstName", is("Ada"))
                .extract().asString();

        assertFalse(response.contains("\"keycloakId\""));
        assertFalse(response.contains("\"ownerId\""));
        assertFalse(response.contains("\"roles\""));
        assertFalse(response.contains("\"status\""));
        assertFalse(response.contains("\"iban\""));
        Mockito.verify(userService).updateCustomer(
                Mockito.eq(23L),
                Mockito.argThat(request -> "Ada".equals(request.firstName)
                        && request.email == null
                        && request.companyName == null)
        );
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testCreateCustomerRequiresValidEmail() {
        given()
                .contentType("application/json")
                .body("{\"firstName\":\"Kunde\"}")
                .when().post("/api/users/customers")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(userService);
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"OWNER"})
    public void testUpdateCustomerRejectsMalformedEmail() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"not-an-email\"}")
                .when().put("/api/users/customers/23")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(userService);
    }

    @Test
    @TestSecurity(user = "testUser", roles = {"CUSTOMER"})
    public void testCreateCustomerForbiddenForCustomerRole() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"customer@example.com\"}")
                .when().post("/api/users/customers")
                .then()
                .statusCode(403);

        given()
                .when().get("/api/users/customers")
                .then()
                .statusCode(403);

        given()
                .when().get("/api/users/customers/23")
                .then()
                .statusCode(403);

        given()
                .contentType("application/json")
                .body("{\"firstName\":\"Not allowed\"}")
                .when().put("/api/users/customers/23")
                .then()
                .statusCode(403);

        given()
                .when().delete("/api/users/customers/23")
                .then()
                .statusCode(403);

        Mockito.verifyNoInteractions(userService);
    }
}
