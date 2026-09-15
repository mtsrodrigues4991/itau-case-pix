# Prompt — Geração da aplicação "Cadastro de Chaves PIX"

> Cole este bloco inteiro na IA de código. Ele é autossuficiente: contém o case, as decisões já tomadas e os contratos das APIs. Siga-o à risca e **não adicione nada além do pedido** (sem over-engineering).

---

## 1. Papel e objetivo

Você é um engenheiro Java sênior. Gere uma aplicação **funcional e completa** de um módulo de cadastro de **chaves PIX**, com APIs REST, testes unitários e instruções de execução. O código será apresentado ao vivo para uma banca avaliadora, então priorize **clareza, consistência e fácil defesa** acima de sofisticação. Entregue todos os arquivos com seus caminhos.

### Contexto do domínio (resumido)
Uma **chave PIX** é um apelido vinculado a uma conta corrente/poupança, usado para identificar o recebedor. O formato do valor indica o tipo. Este módulo permite **incluir, alterar, inativar e consultar** chaves. Cada conta tem um limite de chaves.

---

## 2. Stack e decisões técnicas (fixas — não alterar)

- **Java 17**, **Spring Boot 3.x**, **Maven**.
- **Banco de execução: PostgreSQL em container** (via `docker-compose.yml`), acessado por env vars.
- **Banco de testes: H2 em memória**, isolado via profile `test`. Os testes **não** podem depender de Docker.
- **Schema criado pelo Hibernate** (`ddl-auto`: `update` no dev, `create-drop` no test). **Não** usar Flyway/Liquibase.
- **Spring Data JPA** para persistência.
- **Bean Validation (jakarta.validation)** para regras genéricas de campo.
- Um único pattern de projeto de destaque: **Strategy** para validação por tipo de chave (detalhado na seção 6).
- **JUnit 5 + Mockito** para testes, meta de **90% de cobertura**.

### Tipos de chave a implementar: **APENAS 3** → `CNPJ`, `CELULAR`, `EMAIL`.
Não implementar CPF nem chave aleatória.

---

## 3. Modelo de domínio

### Entidade `PixKey`
| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID | PK, gerado pela aplicação |
| `tipoChave` | enum `KeyType` (CELULAR, EMAIL, CNPJ) | imutável após criação |
| `valorChave` | String | **valor canônico/normalizado**; **único global** |
| `tipoConta` | enum `AccountType` (CORRENTE, POUPANCA) | |
| `numeroAgencia` | String(4) | numérica, armazenar como String para preservar zeros à esquerda |
| `numeroConta` | String(8) | numérica, armazenar como String |
| `nomeCorrentista` | String(30) | |
| `sobrenomeCorrentista` | String(45) | opcional (nullable) |
| `dataHoraInclusao` | LocalDateTime | registrada na criação |
| `dataHoraInativacao` | LocalDateTime | nullable; preenchida na inativação |
| `status` | enum `KeyStatus` (ACTIVE, INACTIVE) | soft delete |

Aplicar **constraint UNIQUE** em `valorChave` no banco.

### Entidade `Account` (suporte à regra de limite)
| Campo | Tipo |
|---|---|
| `id` | Long |
| `numeroAgencia` | String(4) |
| `numeroConta` | String(8) |
| `tipoPessoa` | enum `PersonType` (PF, PJ) |

- **Sem relacionamento JPA** com `PixKey` (não usar `@ManyToOne`). A ligação é feita por lookup no `AccountRepository` por `numeroAgencia` + `numeroConta`.
- Fazer **seed** de algumas contas (ex.: via `data.sql` no profile de dev), incluindo PF e PJ, para viabilizar a demo.
- **Justificativa (colocar no README):** a tabela de entrada do case não traz documento do titular, logo não há como inferir PF/PJ pelo payload. O banco já conhece o tipo de cada conta; por isso `Account` carrega `tipoPessoa`, e o limite (5/20) é lido da conta.

---

## 4. Regras de negócio transversais

1. **ID único** em formato **UUID**, gerado independentemente do tipo de chave.
2. **Unicidade global e definitiva de `valorChave`**: um valor duplicado nunca é aceito, **mesmo que a chave existente esteja INACTIVE**. Garantir nos dois níveis: constraint UNIQUE no banco **e** checagem prévia na aplicação (para devolver 422 amigável).
3. **Limite por conta**: **5** chaves para conta **PF**, **20** para conta **PJ**. O tipo vem do `Account` correspondente à agência+conta. Contam apenas chaves **ACTIVE**? → Não: como a unicidade é definitiva, conte todas as chaves da conta para o limite. (Mantém a regra simples e coerente com a unicidade global.)
4. **Valor canônico** (normalizar antes de validar/salvar/comparar unicidade):
   - **EMAIL**: `trim` + `lowercase`.
   - **CNPJ**: somente dígitos (remover qualquer pontuação).
   - **CELULAR**: `+` seguido apenas de dígitos (ex.: `+5511987654321`).
