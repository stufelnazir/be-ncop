package com.ncop.auth.service;

import com.ncop.auth.Role;
import com.ncop.auth.RoleRepository;
import com.ncop.auth.dto.CreateRoleRequest;
import com.ncop.auth.dto.RoleResponse;
import com.ncop.auth.dto.UpdateRoleRequest;
import com.ncop.auth.exception.DuplicateResourceException;
import com.ncop.auth.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Role with name '" + request.name() + "' already exists");
        }

        Role role = new Role();
        role.setName(request.name());
        role.setModuleRights(request.moduleRights() != null ? request.moduleRights() : new ArrayList<>());

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Override
    public RoleResponse updateRole(String roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (request.name() != null && !request.name().equals(role.getName())) {
            if (roleRepository.existsByName(request.name())) {
                throw new DuplicateResourceException("Role with name '" + request.name() + "' already exists");
            }
            role.setName(request.name());
        }

        if (request.moduleRights() != null) {
            role.setModuleRights(request.moduleRights());
        }

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
        roleRepository.deleteById(roleId);
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getRoleId(), role.getName(), role.getModuleRights());
    }
}
