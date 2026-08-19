package com.ncop.auth.service;

import com.ncop.auth.model.Role;
import com.ncop.auth.repository.RoleRepository;
import com.ncop.auth.model.User;
import com.ncop.auth.repository.UserRepository;
import com.ncop.auth.dto.CreateRoleRequest;
import com.ncop.auth.dto.RoleResponse;
import com.ncop.auth.dto.UpdateRoleRequest;
import com.ncop.auth.exception.DuplicateResourceException;
import com.ncop.auth.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleServiceImpl(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        String trimmedName = request.name().trim();
        if (roleRepository.existsByName(trimmedName)) {
            throw new DuplicateResourceException("Role with name '" + trimmedName + "' already exists");
        }

        Role role = new Role();
        role.setName(trimmedName);
        role.setDescription(request.description());
        role.setActive(request.active() != null ? request.active() : true);
        role.setModuleRights(new ArrayList<>());
        role.setCreatedOn(Instant.now());
        role.setLastUpdatedOn(Instant.now());

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Override
    public RoleResponse updateRole(String roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (request.name() != null && !request.name().trim().equalsIgnoreCase(role.getName())) {
            String trimmedName = request.name().trim();
            if (roleRepository.existsByName(trimmedName)) {
                throw new DuplicateResourceException("Role with name '" + trimmedName + "' already exists");
            }
            role.setName(trimmedName);
        }

        if (request.description() != null) {
            role.setDescription(request.description());
        }

        if (request.active() != null) {
            role.setActive(request.active());
        }

        role.setLastUpdatedOn(Instant.now());
        Role saved = roleRepository.save(role);

        return toResponse(saved);
    }

    @Override
    public RoleResponse getRoleById(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        return toResponse(role);
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deleteRole(String roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role not found with id: " + roleId);
        }

        // Remove roleId from any user that has it
        List<User> usersWithRole = userRepository.findAll().stream()
                .filter(u -> u.getRoleIds() != null && u.getRoleIds().contains(roleId))
                .toList();

        for (User user : usersWithRole) {
            user.getRoleIds().remove(roleId);
            userRepository.save(user);
        }

        roleRepository.deleteById(roleId);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private RoleResponse toResponse(Role role) {
        long userCount = userRepository.findAll().stream()
                .filter(u -> u.getRoleIds() != null && u.getRoleIds().contains(role.getRoleId()))
                .count();

        return new RoleResponse(
                role.getRoleId(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                role.getModuleRights() != null ? role.getModuleRights() : List.of(),
                userCount,
                role.getCreatedOn(),
                role.getLastUpdatedOn()
        );
    }
}
