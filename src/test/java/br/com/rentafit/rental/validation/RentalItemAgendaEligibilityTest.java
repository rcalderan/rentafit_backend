package br.com.rentafit.rental.validation;

import br.com.rentafit.rental.adapter.RentalItemAdapter;
import br.com.rentafit.product.domain.RentalItem;
import br.com.rentafit.product.domain.enums.ProductStatus;
import br.com.rentafit.product.repository.RentalItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RentalItemAgendaEligibilityTest {
    private final RentalItemRepository repository = mock(RentalItemRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final RentalItemAdapter adapter = new RentalItemAdapter(repository, entities);

    @Test
    void availableReservedAndRentedAreEligibleButPhysicalImpedimentsRemainBlocked() {
        UUID id = UUID.randomUUID();
        RentalItem item = RentalItem.builder().id(id).name("Vestido").status(ProductStatus.AVAILABLE).build();
        when(repository.findById(id)).thenReturn(Optional.of(item));
        for (ProductStatus status : ProductStatus.values()) {
            item.setStatus(status);
            assertThat(adapter.isAvailable(id)).isEqualTo(List.of(ProductStatus.AVAILABLE, ProductStatus.RESERVED, ProductStatus.RENTED).contains(status));
        }
        assertThat(adapter.isAvailable(UUID.randomUUID())).isFalse();
    }

    @Test
    void refreshesLockedItemsBeforeReadingPhysicalState() {
        UUID id = UUID.randomUUID();
        RentalItem item = RentalItem.builder().id(id).build();
        when(repository.lockItems(List.of(id))).thenReturn(List.of(item));
        adapter.lockItems(List.of(id));
        verify(entities).refresh(item, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        adapter.lockItems(List.of());
        verify(repository, times(1)).lockItems(any());
    }
}
