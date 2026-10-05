package br.com.rentafit.settings.controller;

import br.com.rentafit.settings.dto.SettingEntry;
import br.com.rentafit.settings.dto.SettingValueRequest;
import br.com.rentafit.settings.service.ApplicationSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class ApplicationSettingsController {
    private final ApplicationSettingsService settings;

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public SettingEntry get(@PathVariable String key) {
        return settings.get(key);
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void put(@PathVariable String key, @Valid @RequestBody SettingValueRequest request) {
        settings.put(key, request.value());
    }
}
