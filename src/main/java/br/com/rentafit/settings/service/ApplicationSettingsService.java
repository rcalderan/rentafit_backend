package br.com.rentafit.settings.service;

import br.com.rentafit.auth.service.CurrentAccountId;
import br.com.rentafit.common.exception.ResourceNotFoundException;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.settings.dto.SettingEntry;
import br.com.rentafit.settings.repository.ApplicationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ApplicationSettingsService {
    public static final String RENTAL_WINDOW_KEY = "rental.conflictWindowDays";
    public static final String PIN_TRUST_MINUTES_KEY = "operator.pinTrustMinutes";
    private final ApplicationSettingRepository repository;
    private final CurrentAccountId currentAccountId;

    @Transactional(readOnly = true)
    public SettingEntry get(String key) {
        String defaultValue = defaultValueFor(key);
        return repository.findById(key).map(entry -> new SettingEntry(key, entry.getValue()))
                .orElse(new SettingEntry(key, defaultValue));
    }

    @Transactional(readOnly = true)
    public int rentalWindowDays() {
        return parseWindowDays(get(RENTAL_WINDOW_KEY).value());
    }

    @Transactional
    public void put(String key, String value) {
        String normalizedValue = validateAndNormalize(key, value);
        var entry = repository.findById(key).orElseGet(br.com.rentafit.settings.domain.ApplicationSetting::new);
        entry.setKey(key);
        entry.setValue(normalizedValue);
        entry.setUpdatedBy(currentAccountId.requireId());
        entry.setUpdatedAt(OffsetDateTime.now());
        repository.save(entry);
    }

    private String defaultValueFor(String key) {
        return switch (key) {
            case RENTAL_WINDOW_KEY -> "2";
            case PIN_TRUST_MINUTES_KEY -> "5";
            default -> throw new ResourceNotFoundException("ApplicationSetting", "key", key);
        };
    }

    private String validateAndNormalize(String key, String value) {
        return switch (key) {
            case RENTAL_WINDOW_KEY -> Integer.toString(parseWindowDays(value));
            case PIN_TRUST_MINUTES_KEY -> Integer.toString(parsePinTrustMinutes(value));
            default -> throw new ResourceNotFoundException("ApplicationSetting", "key", key);
        };
    }

    private int parseWindowDays(String value) {
        try {
            if (value == null || !value.matches("\\d{1,10}")) throw new NumberFormatException();
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ValidationException("Intervalo '" + value + "': esperado inteiro entre 0 e 2147483647 dias");
        }
    }

    private int parsePinTrustMinutes(String value) {
        try {
            if (value == null || !value.matches("\\d{1,3}")) throw new NumberFormatException();
            int minutes = Integer.parseInt(value);
            if (minutes < 1 || minutes > 120) throw new NumberFormatException();
            return minutes;
        } catch (NumberFormatException exception) {
            throw new ValidationException("Janela de confiança '" + value + "': esperado inteiro entre 1 e 120 minutos");
        }
    }
}
