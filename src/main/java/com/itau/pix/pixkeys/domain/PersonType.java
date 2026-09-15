package com.itau.pix.pixkeys.domain;

/**
 * Natureza do titular da conta. Define o limite de chaves PIX (5 para PF, 20 para PJ).
 */
public enum PersonType {

    PF(5),
    PJ(20);

    private final int limiteDeChaves;

    PersonType(int limiteDeChaves) {
        this.limiteDeChaves = limiteDeChaves;
    }

    public int limiteDeChaves() {
        return limiteDeChaves;
    }
}
