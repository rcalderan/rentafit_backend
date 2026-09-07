package br.com.rentafit.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationFileDTO {

    private String name;
    private long size;
    private String type;
    private String status;
    private String error;
}
