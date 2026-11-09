package de.winfprojekt.craftvoice.userservice;

import de.winfprojekt.craftvoice.userservice.infrastructure.AuditLogEntity;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerCreateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerUpdateRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class UserService {

    @Inject
    CurrentUserService currentUserService;

    @Transactional
    public UserEntity createCustomer(CustomerCreateRequest request) {
        if (UserEntity.findByEmail(request.email) != null) {
            throw new BadRequestException("User with this email already exists");
        }

        // Track which craftsman created this customer
        UserEntity owner = currentUserService.syncUserWithDatabase();
        if (owner == null) {
            throw new BadRequestException("Could not determine current user");
        }

        UserEntity customer = new UserEntity();
        customer.email = request.email;
        customer.firstName = request.firstName;
        customer.lastName = request.lastName;
        customer.phoneNumber = request.phoneNumber;
        customer.companyName = request.companyName;
        customer.street = request.street;
        customer.houseNumber = request.houseNumber;
        customer.zipCode = request.zipCode;
        customer.city = request.city;
        customer.state = request.state;
        customer.country = request.country;
        customer.status = UserStatus.ACTIVE;
        customer.roles.clear();
        customer.roles.add(UserRole.CUSTOMER);
        customer.ownerId = owner.id;
        customer.persist();

        AuditLogEntity.log(
                customer.id,
                "CUSTOMER_CREATED",
                "Customer profile created by craftsman " + owner.id
        );

        return customer;
    }

    @Transactional
    public UserEntity updateCustomer(Long id, CustomerUpdateRequest data) {
        UserEntity customer = UserEntity.findById(id);

        if (customer == null || !customer.roles.contains(UserRole.CUSTOMER)) {
            throw new NotFoundException("Customer not found");
        }

        // Verify ownership - only the craftsman who created the customer can update it
        UserEntity owner = currentUserService.syncUserWithDatabase();
        if (owner == null || !owner.id.equals(customer.ownerId)) {
            throw new NotFoundException("Customer not found");
        }

        // Allow updating common fields if provided
        if (data.firstName != null) customer.firstName = data.firstName;
        if (data.lastName != null) customer.lastName = data.lastName;
        if (data.phoneNumber != null) customer.phoneNumber = data.phoneNumber;
        if (data.companyName != null) customer.companyName = data.companyName;
        if (data.email != null) {
            // Prevent email collision with existing users
            UserEntity existing = UserEntity.findByEmail(data.email);
            if (existing != null && !existing.id.equals(id)) {
                throw new BadRequestException("Email already in use");
            }
            customer.email = data.email;
        }

        // Allow updating address fields if provided
        if (data.street != null) customer.street = data.street;
        if (data.houseNumber != null) customer.houseNumber = data.houseNumber;
        if (data.zipCode != null) customer.zipCode = data.zipCode;
        if (data.city != null) customer.city = data.city;
        if (data.state != null) customer.state = data.state;
        if (data.country != null) customer.country = data.country;
        if (data.state != null) customer.state = data.state;

        AuditLogEntity.log(
                customer.id,
                "CUSTOMER_UPDATED",
                "Customer data updated by craftsman " + owner.id
        );

        return customer;
    }

    @Transactional
    public void deleteCustomer(Long id) {
        UserEntity customer = UserEntity.findById(id);

        if (customer == null || !customer.roles.contains(UserRole.CUSTOMER)) {
            throw new NotFoundException("Customer not found");
        }

        // Verify ownership - only the craftsman who created the customer can delete it
        UserEntity owner = currentUserService.syncUserWithDatabase();
        if (owner == null || !owner.id.equals(customer.ownerId)) {
            throw new NotFoundException("Customer not found");
        }

        // Anonymize customer data similarly to deleting accounts
        customer.status = UserStatus.DELETED;
        customer.email = "deleted_customer_" + customer.id + "@handwerker.de";
        customer.keycloakId = null;
        customer.firstName = null;
        customer.lastName = null;
        customer.phoneNumber = null;
        customer.profilePictureUrl = null;

        AuditLogEntity.log(
                customer.id,
                "CUSTOMER_DELETED",
                "Customer deleted/anonymized by craftsman " + owner.id
        );
    }

    public List<UserEntity> listCustomers() {
        UserEntity owner = currentUserService.syncUserWithDatabase();
        if (owner == null) {
            throw new BadRequestException("Could not determine current user");
        }

        return UserEntity.list(
                "from UserEntity u join u.roles r where r = ?1 and u.ownerId = ?2",
                UserRole.CUSTOMER,
                owner.id
        );
    }

    public UserEntity getCustomerById(Long id) {
        UserEntity customer = UserEntity.findById(id);

        if (customer == null || !customer.roles.contains(UserRole.CUSTOMER)) {
            throw new NotFoundException("Customer not found");
        }

        // Verify ownership - only the craftsman who created the customer can view it
        UserEntity owner = currentUserService.syncUserWithDatabase();
        if (owner == null || !owner.id.equals(customer.ownerId)) {
            throw new NotFoundException("Customer not found");
        }

        return customer;
    }

}