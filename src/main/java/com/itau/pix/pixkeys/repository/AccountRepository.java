package com.itau.pix.pixkeys.repository;

import com.itau.pix.pixkeys.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByNumeroAgenciaAndNumeroConta(String numeroAgencia, String numeroConta);
}
