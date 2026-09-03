package br.com.rentafit.migration.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabasePromotionService {

    private static final Logger log = LoggerFactory.getLogger(DatabasePromotionService.class);

    private final DatabaseCloneService cloneService;

    public void promote() {
        if (!cloneService.dumpExists()) {
            throw new IllegalStateException("rentafit_dump não existe. Execute o clone primeiro.");
        }

        cloneService.backupOriginal();
        cloneService.promoteDumpToOriginal();
    }
}
