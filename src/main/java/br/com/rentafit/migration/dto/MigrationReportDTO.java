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
public class MigrationReportDTO {

    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String status;
    private long customersMigrated;
    private long employeesMigrated;
    private long categoriesMigrated;
    private long rentalItemsMigrated;
    private long contractsMigrated;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
