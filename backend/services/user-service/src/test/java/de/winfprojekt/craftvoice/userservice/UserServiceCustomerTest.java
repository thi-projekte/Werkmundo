package de.winfprojekt.craftvoice.userservice;

import de.winfprojekt.craftvoice.userservice.api.dto.CustomerCreateRequest;
import de.winfprojekt.craftvoice.userservice.api.dto.CustomerUpdateRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@QuarkusTest
class UserServiceCustomerTest {

    @Inject
    UserService userService;

    @InjectMock
    CurrentUserService currentUserService;

    @Test
    @TestTransaction
    void customerCreationAlwaysAssignsCustomerRoleAndAuthenticatedOwner() {
        UserEntity owner = new UserEntity();
        owner.id = 8123L;
        when(currentUserService.syncUserWithDatabase()).thenReturn(owner);

        CustomerCreateRequest request = new CustomerCreateRequest();
        request.email = "customer-security-test@example.com";
        request.firstName = "Test";
        UserEntity created = userService.createCustomer(request);

        assertEquals(UserStatus.ACTIVE, created.status);
        assertEquals(1, created.roles.size());
        assertEquals(UserRole.CUSTOMER, created.roles.iterator().next());
        assertEquals(8123L, created.ownerId);
        assertNull(created.keycloakId);
    }

    @Test
    @TestTransaction
    void foreignCustomerCannotBeReadUpdatedOrDeleted() {
        UserEntity customer = new UserEntity();
        customer.email = "foreign-customer-security-test@example.com";
        customer.roles.add(UserRole.CUSTOMER);
        customer.ownerId = 111L;
        customer.persist();

        UserEntity otherOwner = new UserEntity();
        otherOwner.id = 222L;
        when(currentUserService.syncUserWithDatabase()).thenReturn(otherOwner);

        assertThrows(NotFoundException.class, () -> userService.getCustomerById(customer.id));
        assertThrows(
                NotFoundException.class,
                () -> userService.updateCustomer(customer.id, new CustomerUpdateRequest())
        );
        assertThrows(NotFoundException.class, () -> userService.deleteCustomer(customer.id));
    }
}
