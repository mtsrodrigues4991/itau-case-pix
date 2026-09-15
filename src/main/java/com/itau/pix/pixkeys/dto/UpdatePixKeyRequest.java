package com.itau.pix.pixkeys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada da alteracao (tabela 2 do enunciado). O id vem no path; tipoChave e valorChave
 * nao sao aceitos no payload porque o case os declara imutaveis.
 */
public record UpdatePixKeyRequest(

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
