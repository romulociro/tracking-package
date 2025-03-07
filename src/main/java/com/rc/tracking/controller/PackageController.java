package com.rc.tracking.controller;

import com.rc.tracking.model.dto.PackageDetailResponse;
import com.rc.tracking.model.dto.PackageRequest;
import com.rc.tracking.model.dto.PackageResponse;
import com.rc.tracking.model.enums.StatusEnum;
import com.rc.tracking.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    /**
     * Endpoint para criação de um novo pacote.
     * Retorna 201 Created com o cabeçalho Location apontando para o recurso criado.
     */
    @PostMapping
    public ResponseEntity<PackageResponse> createPackage(@Valid @RequestBody PackageRequest request) {
        PackageResponse response = packageService.createPackage(request);
        URI location = URI.create("/api/packages/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para atualizar o status de um pacote.
     * A transição de status deve seguir as regras de negócio (por exemplo, CREATED -> IN_TRANSIT -> DELIVERED).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<PackageResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam("status") StatusEnum newStatus) {
        PackageResponse response = packageService.updateStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para cancelar um pacote.
     * O cancelamento é permitido apenas se o pacote estiver no status CREATED.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<PackageResponse> cancelPackage(@PathVariable Long id) {
        PackageResponse response = packageService.cancelPackage(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obter os detalhes de um pacote.
     * Pode incluir ou não os eventos de rastreamento, conforme parâmetro.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PackageDetailResponse> getPackageDetails(
            @PathVariable Long id,
            @RequestParam(name = "includeEvents", defaultValue = "true") boolean includeEvents) {
        PackageDetailResponse response = packageService.getPackageDetails(id, includeEvents);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para listar pacotes com filtros opcionais de sender e recipient.
     */
    @GetMapping
    public ResponseEntity<List<PackageResponse>> listPackages(
            @RequestParam(required = false) String sender,
            @RequestParam(required = false) String recipient) {
        List<PackageResponse> responses = packageService.listPackages(sender, recipient);
        return ResponseEntity.ok(responses);
    }
}
