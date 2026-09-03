package br.com.rentafit.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationComparisonDTO {

    private List<TableComparisonDTO> tables = new ArrayList<>();
    private boolean equal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableComparisonDTO {
        private String tableName;
        private long originalCount;
        private long dumpCount;
        private long difference;
    }
}
