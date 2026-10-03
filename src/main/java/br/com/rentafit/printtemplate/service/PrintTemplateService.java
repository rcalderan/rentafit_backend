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
import java.util.regex.Pattern;

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
        if (template.getId() == null) {
            template.setId(id);
            template.setVersion(1);
        }
        if (request.isDefault() && request.isActive()) {
            clearOtherDefaults(template.getId(), request.templateType());
        }
        apply(template, request);
        return toResponse(repository.save(template));
    }

    public PrintTemplateResponse createNewVersion(String sourceId, PrintTemplateRequest request) {
        PrintTemplate source = repository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("PrintTemplate", "id", sourceId));
        PrintTemplate version = new PrintTemplate();
        int nextVersion = nextVersionNumber(source);
        version.setId(newVersionId(source, nextVersion));
        version.setVersion(nextVersion);
        version.setPreviousVersionId(source.getId());
        apply(version, request);
        // Versões preservam o tipo do documento original: um contrato versionado continua RENTAL_CONTRACT
        version.setTemplateType(source.getTemplateType());
        version.setDefault(true);
        version.setActive(true);
        clearOtherDefaults(version.getId(), version.getTemplateType());
        return toResponse(repository.save(version));
    }

    public PrintTemplateResponse markAsDefault(String id) {
        PrintTemplate template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PrintTemplate", "id", id));
        clearOtherDefaults(template.getId(), template.getTemplateType());
        template.setDefault(true);
        template.setActive(true);
        return toResponse(repository.save(template));
    }

    private void clearOtherDefaults(String exceptId, String templateType) {
        var defaultsToClear = repository.findAllByTemplateTypeAndIsDefaultTrueAndIsActiveTrueAndIdNot(
                templateType, exceptId);
        defaultsToClear.forEach(existingDefault -> existingDefault.setDefault(false));
        repository.saveAllAndFlush(defaultsToClear);
    }

    private int nextVersionNumber(PrintTemplate source) {
        String base = lineageBaseId(source);
        String lineagePattern = Pattern.quote(base) + "(-v\\d+(-\\d+)?)?";
        int sourceVersion = Math.max(source.getVersion(), 1);
        int max = repository.findByIdStartingWith(base).stream()
                .filter(t -> t.getId().matches(lineagePattern))
                .mapToInt(t -> Math.max(t.getVersion(), 1))
                .max()
                .orElse(sourceVersion);
        return Math.max(max, sourceVersion) + 1;
    }

    private String newVersionId(PrintTemplate source, int nextVersion) {
        String base = lineageBaseId(source);
        String candidate = base + "-v" + nextVersion;
        int suffix = 2;
        while (repository.existsById(candidate)) {
            candidate = base + "-v" + nextVersion + "-" + suffix++;
        }
        return candidate;
    }

    private String lineageBaseId(PrintTemplate source) {
        String base = source.getId().replaceAll("-v\\d+(-\\d+)?$", "");
        return base.length() > 80 ? base.substring(0, 80) : base;
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
                template.getVersion(),
                template.getPreviousVersionId(),
                template.isDefault(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
