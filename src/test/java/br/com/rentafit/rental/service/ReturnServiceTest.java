package br.com.rentafit.rental.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.repository.EmployeeRepository;
import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.RentalContractItem;
import br.com.rentafit.rental.domain.RentalContractItemMeta;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.CloseReturnRequestDTO;
import br.com.rentafit.rental.dto.MarkReturnRequestDTO;
import br.com.rentafit.rental.dto.ReturnEntryDTO;
import br.com.rentafit.rental.dto.ReturnSummaryDTO;
import br.com.rentafit.rental.repository.RentalContractItemMetaRepository;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
import br.com.rentafit.rental.repository.RentalContractRepository;
import br.com.rentafit.rental.repository.RentalPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReturnService - Unit Tests")
class ReturnServiceTest {

    @Mock private RentalContractRepository contractRepository;
    @Mock private RentalContractItemRepository itemRepository;
    @Mock private RentalContractItemMetaRepository metaRepository;
    @Mock private RentalPaymentRepository paymentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private RentalWorkflowService workflowService;

    @InjectMocks
    private ReturnService returnService;

    private UUID contractId;
    private UUID employeeId;
    private UUID itemId;
    private RentalContract finalizedContract;
    private RentalContractItem item;
    private RentalPayment paidPayment;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        item = RentalContractItem.builder()
                .id(itemId)
                .description("Vestido de Noiva")
                .value(BigDecimal.valueOf(1000))
                .delivered(true)
                .returned(false)
                .metadata(new ArrayList<>())
                .build();

        paidPayment = RentalPayment.builder()
                .id(UUID.randomUUID())
                .installmentNumber(1)
                .paymentDate(LocalDate.now().minusDays(1))
                .paymentMethod(PaymentMethod.CASH)
                .value(BigDecimal.valueOf(1000))
                .installments(1)
                .status(PaymentStatus.PAID)
                .build();

        List<RentalContractItem> items = new ArrayList<>();
        items.add(item);
        List<RentalPayment> payments = new ArrayList<>();
        payments.add(paidPayment);

        finalizedContract = RentalContract.builder()
                .id(contractId)
                .legacyId("20260502-1")
                .customerName("Maria Silva")
                .customerId(UUID.randomUUID())
                .status(ContractStatus.FINALIZED)
                .returnDate(LocalDate.now().plusDays(1))
                .pickupDate(LocalDate.now().minusDays(3))
                .eventDate(LocalDate.now().minusDays(1))
                .returned(false)
                .items(items)
                .payments(payments)
                .build();

