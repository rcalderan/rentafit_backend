package br.com.rentafit.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSessionDTO {

    private String id;
    private OffsetDateTime createdAt;
    private String status;
    private List<MigrationFileDTO> files = new ArrayList<>();
    private MigrationReportDTO report;
}
