package com.itau.pix.pixkeys.repository;

import com.itau.pix.pixkeys.domain.KeyStatus;
import com.itau.pix.pixkeys.domain.KeyType;
import com.itau.pix.pixkeys.domain.PixKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {

    /** Unicidade definitiva: vale inclusive para chaves ja inativadas. */
    boolean existsByValorChave(String valorChave);

    /** Limite por conta: conta todas as chaves, ativas ou nao (coerente com a unicidade definitiva). */
    long countByNumeroAgenciaAndNumeroConta(String numeroAgencia, String numeroConta);

    List<PixKey> findByStatusAndNumeroAgenciaAndNumeroConta(KeyStatus status,
                                                            String numeroAgencia,
                                                            String numeroConta);

    List<PixKey> findByStatusAndTipoChave(KeyStatus status, KeyType tipoChave);

    List<PixKey> findByStatusAndNumeroAgenciaAndNumeroContaAndTipoChave(KeyStatus status,
                                                                        String numeroAgencia,
                                                                        String numeroConta,
                                                                        KeyType tipoChave);
}
