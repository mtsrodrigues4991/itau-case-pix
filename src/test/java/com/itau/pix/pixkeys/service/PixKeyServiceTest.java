package com.itau.pix.pixkeys.service;

import com.itau.pix.pixkeys.domain.Account;
import com.itau.pix.pixkeys.domain.AccountType;
import com.itau.pix.pixkeys.domain.KeyStatus;
import com.itau.pix.pixkeys.domain.KeyType;
import com.itau.pix.pixkeys.domain.PersonType;
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
import com.itau.pix.pixkeys.validator.CelularValidator;
import com.itau.pix.pixkeys.validator.CnpjValidator;
import com.itau.pix.pixkeys.validator.EmailValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceTest {

    private static final String AGENCIA = "1234";
    private static final String CONTA = "12345678";

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private AccountRepository accountRepository;

    private PixKeyService service;

    @BeforeEach
    void setUp() {
        // validators reais: sao puros e o teste exercita tambem a selecao da Strategy
        service = new PixKeyService(pixKeyRepository, accountRepository,
                List.of(new CelularValidator(), new EmailValidator(), new CnpjValidator()));
    }

    private CreatePixKeyRequest requisicaoDeInclusao(String tipoChave, String valorChave) {
        return new CreatePixKeyRequest(tipoChave, valorChave, "corrente", AGENCIA, CONTA, "Maria", "Silva");
    }

    private PixKey chaveExistente() {
        return new PixKey(KeyType.EMAIL, "maria@itau.com.br", AccountType.CORRENTE,
                AGENCIA, CONTA, "Maria", "Silva");
    }

    private void contaCadastrada(PersonType tipoPessoa, long chavesJaExistentes) {
        when(accountRepository.findByNumeroAgenciaAndNumeroConta(AGENCIA, CONTA))
                .thenReturn(Optional.of(new Account(AGENCIA, CONTA, tipoPessoa)));
        when(pixKeyRepository.countByNumeroAgenciaAndNumeroConta(AGENCIA, CONTA))
                .thenReturn(chavesJaExistentes);
    }

    @Nested
    @DisplayName("Inclusao")
    class Inclusao {

        @Test
        @DisplayName("grava o valor canonico e devolve o UUID gerado")
        void incluiComSucesso() {
            when(pixKeyRepository.existsByValorChave("maria.silva@itau.com.br")).thenReturn(false);
            contaCadastrada(PersonType.PF, 0);
            when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

            UUID id = service.incluir(requisicaoDeInclusao("email", "  Maria.Silva@Itau.COM.br "));

            ArgumentCaptor<PixKey> captor = ArgumentCaptor.forClass(PixKey.class);
            verify(pixKeyRepository).save(captor.capture());
            PixKey salva = captor.getValue();

            assertThat(id).isNotNull().isEqualTo(salva.getId());
            assertThat(salva.getValorChave()).isEqualTo("maria.silva@itau.com.br");
            assertThat(salva.getTipoChave()).isEqualTo(KeyType.EMAIL);
            assertThat(salva.getTipoConta()).isEqualTo(AccountType.CORRENTE);
            assertThat(salva.getStatus()).isEqualTo(KeyStatus.ACTIVE);
            assertThat(salva.getDataHoraInclusao()).isNotNull();
        }

        @Test
        @DisplayName("normaliza celular com mascara antes de gravar")
        void normalizaCelular() {
            when(pixKeyRepository.existsByValorChave("+5511987654321")).thenReturn(false);
            contaCadastrada(PersonType.PF, 0);
            when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

            service.incluir(requisicaoDeInclusao("celular", "+55 (11) 98765-4321"));

            ArgumentCaptor<PixKey> captor = ArgumentCaptor.forClass(PixKey.class);
            verify(pixKeyRepository).save(captor.capture());
            assertThat(captor.getValue().getValorChave()).isEqualTo("+5511987654321");
        }

        @Test
        @DisplayName("rejeita valor ja cadastrado, mesmo que a chave existente esteja inativa")
        void rejeitaDuplicada() {
            when(pixKeyRepository.existsByValorChave("maria@itau.com.br")).thenReturn(true);

            assertThatThrownBy(() -> service.incluir(requisicaoDeInclusao("email", "maria@itau.com.br")))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("Ja existe chave PIX cadastrada");

            verify(pixKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita agencia/conta desconhecida pelo banco")
        void rejeitaContaInexistente() {
            when(pixKeyRepository.existsByValorChave(anyString())).thenReturn(false);
            when(accountRepository.findByNumeroAgenciaAndNumeroConta(AGENCIA, CONTA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.incluir(requisicaoDeInclusao("email", "maria@itau.com.br")))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("Conta nao encontrada");

            verify(pixKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("aceita a quinta chave de uma conta PF")
        void aceitaQuintaChavePf() {
            when(pixKeyRepository.existsByValorChave(anyString())).thenReturn(false);
            contaCadastrada(PersonType.PF, 4);
            when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.incluir(requisicaoDeInclusao("email", "maria@itau.com.br"))).isNotNull();
        }

        @Test
        @DisplayName("rejeita a sexta chave de uma conta PF")
        void rejeitaSextaChavePf() {
            when(pixKeyRepository.existsByValorChave(anyString())).thenReturn(false);
            contaCadastrada(PersonType.PF, 5);

            assertThatThrownBy(() -> service.incluir(requisicaoDeInclusao("email", "maria@itau.com.br")))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("Limite de 5 chaves");
        }

        @Test
        @DisplayName("aceita a vigesima chave de uma conta PJ")
        void aceitaVigesimaChavePj() {
            when(pixKeyRepository.existsByValorChave(anyString())).thenReturn(false);
            contaCadastrada(PersonType.PJ, 19);
            when(pixKeyRepository.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.incluir(requisicaoDeInclusao("cnpj", "60.701.190/0001-04"))).isNotNull();
        }

        @Test
        @DisplayName("rejeita a vigesima primeira chave de uma conta PJ")
        void rejeitaVigesimaPrimeiraChavePj() {
            when(pixKeyRepository.existsByValorChave(anyString())).thenReturn(false);
            contaCadastrada(PersonType.PJ, 20);

            assertThatThrownBy(() -> service.incluir(requisicaoDeInclusao("cnpj", "60701190000104")))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("Limite de 20 chaves");
        }

        @Test
        @DisplayName("rejeita tipo de chave nao implementado antes de tocar no banco")
        void rejeitaTipoInvalido() {
            assertThatThrownBy(() -> service.incluir(requisicaoDeInclusao("cpf", "11144477735")))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("Tipo de chave invalido");

            verify(pixKeyRepository, never()).existsByValorChave(anyString());
        }

        @Test
        @DisplayName("delega a validacao do valor para a Strategy do tipo")
        void rejeitaValorInvalidoParaOTipo() {
            assertThatThrownBy(() -> service.incluir(requisicaoDeInclusao("cnpj", "60701190000105")))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("digitos verificadores");

            verify(pixKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("falha de forma explicita se nao houver Strategy para o tipo")
        void falhaSemStrategy() {
            PixKeyService semValidators = new PixKeyService(pixKeyRepository, accountRepository, List.of());

            assertThatThrownBy(() -> semValidators.incluir(requisicaoDeInclusao("email", "maria@itau.com.br")))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("Nao ha validador implementado");
        }
    }

    @Nested
    @DisplayName("Alteracao")
    class Alteracao {

        private final UpdatePixKeyRequest requisicao =
                new UpdatePixKeyRequest("poupanca", "4321", "87654321", "Mariana", "Souza");

        @Test
        @DisplayName("altera apenas o vinculo e preserva tipo e valor da chave")
        void alteraVinculo() {
            PixKey chave = chaveExistente();
            when(pixKeyRepository.findById(chave.getId())).thenReturn(Optional.of(chave));
            when(pixKeyRepository.save(chave)).thenReturn(chave);

            PixKeyResponse resposta = service.alterar(chave.getId(), requisicao);

            assertThat(resposta.id()).isEqualTo(chave.getId().toString());
            assertThat(resposta.tipoConta()).isEqualTo("poupanca");
            assertThat(resposta.numeroAgencia()).isEqualTo("4321");
            assertThat(resposta.numeroConta()).isEqualTo("87654321");
            assertThat(resposta.nomeCorrentista()).isEqualTo("Mariana");
            assertThat(resposta.sobrenomeCorrentista()).isEqualTo("Souza");
            assertThat(resposta.tipoChave()).isEqualTo("email");
            assertThat(resposta.valorChave()).isEqualTo("maria@itau.com.br");
            assertThat(resposta.dataHoraInclusao()).isNotEmpty();
        }

        @Test
        @DisplayName("nao encontra o id informado")
        void naoEncontrada() {
            UUID id = UUID.randomUUID();
            when(pixKeyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.alterar(id, requisicao))
                    .isInstanceOf(PixKeyNotFoundException.class);
        }

        @Test
        @DisplayName("nao permite alterar chave inativa")
        void bloqueiaChaveInativa() {
            PixKey chave = chaveExistente();
            chave.inativar();
            when(pixKeyRepository.findById(chave.getId())).thenReturn(Optional.of(chave));

            assertThatThrownBy(() -> service.alterar(chave.getId(), requisicao))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("inativa nao pode ser alterada");

            verify(pixKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita tipo de conta invalido")
        void rejeitaTipoContaInvalido() {
            PixKey chave = chaveExistente();
            when(pixKeyRepository.findById(chave.getId())).thenReturn(Optional.of(chave));

            UpdatePixKeyRequest invalida =
                    new UpdatePixKeyRequest("salario", "4321", "87654321", "Mariana", "Souza");

            assertThatThrownBy(() -> service.alterar(chave.getId(), invalida))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("Tipo de conta invalido");
        }
    }

    @Nested
    @DisplayName("Inativacao")
    class Inativacao {

        @Test
        @DisplayName("marca como inativa e devolve a data de inativacao")
        void inativa() {
            PixKey chave = chaveExistente();
            when(pixKeyRepository.findById(chave.getId())).thenReturn(Optional.of(chave));
            when(pixKeyRepository.save(chave)).thenReturn(chave);

            InactivatedPixKeyResponse resposta = service.inativar(chave.getId());

            assertThat(chave.getStatus()).isEqualTo(KeyStatus.INACTIVE);
            assertThat(resposta.dataHoraInativacao()).isNotEmpty();
            assertThat(resposta.dataHoraInclusao()).isNotEmpty();
            assertThat(resposta.valorChave()).isEqualTo("maria@itau.com.br");
        }

        @Test
        @DisplayName("avisa quando a chave ja foi desativada")
        void jaDesativada() {
            PixKey chave = chaveExistente();
            chave.inativar();
            when(pixKeyRepository.findById(chave.getId())).thenReturn(Optional.of(chave));

            assertThatThrownBy(() -> service.inativar(chave.getId()))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("ja foi desativada");
        }

        @Test
        @DisplayName("nao encontra o id informado")
        void naoEncontrada() {
            UUID id = UUID.randomUUID();
            when(pixKeyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inativar(id)).isInstanceOf(PixKeyNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("por id devolve a chave com datas em dd/MM/aaaa e nulos como vazio")
        void porId() {
            PixKey chave = new PixKey(KeyType.EMAIL, "maria@itau.com.br", AccountType.CORRENTE,
                    AGENCIA, CONTA, "Maria", null);
            when(pixKeyRepository.findById(chave.getId())).thenReturn(Optional.of(chave));

            PixKeySearchResponse resposta = service.consultarPorId(chave.getId());

            assertThat(resposta.dataHoraInclusao()).matches("\\d{2}/\\d{2}/\\d{4}");
            assertThat(resposta.dataHoraInativacao()).isEmpty();
            assertThat(resposta.sobrenomeCorrentista()).isEmpty();
        }

        @Test
        @DisplayName("por id nao devolve chave inativa")
        void porIdIgnoraInativa() {
            PixKey chave = chaveExistente();
            chave.inativar();
            when(pixKeyRepository.findById(chave.getId())).thenReturn(Optional.of(chave));

            assertThatThrownBy(() -> service.consultarPorId(chave.getId()))
                    .isInstanceOf(PixKeyNotFoundException.class);
        }

        @Test
        @DisplayName("por id inexistente")
        void porIdInexistente() {
            UUID id = UUID.randomUUID();
            when(pixKeyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consultarPorId(id)).isInstanceOf(PixKeyNotFoundException.class);
        }

        @Test
        @DisplayName("por agencia e conta")
        void porAgenciaEConta() {
            when(pixKeyRepository.findByStatusAndNumeroAgenciaAndNumeroConta(KeyStatus.ACTIVE, AGENCIA, CONTA))
                    .thenReturn(List.of(chaveExistente()));

            assertThat(service.consultar(null, AGENCIA, CONTA, null)).hasSize(1);
        }

        @Test
        @DisplayName("por tipo de chave")
        void porTipoDeChave() {
            when(pixKeyRepository.findByStatusAndTipoChave(KeyStatus.ACTIVE, KeyType.EMAIL))
                    .thenReturn(List.of(chaveExistente()));

            assertThat(service.consultar(null, null, null, "email")).hasSize(1);
        }

        @Test
        @DisplayName("combinando agencia, conta e tipo de chave")
        void filtrosCombinados() {
            when(pixKeyRepository.findByStatusAndNumeroAgenciaAndNumeroContaAndTipoChave(
                    KeyStatus.ACTIVE, AGENCIA, CONTA, KeyType.EMAIL))
                    .thenReturn(List.of(chaveExistente()));

            assertThat(service.consultar(null, AGENCIA, CONTA, "email")).hasSize(1);
        }

        @Test
        @DisplayName("nao aceita id combinado com a consulta por filtros")
        void rejeitaIdNosFiltros() {
            assertThatThrownBy(() -> service.consultar(UUID.randomUUID().toString(), AGENCIA, CONTA, null))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("nao aceita outros filtros");
        }

        @Test
        @DisplayName("exige agencia e conta informadas juntas")
        void exigeAgenciaEContaJuntas() {
            assertThatThrownBy(() -> service.consultar(null, AGENCIA, null, null))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("devem ser informados juntos");
        }

        @Test
        @DisplayName("exige ao menos um filtro")
        void exigeAoMenosUmFiltro() {
            assertThatThrownBy(() -> service.consultar(null, null, null, null))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("ao menos um filtro");
        }

        @Test
        @DisplayName("rejeita tipo de chave invalido no filtro")
        void rejeitaTipoInvalidoNoFiltro() {
            assertThatThrownBy(() -> service.consultar(null, null, null, "aleatorio"))
                    .isInstanceOf(PixValidationException.class)
                    .hasMessageContaining("Tipo de chave invalido");
        }

        @Test
        @DisplayName("devolve 404 quando nenhum registro atende aos filtros")
        void semResultado() {
            when(pixKeyRepository.findByStatusAndTipoChave(KeyStatus.ACTIVE, KeyType.CNPJ))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.consultar(null, null, null, "cnpj"))
                    .isInstanceOf(PixKeyNotFoundException.class)
                    .hasMessageContaining("Nenhuma chave PIX encontrada");
        }
    }
}
