package com.itau.pix.pixkeys.dto;

import com.itau.pix.pixkeys.domain.PixKey;

/**
 * Saida das consultas (tabela 5 do enunciado): datas em dd/MM/aaaa e nulos como string vazia.
 */
public record PixKeySearchResponse(String id,
                                   String tipoChave,
                                   String valorChave,
                                   String tipoConta,
                                   String numeroAgencia,
                                   String numeroConta,
                                   String nomeCorrentista,
                                   String sobrenomeCorrentista,
                                   String dataHoraInclusao,
                                   String dataHoraInativacao) {

    public static PixKeySearchResponse of(PixKey chave) {
        return new PixKeySearchResponse(
                chave.getId().toString(),
                chave.getTipoChave().toApi(),
                chave.getValorChave(),
                chave.getTipoConta().toApi(),
                chave.getNumeroAgencia(),
                chave.getNumeroConta(),
                chave.getNomeCorrentista(),
                Formatos.texto(chave.getSobrenomeCorrentista()),
                Formatos.data(chave.getDataHoraInclusao(), Formatos.DATA),
                Formatos.data(chave.getDataHoraInativacao(), Formatos.DATA));
    }
}
