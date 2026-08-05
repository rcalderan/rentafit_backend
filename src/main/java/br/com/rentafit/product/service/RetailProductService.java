package br.com.rentafit.product.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.RetailProduct;
import br.com.rentafit.product.domain.Stock;
import br.com.rentafit.product.dto.retail.ProductRetailDTO;
import br.com.rentafit.product.dto.retail.ProductRetailDetailsDTO;
import br.com.rentafit.product.dto.retail.ProductRetailUpdateDTO;
import br.com.rentafit.product.repository.CategoryRepository;
import br.com.rentafit.product.repository.RetailProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RetailProductService {

    private static final Logger log = LoggerFactory.getLogger(RetailProductService.class);
    private final RetailProductRepository retailProductRepository;
    private final CategoryRepository categoryRepository;

    public ProductRetailDetailsDTO create(ProductRetailDTO dto) {
        log.debug("Creating product: {}", dto.name());

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new ValidationException("Category not found"));

        if (dto.sku() != null) {
            if (checkSkuExists(dto.sku())) {
                throw new ValidationException("SKU already exists");
            }
        }
        RetailProduct product = RetailProduct.builder()
                .sku(dto.sku())
                .name(dto.name())
                .category(category)
                .size(dto.size())
                .color(dto.color())
                .brand(dto.brand())
                .value(dto.value())
                .description(dto.description())
                .details(dto.details())
                .warrantyDays(dto.warrantyDays())
                .build();
        // Garante estoque zerado desde o cadastro (defensivo além do @PrePersist)
        Stock zeroedStock = Stock.builder().build();
        zeroedStock.setProduct(product);
        product.setStock(zeroedStock);
        // Save product
        RetailProduct saved = retailProductRepository.save(product);
        log.info("Product created with ID: {}", saved.getId());

        return saved.toDTO();
    }


    public ProductRetailDetailsDTO update(UUID id, @Valid ProductRetailUpdateDTO dto) {
        log.debug("Updating rental item: {}", id);

        RetailProduct product = retailProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Retail", "id", id.toString()));
        log.debug("skus - existing: {}, new: {}", product.getSku(), dto.sku());
        if(dto.sku()!= null) {
            if (checkSkuExists(dto.sku())) {
                throw new ValidationException("SKU already exists");
            }
        }
        product.updateFromDTO(dto);

        RetailProduct saved = retailProductRepository.save(product);

        log.info("Rental item updated with ID: {}", saved.getId());
        return saved.toDTO();
    }

    private boolean checkSkuExists(String s) {
        return retailProductRepository.findBySku(s).isPresent();
    }

    public ProductRetailDetailsDTO findById(UUID id) {
        RetailProduct product = retailProductRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Product", id));
        return product.toDTO();
    }

    public Page<ProductRetailDetailsDTO> findAll(@Valid Pageable pageable) {
        return retailProductRepository.findAll(pageable)
                .map(RetailProduct::toDTO);
    }

    public ProductRetailDetailsDTO findBySku(String sku) {
        RetailProduct product = retailProductRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Retail by SKU", sku));

        return product.toDTO();
    }

    public void delete(UUID id) {
        retailProductRepository.deleteById(id);
    }

}
