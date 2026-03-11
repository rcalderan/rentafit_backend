package br.com.rentafit.rental.service;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
import br.com.rentafit.rental.validation.ConflictSeverity;
import br.com.rentafit.rental.validation.ItemConflict;
import br.com.rentafit.rental.validation.ItemConflictChecker;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemConflictChecker - Unit Tests")
class ItemConflictCheckerTest {

    @Mock private RentalContractItemRepository contractItemRepository;

    @InjectMocks
    private ItemConflictChecker conflictChecker;

    private UUID rentalItemId;
    private UUID contractId;
    private UUID conflictingContractId;
    private LocalDate eventDate;
    private RentalContractItem myItem;

    @BeforeEach
    void setUp() {
        rentalItemId         = UUID.randomUUID();
        contractId           = UUID.randomUUID();
        conflictingContractId = UUID.randomUUID();
        eventDate            = LocalDate.of(2026, 6, 7); // Um sábado

        myItem = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .rentalItemId(rentalItemId)
                .description("Vestido de Noiva Premium")
                .value(new BigDecimal("800.00"))
                .delivered(false)
                .metadata(new ArrayList<>())
                .build();
    }

    private RentalContractItem buildCandidateItem(LocalDate candidateEventDate) {
        RentalContract conflictingContract = RentalContract.builder()
                .id(conflictingContractId)
                .status(ContractStatus.SIGNED)
                .eventDate(candidateEventDate)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        RentalContractItem candidateItem = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .rentalItemId(rentalItemId)
                .description("Vestido de Noiva Premium")
                .value(new BigDecimal("800.00"))
                .delivered(false)
                .metadata(new ArrayList<>())
                .build();
        candidateItem.setContract(conflictingContract);
        return candidateItem;
    }

    // ── Sem conflito ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve retornar lista vazia quando não há conflitos")
    void testNoConflicts() {
        when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                .thenReturn(List.of());

        List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

        assertThat(result).isEmpty();
    }

    // ── BLOCKING ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve retornar BLOCKING quando outro contrato tem o mesmo eventDate")
    void testSameDate_isBlocking() {
        RentalContractItem candidate = buildCandidateItem(eventDate); // mesmo dia!
        when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                .thenReturn(List.of(candidate));

        List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).severity()).isEqualTo(ConflictSeverity.BLOCKING);
        assertThat(result.get(0).conflictingContractId()).isEqualTo(conflictingContractId);
    }

    // ── WARNING ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve retornar WARNING quando conflito está a 1 dia de distância")
    void testOneDayApart_isWarning() {
        RentalContractItem candidate = buildCandidateItem(eventDate.plusDays(1));
        when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                .thenReturn(List.of(candidate));

        List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).severity()).isEqualTo(ConflictSeverity.WARNING);
    }

    @Test
    @DisplayName("Deve retornar WARNING quando conflito está a 3 dias exatos de distância (limite)")
    void testThreeDaysApart_isWarning() {
        RentalContractItem candidate = buildCandidateItem(eventDate.plusDays(3));
        when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                .thenReturn(List.of(candidate));

        List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).severity()).isEqualTo(ConflictSeverity.WARNING);
    }

    @Test
    @DisplayName("Deve retornar lista vazia para conflito a 4 dias (fora da janela de 3 dias)")
    void testFourDaysApart_noConflict() {
        // A query não retornaria resultado, pois [startDate, endDate] = [eventDate-3, eventDate+3]
        // Candidate com +4 dias ficaria fora do range — simulado com lista vazia
        when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                .thenReturn(List.of());

        List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

        assertThat(result).isEmpty();
    }

    // ── Exclusão do próprio contrato ──────────────────────────────────────────

    @Test
    @DisplayName("Deve ignorar o próprio contrato na busca de conflitos")
    void testExcludesOwnContract() {
        // Candidate pertence ao MESMO contractId sendo processado
        RentalContract ownContract = RentalContract.builder()
                .id(contractId) // mesmo ID!
                .status(ContractStatus.SIGNED)
                .eventDate(eventDate)
                .items(new ArrayList<>()).payments(new ArrayList<>()).build();

        RentalContractItem ownItem = RentalContractItem.builder()
                .id(UUID.randomUUID()).rentalItemId(rentalItemId).description("Vestido")
                .value(BigDecimal.valueOf(800)).delivered(false).metadata(new ArrayList<>()).build();
        ownItem.setContract(ownContract);

        when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                .thenReturn(List.of(ownItem));

        List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

        assertThat(result).isEmpty(); // Próprio contrato é excluído
    }

    // ── Itens sem rentalItemId ────────────────────────────────────────────────

    @Test
    @DisplayName("Deve ignorar itens sem rentalItemId (itens legados sem vínculo catalogado)")
    void testIgnoresItemsWithNullRentalItemId() {
        RentalContractItem nullIdItem = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .rentalItemId(null) // sem vínculo!
                .description("Item Legado")
                .value(BigDecimal.valueOf(100))
                .delivered(false).metadata(new ArrayList<>()).build();

        List<ItemConflict> result = conflictChecker.check(List.of(nullIdItem), eventDate, contractId);

        assertThat(result).isEmpty();
        verify(contractItemRepository, never()).findConflictCandidates(any(), any(), any(), any());
    }

    // ── Múltiplos itens ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve verificar conflitos para cada item individualmente")
    void testMultipleItems_checkedIndividually() {
        UUID rentalItemId2 = UUID.randomUUID();
        RentalContractItem item2 = RentalContractItem.builder()
                .id(UUID.randomUUID()).rentalItemId(rentalItemId2)
                .description("Terno").value(BigDecimal.valueOf(300))
                .delivered(false).metadata(new ArrayList<>()).build();

        when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                .thenReturn(List.of());
        when(contractItemRepository.findConflictCandidates(eq(rentalItemId2), any(), any(), any()))
                .thenReturn(List.of());

        List<ItemConflict> result = conflictChecker.check(List.of(myItem, item2), eventDate, contractId);

        assertThat(result).isEmpty();
        verify(contractItemRepository, times(2)).findConflictCandidates(any(), any(), any(), any());
    }
}

