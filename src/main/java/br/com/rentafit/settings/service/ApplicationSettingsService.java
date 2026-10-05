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
    private final ApplicationSettingRepository repository;
    private final CurrentAccountId currentAccountId;

    @Transactional(readOnly = true)
    public SettingEntry get(String key) {
        requireSupportedKey(key);
        return repository.findById(key).map(entry -> new SettingEntry(key, entry.getValue()))
                .orElse(new SettingEntry(key, "2"));
    }

    @Transactional(readOnly = true)
    public int rentalWindowDays() {
        return parseWindowDays(get(RENTAL_WINDOW_KEY).value());
    }

    @Transactional
    public void put(String key, String value) {
        requireSupportedKey(key);
        int days = parseWindowDays(value);
        var entry = repository.findById(key).orElseGet(br.com.rentafit.settings.domain.ApplicationSetting::new);
        entry.setKey(key);
        entry.setValue(Integer.toString(days));
        entry.setUpdatedBy(currentAccountId.requireId());
        entry.setUpdatedAt(OffsetDateTime.now());
        repository.save(entry);
    }

    private int parseWindowDays(String value) {
        try {
            if (value == null || !value.matches("\\d{1,10}")) throw new NumberFormatException();
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ValidationException("Intervalo '" + value + "': esperado inteiro entre 0 e 2147483647 dias");
        }
    }

    private void requireSupportedKey(String key) {
        if (!RENTAL_WINDOW_KEY.equals(key)) {
            throw new ResourceNotFoundException("ApplicationSetting", "key", key);
        }
    }
}