5. **`dataHoraInclusao`** registrada na criação.
6. **Soft delete**: inativar = `status = INACTIVE` + `dataHoraInativacao` preenchida. Chave INACTIVE **não pode ser alterada nem consultada**.
7. Na inclusão, se a agência+conta informada **não existir** em `Account`, devolver **422** com mensagem (a conta precisa ser conhecida pelo banco).

---

## 5. Validação de campos comuns (todas as operações que os recebem)

- `tipoConta`: obrigatório; apenas `corrente` ou `poupanca`; máx. 10 caracteres.
- `numeroAgencia`: obrigatório; somente numérico; máx. 4 dígitos.
- `numeroConta`: obrigatório; somente numérico; máx. 8 dígitos.
- `nomeCorrentista`: obrigatório; máx. 30 caracteres.
- `sobrenomeCorrentista`: opcional; se informado, máx. 45 caracteres.

Usar **Bean Validation** nos DTOs de entrada para essas regras genéricas.

---

## 6. Validação por tipo de chave — **Strategy pattern**

Definir a interface:
```java
public interface PixKeyValidator {
    boolean supports(KeyType tipo);
    String normalize(String valorBruto);   // devolve o valor canônico
    void validate(String valorCanonico);   // lança exceção de validação se inválido
}
```
Uma implementação por tipo. O Spring injeta `List<PixKeyValidator>`; o service seleciona o validador via `supports(...)`. **Não** usar if/else por tipo no service.

### Regras por tipo
**CELULAR** (`valorChave` no formato `+<país><DDD><número>`)
- Deve iniciar com `+`.
- Após o `+`, somente dígitos.
- Código do país: numérico, 1–2 dígitos.
- DDD: numérico, 2–3 dígitos.
- Número local: exatamente **9 dígitos** numéricos.
- Exemplo válido: `+5511987654321`.

**EMAIL**
- Deve conter `@`.
- Pode conter caracteres alfanuméricos (formato de e-mail padrão).
- Máx. **77** caracteres.

**CNPJ**
- Somente números (após normalização).
- Exatamente **14** dígitos.
- **Validar dígitos verificadores** com o algoritmo oficial (mod 11 com os dois DVs). Não validar apenas o tamanho.

---

## 7. Contratos das APIs

Base path sugerido: `/api/v1/pix-keys`. Todas as respostas de erro têm o corpo:
```json
{ "mensagem": "texto livre descrevendo o erro" }
```

### 7.1 Inclusão — `POST /api/v1/pix-keys`
Request:
```json
{
  "tipoChave": "celular",
  "valorChave": "+5511987654321",
  "tipoConta": "corrente",
  "numeroAgencia": "1234",
  "numeroConta": "12345678",
  "nomeCorrentista": "Maria",
  "sobrenomeCorrentista": "Silva"
}
```
Respostas:
- **200 OK** em sucesso, corpo `{ "id": "<uuid>" }`. *(O case pede explicitamente 200, não 201 — seguir 200.)*
- **422 Unprocessable Entity** se qualquer regra de validação/negócio for violada (formato inválido, duplicidade, limite excedido, conta inexistente, campo obrigatório ausente).

### 7.2 Alteração — `PUT /api/v1/pix-keys/{id}`
Só é permitido alterar o **vínculo**. `id`, `tipoChave` e `valorChave` **não** podem ser alterados (não aceitar no payload).
Request:
```json
{
  "tipoConta": "poupanca",
  "numeroAgencia": "1234",
  "numeroConta": "12345678",
  "nomeCorrentista": "Maria",
  "sobrenomeCorrentista": "Souza"
}
```
Regras: não permitir alterar chave **INACTIVE**. Validar campos conforme seção 5.
Respostas:
- **200 OK** com o recurso completo atualizado: `id`, `tipoChave`, `valorChave`, `tipoConta`, `numeroAgencia`, `numeroConta`, `nomeCorrentista`, `sobrenomeCorrentista`, `dataHoraInclusao`.
- **422** se violar validação (inclui tentativa de alterar chave inativa).
- **404** se o `id` não existir.

### 7.3 Inativação (soft delete) — `DELETE /api/v1/pix-keys/{id}`
Recebe apenas o `id` no path. Marca `status = INACTIVE` e grava `dataHoraInativacao`.
Respostas:
- **200 OK** com payload: `id`, `tipoChave`, `valorChave`, `tipoConta`, `numeroAgencia`, `numeroConta`, `nomeCorrentista`, `sobrenomeCorrentista`, `dataHoraInclusao`, `dataHoraInativacao`.
- **422** se a chave **já estiver INACTIVE** (mensagem informando que já foi desativada).
- **404** se o `id` não existir.

### 7.4 Consulta — implementar **3 das 6** (ID obrigatória)
Implementar: **por ID**, **por agência+conta**, **por tipo de chave**.

- **Por ID:** `GET /api/v1/pix-keys/{id}` → 200 com um registro (formato tabela abaixo) ou **404** se não existir.
- **Por filtros combinados:** `GET /api/v1/pix-keys?numeroAgencia=1234&numeroConta=12345678&tipoChave=celular`
  - Filtros podem ser combinados entre si.
  - Devolver **lista** no formato de saída abaixo.
  - **404** se nenhum registro for encontrado.

