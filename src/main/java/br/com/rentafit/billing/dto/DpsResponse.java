package br.com.rentafit.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DpsResponse {
    private String protocol;
    private String status;
    private OffsetDateTime dhProcessamento;
    private String accessKey;
}
