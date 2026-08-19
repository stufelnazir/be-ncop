package com.ncop.auth.controller;

import com.ncop.auth.dto.CreateRoleRequest;
import com.ncop.auth.dto.RoleResponse;
import com.ncop.auth.dto.UpdateRoleRequest;
import com.ncop.auth.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<com.ncop.common.dto.PageResponse<RoleResponse>> getRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(roleService.getRoles(pageable, search));
    }

    @GetMapping("/all")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable String id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable String id,
                                                    @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
