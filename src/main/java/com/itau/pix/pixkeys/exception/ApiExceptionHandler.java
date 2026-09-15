package com.itau.pix.pixkeys.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Ponto unico de traducao de excecao para resposta HTTP.
 * Regra do case: validacao/negocio = 422, recurso inexistente = 404, corpo sempre {"mensagem": "..."}.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PixKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PixKeyNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(PixValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidacao(PixValidationException e) {
        return unprocessable(e.getMessage());
    }

    /** Violacoes de Bean Validation nos DTOs caem no mesmo formato de 422. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return unprocessable(mensagem.isEmpty() ? "Requisicao invalida" : mensagem);
    }

    /** Ex.: id no path que nao e um UUID valido. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException e) {
        return unprocessable("Valor invalido para o parametro '" + e.getName() + "'");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCorpoInvalido(HttpMessageNotReadableException e) {
        return unprocessable("Corpo da requisicao ausente ou mal formatado");
    }

    /** Rede de seguranca da constraint UNIQUE, caso duas inclusoes concorram pelo mesmo valor. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridade(DataIntegrityViolationException e) {
        return unprocessable("Valor de chave PIX ja cadastrado");
    }

    private ResponseEntity<ErrorResponse> unprocessable(String mensagem) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(mensagem));
    }
}
