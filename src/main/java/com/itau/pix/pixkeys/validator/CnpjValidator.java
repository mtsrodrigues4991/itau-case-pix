package com.itau.pix.pixkeys.validator;

import com.itau.pix.pixkeys.domain.KeyType;
import com.itau.pix.pixkeys.exception.PixValidationException;
import org.springframework.stereotype.Component;

/**
 * CNPJ: 14 digitos com os dois digitos verificadores validados pelo modulo 11.
 */
@Component
public class CnpjValidator implements PixKeyValidator {

    private static final int TAMANHO = 14;
    private static final int[] PESOS_PRIMEIRO_DV = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO_DV = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean supports(KeyType tipo) {
        return KeyType.CNPJ == tipo;
    }

    /** Remove a pontuacao usual do CNPJ; letras sao preservadas para que validate() as acuse. */
    @Override
    public String normalize(String valorBruto) {
        if (valorBruto == null) {
            return null;
        }
        return valorBruto.trim().replaceAll("[\\s./\\-]", "");
    }

    @Override
    public void validate(String valorCanonico) {
        if (valorCanonico == null || valorCanonico.isBlank()) {
            throw new PixValidationException("O valor da chave do tipo cnpj e obrigatorio");
        }
        if (!valorCanonico.chars().allMatch(Character::isDigit)) {
            throw new PixValidationException("Chave do tipo cnpj deve conter somente numeros");
        }
        if (valorCanonico.length() != TAMANHO) {
            throw new PixValidationException("Chave do tipo cnpj deve ter exatamente " + TAMANHO + " digitos");
        }
        if (todosDigitosIguais(valorCanonico) || !digitosVerificadoresConferem(valorCanonico)) {
            throw new PixValidationException("Chave do tipo cnpj possui digitos verificadores invalidos");
        }
    }

    private boolean todosDigitosIguais(String cnpj) {
        return cnpj.chars().distinct().count() == 1;
    }

    private boolean digitosVerificadoresConferem(String cnpj) {
        int primeiroDv = calcularDigito(cnpj, PESOS_PRIMEIRO_DV);
        int segundoDv = calcularDigito(cnpj, PESOS_SEGUNDO_DV);
        return primeiroDv == Character.getNumericValue(cnpj.charAt(12))
                && segundoDv == Character.getNumericValue(cnpj.charAt(13));
    }

    private int calcularDigito(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
