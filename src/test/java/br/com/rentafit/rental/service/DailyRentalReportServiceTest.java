package br.com.rentafit.rental.service;

import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.dto.report.ClothingTypeGroupDTO;
import br.com.rentafit.rental.dto.report.DailyRentalReportDTO;
import br.com.rentafit.rental.port.RentalItemPort;
import br.com.rentafit.rental.port.RentalItemPort.RentalItemSnapshot;
import br.com.rentafit.rental.repository.RentalContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyRentalReportService - Unit Tests")
class DailyRentalReportServiceTest {

    @Mock private RentalContractRepository contractRepository;
    @Mock private RentalItemPort rentalItemPort;

    @InjectMocks
    private DailyRentalReportService reportService;

    private LocalDate eventDate;

    @BeforeEach
    void setUp() {
        eventDate = LocalDate.of(2026, 6, 28);
    }

    @Test
    @DisplayName("agrupa itens por tipo de roupa e calcula totais")
    void groupsByClothingTypeAndComputesTotals() {
        UUID ternoId = UUID.randomUUID();
        UUID vestidoId = UUID.randomUUID();

        RentalContract contract = contract("20260628-1", "Ana Lima",
                item(ternoId, "1042", "Terno Slim", meta(ItemMetaType.ACESSORIO, "Gravata")),
                item(vestidoId, "2210", "Vestido Longo",
                        meta(ItemMetaType.OBSERVACAO, "Subir alça"),
                        meta(ItemMetaType.OBSERVACAO, "Barra")));

        when(contractRepository.findByEventDateAndStatusInOrderByCustomerNameAsc(eq(eventDate), anyList()))
                .thenReturn(List.of(contract));
        when(rentalItemPort.findByIds(anyList())).thenReturn(Map.of(
                ternoId, snapshot(ternoId, "Terno / Smoking", "48", "Preto"),
                vestidoId, snapshot(vestidoId, "Vestido de Festa", "38", "Marsala")));

        DailyRentalReportDTO report = reportService.generate(eventDate, null);

        assertThat(report.contractCount()).isEqualTo(1);
        assertThat(report.itemCount()).isEqualTo(2);
        assertThat(report.adjustmentCount()).isEqualTo(3);
        assertThat(report.groups()).extracting(ClothingTypeGroupDTO::clothingType)
                .containsExactly("Terno / Smoking", "Vestido de Festa");
    }

    @Test
    @DisplayName("itens sem rentalItemId ou categoria não resolvida vão para 'Sem categoria' (por último)")
    void fallbacksToUncategorizedAndKeepsItLast() {
        UUID ternoId = UUID.randomUUID();

        RentalContract contract = contract("20260628-1", "Ana Lima",
                item(ternoId, "1042", "Terno Slim"),
                item(null, "9001", "Sapato sem cadastro"));

        when(contractRepository.findByEventDateAndStatusInOrderByCustomerNameAsc(eq(eventDate), anyList()))
                .thenReturn(List.of(contract));
        when(rentalItemPort.findByIds(anyList()))
                .thenReturn(Map.of(ternoId, snapshot(ternoId, "Terno / Smoking", "48", "Preto")));

        DailyRentalReportDTO report = reportService.generate(eventDate, null);

        assertThat(report.groups()).extracting(ClothingTypeGroupDTO::clothingType)
                .containsExactly("Terno / Smoking", "Sem categoria");
    }

    @Test
    @DisplayName("usa status padrão (SIGNED, FINALIZED) quando nenhum é informado")
    void appliesDefaultStatusesWhenNull() {
        when(contractRepository.findByEventDateAndStatusInOrderByCustomerNameAsc(eq(eventDate), anyList()))
                .thenReturn(List.of());
        when(rentalItemPort.findByIds(anyList())).thenReturn(Map.of());

        reportService.generate(eventDate, null);

        verify(contractRepository).findByEventDateAndStatusInOrderByCustomerNameAsc(
                eventDate, List.of(ContractStatus.SIGNED, ContractStatus.FINALIZED));
    }

