package com.patitas.auth_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.patitas.auth_service.model.Rol;
import com.patitas.auth_service.service.RolService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Roles del sistema")
public class RolController {

    private final RolService rolService;

    @Operation(summary = "Listar roles", description = "Público. ADMIN, RECEPCIONISTA y CLIENTE.")
    @ApiResponse(responseCode = "200", description = "Lista de roles",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = Rol.class))))
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<Rol>> listar() {
        return ResponseEntity.ok(rolService.listarRoles());
    }
}
