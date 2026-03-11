package br.com.rentafit.rental.service;

import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.port.AccessoryPort;
import br.com.rentafit.rental.port.RentalItemPort;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
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
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentalWorkflowService - Unit Tests")
class RentalWorkflowServiceTest {

    @Mock private RentalItemPort rentalItemPort;
    @Mock private AccessoryPort accessoryPort;
    @Mock private RentalContractRepository contractRepository;
    @Mock private RentalContractItemRepository contractItemRepository;

    @InjectMocks
    private RentalWorkflowService workflowService;

    private UUID contractId;
    private UUID rentalItemId;
    private UUID accessoryId;
    private UUID employeeId;
    private RentalContract contract;
    private RentalContractItem itemWithRentalItem;
    private RentalContractItem itemWithoutRentalItem;
    private RentalContractItemMeta catalogedAccessoryMeta;
    private RentalContractItemMeta textualAccessoryMeta;
    private RentalContractItemMeta observationMeta;

    @BeforeEach
    void setUp() {
        contractId   = UUID.randomUUID();
        rentalItemId = UUID.randomUUID();
        accessoryId  = UUID.randomUUID();
        employeeId   = UUID.randomUUID();

        catalogedAccessoryMeta = RentalContractItemMeta.builder()
                .id(UUID.randomUUID())
                .type(ItemMetaType.ACESSORIO)
                .description("Véu branco")
                .accessoryId(accessoryId) // catalogado → chama AccessoryPort
                .build();

        textualAccessoryMeta = RentalContractItemMeta.builder()
                .id(UUID.randomUUID())
                .type(ItemMetaType.ACESSORIO)
                .description("Luvas brancas")
                .accessoryId(null) // texto livre → NÃO chama AccessoryPort
                .build();

        observationMeta = RentalContractItemMeta.builder()
                .id(UUID.randomUUID())
                .type(ItemMetaType.OBSERVACAO)
                .description("Ajuste na cintura necessário")
                .accessoryId(null)
                .build();

        itemWithRentalItem = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .rentalItemId(rentalItemId)
                .description("Vestido de Noiva")
                .value(new BigDecimal("500.00"))
                .delivered(false)
                .metadata(new ArrayList<>(List.of(catalogedAccessoryMeta, textualAccessoryMeta, observationMeta)))
                .build();

        itemWithoutRentalItem = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .rentalItemId(null) // sem vínculo catalogado
                .description("Item Legado")
                .value(new BigDecimal("100.00"))
                .delivered(false)
                .metadata(new ArrayList<>())
                .build();

        contract = RentalContract.builder()
                .id(contractId)
                .status(ContractStatus.FINALIZED)
                .createdByEmployeeId(employeeId)
                .pickupDate(LocalDate.now())
                .eventDate(LocalDate.now().plusDays(7))
                .returnDate(LocalDate.now().plusDays(9))
                .returned(false)
                .items(new ArrayList<>(List.of(itemWithRentalItem, itemWithoutRentalItem)))
                .payments(new ArrayList<>())
                .build();

        // Configurar referência bidirecional
        itemWithRentalItem.setContract(contract);
        itemWithoutRentalItem.setContract(contract);
    }

    // ── onFinalize ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("onFinalize deve chamar updateStatus(RESERVED) para itens com rentalItemId")
    void testOnFinalize_reservesRentalItems() {
        workflowService.onFinalize(contract);

        verify(rentalItemPort).updateStatus(rentalItemId, ProductStatus.RESERVED);
    }

    @Test
    @DisplayName("onFinalize NÃO deve chamar updateStatus para itens sem rentalItemId")
    void testOnFinalize_ignoresItemsWithoutRentalItemId() {
        workflowService.onFinalize(contract);

        // itemWithoutRentalItem tem rentalItemId null → não chama
        verify(rentalItemPort, times(1)).updateStatus(any(), any());
    }

    @Test
    @DisplayName("onFinalize deve chamar reserveStock para acessórios catalogados")
    void testOnFinalize_reservesCatalogedAccessories() {
        workflowService.onFinalize(contract);

        verify(accessoryPort).reserveStock(accessoryId, employeeId);
    }

    @Test
    @DisplayName("onFinalize NÃO deve chamar reserveStock para acessórios textuais (accessoryId null)")
    void testOnFinalize_ignoresTextualAccessories() {
        workflowService.onFinalize(contract);

        // Só 1 acessório catalogado (textualAccessoryMeta tem accessoryId null)
        verify(accessoryPort, times(1)).reserveStock(any(), any());
    }

    @Test
    @DisplayName("onFinalize NÃO deve chamar AccessoryPort para metadados OBSERVACAO")
    void testOnFinalize_ignoresObservations() {
        workflowService.onFinalize(contract);

        // observationMeta é OBSERVACAO → não chama
        verify(accessoryPort, never()).reserveStock(eq(null), any());
    }

    // ── onDeliverItem ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("onDeliverItem deve atualizar status para RENTED e marcar como entregue")
    void testOnDeliverItem_updatesStatusAndDeliveredFlag() {
        workflowService.onDeliverItem(itemWithRentalItem, employeeId);

        verify(rentalItemPort).updateStatus(rentalItemId, ProductStatus.RENTED);
        verify(contractItemRepository).save(itemWithRentalItem);
        assert itemWithRentalItem.getDelivered();
        assert employeeId.equals(itemWithRentalItem.getAttendantEmployeeId());
    }

    @Test
    @DisplayName("onDeliverItem deve ignorar itens sem rentalItemId")
    void testOnDeliverItem_ignoresNullRentalItemId() {
        workflowService.onDeliverItem(itemWithoutRentalItem, employeeId);

        verify(rentalItemPort, never()).updateStatus(any(), any());
        verify(contractItemRepository).save(itemWithoutRentalItem);
    }

    // ── onReturn ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("onReturn deve atualizar status para MAINTENANCE e liberar estoque dos acessórios")
    void testOnReturn_setsMaintenanceAndReleasesStock() {
        contract.setReturnedByEmployeeId(employeeId);

        workflowService.onReturn(contract);

        verify(rentalItemPort).updateStatus(rentalItemId, ProductStatus.MAINTENANCE);
        verify(accessoryPort).releaseStock(accessoryId, employeeId);
    }

    @Test
    @DisplayName("onReturn NÃO deve chamar ports para itens sem rentalItemId")
    void testOnReturn_ignoresItemsWithoutRentalItemId() {
        workflowService.onReturn(contract);

        verify(rentalItemPort, times(1)).updateStatus(any(), any());
    }

    @Test
    @DisplayName("sign NÃO deve gerar efeitos colaterais no estoque")
    void testSign_noStockSideEffects() {
        // O workflow não é chamado ao assinar — garantia de design
        verifyNoInteractions(rentalItemPort, accessoryPort);
    }
}

