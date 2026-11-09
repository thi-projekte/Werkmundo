package de.winfprojekt.craftvoice.userservice;

import io.quarkus.security.Authenticated;
import de.winfprojekt.craftvoice.userservice.application.CompanyService;
import de.winfprojekt.craftvoice.userservice.application.ProfileService;
import de.winfprojekt.craftvoice.userservice.application.RegistrationService;
import de.winfprojekt.craftvoice.userservice.api.UserMapper;
import de.winfprojekt.craftvoice.userservice.api.dto.CompanyUpdateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerCreateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerResponse;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerUpdateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.ProfileUpdateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.PasswordResetRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.RegistrationRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.TravelConfigDto;
import de.winfprojekt.craftvoice.userservice.api.dto.UserResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    RegistrationService registrationService;

    @Inject
    ProfileService profileService;

    @Inject
    CompanyService companyService;

    @Inject
    CurrentUserService currentUserService;

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid @NotNull RegistrationRequest request) {
        UserEntity user = new UserEntity();
        user.email = request.email;
        user.firstName = request.firstName;
        user.lastName = request.lastName;

        registrationService.register(user, request.password);

        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/me")
    @Authenticated
    public UserResponse me() {
        return UserMapper.toResponse(currentUserService.syncUserWithDatabase());
    }

    @GET
    @Path("/profile/hourly-rate")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response getHourlyRate() {
        UserEntity user = currentUserService.syncUserWithDatabase();

        Map<String, Object> response = new HashMap<>();
        response.put("stundensatz", user.hourlyRate != null ? user.hourlyRate : 0.0);

        return Response.ok(response).build();
    }

    @GET
    @Path("/profile/travel-config")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response getTravelConfig() {
        UserEntity user = currentUserService.syncUserWithDatabase();

        String formattedAddress = String.format(
                "%s %s, %s %s",
                user.street != null ? user.street : "",
                user.houseNumber != null ? user.houseNumber : "",
                user.zipCode != null ? user.zipCode : "",
                user.city != null ? user.city : ""
        ).trim();

        Map<String, Object> response = new HashMap<>();
        response.put("modell", user.travelModel != null ? user.travelModel : "PAUSCHALE");
        response.put("pauschale", user.travelFlatRate);
        response.put("kmSatz", user.travelKmRate);
        response.put("adresse", formattedAddress);

        return Response.ok(response).build();
    }

    @GET
    @Path("/profile/travel-config-detailed")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response getTravelConfigDetailed() {
        UserEntity user = currentUserService.syncUserWithDatabase();

        Map<String, Object> address = new HashMap<>();
        address.put("strasse", user.street);
        address.put("hausnummer", user.houseNumber);
        address.put("plz", user.zipCode);
        address.put("ort", user.city);
        address.put("bundesland", user.state);
        address.put("land", user.country);

        Map<String, Object> response = new HashMap<>();
        response.put("modell", user.travelModel != null ? user.travelModel : "PAUSCHALE");
        response.put("pauschale", user.travelFlatRate);
        response.put("kmSatz", user.travelKmRate);
        response.put("adresse", address);

        return Response.ok(response).build();
    }

    @PUT
    @Path("/profile/travel-config-detailed")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response updateTravelConfigDetailed(TravelConfigDto data) {
        UserEntity user = currentUserService.syncUserWithDatabase();
        companyService.updateTravelConfig(user.id, data);

        return Response.ok().build();
    }

    @POST
    @Path("/profile-picture")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    public Response uploadProfilePicture(@RestForm("file") FileUpload file) {
        String url = profileService.uploadProfilePicture(getUserId(), file);

        return Response.ok(Map.of("url", url)).build();
    }

    @PUT
    @Path("/profile")
    @Authenticated
    public UserResponse updateProfile(ProfileUpdateRequest data) {
        UserEntity updatedUser = profileService.updateProfile(
                getUserId(),
                UserMapper.toProfileUpdateEntity(data)
        );

        return UserMapper.toResponse(updatedUser);
    }

    @PUT
    @Path("/company")
    @RolesAllowed("OWNER")
    public Response updateCompany(CompanyUpdateRequest data) {
        companyService.updateCompanyData(getUserId(), data);

        return Response.ok().build();
    }

    @POST
    @Path("/password-reset/initiate")
    @PermitAll
    public Response initiateReset(@Valid @NotNull PasswordResetRequest request) {
        registrationService.initiatePasswordReset(request.email);

        return Response
                .ok("If the email exists, a reset instruction has been sent via Keycloak")
                .build();
    }

    @DELETE
    @RolesAllowed("OWNER")
    public Response deleteAccount() {
        registrationService.deleteAccount(getUserId());

        return Response.ok("Account deleted in Keycloak and anonymized locally").build();
    }

    @POST
    @Path("/customers")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response createCustomer(@Valid @NotNull CustomerCreateRequest request) {
        UserEntity created = userService.createCustomer(request);

        return Response.status(Response.Status.CREATED)
                .entity(UserMapper.toCustomerResponse(created))
                .build();
    }

    @GET
    @Path("/customers")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public List<CustomerResponse> listCustomers() {
        return userService.listCustomers().stream()
                .map(UserMapper::toCustomerResponse)
                .toList();
    }

    @GET
    @Path("/customers/{id}")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public CustomerResponse getCustomer(@PathParam("id") Long id) {
        return UserMapper.toCustomerResponse(userService.getCustomerById(id));
    }

    @PUT
    @Path("/customers/{id}")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public CustomerResponse updateCustomer(
            @PathParam("id") Long id,
            @Valid @NotNull CustomerUpdateRequest request
    ) {
        return UserMapper.toCustomerResponse(
                    userService.updateCustomer(id, request)
        );
    }

    @DELETE
    @Path("/customers/{id}")
    @RolesAllowed({"OWNER", "EMPLOYEE"})
    public Response deleteCustomer(@PathParam("id") Long id) {
        userService.deleteCustomer(id);
        return Response.ok().build();
    }

    private Long getUserId() {
        UserEntity user = currentUserService.syncUserWithDatabase();

        if (user == null) {
            throw new NotFoundException("User not synced");
        }

        return user.id;
    }

}