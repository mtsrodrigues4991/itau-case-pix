package com.itau.pix.pixkeys.validator;

import com.itau.pix.pixkeys.domain.KeyType;
import com.itau.pix.pixkeys.exception.PixValidationException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * E-mail: contem '@', formato alfanumerico padrao, maximo de 77 caracteres.
 */
@Component
public class EmailValidator implements PixKeyValidator {

    private static final int MAX_CARACTERES = 77;
    private static final Pattern FORMATO = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    public boolean supports(KeyType tipo) {
        return KeyType.EMAIL == tipo;
    }

    /** E-mail e case-insensitive: o canonico e minusculo, o que tambem torna a unicidade case-insensitive. */
    @Override
    public String normalize(String valorBruto) {
        if (valorBruto == null) {
            return null;
        }
        return valorBruto.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void validate(String valorCanonico) {
        if (valorCanonico == null || valorCanonico.isBlank()) {
            throw new PixValidationException("O valor da chave do tipo email e obrigatorio");
        }
        if (!valorCanonico.contains("@")) {
            throw new PixValidationException("Chave do tipo email deve conter '@'");
        }
        if (valorCanonico.length() > MAX_CARACTERES) {
            throw new PixValidationException("Chave do tipo email deve ter no maximo " + MAX_CARACTERES + " caracteres");
        }
        if (!FORMATO.matcher(valorCanonico).matches()) {
            throw new PixValidationException("Chave do tipo email possui formato invalido. Ex.: maria.silva@itau.com.br");
        }
    }
}
