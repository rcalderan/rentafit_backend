package br.com.rentafit.sector.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sector.domain.Sector;
import br.com.rentafit.sector.dto.CreateSectorRequestDTO;
import br.com.rentafit.sector.dto.SectorDTO;
import br.com.rentafit.sector.dto.UpdateSectorRequestDTO;
import br.com.rentafit.sector.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CRUD de setores de atendimento/retirada vinculados a Matrizes/Filiais (CNPJ).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SectorService {

    private final SectorRepository sectorRepository;

    @Transactional(readOnly = true)
    public List<SectorDTO> listAll(Boolean activeOnly) {
        List<Sector> sectors = Boolean.TRUE.equals(activeOnly)
                ? sectorRepository.findByActiveTrueOrderByIssuerCnpjAscNameAsc()
                : sectorRepository.findAllByOrderByIssuerCnpjAscNameAsc();
        return sectors.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Sector getEntity(UUID id) {
        return sectorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Sector", id));
    }

    @Transactional
    public SectorDTO create(CreateSectorRequestDTO dto) {
        assertUnique(dto.issuerCnpj(), dto.name(), dto.code());
        Sector sector = Sector.builder()
                .issuerCnpj(dto.issuerCnpj())
                .branchName(dto.branchName())
                .name(dto.name())
                .code(normalizeCode(dto.code()))
                .active(true)
                .isDefault(Boolean.TRUE.equals(dto.isDefault()))
                .build();
        clearDefaultIfReplacing(sector.getIssuerCnpj(), sector.getIsDefault(), null);
        Sector saved = sectorRepository.save(sector);
        log.info("Setor criado: {} ({}) para estabelecimento {}", saved.getName(), saved.getCode(), saved.getIssuerCnpj());
        return toDTO(saved);
    }

    @Transactional
    public SectorDTO update(UUID id, UpdateSectorRequestDTO dto) {
        Sector sector = getEntity(id);

        if (dto.name() != null && !dto.name().isBlank()
                && !dto.name().equalsIgnoreCase(sector.getName())) {
            if (sectorRepository.existsByIssuerCnpjAndNameIgnoreCase(sector.getIssuerCnpj(), dto.name())) {
                throw new ValidationException(
                        "Já existe setor chamado '" + dto.name() + "' no estabelecimento " + sector.getIssuerCnpj());
            }
            sector.setName(dto.name());
        }
        if (dto.code() != null && !dto.code().isBlank()) {
            String newCode = normalizeCode(dto.code());
            if (!newCode.equalsIgnoreCase(sector.getCode())
                    && sectorRepository.existsByIssuerCnpjAndCodeIgnoreCase(sector.getIssuerCnpj(), newCode)) {
                throw new ValidationException(
                        "Já existe setor com sigla '" + newCode + "' no estabelecimento " + sector.getIssuerCnpj());
            }
            sector.setCode(newCode);
        }
        if (dto.branchName() != null) sector.setBranchName(dto.branchName());
        if (dto.active() != null) sector.setActive(dto.active());

        Sector saved = sectorRepository.save(sector);
        log.info("Setor atualizado: {} ({}) — active={}", saved.getName(), saved.getId(), saved.getActive());
        return toDTO(saved);
    }

    /**
     * Marca o setor como padrão do estabelecimento, removendo o flag dos demais.
     */
    @Transactional
    public SectorDTO setDefault(UUID id) {
        Sector sector = getEntity(id);
        clearDefaultIfReplacing(sector.getIssuerCnpj(), true, sector.getId());
        sector.setIsDefault(true);
        Sector saved = sectorRepository.save(sector);
        log.info("Setor {} marcado como padrão do estabelecimento {}", saved.getId(), saved.getIssuerCnpj());
        return toDTO(saved);
    }

    private void clearDefaultIfReplacing(String issuerCnpj, boolean willBeDefault, UUID keepId) {
        if (!willBeDefault) return;
        sectorRepository.findFirstByIssuerCnpjAndIsDefaultTrue(issuerCnpj)
                .filter(prev -> !prev.getId().equals(keepId))
                .ifPresent(prev -> {
                    prev.setIsDefault(false);
                    sectorRepository.save(prev);
                });
    }

    private void assertUnique(String issuerCnpj, String name, String code) {
        if (sectorRepository.existsByIssuerCnpjAndNameIgnoreCase(issuerCnpj, name)) {
            throw new ValidationException(
                    "Já existe setor chamado '" + name + "' no estabelecimento " + issuerCnpj);
        }
        if (sectorRepository.existsByIssuerCnpjAndCodeIgnoreCase(issuerCnpj, normalizeCode(code))) {
            throw new ValidationException(
                    "Já existe setor com sigla '" + normalizeCode(code) + "' no estabelecimento " + issuerCnpj);
        }
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public SectorDTO toDTO(Sector sector) {
        return SectorDTO.builder()
                .id(sector.getId())
                .issuerCnpj(sector.getIssuerCnpj())
                .branchName(sector.getBranchName())
                .name(sector.getName())
                .code(sector.getCode())
                .active(sector.getActive())
                .isDefault(sector.getIsDefault())
                .createdAt(sector.getCreatedAt())
                .updatedAt(sector.getUpdatedAt())
                .build();
    }
}
