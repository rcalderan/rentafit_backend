package br.com.rentafit.product.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.product.domain.Category;
import br.com.rentafit.product.domain.enums.ProductTypeCategory;
import br.com.rentafit.product.dto.CategoryDTO;
import br.com.rentafit.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryDTO create(Category category) {
        log.debug("Creating category: {}", category.getName());
        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    public CategoryDTO findById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Category", id));
        return convertToDTO(category);
    }

    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll()
            .stream()
            .map(this::convertToDTO)
            .toList();
    }

    public List<CategoryDTO> findByProductType(ProductTypeCategory productType) {
        return categoryRepository.findByProductType(productType)
            .stream()
            .map(this::convertToDTO)
            .toList();
    }

    public List<CategoryDTO> findActiveCategories() {
        return categoryRepository.findByActiveTrue()
            .stream()
            .map(this::convertToDTO)
            .toList();
    }

    public CategoryDTO update(UUID id, Category categoryData) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Category", id));

        category.setDisplayName(categoryData.getDisplayName());
        category.setDescription(categoryData.getDescription());
        category.setActive(categoryData.getActive());

        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    public void delete(UUID id) {
        categoryRepository.deleteById(id);
    }

    private CategoryDTO convertToDTO(Category category) {
        return CategoryDTO.builder()
            .id(category.getId())
            .name(category.getName())
            .displayName(category.getDisplayName())
            .description(category.getDescription())
            .productType(category.getProductType().name())
            .active(category.getActive())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
