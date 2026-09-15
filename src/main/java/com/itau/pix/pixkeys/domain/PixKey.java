package com.itau.pix.pixkeys.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Chave PIX registrada. O {@code valorChave} guardado e sempre o valor canonico
 * produzido pelo validator do tipo, e possui UNIQUE no banco.
 */
@Entity
@Table(name = "pix_key", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pix_key_valor_chave", columnNames = "valor_chave")
})
public class PixKey {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_chave", nullable = false, updatable = false, length = 10)
    private KeyType tipoChave;

    @Column(name = "valor_chave", nullable = false, updatable = false, length = 77)
    private String valorChave;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta", nullable = false, length = 10)
    private AccountType tipoConta;

    @Column(name = "numero_agencia", nullable = false, length = 4)
    private String numeroAgencia;

    @Column(name = "numero_conta", nullable = false, length = 8)
    private String numeroConta;

    @Column(name = "nome_correntista", nullable = false, length = 30)
    private String nomeCorrentista;

    @Column(name = "sobrenome_correntista", length = 45)
    private String sobrenomeCorrentista;

    @Column(name = "data_hora_inclusao", nullable = false, updatable = false)
    private LocalDateTime dataHoraInclusao;

    @Column(name = "data_hora_inativacao")
    private LocalDateTime dataHoraInativacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private KeyStatus status;

    protected PixKey() {
        // exigido pelo JPA
    }

    public PixKey(KeyType tipoChave,
                  String valorChave,
                  AccountType tipoConta,
                  String numeroAgencia,
                  String numeroConta,
                  String nomeCorrentista,
                  String sobrenomeCorrentista) {
        this.id = UUID.randomUUID();
        this.tipoChave = tipoChave;
        this.valorChave = valorChave;
        this.tipoConta = tipoConta;
        this.numeroAgencia = numeroAgencia;
        this.numeroConta = numeroConta;
        this.nomeCorrentista = nomeCorrentista;
        this.sobrenomeCorrentista = sobrenomeCorrentista;
        this.dataHoraInclusao = agora();
        this.status = KeyStatus.ACTIVE;
    }

    /**
     * Alteracao permitida pelo case: apenas o vinculo (conta/titular).
     * Id, tipo e valor da chave sao imutaveis.
     */
    public void alterarVinculo(AccountType tipoConta,
                               String numeroAgencia,
                               String numeroConta,
                               String nomeCorrentista,
                               String sobrenomeCorrentista) {
        this.tipoConta = tipoConta;
        this.numeroAgencia = numeroAgencia;
        this.numeroConta = numeroConta;
        this.nomeCorrentista = nomeCorrentista;
        this.sobrenomeCorrentista = sobrenomeCorrentista;
    }

    public void inativar() {
        this.status = KeyStatus.INACTIVE;
        this.dataHoraInativacao = agora();
    }

    /** Precisao de segundo: o case trabalha com DATETIME, fracao de segundo so poluiria a resposta. */
    private static LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public boolean isInativa() {
        return status == KeyStatus.INACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public KeyType getTipoChave() {
        return tipoChave;
    }

    public String getValorChave() {
        return valorChave;
    }

    public AccountType getTipoConta() {
        return tipoConta;
    }

    public String getNumeroAgencia() {
        return numeroAgencia;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public String getNomeCorrentista() {
        return nomeCorrentista;
    }

    public String getSobrenomeCorrentista() {
        return sobrenomeCorrentista;
    }

    public LocalDateTime getDataHoraInclusao() {
        return dataHoraInclusao;
    }

    public LocalDateTime getDataHoraInativacao() {
        return dataHoraInativacao;
    }

    public KeyStatus getStatus() {
        return status;
    }
}
