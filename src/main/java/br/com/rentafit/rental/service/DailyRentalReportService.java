package br.com.rentafit.rental.service;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.dto.report.ClothingTypeGroupDTO;
import br.com.rentafit.rental.dto.report.DailyRentalReportDTO;
import br.com.rentafit.rental.dto.report.ReportAdjustmentDTO;
import br.com.rentafit.rental.dto.report.ReportItemDTO;
import br.com.rentafit.rental.port.RentalItemPort;
import br.com.rentafit.rental.port.RentalItemPort.RentalItemSnapshot;
import br.com.rentafit.rental.repository.RentalContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Monta o relatório diário de locação (checklist de preparação) agrupando os itens
 * dos contratos de uma data de evento por tipo de roupa (categoria do item).
 *
 * <p>O tipo de roupa é resolvido via {@link RentalItemPort} em lote (batch) para evitar N+1.
 * Itens sem {@code rentalItemId} ou cuja categoria não foi resolvida caem no grupo
 * {@value #UNCATEGORIZED}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DailyRentalReportService {

    static final String UNCATEGORIZED = "Sem categoria";
    static final List<ContractStatus> DEFAULT_STATUSES =
            List.of(ContractStatus.SIGNED, ContractStatus.FINALIZED);

    private final RentalContractRepository contractRepository;
    private final RentalItemPort rentalItemPort;

    /**
     * Gera o relatório para um único dia. Delega para {@link #generate(LocalDate, LocalDate, List)}.
     */
    public DailyRentalReportDTO generate(LocalDate eventDate, List<ContractStatus> statuses) {
        return generate(eventDate, eventDate, statuses);
    }

    /**
     * Gera o relatório para um período (startDate a endDate, ambos inclusivos).
     * Se endDate for nulo, assume o mesmo que startDate.
     */
    public DailyRentalReportDTO generate(LocalDate startDate, LocalDate endDate, List<ContractStatus> statuses) {
        LocalDate effectiveEnd = (endDate == null) ? startDate : endDate;
        List<ContractStatus> effectiveStatuses =
                (statuses == null || statuses.isEmpty()) ? DEFAULT_STATUSES : statuses;

        List<RentalContract> contracts = startDate.isEqual(effectiveEnd)
                ? contractRepository.findByEventDateAndStatusInOrderByCustomerNameAsc(startDate, effectiveStatuses)
                : contractRepository.findByEventDateBetweenAndStatusInOrderByEventDateAscCustomerNameAsc(
                        startDate, effectiveEnd, effectiveStatuses);

        Map<UUID, RentalItemSnapshot> snapshots = resolveSnapshots(contracts);
        List<ClothingTypeGroupDTO> groups = buildGroups(contracts, snapshots);

        int itemCount = groups.stream().mapToInt(ClothingTypeGroupDTO::itemCount).sum();
        int adjustmentCount = groups.stream()
                .flatMap(g -> g.items().stream())
                .mapToInt(i -> i.adjustments().size())
                .sum();

        log.info("Rental report for {} to {} ({} contracts, {} items, {} adjustments)",
                startDate, effectiveEnd, contracts.size(), itemCount, adjustmentCount);

        return new DailyRentalReportDTO(
                startDate, effectiveEnd, OffsetDateTime.now(),
                contracts.size(), itemCount, adjustmentCount, groups);
    }

    private Map<UUID, RentalItemSnapshot> resolveSnapshots(List<RentalContract> contracts) {
        List<UUID> ids = contracts.stream()
                .flatMap(c -> c.getItems().stream())
                .map(RentalContractItem::getRentalItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return rentalItemPort.findByIds(ids);
    }

    /** Agrupa por tipo de roupa preservando a ordem por cliente; "Sem categoria" vai por último. */
    private List<ClothingTypeGroupDTO> buildGroups(
            List<RentalContract> contracts, Map<UUID, RentalItemSnapshot> snapshots) {

        Map<String, List<ReportItemDTO>> byType = new LinkedHashMap<>();
        for (RentalContract contract : contracts) {
            for (RentalContractItem item : contract.getItems()) {
                RentalItemSnapshot snapshot =
                        item.getRentalItemId() == null ? null : snapshots.get(item.getRentalItemId());
                String clothingType = resolveClothingType(snapshot);
                byType.computeIfAbsent(clothingType, k -> new ArrayList<>())
                        .add(toReportItem(contract, item, snapshot));
            }
        }

        return byType.entrySet().stream()
                .sorted(groupOrder())
                .map(e -> new ClothingTypeGroupDTO(e.getKey(), e.getValue().size(), e.getValue()))
                .collect(Collectors.toList());
    }

    private Comparator<Map.Entry<String, List<ReportItemDTO>>> groupOrder() {
        return Comparator
                .comparing((Map.Entry<String, List<ReportItemDTO>> e) -> e.getKey().equals(UNCATEGORIZED))
                .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER);
    }

    private String resolveClothingType(RentalItemSnapshot snapshot) {
        if (snapshot == null || snapshot.categoryName() == null || snapshot.categoryName().isBlank()) {
            return UNCATEGORIZED;
        }
        return snapshot.categoryName();
    }

    private ReportItemDTO toReportItem(
            RentalContract contract, RentalContractItem item, RentalItemSnapshot snapshot) {
        return new ReportItemDTO(
                contract.getLegacyId(),
                contract.getCustomerName(),
                item.getLegacyProductCode(),
                item.getDescription(),
                snapshot != null ? snapshot.size() : null,
                snapshot != null ? snapshot.color() : null,
                contract.getPickupDate(),
                toAdjustments(item)
        );
    }

    private List<ReportAdjustmentDTO> toAdjustments(RentalContractItem item) {
        if (item.getMetadata() == null) {
            return List.of();
        }
        return item.getMetadata().stream()
                .map(this::toAdjustment)
                .collect(Collectors.toList());
    }

    private ReportAdjustmentDTO toAdjustment(RentalContractItemMeta meta) {
        return new ReportAdjustmentDTO(
                meta.getType().name(),
                meta.getType().getDescription(),
                meta.getDescription());
    }
}
