package br.com.rentafit.auth.domain;

import br.com.rentafit.people.domain.Person;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
@Data
@Schema(description = "Security credentials and access control")
public class UserAccount implements UserDetails {

    @Id
    @Schema(description = "Same UUID as the associated Person")
    private UUID id;

    @Column(unique = true, nullable = false)
    @Schema(description = "Login username", example = "john.doe")
    private String username;

    @Column(nullable = false)
    @Schema(description = "Hashed password")
    private String password;

    @Column(length = 255)
    @Schema(description = "BCrypt hash of the 4-digit security PIN")
    private String pin;


    @Column(name = "password_changed_at")
    @Schema(description = "Timestamp of last password change")
    private java.time.OffsetDateTime passwordChangedAt;

    @Column(name = "is_active")
    @Schema(description = "Whether the account is active")
    private Boolean isActive = true;

    @Column(name = "failed_login_attempts", nullable = false)
    @Schema(description = "Consecutive failed password logins since the last success or lock")
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    @Schema(description = "Login is refused until this instant after too many failed attempts")
    private java.time.OffsetDateTime lockedUntil;

    @Column(name = "email_verification_pending", nullable = false)
    @Schema(description = "True while a self-registered account has not confirmed its e-mail")
    private boolean emailVerificationPending;

    @Column(name = "issuer_cnpj", length = 14)
    @Schema(description = "CNPJ do emitente vinculado ao usuário", example = "08299621000120")
    private String issuerCnpj;

    @Column(name = "ui_variant", length = 20, nullable = false)
    @Schema(description = "Variante de interface preferida pelo usuário", example = "simplified")
    private String uiVariant = "simplified";

    @Column(name = "sidebar_visible", nullable = false)
    @Schema(description = "Visibilidade da barra lateral no desktop", example = "true")
    private Boolean sidebarVisible = true;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    @Schema(description = "Associated person profile")
    private Person person;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles = new ArrayList<>();


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new ArrayList<>(this.roles);
    }

    public RoleName getRole() {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.get(0).getRole();
    }

    public boolean hasOperationalRole() {
        return roles != null && roles.stream().anyMatch(role -> role != null
                && (role.getRole() == RoleName.EMPLOYEE || role.getRole() == RoleName.MANAGER
                || role.getRole() == RoleName.ADMIN));
    }

    public UserAccount() { }

    public UserAccount(Person person, String username, String password) {
        this.username = username;
        this.password = password;
        this.person = person;
        this.id = person.getId();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLockedAt(java.time.OffsetDateTime.now());
    }

    public boolean isLockedAt(java.time.OffsetDateTime instant) {
        return lockedUntil != null && lockedUntil.isAfter(instant);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive) && !emailVerificationPending;
    }
}

