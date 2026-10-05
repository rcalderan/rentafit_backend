package br.com.rentafit.rental.service;

import br.com.rentafit.auth.service.CurrentAccountId;
import br.com.rentafit.rental.domain.*;
import br.com.rentafit.rental.domain.enums.*;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.repository.RentalContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RentalRevisionServiceTest {
    private final RentalContractRepository repository = mock(RentalContractRepository.class);
    private final CurrentAccountId actor = mock(CurrentAccountId.class);
    private final RentalReservationDelta delta = mock(RentalReservationDelta.class);
    private final RentalRevisionService revisions = new RentalRevisionService(repository, new RentalMapper(), actor, delta);
    private RentalContract original;
    private RentalContract revision;

    @BeforeEach
    void setup() {
        original = RentalContract.builder().id(UUID.randomUUID()).customerId(UUID.randomUUID())
                .customerName("Original").customerDocument("snapshot").status(ContractStatus.FINALIZED)
                .pickupDate(LocalDate.now().plusDays(2)).eventDate(LocalDate.now().plusDays(3))
                .returnDate(LocalDate.now().plusDays(4)).build();
        original.getPayments().add(RentalPayment.builder().id(UUID.randomUUID()).installmentNumber(1)
                .paymentDate(LocalDate.now()).paymentMethod(PaymentMethod.PIX).value(BigDecimal.TEN)
                .processedByEmployeeId(UUID.randomUUID()).status(PaymentStatus.PAID).build());
        when(actor.requireId()).thenReturn(UUID.randomUUID());
        when(repository.findById(original.getId())).thenReturn(Optional.of(original));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            revision = invocation.getArgument(0); revision.setId(UUID.randomUUID()); return revision;
        });
    }

    @Test
    void finalizedRevisionPreservesCustomerPaidValuesAndAppliesOnlyReservationDelta() {
        var response = revisions.create(original.getId(), () -> "new-1");
        assertThat(response.customerName()).isEqualTo("Original");
        assertThat(response.paidValue()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(original.getStatus()).isEqualTo(ContractStatus.FINALIZED);
        revisions.confirm(revision);
        assertThat(original.getStatus()).isEqualTo(ContractStatus.SUPERSEDED);
        assertThat(revision.getStatus()).isEqualTo(ContractStatus.FINALIZED);
        assertThat(revision.getRevisionConfirmedAt()).isNotNull();
        verify(delta).apply(original, revision);
    }

    @Test
    void rejectsChangedParentWithoutReplacingIt() {
        revisions.create(original.getId(), () -> "new-1");
        original.getPayments().getFirst().setValue(BigDecimal.ONE);
        assertThatThrownBy(() -> revisions.confirm(revision)).hasMessageContaining("desatualizada");
        assertThat(original.getStatus()).isEqualTo(ContractStatus.FINALIZED);
        verifyNoInteractions(delta);
    }

    @Test
    void signedRevisionDoesNotReserveStock() {
        original.setStatus(ContractStatus.SIGNED);
        revisions.create(original.getId(), () -> "new-1");
        revisions.confirm(revision);
        assertThat(revision.getStatus()).isEqualTo(ContractStatus.SIGNED);
        verifyNoInteractions(delta);
    }

    @Test
    void deliveredContractCannotBeRevised() {
        original.getItems().add(RentalContractItem.builder().delivered(true).metadata(new ArrayList<>()).build());
        assertThatThrownBy(() -> RentalRevisionService.requireEligible(original)).hasMessageContaining("sem entrega/devolução");
    }
}
