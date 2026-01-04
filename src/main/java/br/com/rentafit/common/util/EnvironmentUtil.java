package br.com.rentafit.common.util;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Utility class to check application environment globally.
 * Uses Spring Environment to detect production profile.
 */
@Component
public class EnvironmentUtil {

    private static Environment environment;

    public EnvironmentUtil(Environment environment) {
        EnvironmentUtil.environment = environment;
    }

    /**
     * Checks if the application is running in production environment.
     * Considers profiles: prod, production
     *
     * @return true if running in production, false otherwise
     */
    public static boolean isProduction() {
        if (environment == null) {
            // Fallback para quando Spring context não está disponível
            String profile = System.getProperty("spring.profiles.active");
            if (profile == null) {
                profile = System.getenv("SPRING_PROFILES_ACTIVE");
            }
            return profile != null && (profile.contains("prod") || profile.contains("production"));
        }

        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod") ||
                                   profile.equalsIgnoreCase("production"));
    }

    /**
     * Checks if the application is running in test environment.
     *
     * @return true if running in test, false otherwise
     */
    public static boolean isTest() {
        if (environment == null) {
            return false;
        }

        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("test"));
    }

    /**
     * Gets the active profile name.
     *
     * @return active profile or "default" if none specified
     */
    public static String getActiveProfile() {
        if (environment == null) {
            return "default";
        }

        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? profiles[0] : "default";
    }
}

