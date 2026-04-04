package br.com.rentafit.rental.validation;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    private RentalContractItem buildCandidateItem(UUID itemRentalId, LocalDate candidateEventDate) {
        return buildCandidateItem(itemRentalId, candidateEventDate, conflictingContractId);
    }

    private RentalContractItem buildCandidateItem(UUID itemRentalId, LocalDate candidateEventDate, UUID candContractId) {
        RentalContract conflictingContract = RentalContract.builder()
                .id(candContractId)
                .status(ContractStatus.SIGNED)
                .eventDate(candidateEventDate)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        RentalContractItem candidateItem = RentalContractItem.builder()
                .id(UUID.randomUUID())
                .rentalItemId(itemRentalId)
                .description("Vestido de Noiva Premium")
                .value(new BigDecimal("800.00"))
                .delivered(false)
                .metadata(new ArrayList<>())
                .build();
        candidateItem.setContract(conflictingContract);
        return candidateItem;
    }

    /** Overload de conveniência: usa rentalItemId padrão */
    private RentalContractItem buildCandidateItem(LocalDate candidateEventDate) {
        return buildCandidateItem(rentalItemId, candidateEventDate);
    }

    // ── Sem conflito ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Sem conflitos")
    class NoConflicts {

        @Test
        @DisplayName("Deve retornar lista vazia quando não há conflitos")
        void testNoConflicts() {
            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of());

            List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar lista vazia para conflito a 4 dias (fora da janela de 3 dias)")
        void testFourDaysApart_noConflict() {
            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of());

            List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando lista de itens está vazia")
        void testEmptyItemsList() {
            List<ItemConflict> result = conflictChecker.check(List.of(), eventDate, contractId);

            assertThat(result).isEmpty();
            verify(contractItemRepository, never()).findConflictCandidates(any(), any(), any(), any());
        }
    }

    // ── BLOCKING ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Conflitos BLOCKING")
    class BlockingConflicts {

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
            assertThat(result.get(0).rentalItemId()).isEqualTo(rentalItemId);
            assertThat(result.get(0).conflictingEventDate()).isEqualTo(eventDate);
        }

        @Test
        @DisplayName("BLOCKING toMessage deve conter 'BLOQUEIO — mesma data'")
        void testBlockingToMessage() {
            RentalContractItem candidate = buildCandidateItem(eventDate);
            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of(candidate));

            List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

            assertThat(result.get(0).toMessage())
                    .contains("Vestido de Noiva Premium")
                    .contains(conflictingContractId.toString())
                    .contains("BLOQUEIO");
        }
    }

    // ── WARNING ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Conflitos WARNING")
    class WarningConflicts {

        @Test
        @DisplayName("Deve retornar WARNING quando conflito está a 1 dia de distância (futuro)")
        void testOneDayApart_isWarning() {
            RentalContractItem candidate = buildCandidateItem(eventDate.plusDays(1));
            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of(candidate));

            List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).severity()).isEqualTo(ConflictSeverity.WARNING);
        }

        @Test
        @DisplayName("Deve retornar WARNING quando conflito está a 1 dia de distância (passado)")
        void testOneDayBefore_isWarning() {
            RentalContractItem candidate = buildCandidateItem(eventDate.minusDays(1));
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
        @DisplayName("WARNING toMessage deve conter 'ALERTA — dentro de 3 dias'")
        void testWarningToMessage() {
            RentalContractItem candidate = buildCandidateItem(eventDate.plusDays(2));
            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of(candidate));

            List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

            assertThat(result.get(0).toMessage())
                    .contains("Vestido de Noiva Premium")
                    .contains(conflictingContractId.toString())
                    .contains("ALERTA");
        }
    }

    // ── Exclusão do próprio contrato ──────────────────────────────────────────

    @Nested
    @DisplayName("Exclusão do próprio contrato")
    class OwnContractExclusion {

        @Test
        @DisplayName("Deve ignorar o próprio contrato na busca de conflitos")
        void testExcludesOwnContract() {
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

        @Test
        @DisplayName("Deve excluir próprio contrato mas manter outros conflitos")
        void testExcludesOwnButKeepsOther() {
            // Item do próprio contrato
            RentalContract ownContract = RentalContract.builder()
                    .id(contractId).status(ContractStatus.SIGNED)
                    .eventDate(eventDate).items(new ArrayList<>()).payments(new ArrayList<>()).build();
            RentalContractItem ownItem = RentalContractItem.builder()
                    .id(UUID.randomUUID()).rentalItemId(rentalItemId).description("Vestido")
                    .value(BigDecimal.valueOf(800)).delivered(false).metadata(new ArrayList<>()).build();
            ownItem.setContract(ownContract);

            // Item de outro contrato
            RentalContractItem otherItem = buildCandidateItem(eventDate);

            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of(ownItem, otherItem));

            List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).conflictingContractId()).isEqualTo(conflictingContractId);
        }
    }

    // ── Itens sem rentalItemId ────────────────────────────────────────────────

    @Nested
    @DisplayName("Itens sem rentalItemId")
    class NullRentalItemId {

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

        @Test
        @DisplayName("Deve verificar apenas itens com rentalItemId numa lista mista")
        void testMixedItems_onlyChecksNonNull() {
            RentalContractItem nullIdItem = RentalContractItem.builder()
                    .id(UUID.randomUUID()).rentalItemId(null)
                    .description("Item Legado").value(BigDecimal.valueOf(100))
                    .delivered(false).metadata(new ArrayList<>()).build();

            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of());

            List<ItemConflict> result = conflictChecker.check(List.of(nullIdItem, myItem), eventDate, contractId);

            assertThat(result).isEmpty();
            verify(contractItemRepository, times(1)).findConflictCandidates(any(), any(), any(), any());
        }
    }

    // ── Múltiplos itens ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Múltiplos itens")
    class MultipleItems {

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

        @Test
        @DisplayName("Deve retornar BLOCKING e WARNING combinados quando há múltiplos conflitos")
        void testMultipleItems_blockingAndWarningCombined() {
            UUID rentalItemId2 = UUID.randomUUID();
            RentalContractItem item2 = RentalContractItem.builder()
                    .id(UUID.randomUUID()).rentalItemId(rentalItemId2)
                    .description("Terno do Noivo").value(BigDecimal.valueOf(400))
                    .delivered(false).metadata(new ArrayList<>()).build();

            // Item 1: conflito BLOCKING (mesmo dia)
            UUID otherContract1 = UUID.randomUUID();
            RentalContractItem candidate1 = buildCandidateItem(rentalItemId, eventDate, otherContract1);

            // Item 2: conflito WARNING (2 dias de distância)
            UUID otherContract2 = UUID.randomUUID();
            RentalContractItem candidate2 = buildCandidateItem(rentalItemId2, eventDate.plusDays(2), otherContract2);

            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of(candidate1));
            when(contractItemRepository.findConflictCandidates(eq(rentalItemId2), any(), any(), any()))
                    .thenReturn(List.of(candidate2));

            List<ItemConflict> result = conflictChecker.check(List.of(myItem, item2), eventDate, contractId);

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(c -> c.severity() == ConflictSeverity.BLOCKING);
            assertThat(result).anyMatch(c -> c.severity() == ConflictSeverity.WARNING);
        }

        @Test
        @DisplayName("Deve retornar múltiplos BLOCKING para o mesmo item com diferentes contratos")
        void testSameItem_multipleBlockingConflicts() {
            UUID otherContract1 = UUID.randomUUID();
            UUID otherContract2 = UUID.randomUUID();

            RentalContractItem candidate1 = buildCandidateItem(rentalItemId, eventDate, otherContract1);
            RentalContractItem candidate2 = buildCandidateItem(rentalItemId, eventDate, otherContract2);

            when(contractItemRepository.findConflictCandidates(eq(rentalItemId), any(), any(), any()))
                    .thenReturn(List.of(candidate1, candidate2));

            List<ItemConflict> result = conflictChecker.check(List.of(myItem), eventDate, contractId);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(c -> c.severity() == ConflictSeverity.BLOCKING);
        }
    }

    // ── Janela de consulta ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Janela de consulta (±3 dias)")
    class QueryWindow {

        @Test
        @DisplayName("Deve passar start/end corretos (eventDate ±3 dias) para o repositório")
        void testQueryWindowDates() {
            when(contractItemRepository.findConflictCandidates(
                    eq(rentalItemId),
                    eq(eventDate.minusDays(3)),
                    eq(eventDate.plusDays(3)),
                    any()))
                    .thenReturn(List.of());

            conflictChecker.check(List.of(myItem), eventDate, contractId);

            verify(contractItemRepository).findConflictCandidates(
                    eq(rentalItemId),
                    eq(eventDate.minusDays(3)),
                    eq(eventDate.plusDays(3)),
                    any());
        }

        @Test
        @DisplayName("Deve buscar apenas contratos com status SIGNED ou FINALIZED")
        void testActiveStatusesFilter() {
            when(contractItemRepository.findConflictCandidates(
                    any(), any(), any(),
                    eq(List.of(ContractStatus.SIGNED, ContractStatus.FINALIZED))))
                    .thenReturn(List.of());

            conflictChecker.check(List.of(myItem), eventDate, contractId);

            verify(contractItemRepository).findConflictCandidates(
                    any(), any(), any(),
                    eq(List.of(ContractStatus.SIGNED, ContractStatus.FINALIZED)));
        }
    }
}