**Formato de saída da consulta (cada item):**
```json
{
  "id": "<uuid>",
  "tipoChave": "celular",
  "valorChave": "+5511987654321",
  "tipoConta": "corrente",
  "numeroAgencia": "1234",
  "numeroConta": "12345678",
  "nomeCorrentista": "Maria",
  "sobrenomeCorrentista": "Silva",
  "dataHoraInclusao": "14/09/2026",
  "dataHoraInativacao": ""
}
```
- **Datas na consulta são formatadas como `dd/MM/yyyy`** (na inclusão/alteração são DATETIME — isto é intencional, conforme o case).
- **Campos nulos devem ser retornados como string vazia `""`** (não `null`).
- Nas consultas, retornar apenas chaves **ACTIVE** (chave inativa não pode ser consultada), **exceto** que a consulta deve permitir enxergar o histórico de inativação quando aplicável — se houver dúvida, priorize: não retornar INACTIVE em consultas por filtro.

---

## 8. Tratamento de erros

- Um único `@RestControllerAdvice` central.
- Exceções de validação/negócio → **422**; recurso não encontrado → **404**.
- Corpo padronizado `{ "mensagem": "..." }`.
- Mapear as violações de Bean Validation para o mesmo formato de 422.

---

## 9. Testes (meta: 90% de cobertura)

- **Foco principal:** cada `PixKeyValidator` testado isoladamente (casos válidos e inválidos, incluindo DV de CNPJ e formato de celular).
- Testar o service: unicidade, limite por conta (PF=5, PJ=20), bloqueio de alteração/consulta de chave inativa, geração de UUID.
- Testar o advice de erros (422/404).
- Usar JUnit 5 + Mockito. Incluir plugin de cobertura (JaCoCo) no `pom.xml`.

---

## 10. Estrutura de projeto (camadas simples)

```
src/main/java/.../pixkeys/
  controller/    → PixKeyController
  service/       → PixKeyService
  domain/        → PixKey, Account, enums (KeyType, AccountType, PersonType, KeyStatus)
  repository/    → PixKeyRepository, AccountRepository
  validator/     → PixKeyValidator + CelularValidator, EmailValidator, CnpjValidator
  dto/           → requests e responses
  exception/     → RestControllerAdvice + exceções custom
src/test/java/...
docker-compose.yml
src/main/resources/application.yml        (profile dev → Postgres via env vars)
src/main/resources/application-test.yml   (profile test → H2)
src/main/resources/data.sql               (seed de contas Account)
README.md
```

---

## 11. Entregáveis obrigatórios

1. Projeto Maven completo e compilável (`pom.xml` com todas as dependências).
2. `docker-compose.yml` subindo o PostgreSQL, com credenciais lidas por env var na app.
3. `application.yml` (dev/Postgres) + `application-test.yml` (H2), usando profiles.
4. `data.sql` com seed de contas PF e PJ para a demo.
5. Testes unitários com JaCoCo configurado.
6. **README.md** contendo:
   - Como rodar: `docker compose up -d` + `mvn spring-boot:run`, e como rodar os testes.
   - Exemplos de chamada de cada endpoint (curl/Postman).
   - Seção **"12 Factor App"** nomeando e explicando os fatores atendidos: **codebase** (git), **dependencies** (Maven), **config** (env vars/profiles), **backing services** (Postgres como recurso anexo), **build/release/run**, **processes** (stateless), **logs** (stdout), **dev/prod parity** (H2 nos testes ↔ Postgres em container).
   - Breve nota sobre o pattern **Strategy** usado na validação e o porquê.
   - Nota sobre as inconsistências do enunciado que foram tratadas (alteração não muda o valor da chave; formato de data difere entre inclusão e consulta; inferência de PF/PJ via `Account`).

---

## 12. Restrições — o que **NÃO** fazer

- Não adicionar Spring Security, autenticação, cache, mensageria, API Gateway ou qualquer coisa fora do escopo.
- Não usar Specification/QueryDSL — usar **métodos de repositório nomeados** (`findById`, `findByNumeroAgenciaAndNumeroConta`, `findByTipoChave`, com combinação simples no service).
- Não usar Flyway/Liquibase.
- Não implementar CPF nem chave aleatória.
- Não criar frontend.
- Não introduzir outros patterns além do **Strategy** para validação.
- Manter o mínimo de dependências. Simplicidade e legibilidade acima de tudo.

---

## 13. Formato da sua resposta

Entregue **todos os arquivos**, cada um em um bloco de código com o **caminho no topo**. Comece por `pom.xml`, depois as classes de domínio, repositórios, validators, service, controller, DTOs, advice, arquivos de config, `docker-compose.yml`, `data.sql`, testes e por fim o `README.md`. Ao final, liste em 3–5 linhas as principais decisões de design tomadas. Não peça confirmação: gere a solução completa de uma vez.
