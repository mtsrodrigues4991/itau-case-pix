package com.itau.pix.pixkeys.exception;

/**
 * Recurso inexistente. Mapeada para HTTP 404 pelo {@link ApiExceptionHandler}.
 */
public class PixKeyNotFoundException extends RuntimeException {

    public PixKeyNotFoundException(String mensagem) {
        super(mensagem);
    }
}
