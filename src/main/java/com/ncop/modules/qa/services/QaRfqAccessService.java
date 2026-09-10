package com.ncop.modules.qa.services;

import com.ncop.auth.repository.RoleRepository;
import com.ncop.auth.repository.UserRepository;
import com.ncop.modules.qa.entity.QaRfq;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class QaRfqAccessService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public Predicate<QaRfq> currentUserFilter() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return rfq -> false;
        var user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return rfq -> false;
        boolean admin = user.getRoleIds() != null && roleRepository.findAllById(user.getRoleIds()).stream()
                .anyMatch(role -> role.isActive() && ("ADMIN".equalsIgnoreCase(role.getName())
                        || "SUPER_ADMIN".equalsIgnoreCase(role.getName())));
        return rfq -> admin || (user.getId() != null && user.getId().equals(rfq.getAssignedToId()));
    }
}