    @Test
    @DisplayName("repassa status informados explicitamente")
    void usesProvidedStatuses() {
        List<ContractStatus> statuses = List.of(ContractStatus.FINALIZED);
        when(contractRepository.findByEventDateAndStatusInOrderByCustomerNameAsc(eq(eventDate), eq(statuses)))
                .thenReturn(List.of());
        when(rentalItemPort.findByIds(anyList())).thenReturn(Map.of());

        reportService.generate(eventDate, statuses);

        verify(contractRepository).findByEventDateAndStatusInOrderByCustomerNameAsc(eventDate, statuses);
    }

    @Test
    @DisplayName("relatório vazio quando não há contratos")
    void emptyReportWhenNoContracts() {
        when(contractRepository.findByEventDateAndStatusInOrderByCustomerNameAsc(eq(eventDate), anyList()))
                .thenReturn(List.of());
        when(rentalItemPort.findByIds(anyList())).thenReturn(Map.of());

        DailyRentalReportDTO report = reportService.generate(eventDate, null);

        assertThat(report.contractCount()).isZero();
        assertThat(report.itemCount()).isZero();
        assertThat(report.adjustmentCount()).isZero();
        assertThat(report.groups()).isEmpty();
    }

    @Test
    @DisplayName("preenche dados da linha do item (cliente, retirada, tamanho/cor)")
    void mapsReportItemFields() {
        UUID ternoId = UUID.randomUUID();
        RentalContract contract = contract("20260628-7", "Carlos Lima",
                item(ternoId, "1042", "Terno Slim"));
        contract.setPickupDate(LocalDate.of(2026, 6, 26));

        when(contractRepository.findByEventDateAndStatusInOrderByCustomerNameAsc(eq(eventDate), anyList()))
                .thenReturn(List.of(contract));
        when(rentalItemPort.findByIds(anyList()))
                .thenReturn(Map.of(ternoId, snapshot(ternoId, "Terno / Smoking", "48", "Preto")));

        DailyRentalReportDTO report = reportService.generate(eventDate, null);

        var reportItem = report.groups().get(0).items().get(0);
        assertThat(reportItem.contractLegacyId()).isEqualTo("20260628-7");
        assertThat(reportItem.customerName()).isEqualTo("Carlos Lima");
        assertThat(reportItem.pickupDate()).isEqualTo(LocalDate.of(2026, 6, 26));
        assertThat(reportItem.size()).isEqualTo("48");
        assertThat(reportItem.color()).isEqualTo("Preto");
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    private RentalContract contract(String legacyId, String customerName, RentalContractItem... items) {
        RentalContract contract = RentalContract.builder()
                .id(UUID.randomUUID())
                .legacyId(legacyId)
                .customerName(customerName)
                .customerId(UUID.randomUUID())
                .pickupDate(eventDate.minusDays(2))
                .eventDate(eventDate)
                .returnDate(eventDate.plusDays(1))
                .status(ContractStatus.SIGNED)
                .items(new ArrayList<>(List.of(items)))
                .build();
        contract.getItems().forEach(i -> i.setContract(contract));
        return contract;
    }

    private RentalContractItem item(UUID rentalItemId, String code, String description,
                                    RentalContractItemMeta... metadata) {
        return RentalContractItem.builder()
                .id(UUID.randomUUID())
                .rentalItemId(rentalItemId)
                .legacyProductCode(code)
                .description(description)
                .value(BigDecimal.TEN)
                .metadata(new ArrayList<>(List.of(metadata)))
                .build();
    }

    private RentalContractItemMeta meta(ItemMetaType type, String description) {
        return RentalContractItemMeta.builder()
                .id(UUID.randomUUID())
                .type(type)
                .description(description)
                .build();
    }

    private RentalItemSnapshot snapshot(UUID id, String categoryName, String size, String color) {
        return new RentalItemSnapshot(
                id, "L-" + code(id), "Item", categoryName, size, color,
                BigDecimal.TEN, ProductStatus.AVAILABLE);
    }

    private String code(UUID id) {
        return id.toString().substring(0, 4);
    }
}
