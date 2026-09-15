package com.itau.pix.pixkeys.exception;

/**
 * Violacao de regra de validacao ou de negocio. Mapeada para HTTP 422 pelo {@link ApiExceptionHandler}.
 */
public class PixValidationException extends RuntimeException {

    public PixValidationException(String mensagem) {
        super(mensagem);
    }
}
