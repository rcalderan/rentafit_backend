package br.com.rentafit.sector.service;

import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.sector.domain.Sector;
import br.com.rentafit.sector.repository.SectorRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Resolve o setor do terminal a partir do header {@code X-Terminal-Sector-Id},
 * enviado pelo frontend (configurado em /admin/system e persistido em localStorage).
 *
 * <p>Operações que registram local físico (criação de contrato/venda, baixa de
 * parcela, entrega e devolução de itens) devem chamar
 * {@link #requireTerminalSector()} — lança {@link ValidationException} (422) se o
 * terminal não informar setor válido.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TerminalSectorResolver {

    public static final String TERMINAL_SECTOR_HEADER = "X-Terminal-Sector-Id";

    private final SectorRepository sectorRepository;

    /**
     * Exige um setor válido e ativo para a requisição corrente.
     */
    public Sector requireTerminalSector() {
        return resolveTerminalSector()
                .orElseThrow(() -> new ValidationException(
                        "Terminal sem setor configurado. Selecione o setor deste terminal em Sistema (/admin/system)."));
    }

    /**
     * Resolve o setor do terminal se o header estiver presente e válido.
     * Não lança exceção — útil para endpoints em que o setor é opcional.
     */
    public java.util.Optional<Sector> resolveTerminalSector() {
        String raw = readHeader();
        if (raw == null || raw.isBlank()) {
            return java.util.Optional.empty();
        }
        UUID id;
        try {
            id = UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Header {} com valor inválido: '{}'", TERMINAL_SECTOR_HEADER, raw);
            return java.util.Optional.empty();
        }
        return sectorRepository.findById(id)
                .filter(s -> Boolean.TRUE.equals(s.getActive()));
    }

    private String readHeader() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        return request.getHeader(TERMINAL_SECTOR_HEADER);
    }
}