        item.setContract(finalizedContract);
        paidPayment.setContract(finalizedContract);
    }

    @Nested
    @DisplayName("getReturnSummary")
    class GetReturnSummary {

        @Test
        @DisplayName("retorna summary para contrato FINALIZED")
        void shouldReturnSummaryForFinalizedContract() {
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            ReturnSummaryDTO result = returnService.getReturnSummary(contractId);

            assertThat(result.contractId()).isEqualTo(contractId);
            assertThat(result.customerName()).isEqualTo("Maria Silva");
            assertThat(result.isFullyReturned()).isFalse();
            assertThat(result.pendingCount()).isEqualTo(1);
            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("lança 422 para contrato não FINALIZED")
        void shouldRejectNonFinalizedContract() {
            finalizedContract.setStatus(ContractStatus.SIGNED);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            assertThatThrownBy(() -> returnService.getReturnSummary(contractId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("FINALIZED");
        }

        @Test
        @DisplayName("lança 404 para contrato inexistente")
        void shouldThrowNotFoundForMissingContract() {
            when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> returnService.getReturnSummary(contractId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markItemsReturned")
    class MarkItemsReturned {

        @Test
        @DisplayName("marca item como devolvido e retorna summary atualizado")
        void shouldMarkItemReturnedAndReturnUpdatedSummary() {
            OffsetDateTime now = OffsetDateTime.now();
            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João (amigo)", List.of(new ReturnEntryDTO(itemId, null, now)), null, null);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReturnSummaryDTO result = returnService.markItemsReturned(contractId, request);

            assertThat(item.getReturned()).isTrue();
            assertThat(item.getReturnedByName()).isEqualTo("João (amigo)");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("ignora item já devolvido silenciosamente (idempotente)")
        void shouldIgnoreAlreadyReturnedItem() {
            item.setReturned(true);
            OffsetDateTime now = OffsetDateTime.now();
            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João", List.of(new ReturnEntryDTO(itemId, null, now)), null, null);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            returnService.markItemsReturned(contractId, request);

            verify(itemRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança 404 para itemId inexistente no contrato")
        void shouldThrowNotFoundForUnknownItemId() {
            UUID unknownId = UUID.randomUUID();
            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João", List.of(new ReturnEntryDTO(unknownId, null, OffsetDateTime.now())), null, null);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            assertThatThrownBy(() -> returnService.markItemsReturned(contractId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }

        @Test
        @DisplayName("BUG-6: cria parcela MULTA quando applyFine=true e fineAmount > 0")
        void shouldCreateFinePaymentWhenApplyFineIsTrue() {
            OffsetDateTime now = OffsetDateTime.now();
            BigDecimal fineAmount = BigDecimal.valueOf(50.00);
            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João", List.of(new ReturnEntryDTO(itemId, null, now)), true, fineAmount);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(1);

            returnService.markItemsReturned(contractId, request);

            ArgumentCaptor<RentalPayment> captor = ArgumentCaptor.forClass(RentalPayment.class);
            verify(paymentRepository).save(captor.capture());
            RentalPayment fine = captor.getValue();
            assertThat(fine.getStatus()).isEqualTo(PaymentStatus.MULTA);
            assertThat(fine.getValue()).isEqualByComparingTo(fineAmount);
            assertThat(fine.getInstallmentNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("BUG-6: não cria parcela MULTA quando applyFine=false")
        void shouldNotCreateFinePaymentWhenApplyFineIsFalse() {
            OffsetDateTime now = OffsetDateTime.now();
            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João", List.of(new ReturnEntryDTO(itemId, null, now)), false, BigDecimal.valueOf(50));

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            returnService.markItemsReturned(contractId, request);

            verify(paymentRepository, never()).findMaxInstallmentNumberByContractId(any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("BUG-6: não cria parcela MULTA quando applyFine=true mas fineAmount=null")
        void shouldNotCreateFinePaymentWhenFineAmountIsNull() {
            OffsetDateTime now = OffsetDateTime.now();
            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João", List.of(new ReturnEntryDTO(itemId, null, now)), true, null);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            returnService.markItemsReturned(contractId, request);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("BUG-6: não cria parcela MULTA quando fineAmount = 0")
        void shouldNotCreateFinePaymentWhenFineAmountIsZero() {
            OffsetDateTime now = OffsetDateTime.now();
            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João", List.of(new ReturnEntryDTO(itemId, null, now)), true, BigDecimal.ZERO);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            returnService.markItemsReturned(contractId, request);

            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("closeReturn")
    class CloseReturn {

        @Test
        @DisplayName("fecha contrato com todos os itens devolvidos e pagamentos pagos")
        void shouldCloseContractSuccessfully() {
            item.setReturned(true);
            Employee employee = mock(Employee.class);
            CloseReturnRequestDTO request = new CloseReturnRequestDTO(employeeId, false, null);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(contractRepository.save(any())).thenReturn(finalizedContract);

            ReturnSummaryDTO result = returnService.closeReturn(contractId, request);

            assertThat(finalizedContract.getStatus()).isEqualTo(ContractStatus.CLOSED);
            assertThat(finalizedContract.getReturned()).isTrue();
            verify(workflowService).onReturn(finalizedContract);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("cria parcela de multa quando applyFine=true e fineAmount > 0")
        void shouldCreateFinePaymentWhenApplied() {
            item.setReturned(true);
            Employee employee = mock(Employee.class);
            BigDecimal fineAmount = BigDecimal.valueOf(75.00);
            CloseReturnRequestDTO request = new CloseReturnRequestDTO(employeeId, true, fineAmount);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(paymentRepository.findMaxInstallmentNumberByContractId(contractId)).thenReturn(1);
            when(contractRepository.save(any())).thenReturn(finalizedContract);

            returnService.closeReturn(contractId, request);

            ArgumentCaptor<RentalPayment> captor = ArgumentCaptor.forClass(RentalPayment.class);
            verify(paymentRepository).save(captor.capture());
            RentalPayment fine = captor.getValue();
            assertThat(fine.getStatus()).isEqualTo(PaymentStatus.MULTA);
            assertThat(fine.getValue()).isEqualByComparingTo(fineAmount);
            assertThat(fine.getInstallmentNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("lança 422 quando itens pendentes de devolução")
        void shouldRejectClosingWithPendingItems() {
            CloseReturnRequestDTO request = new CloseReturnRequestDTO(employeeId, false, null);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            assertThatThrownBy(() -> returnService.closeReturn(contractId, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("itens devem ser devolvidos");
        }

        @Test
        @DisplayName("lança 422 quando existem parcelas PENDING")
        void shouldRejectClosingWithPendingPayments() {
            item.setReturned(true);
            paidPayment.setStatus(PaymentStatus.PENDING);
            CloseReturnRequestDTO request = new CloseReturnRequestDTO(employeeId, false, null);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            assertThatThrownBy(() -> returnService.closeReturn(contractId, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("parcela(s) pendente(s)");
        }

        @Test
        @DisplayName("lança 404 para employeeId inexistente")
        void shouldRejectClosingWithUnknownEmployee() {
            item.setReturned(true);
            CloseReturnRequestDTO request = new CloseReturnRequestDTO(employeeId, false, null);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> returnService.closeReturn(contractId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(employeeId.toString());
        }

        @Test
        @DisplayName("não cria multa quando applyFine=false mesmo com fineAmount preenchido")
        void shouldNotCreateFineWhenApplyFineIsFalse() {
            item.setReturned(true);
            Employee employee = mock(Employee.class);
            CloseReturnRequestDTO request = new CloseReturnRequestDTO(employeeId, false, BigDecimal.valueOf(50));

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
            when(contractRepository.save(any())).thenReturn(finalizedContract);

            returnService.closeReturn(contractId, request);

            verify(paymentRepository, never()).findMaxInstallmentNumberByContractId(any());
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAccessoryReturned")
    class MarkAccessoryReturned {

        @Test
        @DisplayName("BUG-2026-05-10-4 REGRESSION: deve encontrar meta pelo accessoryId do catálogo, não pelo id interno do meta")
        void shouldMarkAccessoryReturnedByAccessoryId() {
            UUID accessoryId = UUID.randomUUID(); // FK para o catálogo de acessórios
            UUID metaId      = UUID.randomUUID(); // PK interna do RentalContractItemMeta — diferente de accessoryId

            RentalContractItemMeta accessoryMeta = RentalContractItemMeta.builder()
                    .id(metaId)              // id interno do meta (NÃO o accessoryId do catálogo)
                    .accessoryId(accessoryId) // referência ao catálogo
                    .type(ItemMetaType.ACESSORIO)
                    .description("Gravata")
                    .returned(false)
                    .build();
            accessoryMeta.setContractItem(item);
            item.getMetadata().add(accessoryMeta);

            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João",
                    List.of(new ReturnEntryDTO(itemId, accessoryId, OffsetDateTime.now())),
                    null, null);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));
            when(metaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // BUG-4: se o filtro usar m.getId() (metaId) em vez de m.getAccessoryId() (accessoryId),
            // o meta não será encontrado e uma ResourceNotFoundException será lançada.
            // O teste deve passar SEM exceção após a correção.
            assertThatCode(() -> returnService.markItemsReturned(contractId, request))
                    .doesNotThrowAnyException();

            assertThat(accessoryMeta.getReturned()).isTrue();
        }

        @Test
        @DisplayName("BUG-2026-05-10-4 REGRESSION: lança ResourceNotFoundException quando o accessoryId não existe no contrato")
        void shouldThrowNotFoundForUnknownAccessoryId() {
            UUID unknownAccessoryId = UUID.randomUUID();

            // Item sem metadados de acessório
            MarkReturnRequestDTO request = new MarkReturnRequestDTO(
                    "João",
                    List.of(new ReturnEntryDTO(itemId, unknownAccessoryId, OffsetDateTime.now())),
                    null, null);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            assertThatThrownBy(() -> returnService.markItemsReturned(contractId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownAccessoryId.toString());
        }
    }

    @Nested
    @DisplayName("delayDays e suggestedFine")
    class DelayDaysAndSuggestedFine {

        @Test
        @DisplayName("calcula delayDays=0 quando returnDate é hoje ou futuro")
        void shouldReturnZeroDelayWhenReturnDateIsInFuture() {
            finalizedContract.setReturnDate(LocalDate.now().plusDays(2));
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            ReturnSummaryDTO result = returnService.getReturnSummary(contractId);

            assertThat(result.delayDays()).isEqualTo(0);
        }

        @Test
        @DisplayName("calcula delayDays correto quando returnDate é passado")
        void shouldCalculatePositiveDelayWhenOverdue() {
            finalizedContract.setReturnDate(LocalDate.now().minusDays(3));
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            ReturnSummaryDTO result = returnService.getReturnSummary(contractId);

            assertThat(result.delayDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("BUG-2: suggestedFine = 0 quando sem atraso")
        void shouldReturnZeroSuggestedFineWhenNoDelay() {
            finalizedContract.setReturnDate(LocalDate.now().plusDays(1));
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            ReturnSummaryDTO result = returnService.getReturnSummary(contractId);

            assertThat(result.suggestedFine()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("BUG-2: suggestedFine calculada como 1% do total por dia (item = R$1000, 5 dias = R$50)")
        void shouldCalculateSuggestedFineAsOnePercentPerDay() {
            // item.value = 1000, delayDays = 5 → 1000 * 0.05 = 50.00
            finalizedContract.setReturnDate(LocalDate.now().minusDays(5));
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            ReturnSummaryDTO result = returnService.getReturnSummary(contractId);

            assertThat(result.suggestedFine()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        }

        @Test
        @DisplayName("BUG-2: suggestedFine limitada a 20% do total (item = R$1000, 25 dias → cap R$200)")
        void shouldCapSuggestedFineAtTwentyPercent() {
            // item.value = 1000, delayDays = 25 → 25% → capped at 20% = 200.00
            finalizedContract.setReturnDate(LocalDate.now().minusDays(25));
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            ReturnSummaryDTO result = returnService.getReturnSummary(contractId);

            assertThat(result.suggestedFine()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        }

        @Test
        @DisplayName("BUG-2: suggestedFine = 0 quando contrato já devolvido (isReturned=true)")
        void shouldReturnZeroSuggestedFineWhenContractAlreadyReturned() {
            finalizedContract.setReturnDate(LocalDate.now().minusDays(5));
            finalizedContract.setReturned(true);
            when(contractRepository.findById(contractId)).thenReturn(Optional.of(finalizedContract));

            ReturnSummaryDTO result = returnService.getReturnSummary(contractId);

            assertThat(result.suggestedFine()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
