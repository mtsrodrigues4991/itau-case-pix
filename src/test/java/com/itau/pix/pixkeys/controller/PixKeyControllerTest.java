package com.itau.pix.pixkeys.controller;

import com.itau.pix.pixkeys.dto.InactivatedPixKeyResponse;
import com.itau.pix.pixkeys.dto.PixKeyResponse;
import com.itau.pix.pixkeys.dto.PixKeySearchResponse;
import com.itau.pix.pixkeys.dto.UpdatePixKeyRequest;
import com.itau.pix.pixkeys.exception.PixKeyNotFoundException;
import com.itau.pix.pixkeys.exception.PixValidationException;
import com.itau.pix.pixkeys.service.PixKeyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PixKeyController.class)
class PixKeyControllerTest {

    private static final String BASE = "/api/v1/pix-keys";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PixKeyService service;

    private static final String INCLUSAO_VALIDA = """
            {
              "tipoChave": "celular",
              "valorChave": "+5511987654321",
              "tipoConta": "corrente",
              "numeroAgencia": "1234",
              "numeroConta": "12345678",
              "nomeCorrentista": "Maria",
              "sobrenomeCorrentista": "Silva"
            }
            """;

    private static final String ALTERACAO_VALIDA = """
            {
              "tipoConta": "poupanca",
              "numeroAgencia": "1234",
              "numeroConta": "12345678",
              "nomeCorrentista": "Maria",
              "sobrenomeCorrentista": "Souza"
            }
            """;

    private PixKeySearchResponse consulta(UUID id) {
        return new PixKeySearchResponse(id.toString(), "celular", "+5511987654321", "corrente",
                "1234", "12345678", "Maria", "", "15/09/2026", "");
    }

    @Test
    @DisplayName("POST devolve 200 e o id gerado")
    void incluiComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.incluir(any())).thenReturn(id);

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(INCLUSAO_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("POST devolve 422 com a mensagem da regra de negocio violada")
    void incluiComRegraViolada() throws Exception {
        when(service.incluir(any())).thenThrow(new PixValidationException("Limite de 5 chaves atingido"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(INCLUSAO_VALIDA))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Limite de 5 chaves atingido"));
    }

    @Test
    @DisplayName("POST devolve 422 traduzindo as violacoes de Bean Validation")
    void incluiComCamposInvalidos() throws Exception {
        String payload = """
                {
                  "tipoChave": "celular",
                  "valorChave": "+5511987654321",
                  "tipoConta": "corrente",
                  "numeroAgencia": "12A4",
                  "numeroConta": "12345678",
                  "nomeCorrentista": ""
                }
                """;

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("numeroAgencia")));
    }

    @Test
    @DisplayName("POST devolve 422 quando o corpo esta mal formatado")
    void incluiComCorpoInvalido() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisicao ausente ou mal formatado"));
    }

    @Test
    @DisplayName("POST devolve 422 quando a constraint UNIQUE do banco e violada")
    void incluiComValorConcorrente() throws Exception {
        when(service.incluir(any())).thenThrow(new DataIntegrityViolationException("uk_pix_key_valor_chave"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(INCLUSAO_VALIDA))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Valor de chave PIX ja cadastrado"));
    }

    @Test
    @DisplayName("PUT devolve 200 com o recurso atualizado")
    void alteraComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.alterar(eq(id), any(UpdatePixKeyRequest.class))).thenReturn(
                new PixKeyResponse(id.toString(), "celular", "+5511987654321", "poupanca",
                        "1234", "12345678", "Maria", "Souza", "2026-09-15T10:00:00"));

        mockMvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content(ALTERACAO_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoConta").value("poupanca"))
                .andExpect(jsonPath("$.valorChave").value("+5511987654321"))
                .andExpect(jsonPath("$.dataHoraInclusao").value("2026-09-15T10:00:00"));
    }

    @Test
    @DisplayName("PUT devolve 404 quando o id nao existe")
    void alteraInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.alterar(eq(id), any())).thenThrow(new PixKeyNotFoundException("Chave PIX nao encontrada"));

        mockMvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content(ALTERACAO_VALIDA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Chave PIX nao encontrada"));
    }

    @Test
    @DisplayName("PUT devolve 422 quando o id do path nao e um UUID")
    void alteraComIdInvalido() throws Exception {
        mockMvc.perform(put(BASE + "/nao-e-uuid").contentType(MediaType.APPLICATION_JSON).content(ALTERACAO_VALIDA))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    @DisplayName("DELETE devolve 200 com a data de inativacao")
    void inativaComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.inativar(id)).thenReturn(new InactivatedPixKeyResponse(id.toString(), "celular",
                "+5511987654321", "corrente", "1234", "12345678", "Maria", "",
                "2026-09-15T10:00:00", "2026-09-15T11:00:00"));

        mockMvc.perform(delete(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataHoraInativacao").value("2026-09-15T11:00:00"))
                .andExpect(jsonPath("$.sobrenomeCorrentista").value(""));
    }

    @Test
    @DisplayName("DELETE devolve 422 quando a chave ja esta inativa")
    void inativaDuasVezes() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.inativar(id)).thenThrow(new PixValidationException("Chave PIX ja foi desativada"));

        mockMvc.perform(delete(BASE + "/" + id))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Chave PIX ja foi desativada"));
    }

    @Test
    @DisplayName("GET por id devolve 200 com a data em dd/MM/aaaa")
    void consultaPorId() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.consultarPorId(id)).thenReturn(consulta(id));

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataHoraInclusao").value("15/09/2026"))
                .andExpect(jsonPath("$.dataHoraInativacao").value(""));
    }

    @Test
    @DisplayName("GET por id devolve 404 quando nao existe")
    void consultaPorIdInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.consultarPorId(id)).thenThrow(new PixKeyNotFoundException("Chave PIX nao encontrada"));

        mockMvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET por filtros devolve a lista de chaves")
    void consultaPorFiltros() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.consultar(null, "1234", "12345678", "celular")).thenReturn(List.of(consulta(id)));

        mockMvc.perform(get(BASE)
                        .param("numeroAgencia", "1234")
                        .param("numeroConta", "12345678")
                        .param("tipoChave", "celular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].tipoChave").value("celular"));
    }

    @Test
    @DisplayName("GET por filtros devolve 404 quando nada e encontrado")
    void consultaSemResultado() throws Exception {
        when(service.consultar(null, null, null, "cnpj"))
                .thenThrow(new PixKeyNotFoundException("Nenhuma chave PIX encontrada"));

        mockMvc.perform(get(BASE).param("tipoChave", "cnpj"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Nenhuma chave PIX encontrada"));
    }
}
