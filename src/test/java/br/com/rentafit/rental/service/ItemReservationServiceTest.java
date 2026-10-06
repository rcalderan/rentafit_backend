package br.com.rentafit.rental.service;

import br.com.rentafit.rental.domain.RentalContract;
import br.com.rentafit.rental.domain.enums.ContractStatus;
import br.com.rentafit.rental.dto.ItemReservationDTO;
import br.com.rentafit.rental.mapper.RentalMapper;
import br.com.rentafit.rental.port.CustomerPort;
import br.com.rentafit.rental.repository.RentalContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemReservationService - Unit Tests")
class ItemReservationServiceTest {

    @Mock private RentalContractRepository contractRepository;
    @Mock private CustomerPort customerPort;

    private ItemReservationService service;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        service = new ItemReservationService(contractRepository, customerPort, new RentalMapper());
        itemId = UUID.randomUUID();
    }

    private RentalContract contract(ContractStatus status, LocalDate eventDate) {
        return RentalContract.builder()
                .id(UUID.randomUUID())
                .legacyId("251006-1")
                .customerId(UUID.randomUUID())
                .customerName("Ana Lima")
                .status(status)
                .pickupDate(eventDate.minusDays(2))
                .eventDate(eventDate)
                .returnDate(eventDate.plusDays(2))
                .build();
    }

    @Test
    @DisplayName("deve mapear reservas enriquecidas com customerLegacyId")
    void deveMapearReservasComCustomerLegacyId() {
        LocalDate eventDate = LocalDate.now().plusDays(5);
        RentalContract signed = contract(ContractStatus.SIGNED, eventDate);
        when(contractRepository.findReservationsByRentalItemId(any(), any(), any()))
                .thenReturn(List.of(signed));
        when(customerPort.findLegacyIdsByIds(any()))
                .thenReturn(Map.of(signed.getCustomerId(), 101));

        List<ItemReservationDTO> result = service.findReservationsByItem(itemId, null);

        assertThat(result).hasSize(1);
        ItemReservationDTO dto = result.getFirst();
        assertThat(dto.contractId()).isEqualTo(signed.getId());
        assertThat(dto.legacyId()).isEqualTo("251006-1");
        assertThat(dto.customerId()).isEqualTo(signed.getCustomerId());
        assertThat(dto.customerName()).isEqualTo("Ana Lima");
        assertThat(dto.customerLegacyId()).isEqualTo(101);
        assertThat(dto.eventDate()).isEqualTo(eventDate);
        assertThat(dto.pickupDate()).isEqualTo(eventDate.minusDays(2));
        assertThat(dto.returnDate()).isEqualTo(eventDate.plusDays(2));
        assertThat(dto.status()).isEqualTo("SIGNED");
        assertThat(dto.statusDescription()).isEqualTo("Assinado");
    }

    @Test
    @DisplayName("deve excluir o contrato em edição da lista")
    void deveExcluirContratoEmEdicao() {
        RentalContract keep = contract(ContractStatus.SIGNED, LocalDate.now().plusDays(5));
        RentalContract editing = contract(ContractStatus.FINALIZED, LocalDate.now().plusDays(8));
        when(contractRepository.findReservationsByRentalItemId(any(), any(), any()))
                .thenReturn(List.of(keep, editing));
        when(customerPort.findLegacyIdsByIds(any()))
                .thenReturn(Map.of(keep.getCustomerId(), 101));

        List<ItemReservationDTO> result = service.findReservationsByItem(itemId, editing.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().contractId()).isEqualTo(keep.getId());
    }

    @Test
    @DisplayName("deve retornar vazio sem consultar clientes quando não há reservas")
    void deveRetornarVazioSemConsultarClientes() {
        when(contractRepository.findReservationsByRentalItemId(any(), any(), any()))
                .thenReturn(List.of());

        assertThat(service.findReservationsByItem(itemId, null)).isEmpty();
        verifyNoInteractions(customerPort);
    }

    @Test
    @DisplayName("deve retornar customerLegacyId nulo quando cliente não tem legado")
    void deveRetornarCustomerLegacyIdNulo() {
        RentalContract signed = contract(ContractStatus.SIGNED, LocalDate.now().plusDays(5));
        when(contractRepository.findReservationsByRentalItemId(any(), any(), any()))
                .thenReturn(List.of(signed));
        when(customerPort.findLegacyIdsByIds(any())).thenReturn(Map.of());

        List<ItemReservationDTO> result = service.findReservationsByItem(itemId, null);

        assertThat(result.getFirst().customerLegacyId()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("deve consultar apenas SIGNED/FINALIZED com eventDate a partir de hoje")
    void deveConsultarApenasReservasAtivas() {
        when(contractRepository.findReservationsByRentalItemId(any(), any(), any()))
                .thenReturn(List.of());

        service.findReservationsByItem(itemId, null);

        ArgumentCaptor<List<ContractStatus>> statuses = ArgumentCaptor.forClass(List.class);
        verify(contractRepository).findReservationsByRentalItemId(
                eq(itemId), statuses.capture(), eq(LocalDate.now()));
        assertThat(statuses.getValue())
                .containsExactly(ContractStatus.SIGNED, ContractStatus.FINALIZED);
    }
}
