package br.com.rentafit.rental.validation;

import br.com.rentafit.auth.service.CurrentAccountId;
import br.com.rentafit.common.exception.ValidationException;
import br.com.rentafit.settings.domain.ApplicationSetting;
import br.com.rentafit.settings.repository.ApplicationSettingRepository;
import br.com.rentafit.settings.service.ApplicationSettingsService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationSettingsTest {
    private final ApplicationSettingRepository repository = mock(ApplicationSettingRepository.class);
    private final CurrentAccountId actor = mock(CurrentAccountId.class);
    private final ApplicationSettingsService settings = new ApplicationSettingsService(repository, actor);

    @Test
    void defaultsToTwoAndPersistsConfiguredWindowWithAuthor() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        UUID accountId = UUID.randomUUID();
        when(actor.requireId()).thenReturn(accountId);
        assertThat(settings.rentalWindowDays()).isEqualTo(2);
        settings.put(ApplicationSettingsService.RENTAL_WINDOW_KEY, "3");
        verify(repository).save(argThat(entry -> entry.getValue().equals("3") && entry.getUpdatedBy().equals(accountId)));
    }

    @Test
    void readsZeroAndRejectsInvalidValues() {
        ApplicationSetting entry = new ApplicationSetting();
        entry.setValue("0");
        when(repository.findById(anyString())).thenReturn(Optional.of(entry));
        assertThat(settings.rentalWindowDays()).isZero();
        for (String invalid : new String[]{"-1", "1.5", "abc", "2147483648", ""}) {
            assertThatThrownBy(() -> settings.put(ApplicationSettingsService.RENTAL_WINDOW_KEY, invalid))
                    .isInstanceOf(ValidationException.class).hasMessageContaining(invalid);
        }
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnsupportedKeys() {
        assertThatThrownBy(() -> settings.get("security.secret")).hasMessageContaining("security.secret");
        verifyNoInteractions(repository);
    }
}
