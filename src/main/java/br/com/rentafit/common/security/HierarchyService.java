package br.com.rentafit.common.security;

import br.com.rentafit.auth.domain.UserAccount;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HierarchyService {
    private final RoleHierarchy roleHierarchy;

    public HierarchyService(RoleHierarchy roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    public boolean userNotAllowedTo(UserAccount user, UserAccount autor, String loggedInRole) {
        for(GrantedAuthority authority: user.getAuthorities()){
            var reachableGrantedAuthorities =  roleHierarchy.getReachableGrantedAuthorities(List.of(authority));

            for(GrantedAuthority role: reachableGrantedAuthorities){
                if(role.getAuthority().equals(loggedInRole) || user.getId().equals(autor.getId()))
                    return false;
            }
        }
        return true;
    }
}
