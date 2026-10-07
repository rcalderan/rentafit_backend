package br.com.rentafit.settings.dto;

import jakarta.validation.constraints.NotNull;

public record SettingValueRequest(@NotNull String value) {}
