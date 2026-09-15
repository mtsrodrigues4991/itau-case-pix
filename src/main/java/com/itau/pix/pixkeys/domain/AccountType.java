package com.itau.pix.pixkeys.domain;

import com.itau.pix.pixkeys.exception.PixValidationException;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Tipo da conta vinculada a chave.
 */
public enum AccountType {

    CORRENTE,
    POUPANCA;

    public String toApi() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Aceita "poupanca" e "poupanca" acentuado (o enunciado usa a forma com cedilha).
     */
    public static AccountType from(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new PixValidationException("O campo tipoConta e obrigatorio");
        }
        String semAcento = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        try {
            return valueOf(semAcento);
        } catch (IllegalArgumentException e) {
            throw new PixValidationException(
                    "Tipo de conta invalido: '" + valor + "'. Valores aceitos: corrente, poupanca");
        }
    }
}
