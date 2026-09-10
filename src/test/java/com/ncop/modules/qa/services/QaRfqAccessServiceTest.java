package com.ncop.modules.qa.services;

import com.ncop.auth.model.User;
import com.ncop.auth.model.Role;
import com.ncop.auth.repository.UserRepository;
import com.ncop.auth.repository.RoleRepository;
import com.ncop.modules.qa.entity.QaRfq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QaRfqAccessServiceTest {
    @AfterEach
    void clearSession() { SecurityContextHolder.clearContext(); }

    @Test
    void qaAndQcSeeOnlyTheirAssignmentsWhileAdminSeesAll() {
        for (String roleName : List.of("QA", "QC", "ADMIN")) {
            var users = mock(UserRepository.class);
            var roles = mock(RoleRepository.class);
            User user = new User();
            user.setId("reviewer");
            user.setRoleIds(List.of("role"));
            when(users.findByEmail("reviewer@example.com")).thenReturn(Optional.of(user));
            when(roles.findAllById(List.of("role"))).thenReturn(List.of(new Role(roleName)));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("reviewer@example.com", "", List.of()));
            var filter = new QaRfqAccessService(users, roles).currentUserFilter();
            QaRfq mine = new QaRfq();
            mine.setAssignedToId("reviewer");
            QaRfq other = new QaRfq();
            other.setAssignedToId("someone-else");
            assertTrue(filter.test(mine));
            assertEquals("ADMIN".equals(roleName), filter.test(other));
            assertEquals("ADMIN".equals(roleName), filter.test(new QaRfq()));
        }
    }

    @Test
    void missingSessionSeesNothing() {
        var service = new QaRfqAccessService(mock(UserRepository.class), mock(RoleRepository.class));
        assertFalse(service.currentUserFilter().test(new QaRfq()));
    }
}
