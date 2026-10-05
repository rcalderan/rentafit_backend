package br.com.rentafit.rental.service;

import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.rental.domain.*;
import br.com.rentafit.rental.domain.enums.ItemMetaType;
import br.com.rentafit.rental.port.*;
import br.com.rentafit.rental.repository.RentalContractItemRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.*;

class RentalReservationDeltaTest {
    private final RentalItemPort items = mock(RentalItemPort.class);
    private final AccessoryPort accessories = mock(AccessoryPort.class);
    private final RentalContractItemRepository repository = mock(RentalContractItemRepository.class);
    private final RentalReservationDelta delta = new RentalReservationDelta(items, accessories, repository);

    @Test
    void doesNotOverwriteRentedPhysicalStateForFutureReservation() {
        UUID id = UUID.randomUUID();
        when(items.findById(id)).thenReturn(Optional.of(new RentalItemPort.RentalItemSnapshot(id, 1, "Vestido", null,
                null, null, BigDecimal.TEN, ProductStatus.RENTED)));
        delta.reserveItem(id);
        verify(items, never()).updateStatus(any(), any());
    }

    @Test
    void transfersOnlyChangedItemsAndAccessoryQuantities() {
        UUID removed = UUID.randomUUID(); UUID added = UUID.randomUUID(); UUID unchanged = UUID.randomUUID();
        UUID accessory = UUID.randomUUID(); UUID author = UUID.randomUUID();
        RentalContract original = contract(removed, unchanged, accessory, author);
        RentalContract revision = contract(added, unchanged, accessory, author);
        when(items.findById(removed)).thenReturn(Optional.of(snapshot(removed, ProductStatus.RESERVED)));
        when(items.findById(added)).thenReturn(Optional.of(snapshot(added, ProductStatus.AVAILABLE)));
        delta.apply(original, revision);
        verify(items).updateStatus(removed, ProductStatus.AVAILABLE);
        verify(items).updateStatus(added, ProductStatus.RESERVED);
        verify(items, never()).updateStatus(eq(unchanged), any());
        verifyNoInteractions(accessories);
    }

    private RentalContract contract(UUID changed, UUID unchanged, UUID accessory, UUID author) {
        RentalContract contract = RentalContract.builder().id(UUID.randomUUID()).createdByEmployeeId(author).build();
        contract.getItems().add(RentalContractItem.builder().rentalItemId(changed).build());
        contract.getItems().add(RentalContractItem.builder().rentalItemId(unchanged).metadata(List.of(
                RentalContractItemMeta.builder().type(ItemMetaType.ACESSORIO).accessoryId(accessory).build())).build());
        return contract;
    }

    private RentalItemPort.RentalItemSnapshot snapshot(UUID id, ProductStatus status) {
        return new RentalItemPort.RentalItemSnapshot(id, 1, "Vestido", null, null, null, BigDecimal.TEN, status);
    }
}
