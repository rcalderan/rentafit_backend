package br.com.rentafit.printtemplate.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "print_templates")
@Getter
@Setter
@NoArgsConstructor
public class PrintTemplate {

    @Id
    @Column(length = 100, nullable = false)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "template_type", nullable = false, length = 40)
    private String templateType;

    @Column(name = "page_format", nullable = false, length = 20)
    private String pageFormat;

    @Column(nullable = false, length = 10)
    private String orientation;

    @Column(name = "page_width_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal pageWidthMm;

    @Column(name = "page_height_mm", precision = 8, scale = 2)
    private BigDecimal pageHeightMm;

    @Column(name = "margin_top_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal marginTopMm;

    @Column(name = "margin_bottom_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal marginBottomMm;

    @Column(name = "margin_left_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal marginLeftMm;

    @Column(name = "margin_right_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal marginRightMm;

    @Column(name = "print_offset_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal printOffsetMm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "jsonb")
    private JsonNode contentJson;

    @Column(name = "content_html", nullable = false, columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "css_styles", columnDefinition = "TEXT")
    private String cssStyles;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
