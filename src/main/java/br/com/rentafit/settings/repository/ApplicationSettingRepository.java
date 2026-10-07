package br.com.rentafit.settings.repository;

import br.com.rentafit.settings.domain.ApplicationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingRepository extends JpaRepository<ApplicationSetting, String> {}
