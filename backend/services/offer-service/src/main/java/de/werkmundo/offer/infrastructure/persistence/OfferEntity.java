package de.werkmundo.offer.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offers")
public class OfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "business_key", nullable = false, unique = true)
    public String businessKey;

    @Column(name = "customer_id")
    public String customerId;

    @Column(name = "craftsman_id")
    public String craftsmanId;

    @Column(name = "customer_access_token_hash")
    public String customerAccessTokenHash;

    @Column(name = "customer_access_token_expires_at")
    public LocalDateTime customerAccessTokenExpiresAt;

    @Column(name = "estimated_work_duration_hours", precision = 19, scale = 4)
    public BigDecimal estimatedWorkDurationHours;

    @Column(name = "status", nullable = false, length = 40)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionOrder ASC")
    public List<OfferPositionEntity> positions = new ArrayList<>();

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    public List<OfferStatusHistoryEntity> statusHistory = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "offer_correction_suggestions", joinColumns = @JoinColumn(name = "offer_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "suggestion", nullable = false, columnDefinition = "TEXT")
    public List<String> correctionSuggestions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "offer_customer_change_requests", joinColumns = @JoinColumn(name = "offer_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "change_request", nullable = false, columnDefinition = "TEXT")
    public List<String> customerChangeRequests = new ArrayList<>();
}
