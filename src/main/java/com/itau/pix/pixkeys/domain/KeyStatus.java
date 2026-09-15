package com.itau.pix.pixkeys.domain;

/**
 * Situacao da chave. A inativacao e um soft delete: o registro permanece no banco,
 * o que sustenta a regra de unicidade definitiva do valorChave.
 */
public enum KeyStatus {

    ACTIVE,
    INACTIVE
}
