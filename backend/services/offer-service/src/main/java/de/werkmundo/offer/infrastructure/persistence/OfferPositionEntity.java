package de.werkmundo.offer.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "offer_positions")
public class OfferPositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    public OfferEntity offer;

    @Column(name = "type", length = 20)
    public String type;

    @Column(name = "name", columnDefinition = "TEXT")
    public String name;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "manufacturer", columnDefinition = "TEXT")
    public String manufacturer;

    @Column(name = "quantity", nullable = false,
            precision = 19, scale = 4)
    public BigDecimal quantity;

    @Column(name = "unit", length = 60)
    public String unit;

    @Column(name = "catalog_product_id")
    public String catalogProductId;

    @Column(name = "unit_price", nullable = false,
            precision = 19, scale = 4)
    public BigDecimal unitPrice;

    @Column(name = "position_order", nullable = false)
    public int positionOrder;
}
