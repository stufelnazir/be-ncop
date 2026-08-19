package com.ncop.auth.service;

import com.ncop.auth.dto.CreateRoleRequest;
import com.ncop.auth.dto.RoleResponse;
import com.ncop.auth.dto.UpdateRoleRequest;

import java.util.List;

public interface RoleService {

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse updateRole(String roleId, UpdateRoleRequest request);

    RoleResponse getRoleById(String roleId);

    List<RoleResponse> getAllRoles();

    com.ncop.common.dto.PageResponse<RoleResponse> getRoles(org.springframework.data.domain.Pageable pageable, String search);

    void deleteRole(String roleId);
}
