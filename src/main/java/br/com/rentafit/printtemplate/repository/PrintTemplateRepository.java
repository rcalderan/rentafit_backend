package br.com.rentafit.printtemplate.repository;

import br.com.rentafit.printtemplate.domain.PrintTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrintTemplateRepository extends JpaRepository<PrintTemplate, String> {
    List<PrintTemplate> findAllByOrderByNameAsc();

    List<PrintTemplate> findAllByTemplateTypeAndIsDefaultTrueAndIsActiveTrueAndIdNot(
            String templateType,
            String id
    );
}
