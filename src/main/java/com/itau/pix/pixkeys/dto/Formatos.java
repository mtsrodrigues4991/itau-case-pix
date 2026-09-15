package com.itau.pix.pixkeys.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * O enunciado pede formatos de data diferentes por operacao: DATETIME na alteracao/inativacao
 * e DATE (dd/MM/aaaa) na consulta. Campos nulos sao devolvidos como string vazia.
 */
final class Formatos {

    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatos() {
    }

    static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    static String data(LocalDateTime valor, DateTimeFormatter formatter) {
        return valor == null ? "" : valor.format(formatter);
    }
}
