package com.itau.pix.pixkeys.domain;

import com.itau.pix.pixkeys.exception.PixValidationException;

import java.util.Locale;

/**
 * Tipos de chave implementados neste modulo (o case exige no minimo tres dos cinco).
 */
public enum KeyType {

    CELULAR,
    EMAIL,
    CNPJ;

    /**
     * A API troca o tipo em caixa baixa ("celular"); a persistencia usa o nome do enum.
     */
    public String toApi() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static KeyType from(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new PixValidationException("O campo tipoChave e obrigatorio");
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PixValidationException(
                    "Tipo de chave invalido: '" + valor + "'. Valores aceitos: celular, email, cnpj");
        }
    }
}
