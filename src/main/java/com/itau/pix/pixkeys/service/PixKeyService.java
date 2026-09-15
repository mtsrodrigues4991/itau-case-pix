package com.itau.pix.pixkeys.service;

import com.itau.pix.pixkeys.domain.Account;
import com.itau.pix.pixkeys.domain.AccountType;
import com.itau.pix.pixkeys.domain.KeyStatus;
import com.itau.pix.pixkeys.domain.KeyType;
import com.itau.pix.pixkeys.domain.PixKey;
import com.itau.pix.pixkeys.dto.CreatePixKeyRequest;
import com.itau.pix.pixkeys.dto.InactivatedPixKeyResponse;
import com.itau.pix.pixkeys.dto.PixKeyResponse;
import com.itau.pix.pixkeys.dto.PixKeySearchResponse;
import com.itau.pix.pixkeys.dto.UpdatePixKeyRequest;
import com.itau.pix.pixkeys.exception.PixKeyNotFoundException;
import com.itau.pix.pixkeys.exception.PixValidationException;
import com.itau.pix.pixkeys.repository.AccountRepository;
import com.itau.pix.pixkeys.repository.PixKeyRepository;
import com.itau.pix.pixkeys.validator.PixKeyValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PixKeyService {

    private final PixKeyRepository pixKeyRepository;
    private final AccountRepository accountRepository;
    private final List<PixKeyValidator> validators;

    public PixKeyService(PixKeyRepository pixKeyRepository,
                         AccountRepository accountRepository,
                         List<PixKeyValidator> validators) {
        this.pixKeyRepository = pixKeyRepository;
        this.accountRepository = accountRepository;
        this.validators = validators;
    }

    @Transactional
    public UUID incluir(CreatePixKeyRequest request) {
        KeyType tipoChave = KeyType.from(request.tipoChave());
        AccountType tipoConta = AccountType.from(request.tipoConta());

        PixKeyValidator validator = validatorDe(tipoChave);
        String valorCanonico = validator.normalize(request.valorChave());
        validator.validate(valorCanonico);

        // Unicidade definitiva: vale mesmo se a chave existente estiver inativa.
        if (pixKeyRepository.existsByValorChave(valorCanonico)) {
            throw new PixValidationException("Ja existe chave PIX cadastrada com o valor '" + valorCanonico + "'");
        }

        Account conta = accountRepository
                .findByNumeroAgenciaAndNumeroConta(request.numeroAgencia(), request.numeroConta())
                .orElseThrow(() -> new PixValidationException("Conta nao encontrada para agencia "
                        + request.numeroAgencia() + " e conta " + request.numeroConta()));

        long chavesDaConta = pixKeyRepository
                .countByNumeroAgenciaAndNumeroConta(request.numeroAgencia(), request.numeroConta());
        if (chavesDaConta >= conta.limiteDeChaves()) {
            throw new PixValidationException("Limite de " + conta.limiteDeChaves() + " chaves atingido para a conta "
                    + request.numeroAgencia() + "/" + request.numeroConta()
                    + " (" + conta.getTipoPessoa() + ")");
        }

        PixKey chave = new PixKey(tipoChave,
                valorCanonico,
                tipoConta,
                request.numeroAgencia(),
                request.numeroConta(),
                request.nomeCorrentista(),
                request.sobrenomeCorrentista());

        return pixKeyRepository.save(chave).getId();
    }

    @Transactional
    public PixKeyResponse alterar(UUID id, UpdatePixKeyRequest request) {
        PixKey chave = buscarObrigatoria(id);
        if (chave.isInativa()) {
            throw new PixValidationException("Chave PIX inativa nao pode ser alterada");
        }

        chave.alterarVinculo(AccountType.from(request.tipoConta()),
                request.numeroAgencia(),
                request.numeroConta(),
                request.nomeCorrentista(),
                request.sobrenomeCorrentista());

        return PixKeyResponse.of(pixKeyRepository.save(chave));
    }

    @Transactional
    public InactivatedPixKeyResponse inativar(UUID id) {
        PixKey chave = buscarObrigatoria(id);
        if (chave.isInativa()) {
            throw new PixValidationException("Chave PIX ja foi desativada em "
                    + chave.getDataHoraInativacao());
        }

        chave.inativar();
        return InactivatedPixKeyResponse.of(pixKeyRepository.save(chave));
    }

    /** Consulta por ID. Chave inativa nao pode ser consultada, logo responde 404. */
    @Transactional(readOnly = true)
    public PixKeySearchResponse consultarPorId(UUID id) {
        PixKey chave = buscarObrigatoria(id);
        if (chave.isInativa()) {
            throw new PixKeyNotFoundException("Chave PIX nao encontrada para o id " + id);
        }
        return PixKeySearchResponse.of(chave);
    }

    /**
     * Consulta por filtros combinaveis: agencia+conta e/ou tipo de chave.
     * O id nao e aceito aqui — o case proibe combina-lo com outros filtros e ha endpoint proprio para ele.
     */
    @Transactional(readOnly = true)
    public List<PixKeySearchResponse> consultar(String id,
                                                String numeroAgencia,
                                                String numeroConta,
                                                String tipoChave) {
        if (id != null && !id.isBlank()) {
            throw new PixValidationException(
                    "A consulta por id nao aceita outros filtros. Utilize GET /api/v1/pix-keys/{id}");
        }

        boolean temAgencia = numeroAgencia != null && !numeroAgencia.isBlank();
        boolean temConta = numeroConta != null && !numeroConta.isBlank();
        if (temAgencia != temConta) {
            throw new PixValidationException("Os filtros numeroAgencia e numeroConta devem ser informados juntos");
        }

        KeyType tipo = (tipoChave == null || tipoChave.isBlank()) ? null : KeyType.from(tipoChave);
        if (!temAgencia && tipo == null) {
            throw new PixValidationException(
                    "Informe ao menos um filtro: numeroAgencia + numeroConta e/ou tipoChave");
        }

        List<PixKey> encontradas = buscar(temAgencia, numeroAgencia, numeroConta, tipo);
        if (encontradas.isEmpty()) {
            throw new PixKeyNotFoundException("Nenhuma chave PIX encontrada para os filtros informados");
        }

        return encontradas.stream().map(PixKeySearchResponse::of).toList();
    }

    private List<PixKey> buscar(boolean porConta, String numeroAgencia, String numeroConta, KeyType tipo) {
        if (porConta && tipo != null) {
            return pixKeyRepository.findByStatusAndNumeroAgenciaAndNumeroContaAndTipoChave(
                    KeyStatus.ACTIVE, numeroAgencia, numeroConta, tipo);
        }
        if (porConta) {
            return pixKeyRepository.findByStatusAndNumeroAgenciaAndNumeroConta(
                    KeyStatus.ACTIVE, numeroAgencia, numeroConta);
        }
        return pixKeyRepository.findByStatusAndTipoChave(KeyStatus.ACTIVE, tipo);
    }

    private PixKey buscarObrigatoria(UUID id) {
        return pixKeyRepository.findById(id)
                .orElseThrow(() -> new PixKeyNotFoundException("Chave PIX nao encontrada para o id " + id));
    }

    /** Selecao da Strategy: quem sabe validar o tipo e o proprio validator, nao o service. */
    private PixKeyValidator validatorDe(KeyType tipo) {
        return validators.stream()
                .filter(validator -> validator.supports(tipo))
                .findFirst()
                .orElseThrow(() -> new PixValidationException(
                        "Nao ha validador implementado para o tipo de chave " + tipo.toApi()));
    }
}
