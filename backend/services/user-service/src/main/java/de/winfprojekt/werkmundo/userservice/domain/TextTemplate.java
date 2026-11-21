package de.winfprojekt.werkmundo.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Standardtext (AGB, Zahlungsbedingungen, ...) eines Betriebs. Wird über {@link Company} verwaltet. */
@Entity
@Table(name = "text_templates")
public class TextTemplate extends AuditableEntity {

    static final int MAX_CONTENT_LENGTH = 20000;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 30, updatable = false)
    private TextTemplateType type;

    @Column(name = "content", nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    protected TextTemplate() {
        // für JPA
    }

    TextTemplate(Company company, TextTemplateType type, String content) {
        this.id = UUID.randomUUID();
        this.company = company;
        this.type = DomainRules.requireValue(type, "Texttyp");
        changeContent(content);
    }

    void changeContent(String content) {
        this.content = DomainRules.requireText(content, "Text", MAX_CONTENT_LENGTH);
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public TextTemplateType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
