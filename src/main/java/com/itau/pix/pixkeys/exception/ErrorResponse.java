package com.itau.pix.pixkeys.exception;

/**
 * Corpo unico de erro da API: {"mensagem": "..."}.
 */
public record ErrorResponse(String mensagem) {
}
