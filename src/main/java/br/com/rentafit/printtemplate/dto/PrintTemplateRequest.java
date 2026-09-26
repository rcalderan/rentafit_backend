package br.com.rentafit.printtemplate.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PrintTemplateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotBlank @Pattern(regexp = "RENTAL_CONTRACT|RENTAL_CANCELLATION|FISCAL_RECEIPT_80MM|FISCAL_RECEIPT_58MM|CUSTOM") String templateType,
        @NotBlank @Pattern(regexp = "A4|THERMAL_80MM|THERMAL_58MM|CUSTOM") String pageFormat,
        @NotBlank @Pattern(regexp = "PORTRAIT|LANDSCAPE") String orientation,
        @NotNull @DecimalMin("1") @DecimalMax("1000") BigDecimal pageWidthMm,
        @DecimalMin("1") @DecimalMax("2000") BigDecimal pageHeightMm,
        @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal marginTopMm,
        @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal marginBottomMm,
        @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal marginLeftMm,
        @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal marginRightMm,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal printOffsetMm,
        JsonNode contentJson,
        @NotNull @Size(max = 2_000_000) String contentHtml,
        @Size(max = 100_000) String cssStyles,
        boolean isDefault,
        boolean isActive
) {
}
