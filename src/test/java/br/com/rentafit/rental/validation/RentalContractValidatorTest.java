package br.com.rentafit.rental.validation;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.rental.domain.RentalPayment;
import br.com.rentafit.rental.domain.enums.PaymentMethod;
import br.com.rentafit.rental.domain.enums.PaymentStatus;
import br.com.rentafit.rental.dto.ContractItemInputDTO;
import br.com.rentafit.rental.dto.RentalPaymentInputDTO;
import br.com.rentafit.rental.port.AccessoryPort;
import br.com.rentafit.rental.port.CustomerPort;
import br.com.rentafit.rental.port.CustomerPort.CustomerSnapshot;
import br.com.rentafit.rental.port.RentalItemPort;
import br.com.rentafit.rental.port.RentalItemPort.RentalItemSnapshot;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentalContractValidator - Testes Unitários")
class RentalContractValidatorTest {

    @Mock private CustomerPort customerPort;
    @Mock private RentalItemPort rentalItemPort;
    @Mock private AccessoryPort accessoryPort;
    @Mock private ItemConflictChecker conflictChecker;

    @InjectMocks
    private RentalContractValidator validator;

    // ── validateDateOrder ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateDateOrder")
    class ValidateDateOrder {

        @Test
        @DisplayName("Deve aceitar datas em ordem válida")
        void deveAceitarDatasEmOrdemValida() {
            assertThatCode(() -> validator.validateDateOrder(
                    LocalDate.now(),
                    LocalDate.now().plusDays(2),
                    LocalDate.now().plusDays(4)
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar datas iguais (pickup == event == return)")
        void deveAceitarDatasIguais() {
            LocalDate today = LocalDate.now();
            assertThatCode(() -> validator.validateDateOrder(today, today, today))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando pickupDate > eventDate")
        void deveLancarQuandoPickupDepoisDeEvent() {
            assertThatThrownBy(() -> validator.validateDateOrder(
                    LocalDate.now().plusDays(5),
                    LocalDate.now(),
                    LocalDate.now().plusDays(7)
            ))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("retirada")
                    .hasMessageContaining("uso");
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando eventDate > returnDate")
        void deveLancarQuandoEventDepoisDeReturn() {
            assertThatThrownBy(() -> validator.validateDateOrder(
                    LocalDate.now(),
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(2)
            ))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("uso")
                    .hasMessageContaining("devolução");
        }

        @Test
        @DisplayName("Deve ignorar validação quando datas são nulas")
        void deveIgnorarQuandoDatasNulas() {
            assertThatCode(() -> validator.validateDateOrder(null, null, null))
                    .doesNotThrowAnyException();
        }
    }

    // ── validateAndGetCustomer ────────────────────────────────────────────────

    @Nested
    @DisplayName("validateAndGetCustomer")
    class ValidateAndGetCustomer {

        @Test
        @DisplayName("Deve retornar snapshot quando cliente existe")
        void deveRetornarSnapshotQuandoClienteExiste() {
            UUID customerId = UUID.randomUUID();
            CustomerSnapshot snapshot = new CustomerSnapshot(customerId, "Maria", "12345678901");
            when(customerPort.findById(customerId)).thenReturn(Optional.of(snapshot));

            CustomerSnapshot result = validator.validateAndGetCustomer(customerId);

            assertThat(result.id()).isEqualTo(customerId);
            assertThat(result.name()).isEqualTo("Maria");
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando cliente não encontrado")
        void deveLancarQuandoClienteNaoEncontrado() {
            UUID customerId = UUID.randomUUID();
            when(customerPort.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> validator.validateAndGetCustomer(customerId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining(customerId.toString());
        }
    }

    // ── validateItemsAvailability ─────────────────────────────────────────────

    @Nested
    @DisplayName("validateItemsAvailability")
    class ValidateItemsAvailability {

        @Test
        @DisplayName("Deve aceitar lista vazia sem exceção")
        void deveAceitarListaVazia() {
            assertThatCode(() -> validator.validateItemsAvailability(List.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar item disponível")
        void deveAceitarItemDisponivel() {
            UUID itemId = UUID.randomUUID();
            when(rentalItemPort.isAvailable(itemId)).thenReturn(true);

            assertThatCode(() -> validator.validateItemsAvailability(List.of(itemId)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve ignorar itemId nulo na lista")
        void deveIgnorarItemIdNulo() {
            List<UUID> ids = new ArrayList<>();
            ids.add(null);

            assertThatCode(() -> validator.validateItemsAvailability(ids))
                    .doesNotThrowAnyException();

            verify(rentalItemPort, never()).isAvailable(any());
        }

        @Test
        @DisplayName("Deve lançar ValidationException com nome do item quando indisponível e item existe")
        void deveLancarComNomeQuandoIndisponivelEItemExiste() {
            UUID itemId = UUID.randomUUID();
            when(rentalItemPort.isAvailable(itemId)).thenReturn(false);
            RentalItemSnapshot snap = new RentalItemSnapshot(itemId, null, "Vestido de Noiva", null, null, null, null, ProductStatus.RENTED);
            when(rentalItemPort.findById(itemId)).thenReturn(Optional.of(snap));

            assertThatThrownBy(() -> validator.validateItemsAvailability(List.of(itemId)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Vestido de Noiva")
                    .hasMessageContaining("RENTED");
        }

        @Test
        @DisplayName("Deve lançar ValidationException com ID quando item não encontrado no catálogo")
        void deveLancarComIdQuandoItemNaoEncontrado() {
            UUID itemId = UUID.randomUUID();
            when(rentalItemPort.isAvailable(itemId)).thenReturn(false);
            when(rentalItemPort.findById(itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> validator.validateItemsAvailability(List.of(itemId)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("não encontrado");
        }
    }

    // ── validateAccessoriesAvailability ──────────────────────────────────────

    @Nested
    @DisplayName("validateAccessoriesAvailability")
    class ValidateAccessoriesAvailability {

        @Test
        @DisplayName("Deve aceitar lista vazia")
        void deveAceitarListaVazia() {
            assertThatCode(() -> validator.validateAccessoriesAvailability(List.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar acessório com estoque disponível")
        void deveAceitarAcessorioDisponivel() {
            UUID accessoryId = UUID.randomUUID();
            when(accessoryPort.isAvailableInStock(accessoryId)).thenReturn(true);

            assertThatCode(() -> validator.validateAccessoriesAvailability(List.of(accessoryId)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve ignorar accessoryId nulo na lista")
        void deveIgnorarAccessoryIdNulo() {
            List<UUID> ids = new ArrayList<>();
            ids.add(null);

            assertThatCode(() -> validator.validateAccessoriesAvailability(ids))
                    .doesNotThrowAnyException();

            verify(accessoryPort, never()).isAvailableInStock(any());
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando acessório sem estoque")
        void deveLancarQuandoAcessorioSemEstoque() {
            UUID accessoryId = UUID.randomUUID();
            when(accessoryPort.isAvailableInStock(accessoryId)).thenReturn(false);

            assertThatThrownBy(() -> validator.validateAccessoriesAvailability(List.of(accessoryId)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining(accessoryId.toString());
        }
    }

    // ── validateItemsHaveAttendant ────────────────────────────────────────────

    @Nested
    @DisplayName("validateItemsHaveAttendant")
    class ValidateItemsHaveAttendant {

        @Test
        @DisplayName("Deve aceitar lista nula sem exceção")
        void deveAceitarListaNula() {
            assertThatCode(() -> validator.validateItemsHaveAttendant(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar lista vazia sem exceção")
        void deveAceitarListaVazia() {
            assertThatCode(() -> validator.validateItemsHaveAttendant(List.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar item com attendantEmployeeId")
        void deveAceitarItemComAttendant() {
            ContractItemInputDTO item = new ContractItemInputDTO(
                    UUID.randomUUID(), "001", "Vestido", new BigDecimal("500"), UUID.randomUUID(), List.of());

            assertThatCode(() -> validator.validateItemsHaveAttendant(List.of(item)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar ValidationException com nome do item quando attendant ausente")
        void deveLancarComNomeQuandoAttendantAusente() {
            ContractItemInputDTO item = new ContractItemInputDTO(
                    UUID.randomUUID(), "001", "Smoking Slim", new BigDecimal("500"), null, List.of());

            assertThatThrownBy(() -> validator.validateItemsHaveAttendant(List.of(item)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Smoking Slim")
                    .hasMessageContaining("atendente");
        }

        @Test
        @DisplayName("Deve lançar ValidationException com índice quando descrição ausente e attendant nulo")
        void deveLancarComIndiceQuandoDescricaoNula() {
            ContractItemInputDTO item = new ContractItemInputDTO(
                    UUID.randomUUID(), "001", null, new BigDecimal("500"), null, List.of());

            assertThatThrownBy(() -> validator.validateItemsHaveAttendant(List.of(item)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("índice 1");
        }
    }

    // ── validateSinglePaidPaymentHasEmployee ──────────────────────────────────

    @Nested
    @DisplayName("validateSinglePaidPaymentHasEmployee")
    class ValidateSinglePaidPaymentHasEmployee {

        @Test
        @DisplayName("Deve aceitar parcela PENDING sem employeeId")
        void deveAceitarPendingSemEmployee() {
            RentalPaymentInputDTO dto = new RentalPaymentInputDTO(
                    1, LocalDate.now().plusDays(5), "PIX", new BigDecimal("100"), 1, null, "PENDING");

            assertThatCode(() -> validator.validateSinglePaidPaymentHasEmployee(dto))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar parcela PAID com employeeId")
        void deveAceitarPaidComEmployee() {
            RentalPaymentInputDTO dto = new RentalPaymentInputDTO(
                    1, LocalDate.now().plusDays(5), "PIX", new BigDecimal("100"), 1, UUID.randomUUID(), "PAID");

            assertThatCode(() -> validator.validateSinglePaidPaymentHasEmployee(dto))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando PAID sem employeeId")
        void deveLancarQuandoPaidSemEmployee() {
            RentalPaymentInputDTO dto = new RentalPaymentInputDTO(
                    2, LocalDate.now().plusDays(5), "PIX", new BigDecimal("100"), 1, null, "PAID");

            assertThatThrownBy(() -> validator.validateSinglePaidPaymentHasEmployee(dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("#2")
                    .hasMessageContaining("processedByEmployeeId");
        }

        @Test
        @DisplayName("Deve aceitar DTO nulo sem exceção")
        void deveAceitarDtoNulo() {
            assertThatCode(() -> validator.validateSinglePaidPaymentHasEmployee(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar status nulo como não PAID")
        void deveAceitarStatusNuloComoNaoPaid() {
            RentalPaymentInputDTO dto = new RentalPaymentInputDTO(
                    1, LocalDate.now().plusDays(5), "PIX", new BigDecimal("100"), 1, null, null);

            assertThatCode(() -> validator.validateSinglePaidPaymentHasEmployee(dto))
                    .doesNotThrowAnyException();
        }
    }

    // ── validatePaidPaymentsHaveEmployee (lista) ──────────────────────────────

    @Nested
    @DisplayName("validatePaidPaymentsHaveEmployee (lista DTO)")
    class ValidatePaidPaymentsHaveEmployee {

        @Test
        @DisplayName("Deve aceitar lista vazia")
        void deveAceitarListaVazia() {
            assertThatCode(() -> validator.validatePaidPaymentsHaveEmployee(List.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar quando qualquer parcela PAID não tem employeeId")
        void deveLancarQuandoParcialaSemEmployee() {
            List<RentalPaymentInputDTO> payments = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("100"), 1, UUID.randomUUID(), "PAID"),
                    new RentalPaymentInputDTO(2, LocalDate.now(), "PIX", new BigDecimal("100"), 1, null, "PAID")
            );

            assertThatThrownBy(() -> validator.validatePaidPaymentsHaveEmployee(payments))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("#2");
        }
    }

    // ── validatePersistedPaidPaymentsHaveEmployee ─────────────────────────────

    @Nested
    @DisplayName("validatePersistedPaidPaymentsHaveEmployee (entidades)")
    class ValidatePersistedPaidPaymentsHaveEmployee {

        @Test
        @DisplayName("Deve aceitar lista nula")
        void deveAceitarListaNula() {
            assertThatCode(() -> validator.validatePersistedPaidPaymentsHaveEmployee(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar parcela PAID com employeeId persistido")
        void deveAceitarPaidComEmployee() {
            RentalPayment payment = RentalPayment.builder()
                    .id(UUID.randomUUID()).installmentNumber(1)
                    .status(PaymentStatus.PAID).processedByEmployeeId(UUID.randomUUID())
                    .paymentMethod(PaymentMethod.PIX).value(new BigDecimal("100")).installments(1)
                    .paymentDate(LocalDate.now())
                    .build();

            assertThatCode(() -> validator.validatePersistedPaidPaymentsHaveEmployee(List.of(payment)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar quando parcela PAID persistida sem employeeId")
        void deveLancarQuandoPaidSemEmployee() {
            RentalPayment payment = RentalPayment.builder()
                    .id(UUID.randomUUID()).installmentNumber(3)
                    .status(PaymentStatus.PAID).processedByEmployeeId(null)
                    .paymentMethod(PaymentMethod.CASH).value(new BigDecimal("200")).installments(1)
                    .paymentDate(LocalDate.now())
                    .build();

            assertThatThrownBy(() -> validator.validatePersistedPaidPaymentsHaveEmployee(List.of(payment)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("#3");
        }

        @Test
        @DisplayName("Deve ignorar parcelas PENDING")
        void deveIgnorarParcelas_PENDING() {
            RentalPayment payment = RentalPayment.builder()
                    .id(UUID.randomUUID()).installmentNumber(1)
                    .status(PaymentStatus.PENDING).processedByEmployeeId(null)
                    .paymentMethod(PaymentMethod.PIX).value(new BigDecimal("100")).installments(1)
                    .paymentDate(LocalDate.now())
                    .build();

            assertThatCode(() -> validator.validatePersistedPaidPaymentsHaveEmployee(List.of(payment)))
                    .doesNotThrowAnyException();
        }
    }

    // ── validateRevisionPaymentIntegrity ──────────────────────────────────────

    @Nested
    @DisplayName("validateRevisionPaymentIntegrity")
    class ValidateRevisionPaymentIntegrity {

        @Test
        @DisplayName("Deve aceitar quando não há parcelas PAID existentes")
        void deveAceitarSemPaidExistentes() {
            RentalPayment pending = RentalPayment.builder()
                    .id(UUID.randomUUID()).installmentNumber(1).value(new BigDecimal("100"))
                    .paymentMethod(PaymentMethod.PIX).status(PaymentStatus.PENDING)
                    .paymentDate(LocalDate.now()).installments(1).build();

            List<RentalPaymentInputDTO> incoming = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("100"), 1, null, "PENDING"));

            assertThatCode(() -> validator.validateRevisionPaymentIntegrity(incoming, List.of(pending)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve aceitar quando parcela PAID está preservada no incoming")
        void deveAceitarQuandoPaidPreservada() {
            UUID empId = UUID.randomUUID();
            RentalPayment paid = RentalPayment.builder()
                    .id(UUID.randomUUID()).installmentNumber(1).value(new BigDecimal("500"))
                    .paymentMethod(PaymentMethod.PIX).status(PaymentStatus.PAID)
                    .processedByEmployeeId(empId).paymentDate(LocalDate.now()).installments(1).build();

            List<RentalPaymentInputDTO> incoming = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("500"), 1, empId, "PAID"));

            assertThatCode(() -> validator.validateRevisionPaymentIntegrity(incoming, List.of(paid)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve lançar quando parcela PAID foi removida do incoming")
        void deveLancarQuandoPaidRemovida() {
            RentalPayment paid = RentalPayment.builder()
                    .id(UUID.randomUUID()).installmentNumber(1).value(new BigDecimal("500"))
                    .paymentMethod(PaymentMethod.PIX).status(PaymentStatus.PAID)
                    .processedByEmployeeId(UUID.randomUUID()).paymentDate(LocalDate.now()).installments(1).build();

            List<RentalPaymentInputDTO> incoming = List.of(
                    new RentalPaymentInputDTO(2, LocalDate.now(), "PIX", new BigDecimal("200"), 1, null, "PENDING"));

            assertThatThrownBy(() -> validator.validateRevisionPaymentIntegrity(incoming, List.of(paid)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("PAGA #1");
        }

        @Test
        @DisplayName("Deve lançar quando valor da parcela PAID foi alterado")
        void deveLancarQuandoValorPaidAlterado() {
            RentalPayment paid = RentalPayment.builder()
                    .id(UUID.randomUUID()).installmentNumber(1).value(new BigDecimal("500"))
                    .paymentMethod(PaymentMethod.PIX).status(PaymentStatus.PAID)
                    .processedByEmployeeId(UUID.randomUUID()).paymentDate(LocalDate.now()).installments(1).build();

            List<RentalPaymentInputDTO> incoming = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("400"), 1, null, "PAID"));

            assertThatThrownBy(() -> validator.validateRevisionPaymentIntegrity(incoming, List.of(paid)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("#1");
        }
    }

    // ── calculatePaymentDeficit / validatePaymentsMatchTotal ─────────────────

    @Nested
    @DisplayName("calculatePaymentDeficit e validatePaymentsMatchTotal")
    class PaymentDeficit {

        private final ContractItemInputDTO item500 = new ContractItemInputDTO(
                UUID.randomUUID(), "001", "Vestido", new BigDecimal("500.00"), UUID.randomUUID(), List.of());

        @Test
        @DisplayName("calculatePaymentDeficit deve retornar zero quando bate")
        void deveRetornarZeroQuandoBate() {
            List<RentalPaymentInputDTO> payments = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("500.00"), 1, null, "PENDING"));

            BigDecimal deficit = validator.calculatePaymentDeficit(payments, List.of(item500));

            assertThat(deficit).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("calculatePaymentDeficit deve retornar positivo quando faltam parcelas")
        void deveRetornarPositivoQuandoFaltam() {
            List<RentalPaymentInputDTO> payments = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("300.00"), 1, null, "PENDING"));

            BigDecimal deficit = validator.calculatePaymentDeficit(payments, List.of(item500));

            assertThat(deficit).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("calculatePaymentDeficit deve ignorar parcelas CANCELLED")
        void deveIgnorarParcelasCancelled() {
            List<RentalPaymentInputDTO> payments = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("500.00"), 1, null, "CANCELLED"));

            BigDecimal deficit = validator.calculatePaymentDeficit(payments, List.of(item500));

            assertThat(deficit).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("validatePaymentsMatchTotal deve aceitar quando bate")
        void deveAceitarQuandoBate() {
            List<RentalPaymentInputDTO> payments = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("500.00"), 1, null, "PENDING"));

            assertThatCode(() -> validator.validatePaymentsMatchTotal(payments, List.of(item500)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validatePaymentsMatchTotal deve lançar quando não bate")
        void deveLancarQuandoNaoBate() {
            List<RentalPaymentInputDTO> payments = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("300.00"), 1, null, "PENDING"));

            assertThatThrownBy(() -> validator.validatePaymentsMatchTotal(payments, List.of(item500)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("300")
                    .hasMessageContaining("500");
        }

        @Test
        @DisplayName("validatePaymentsNotExceedTotal deve aceitar exceto quando excede")
        void deveLancarQuandoExcede() {
            List<RentalPaymentInputDTO> payments = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("600.00"), 1, null, "PENDING"));

            assertThatThrownBy(() -> validator.validatePaymentsNotExceedTotal(payments, List.of(item500)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("ultrapassa");
        }

        @Test
        @DisplayName("validatePaymentsNotExceedTotal deve aceitar quando parcelas < total")
        void deveAceitarQuandoMenorQueTotal() {
            List<RentalPaymentInputDTO> payments = List.of(
                    new RentalPaymentInputDTO(1, LocalDate.now(), "PIX", new BigDecimal("300.00"), 1, null, "PENDING"));

            assertThatCode(() -> validator.validatePaymentsNotExceedTotal(payments, List.of(item500)))
                    .doesNotThrowAnyException();
        }
    }

    // ── checkConflictsForTransition ───────────────────────────────────────────

    @Nested
    @DisplayName("checkConflictsForTransition")
    class CheckConflictsForTransition {

        @Test
        @DisplayName("Deve retornar null quando não há conflitos")
        void deveRetornarNullSemConflitos() {
            when(conflictChecker.check(any(), any(), any())).thenReturn(List.of());

            List<String> result = validator.checkConflictsForTransition(List.of(), LocalDate.now(), UUID.randomUUID());

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Deve retornar warnings quando há conflitos WARNING")
        void deveRetornarWarnings() {
            UUID contractId = UUID.randomUUID();
            ItemConflict warning = new ItemConflict(UUID.randomUUID(), "Vestido", LocalDate.now(), contractId, ConflictSeverity.WARNING);
            when(conflictChecker.check(any(), any(), any())).thenReturn(List.of(warning));

            List<String> result = validator.checkConflictsForTransition(List.of(), LocalDate.now(), contractId);

            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0)).contains("Vestido");
        }

        @Test
        @DisplayName("Deve lançar ValidationException quando há conflito BLOCKING")
        void deveLancarQuandoConflitoBloqueante() {
            UUID contractId = UUID.randomUUID();
            ItemConflict blocking = new ItemConflict(UUID.randomUUID(), "Smoking", LocalDate.now(), contractId, ConflictSeverity.BLOCKING);
            when(conflictChecker.check(any(), any(), any())).thenReturn(List.of(blocking));

            assertThatThrownBy(() -> validator.checkConflictsForTransition(List.of(), LocalDate.now(), contractId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Conflito de reserva");
        }
    }
}
