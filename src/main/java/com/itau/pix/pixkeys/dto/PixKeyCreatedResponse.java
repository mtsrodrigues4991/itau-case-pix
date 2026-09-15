package com.itau.pix.pixkeys.dto;

import java.util.UUID;

/**
 * Saida da inclusao: o case pede apenas o id gerado, com HTTP 200.
 */
public record PixKeyCreatedResponse(String id) {

    public static PixKeyCreatedResponse of(UUID id) {
        return new PixKeyCreatedResponse(id.toString());
    }
}
