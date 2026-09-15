package com.itau.pix.pixkeys.validator;

import com.itau.pix.pixkeys.domain.KeyType;
import com.itau.pix.pixkeys.exception.PixValidationException;
import org.springframework.stereotype.Component;

/**
 * Celular no formato +<pais><DDD><numero>, ex.: +5511987654321.
 */
@Component
public class CelularValidator implements PixKeyValidator {

    /** pais (1-2) + DDD (2-3) + numero (9) = 12 a 14 digitos depois do '+'. */
    private static final int MIN_DIGITOS = 12;
    private static final int MAX_DIGITOS = 14;

    @Override
    public boolean supports(KeyType tipo) {
        return KeyType.CELULAR == tipo;
    }

    /**
     * Remove apenas mascara de digitacao (espaco, parenteses, hifen, ponto).
     * Letras sao preservadas de proposito, para que {@link #validate(String)} acuse "somente digitos".
     */
    @Override
    public String normalize(String valorBruto) {
        if (valorBruto == null) {
            return null;
        }
        return valorBruto.trim().replaceAll("[\\s()\\-.]", "");
    }

    @Override
    public void validate(String valorCanonico) {
        if (valorCanonico == null || valorCanonico.isBlank()) {
            throw new PixValidationException("O valor da chave do tipo celular e obrigatorio");
        }
        if (!valorCanonico.startsWith("+")) {
            throw new PixValidationException("Chave do tipo celular deve iniciar com '+' seguido do codigo do pais");
        }
        String digitos = valorCanonico.substring(1);
        if (digitos.isEmpty() || !digitos.chars().allMatch(Character::isDigit)) {
            throw new PixValidationException("Chave do tipo celular deve conter somente numeros apos o '+'");
        }
        if (digitos.length() < MIN_DIGITOS || digitos.length() > MAX_DIGITOS) {
            throw new PixValidationException("Chave do tipo celular deve ter codigo do pais (1 a 2 digitos), "
                    + "DDD (2 a 3 digitos) e numero com 9 digitos. Ex.: +5511987654321");
        }
    }
}
