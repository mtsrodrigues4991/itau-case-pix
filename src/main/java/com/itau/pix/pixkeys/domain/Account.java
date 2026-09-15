package com.itau.pix.pixkeys.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Conta conhecida pelo banco. Existe para responder uma pergunta que o payload do case nao responde:
 * a conta e PF ou PJ? Dessa resposta sai o limite de chaves (5 ou 20).
 *
 * Nao ha relacionamento JPA com {@link PixKey}: a ligacao e por lookup de agencia + conta.
 */
@Entity
@Table(name = "account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_agencia_conta", columnNames = {"numero_agencia", "numero_conta"})
})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "numero_agencia", nullable = false, length = 4)
    private String numeroAgencia;

    @Column(name = "numero_conta", nullable = false, length = 8)
    private String numeroConta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 2)
    private PersonType tipoPessoa;

    protected Account() {
        // exigido pelo JPA
    }

    public Account(String numeroAgencia, String numeroConta, PersonType tipoPessoa) {
        this.numeroAgencia = numeroAgencia;
        this.numeroConta = numeroConta;
        this.tipoPessoa = tipoPessoa;
    }

    public int limiteDeChaves() {
        return tipoPessoa.limiteDeChaves();
    }

    public Long getId() {
        return id;
    }

    public String getNumeroAgencia() {
        return numeroAgencia;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public PersonType getTipoPessoa() {
        return tipoPessoa;
    }
}
