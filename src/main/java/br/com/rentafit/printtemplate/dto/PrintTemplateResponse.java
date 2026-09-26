package br.com.rentafit.printtemplate.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PrintTemplateResponse(
        String id,
        String name,
        String description,
        String templateType,
        String pageFormat,
        String orientation,
        BigDecimal pageWidthMm,
        BigDecimal pageHeightMm,
        BigDecimal marginTopMm,
        BigDecimal marginBottomMm,
        BigDecimal marginLeftMm,
        BigDecimal marginRightMm,
        BigDecimal printOffsetMm,
        JsonNode contentJson,
        String contentHtml,
        String cssStyles,
        boolean isDefault,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
