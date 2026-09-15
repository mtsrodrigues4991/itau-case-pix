package com.itau.pix.pixkeys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada da inclusao (tabela 1 do enunciado).
 * Aqui ficam apenas as regras genericas de campo (Bean Validation);
 * as regras por tipo de chave ficam nos {@link com.itau.pix.pixkeys.validator.PixKeyValidator}.
 */
public record CreatePixKeyRequest(

        @NotBlank(message = "tipoChave e obrigatorio")
        @Size(max = 9, message = "tipoChave deve ter no maximo 9 caracteres")
        String tipoChave,

        @NotBlank(message = "valorChave e obrigatorio")
        @Size(max = 77, message = "valorChave deve ter no maximo 77 caracteres")
        String valorChave,

        @NotBlank(message = "tipoConta e obrigatorio")
        @Size(max = 10, message = "tipoConta deve ter no maximo 10 caracteres")
        String tipoConta,

        @NotBlank(message = "numeroAgencia e obrigatorio")
        @Pattern(regexp = "\\d{1,4}", message = "numeroAgencia deve ser numerico com no maximo 4 digitos")
        String numeroAgencia,

        @NotBlank(message = "numeroConta e obrigatorio")
        @Pattern(regexp = "\\d{1,8}", message = "numeroConta deve ser numerico com no maximo 8 digitos")
        String numeroConta,

        @NotBlank(message = "nomeCorrentista e obrigatorio")
        @Size(max = 30, message = "nomeCorrentista deve ter no maximo 30 caracteres")
        String nomeCorrentista,

        @Size(max = 45, message = "sobrenomeCorrentista deve ter no maximo 45 caracteres")
        String sobrenomeCorrentista) {
}
