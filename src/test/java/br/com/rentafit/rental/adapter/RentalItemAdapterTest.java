package br.com.rentafit.rental.adapter;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.product.domain.RentalItem;
import br.com.rentafit.product.repository.RentalItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentalItemAdapter - Unit Tests")
class RentalItemAdapterTest {

    @Mock private RentalItemRepository rentalItemRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private RentalItemAdapter adapter;

    @Test
    @DisplayName("recordRentalUsage incrementa rentalCount e atualiza lastRentalDate")
    void recordRentalUsageIncrementsCountAndDate() {
        UUID itemId = UUID.randomUUID();
        LocalDateTime rentalDate = LocalDateTime.now();
        RentalItem item = RentalItem.builder().id(itemId).rentalCount(2).build();
        when(rentalItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        adapter.recordRentalUsage(itemId, rentalDate);

        assertThat(item.getRentalCount()).isEqualTo(3);
        assertThat(item.getLastRentalDate()).isEqualTo(rentalDate);
        verify(rentalItemRepository).save(item);
    }

    @Test
    @DisplayName("recordRentalUsage trata rentalCount nulo como zero")
    void recordRentalUsageTreatsNullCountAsZero() {
        UUID itemId = UUID.randomUUID();
        RentalItem item = RentalItem.builder().id(itemId).rentalCount(null).build();
        when(rentalItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        adapter.recordRentalUsage(itemId, LocalDateTime.now());

        assertThat(item.getRentalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("recordRentalUsage falha quando o item não existe")
    void recordRentalUsageFailsWhenItemMissing() {
        UUID itemId = UUID.randomUUID();
        when(rentalItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.recordRentalUsage(itemId, LocalDateTime.now()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(rentalItemRepository, never()).save(any());
    }
}
