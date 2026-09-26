package br.com.rentafit.printtemplate.service;

import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.printtemplate.domain.PrintTemplate;
import br.com.rentafit.printtemplate.dto.PrintTemplateRequest;
import br.com.rentafit.printtemplate.dto.PrintTemplateResponse;
import br.com.rentafit.printtemplate.repository.PrintTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrintTemplateService {

    private final PrintTemplateRepository repository;

    @Transactional(readOnly = true)
    public List<PrintTemplateResponse> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public PrintTemplateResponse findById(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PrintTemplate", "id", id));
    }

    public PrintTemplateResponse save(String id, PrintTemplateRequest request) {
        PrintTemplate template = repository.findById(id).orElseGet(PrintTemplate::new);
        if (template.getId() == null) template.setId(id);
        if (request.isDefault() && request.isActive()) {
            var defaultsToClear = repository.findAllByTemplateTypeAndIsDefaultTrueAndIsActiveTrueAndIdNot(
                    request.templateType(), template.getId());
            defaultsToClear.forEach(existingDefault -> existingDefault.setDefault(false));
            repository.saveAllAndFlush(defaultsToClear);
        }
        apply(template, request);
        return toResponse(repository.save(template));
    }

    public void delete(String id) {
        PrintTemplate template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrintTemplate", "id", id));
        repository.delete(template);
    }

    private void apply(PrintTemplate template, PrintTemplateRequest request) {
        template.setName(request.name().trim());
        template.setDescription(request.description());
        template.setTemplateType(request.templateType());
        template.setPageFormat(request.pageFormat());
        template.setOrientation(request.orientation());
        template.setPageWidthMm(request.pageWidthMm());
        template.setPageHeightMm(request.pageHeightMm());
        template.setMarginTopMm(request.marginTopMm());
        template.setMarginBottomMm(request.marginBottomMm());
        template.setMarginLeftMm(request.marginLeftMm());
        template.setMarginRightMm(request.marginRightMm());
        template.setPrintOffsetMm(request.printOffsetMm());
        template.setContentJson(request.contentJson());
        template.setContentHtml(request.contentHtml());
        template.setCssStyles(request.cssStyles());
        template.setDefault(request.isDefault());
        template.setActive(request.isActive());
    }

    private PrintTemplateResponse toResponse(PrintTemplate template) {
        return new PrintTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getTemplateType(),
                template.getPageFormat(),
                template.getOrientation(),
                template.getPageWidthMm(),
                template.getPageHeightMm(),
                template.getMarginTopMm(),
                template.getMarginBottomMm(),
                template.getMarginLeftMm(),
                template.getMarginRightMm(),
                template.getPrintOffsetMm(),
                template.getContentJson(),
                template.getContentHtml(),
                template.getCssStyles(),
                template.isDefault(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
