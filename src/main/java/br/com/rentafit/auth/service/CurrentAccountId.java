package br.com.rentafit.auth.service;

import br.com.rentafit.auth.domain.UserAccount;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CurrentAccountId {
    public UUID requireId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount account)) {
            throw new AccessDeniedException("Authenticated UserAccount required");
        }
        return account.getId();
    }
}
