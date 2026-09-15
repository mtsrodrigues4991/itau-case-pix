package com.itau.pix.pixkeys.dto;

import com.itau.pix.pixkeys.domain.PixKey;

/**
 * Saida da inativacao (tabela 4 do enunciado): datas em DATETIME, com a data de inativacao.
 */
public record InactivatedPixKeyResponse(String id,
                                        String tipoChave,
                                        String valorChave,
                                        String tipoConta,
                                        String numeroAgencia,
                                        String numeroConta,
                                        String nomeCorrentista,
                                        String sobrenomeCorrentista,
                                        String dataHoraInclusao,
                                        String dataHoraInativacao) {

    public static InactivatedPixKeyResponse of(PixKey chave) {
        return new InactivatedPixKeyResponse(
                chave.getId().toString(),
                chave.getTipoChave().toApi(),
                chave.getValorChave(),
                chave.getTipoConta().toApi(),
                chave.getNumeroAgencia(),
                chave.getNumeroConta(),
                chave.getNomeCorrentista(),
                Formatos.texto(chave.getSobrenomeCorrentista()),
                Formatos.data(chave.getDataHoraInclusao(), Formatos.DATA_HORA),
                Formatos.data(chave.getDataHoraInativacao(), Formatos.DATA_HORA));
    }
}
