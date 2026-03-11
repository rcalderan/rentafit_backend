package br.com.rentafit.rental.adapter;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.repository.AccessoryRepository;
import br.com.rentafit.product.service.StockService;
import br.com.rentafit.rental.port.AccessoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter que implementa AccessoryPort usando StockService + AccessoryRepository do componente Product.
 *
 * <p>Acessórios utilizam controle de estoque por quantidade (Stock.reserve/release),
 * diferente dos RentalItems que gerenciam ProductStatus individualmente.</p>
 *
 * <p>Ao migrar para microserviço, substituir por cliente HTTP do serviço Product.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessoryAdapter implements AccessoryPort {

    private final AccessoryRepository accessoryRepository;
    private final StockService stockService;

    @Override
    public boolean isAvailableInStock(UUID accessoryId) {
        return accessoryRepository.findById(accessoryId)
                .map(a -> a.getStock() != null && a.getStock().getQuantityAvailable() > 0)
                .orElse(false);
    }

    @Override
    public void reserveStock(UUID accessoryId, UUID userId) {
        validateAccessoryExists(accessoryId);
        stockService.reserveStock(accessoryId, 1, userId);
        log.info("Accessory {} stock reserved by user {}", accessoryId, userId);
    }

    @Override
    public void releaseStock(UUID accessoryId, UUID userId) {
        validateAccessoryExists(accessoryId);
        stockService.releaseReservation(accessoryId, 1, userId);
        log.info("Accessory {} stock released by user {}", accessoryId, userId);
    }

    private void validateAccessoryExists(UUID accessoryId) {
        if (!accessoryRepository.existsById(accessoryId)) {
            throw new ValidationException("Acessório não encontrado: " + accessoryId);
        }
    }
}

