package de.werkmundo.offer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OfferPositionTest {

    @Test
    void constructorSetsPositionFields() {
        OfferPosition position = new OfferPosition(
                OfferPositionType.MATERIAL,
                "Wood",
                "Oak board",
                "Example Manufacturer",
                new BigDecimal("2.5"),
                "meter",
                "catalog-123",
                new BigDecimal("12.00"),
                2);

        assertEquals(OfferPositionType.MATERIAL, position.getType());
        assertEquals("Wood", position.getName());
        assertEquals("Oak board", position.getDescription());
        assertEquals("Example Manufacturer", position.getManufacturer());
        assertEquals(new BigDecimal("2.5"), position.getQuantity());
        assertEquals("meter", position.getUnit());
        assertEquals("catalog-123", position.getCatalogProductId());
        assertEquals(new BigDecimal("12.00"), position.getUnitPrice());
        assertEquals(new BigDecimal("30.000"), position.getLineTotal());
        assertEquals(2, position.getPositionOrder());
        assertNull(position.getId());
        assertNull(position.getOffer());
    }

    @Test
    void settersUpdatePositionFields() {
        OfferPosition position = new OfferPosition(
                OfferPositionType.OTHER, "Initial", null, null, BigDecimal.ONE, "item", null,
                BigDecimal.ZERO, 1);
        Offer offer = new Offer("business-123");

        position.setId(7L);
        position.setOffer(offer);
        position.setType(OfferPositionType.LABOR);
        position.setName("Updated");
        position.setDescription("Updated description");
        position.setManufacturer("Manufacturer");
        position.setQuantity(new BigDecimal("3"));
        position.setUnit("hour");
        position.setCatalogProductId("catalog-456");
        position.setUnitPrice(new BigDecimal("10.00"));
        position.setPositionOrder(4);

        assertEquals(7L, position.getId());
        assertEquals(offer, position.getOffer());
        assertEquals(OfferPositionType.LABOR, position.getType());
        assertEquals("Updated", position.getName());
        assertEquals("Updated description", position.getDescription());
        assertEquals("Manufacturer", position.getManufacturer());
        assertEquals(new BigDecimal("3"), position.getQuantity());
        assertEquals("hour", position.getUnit());
        assertEquals("catalog-456", position.getCatalogProductId());
        assertEquals(new BigDecimal("10.00"), position.getUnitPrice());
        assertEquals(new BigDecimal("30.00"), position.getLineTotal());
        assertEquals(4, position.getPositionOrder());
    }
}
