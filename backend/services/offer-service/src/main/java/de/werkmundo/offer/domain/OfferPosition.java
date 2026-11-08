package de.werkmundo.offer.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class OfferPosition {
    private static final int PRICE_SCALE = 2;
    private Long id;
    private Offer offer;
    private OfferPositionType type;
    private String name;
    private String description;
    private String manufacturer;
    private BigDecimal quantity;
    private String unit;
    private String catalogProductId;
    private BigDecimal unitPrice;
    private int positionOrder;

    public OfferPosition(
            OfferPositionType type,
            String name,
            String description,
            String manufacturer,
            BigDecimal quantity,
            String unit,
            String catalogProductId,
            BigDecimal unitPrice,
            int positionOrder) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.manufacturer = manufacturer;
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.unit = unit;
        this.catalogProductId = catalogProductId;
        this.unitPrice = roundPrice(unitPrice);
        this.positionOrder = positionOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Offer getOffer() {
        return offer;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }

    public OfferPositionType getType() {
        return type;
    }

    public void setType(OfferPositionType type) {
        requireOfferEditable();
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        requireOfferEditable();
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        requireOfferEditable();
        this.description = description;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        requireOfferEditable();
        this.manufacturer = manufacturer;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        requireOfferEditable();
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        requireOfferEditable();
        this.unit = unit;
    }

    public String getCatalogProductId() {
        return catalogProductId;
    }

    public void setCatalogProductId(String catalogProductId) {
        requireOfferEditable();
        this.catalogProductId = catalogProductId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        requireOfferEditable();
        this.unitPrice = roundPrice(unitPrice);
    }

    public BigDecimal getLineTotal() {
        return quantity.multiply(unitPrice).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    public int getPositionOrder() {
        return positionOrder;
    }

    public void setPositionOrder(int positionOrder) {
        requireOfferEditable();
        this.positionOrder = positionOrder;
    }

    private void requireOfferEditable() {
        if (offer != null) {
            offer.requireEditable();
        }
    }

    private BigDecimal roundPrice(BigDecimal price) {
        return Objects.requireNonNull(price, "unitPrice must not be null")
                .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }
}
