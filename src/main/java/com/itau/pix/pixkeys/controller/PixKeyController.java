package com.itau.pix.pixkeys.controller;

import com.itau.pix.pixkeys.dto.CreatePixKeyRequest;
import com.itau.pix.pixkeys.dto.InactivatedPixKeyResponse;
import com.itau.pix.pixkeys.dto.PixKeyCreatedResponse;
import com.itau.pix.pixkeys.dto.PixKeyResponse;
import com.itau.pix.pixkeys.dto.PixKeySearchResponse;
import com.itau.pix.pixkeys.dto.UpdatePixKeyRequest;
import com.itau.pix.pixkeys.service.PixKeyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pix-keys")
public class PixKeyController {

    private final PixKeyService service;

    public PixKeyController(PixKeyService service) {
        this.service = service;
    }

    /** O case pede explicitamente HTTP 200 na inclusao (nao 201). */
    @PostMapping
    public PixKeyCreatedResponse incluir(@Valid @RequestBody CreatePixKeyRequest request) {
        return PixKeyCreatedResponse.of(service.incluir(request));
    }

    @PutMapping("/{id}")
    public PixKeyResponse alterar(@PathVariable UUID id, @Valid @RequestBody UpdatePixKeyRequest request) {
        return service.alterar(id, request);
    }

    @DeleteMapping("/{id}")
    public InactivatedPixKeyResponse inativar(@PathVariable UUID id) {
        return service.inativar(id);
    }

    @GetMapping("/{id}")
    public PixKeySearchResponse consultarPorId(@PathVariable UUID id) {
        return service.consultarPorId(id);
    }

    @GetMapping
    public List<PixKeySearchResponse> consultar(@RequestParam(required = false) String id,
                                                @RequestParam(required = false) String numeroAgencia,
                                                @RequestParam(required = false) String numeroConta,
                                                @RequestParam(required = false) String tipoChave) {
        return service.consultar(id, numeroAgencia, numeroConta, tipoChave);
    }
}
