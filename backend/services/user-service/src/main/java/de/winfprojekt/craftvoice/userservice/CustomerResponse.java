
package de.winfprojekt.craftvoice.userservice;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String street,
        String houseNumber,
        String zipCode,
        String city,
        String state,
        String country
) {

    public static CustomerResponse fromEntity(UserEntity customer) {
        return new CustomerResponse(
                customer.id,
                customer.firstName,
                customer.lastName,
                customer.email,
                customer.phoneNumber,
                customer.street,
                customer.houseNumber,
                customer.zipCode,
                customer.city,
                customer.state,
                customer.country
        );
    }
}
