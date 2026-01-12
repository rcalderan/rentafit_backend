package br.com.rentafit.migration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário para mapear ObjectIds do MongoDB para valores numéricos legacyId
 * e para rastrear mapeamento de UUIDs gerados
 */
@Component
public class LegacyIdMapper {

    private static final Logger log = LoggerFactory.getLogger(LegacyIdMapper.class);

    // Cache de mapeamento ObjectId (MongoDB) -> Integer (legacy_id)
    private final Map<String, Integer> objectIdToLegacyIdMap = new HashMap<>();

    /**
     * Extrai um ID numérico a partir de um ObjectId do MongoDB
     *
     * Estratégia:
     * 1. Tenta extrair os últimos 6 caracteres como valor hexadecimal
     * 2. Se falhar, usa hash do ObjectId completo
     *
     * @param mongoObjectId String com o ObjectId (ex: "507f1f77bcf86cd799439011")
     * @return Integer entre 0-999999 para garantir compatibilidade
     */
    public Integer extractLegacyId(String mongoObjectId) {
        if (mongoObjectId == null || mongoObjectId.trim().isEmpty()) {
            log.warn("Null or empty ObjectId provided");
            return null;
        }

        // Verificar se já foi mapeado
        if (objectIdToLegacyIdMap.containsKey(mongoObjectId)) {
            return objectIdToLegacyIdMap.get(mongoObjectId);
        }

        Integer legacyId = null;

        try {
            // Estratégia 1: Extrair últimos 6 caracteres (3 bytes em hex)
            if (mongoObjectId.length() >= 6) {
                String numericPart = mongoObjectId.substring(mongoObjectId.length() - 6);
                legacyId = Integer.parseInt(numericPart, 16);

                // Garantir que está no range 0-999999
                legacyId = Math.abs(legacyId % 1000000);
            }
        } catch (NumberFormatException e) {
            log.debug("Could not parse as hex, using hash fallback for ObjectId: {}", mongoObjectId);
        }

        // Fallback: Usar hash do ObjectId completo
        if (legacyId == null) {
            legacyId = Math.abs(mongoObjectId.hashCode() % 1000000);
        }

        // Armazenar no cache
        objectIdToLegacyIdMap.put(mongoObjectId, legacyId);

        return legacyId;
    }

    /**
     * Retorna o mapeamento completo de ObjectId → legacy_id
     * Útil para relatórios e auditoria
     */
    public Map<String, Integer> getMappingCache() {
        return new HashMap<>(objectIdToLegacyIdMap);
    }

    /**
     * Limpa o cache de mapeamento
     */
    public void clearCache() {
        objectIdToLegacyIdMap.clear();
        log.info("Legacy ID mapping cache cleared");
    }
}

