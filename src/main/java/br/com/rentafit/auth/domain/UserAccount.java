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

    @Column(length = 4)
    @Schema(description = "4-digit security PIN", example = "1234")
    private String pin;


    @Column(name = "password_changed_at")
    @Schema(description = "Timestamp of last password change")
    private java.time.OffsetDateTime passwordChangedAt;

    @Column(name = "is_active")
    @Schema(description = "Whether the account is active")
    private Boolean isActive = true;

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

    public UserAccount() { }

    public UserAccount(Person person, String password) {
        this.username = person.getEmail();
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}

