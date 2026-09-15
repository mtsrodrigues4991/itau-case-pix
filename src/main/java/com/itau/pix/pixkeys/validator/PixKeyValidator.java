package com.itau.pix.pixkeys.validator;

import com.itau.pix.pixkeys.domain.KeyType;

/**
 * Strategy de validacao por tipo de chave.
 *
 * O Spring injeta todas as implementacoes como {@code List<PixKeyValidator>} e o service escolhe
 * a estrategia por {@link #supports(KeyType)} — nunca por if/else sobre o tipo. Incluir um novo
 * tipo de chave (CPF, aleatoria) e criar uma nova classe, sem tocar no service.
 */
public interface PixKeyValidator {

    boolean supports(KeyType tipo);

    /** Devolve o valor canonico: o unico formato que e validado, gravado e comparado por unicidade. */
    String normalize(String valorBruto);

    /** Lanca PixValidationException se o valor canonico nao atender as regras do tipo. */
    void validate(String valorCanonico);
}
