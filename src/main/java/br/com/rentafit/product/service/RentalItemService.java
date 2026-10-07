package br.com.rentafit.product.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.common.search.SearchMode;
import br.com.rentafit.common.search.TsQueryBuilder;
import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.RentalItem;
import br.com.rentafit.product.dto.rental.*;
import br.com.rentafit.product.repository.CategoryRepository;
import br.com.rentafit.product.repository.RentalItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalItemService {
    private static final Logger log = LoggerFactory.getLogger(RentalItemService.class);

    private final RentalItemRepository rentalItemRepository;
    private final CategoryRepository categoryRepository;

    public RentalItemDetailsDTO create(RentalItemDTO dto) {
        log.debug("Creating product: {}", dto.name());
        boolean idExists = dto.legacyId() != null;

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(()-> new ValidationException("Category not found"));
        if (idExists) {
            if (checkLegacyIdExists(dto.legacyId())) {
                throw new ValidationException("Legacy ID already exists");
            }
        }
        
        RentalItem product = RentalItem.builder()
                .name(dto.name())
                .category(category)
                .size(dto.size())
                .color(dto.color())
                .brand(dto.brand())
                .value(dto.value())
                .description(dto.description())
                .legacyId(idExists ? dto.legacyId() : generateLegacyId())
                .notes(dto.notes())
                .build();
        // Save product
        RentalItem saved = rentalItemRepository.save(product);
        log.info("Product created with ID: {}", saved.getId());

        return saved.toDTO();
    }
    /** Gera o próximo legacyId numérico no backend, serializando criações concorrentes. */
    Integer generateLegacyId() {
        rentalItemRepository.lockLegacyIdGeneration();
        return rentalItemRepository.findMaxLegacyId()
                .map(Math::incrementExact)
                .orElse(1);
    }

    public RentalItemDetailsDTO findById(UUID id) {
        RentalItem product = rentalItemRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Product", id));
        return product.toDTO();
    }

    public Page<RentalItemDetailsDTO> findAll(Pageable pageable) {
        return rentalItemRepository.findAll(pageable)
                .map(RentalItem::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<RentalItemDetailsDTO> search(String q, SearchMode mode, Pageable pageable) {
        String tsQuery = TsQueryBuilder.toTsQuery(q, mode);
        if (tsQuery == null) {
            return findAll(pageable);
        }
        List<UUID> categoryIds = TsQueryBuilder.idsOrNeverMatch(categoryRepository.findIdsMatchingTsQuery(tsQuery));
        return rentalItemRepository.searchByFullText(tsQuery, TsQueryBuilder.toExactQuery(q), categoryIds, pageable)
                .map(RentalItem::toDTO);
    }

    public RentalItemDetailsDTO update(UUID id, RentalItemUpdateDTO dto) {
        log.debug("Updating rental item: {}", id);

        RentalItem product = rentalItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Rental update", "id", id.toString()));
        rentalItemRepository.lockItems(java.util.List.of(id));
        product.updateFromDTO(dto);
        if ("AVAILABLE".equals(dto.status()) && rentalItemRepository.hasPendingReservation(id)) {
            product.setStatus(br.com.rentafit.product.domain.enums.ProductStatus.RESERVED);
        }

        RentalItem saved = rentalItemRepository.save(product);

        log.info("Rental item updated with ID: {}", saved.getId());
        return saved.toDTO();
    }

    public RentalItemDetailsDTO findByLegacyId(Integer legacyId) {
        RentalItem product = rentalItemRepository.findByLegacyId(legacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Rental lecacyId", legacyId));

        return product.toDTO();
    }

    public boolean checkLegacyIdExists(Integer legacyId) {
        return legacyId != null && rentalItemRepository.findByLegacyId(legacyId).isPresent();
    }

    public void delete(UUID id) {
        rentalItemRepository.deleteById(id);
    }
}
