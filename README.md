# Cadastro de Chaves PIX

Módulo de cadastro de chaves PIX: inclusão, alteração, inativação (soft delete) e consulta.

**Stack:** Java 17 · Spring Boot 4.1.1 · Spring Data JPA · Maven · PostgreSQL (container) · H2 (testes) · JUnit 5 + Mockito + JaCoCo

Tipos de chave implementados: **`celular`, `email`, `cnpj`** (o case exige no mínimo 3 dos 5).
Consultas implementadas: **por ID**, **por agência + conta**, **por tipo de chave** (o case exige no mínimo 3 das 6, sendo a por ID obrigatória).

---

## Como rodar

### 1. Subir o banco

```bash
docker compose up -d
```

Sobe um PostgreSQL 16 em `localhost:5432` (banco `pixdb`, usuário `pix`, senha `pix`). O schema é criado pelo Hibernate (`ddl-auto: update`) e o `data.sql` semeia as contas da demo.

### 2. Subir a aplicação

```bash
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

A API sobe em `http://localhost:8080`.

Toda a configuração sensível vem de variável de ambiente, com default para desenvolvimento local:

| Variável | Default |
|---|---|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/pixdb` |
| `POSTGRES_USER` | `pix` |
| `POSTGRES_PASSWORD` | `pix` |
| `SERVER_PORT` | `8080` |

### 3. Rodar os testes

```bash
./mvnw test
```

Os testes usam **H2 em memória** (profile `test`) e **não dependem de Docker**. O relatório de cobertura sai em `target/site/jacoco/index.html`.

Cobertura atual: **99,3% de linhas / 93,9% de branches** em 116 testes (meta do case: 90%).

### Contas semeadas para a demo (`data.sql`)

| Agência | Conta | Tipo | Limite de chaves |
|---|---|---|---|
| 1234 | 12345678 | PF | 5 |
| 1234 | 87654321 | PF | 5 |
| 4321 | 11223344 | PJ | 20 |
| 0001 | 00000001 | PJ | 20 |

---

## Endpoints

Base path: `/api/v1/pix-keys`. Todo erro responde com o mesmo corpo:

```json
{ "mensagem": "texto descrevendo o erro" }
```

### Inclusão — `POST /api/v1/pix-keys` → 200 / 422

```bash
curl -i -X POST http://localhost:8080/api/v1/pix-keys \
  -H "Content-Type: application/json" \
  -d '{
    "tipoChave": "celular",
    "valorChave": "+5511987654321",
    "tipoConta": "corrente",
    "numeroAgencia": "1234",
    "numeroConta": "12345678",
    "nomeCorrentista": "Maria",
    "sobrenomeCorrentista": "Silva"
  }'
```

```json
{ "id": "3f1c2d4e-5a6b-4c7d-8e9f-0a1b2c3d4e5f" }
```

Outros valores para demonstrar: `"tipoChave": "email"` com `"maria.silva@itau.com.br"`, `"tipoChave": "cnpj"` com `"60.701.190/0001-04"` (a pontuação é normalizada).

Casos de 422 para a demo:
- valor duplicado (repita a mesma chamada);
- `"valorChave": "5511987654321"` → celular sem `+`;
- `"valorChave": "60701190000105"` com tipo `cnpj` → dígito verificador inválido;
- `"numeroAgencia": "9999"` → conta desconhecida pelo banco;
- 6ª chave numa conta PF ou 21ª numa PJ → limite excedido.

### Alteração — `PUT /api/v1/pix-keys/{id}` → 200 / 422 / 404

Só o vínculo muda. `id`, `tipoChave` e `valorChave` não são aceitos no payload.

```bash
curl -i -X PUT http://localhost:8080/api/v1/pix-keys/3f1c2d4e-5a6b-4c7d-8e9f-0a1b2c3d4e5f \
  -H "Content-Type: application/json" \
  -d '{
    "tipoConta": "poupanca",
    "numeroAgencia": "1234",
    "numeroConta": "12345678",
    "nomeCorrentista": "Maria",
    "sobrenomeCorrentista": "Souza"
  }'
```

```json
{
  "id": "3f1c2d4e-5a6b-4c7d-8e9f-0a1b2c3d4e5f",
  "tipoChave": "celular",
  "valorChave": "+5511987654321",
  "tipoConta": "poupanca",
  "numeroAgencia": "1234",
  "numeroConta": "12345678",
  "nomeCorrentista": "Maria",
  "sobrenomeCorrentista": "Souza",
  "dataHoraInclusao": "2026-09-15T10:30:00"
}
```

### Inativação — `DELETE /api/v1/pix-keys/{id}` → 200 / 422 / 404

```bash
curl -i -X DELETE http://localhost:8080/api/v1/pix-keys/3f1c2d4e-5a6b-4c7d-8e9f-0a1b2c3d4e5f
```

Devolve o mesmo payload da alteração acrescido de `dataHoraInativacao`. Chamar duas vezes devolve 422 informando que a chave já foi desativada.

### Consulta por ID — `GET /api/v1/pix-keys/{id}` → 200 / 404

```bash
curl -i http://localhost:8080/api/v1/pix-keys/3f1c2d4e-5a6b-4c7d-8e9f-0a1b2c3d4e5f
```

```json
{
  "id": "3f1c2d4e-5a6b-4c7d-8e9f-0a1b2c3d4e5f",
  "tipoChave": "celular",
  "valorChave": "+5511987654321",
  "tipoConta": "corrente",
  "numeroAgencia": "1234",
  "numeroConta": "12345678",
  "nomeCorrentista": "Maria",
  "sobrenomeCorrentista": "Silva",
  "dataHoraInclusao": "15/09/2026",
  "dataHoraInativacao": ""
}
```

### Consulta por filtros — `GET /api/v1/pix-keys` → 200 / 422 / 404

Filtros combináveis: `numeroAgencia` + `numeroConta` e/ou `tipoChave`. Devolve lista no mesmo formato acima.

```bash
# por agência + conta
curl -i "http://localhost:8080/api/v1/pix-keys?numeroAgencia=1234&numeroConta=12345678"

# por tipo de chave
curl -i "http://localhost:8080/api/v1/pix-keys?tipoChave=celular"

# combinado
curl -i "http://localhost:8080/api/v1/pix-keys?numeroAgencia=1234&numeroConta=12345678&tipoChave=celular"
```

Respondem 422: informar `id` junto dos filtros (o case proíbe combinar), informar só a agência sem a conta, não informar filtro algum, ou informar um `tipoChave` inexistente. Responde 404 quando nenhum registro atende.

---

## Pattern: Strategy na validação por tipo de chave

Cada tipo de chave tem regras de formato próprias e a lista de tipos é a parte do domínio que mais tende a crescer (o PIX ainda tem CPF e chave aleatória). Um `if/else` ou `switch` no service concentraria todas essas regras num lugar só e obrigaria a editar o service a cada tipo novo — violando o princípio aberto/fechado.

A interface:

```java
public interface PixKeyValidator {
    boolean supports(KeyType tipo);
    String normalize(String valorBruto);   // devolve o valor canônico
    void validate(String valorCanonico);   // lança PixValidationException se inválido
}
```

Cada tipo tem uma implementação (`CelularValidator`, `EmailValidator`, `CnpjValidator`), todas `@Component`. O Spring injeta `List<PixKeyValidator>` no `PixKeyService`, que escolhe a estratégia por `supports(...)` — **não há if/else por tipo no service**. Incluir CPF é criar uma classe nova; nenhuma linha do service muda.

O `normalize` faz parte da estratégia porque canonicalização também é específica do tipo: e-mail vira minúsculo, CNPJ perde a pontuação, celular perde a máscara de digitação. O valor canônico é o único que é validado, gravado e comparado por unicidade — o que torna a unicidade consistente (`Maria@Itau.com` e `maria@itau.com` são a mesma chave).

---

## 12 Factor App

| Fator | Como foi atendido |
|---|---|
| **I. Codebase** | Um único repositório Git versionando toda a aplicação, do qual saem todos os deploys. |
| **II. Dependencies** | Todas as dependências declaradas explicitamente no `pom.xml`, com versões governadas pelo BOM do Spring Boot. O Maven Wrapper (`mvnw`) garante que até a versão do build tool seja explícita, sem depender de nada instalado na máquina. |
| **III. Config** | Nada de credencial no código: `application.yaml` lê `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e `SERVER_PORT` do ambiente. Profiles (`default` → Postgres, `test` → H2) separam ambientes sem recompilar. |
| **IV. Backing services** | O PostgreSQL é um recurso anexo, acessado por URL de conexão. Trocar o banco do container por um RDS gerenciado é mudar uma variável de ambiente — nenhum código muda. |
| **V. Build, release, run** | Estágios separados: `mvn package` (build, gera o JAR), o JAR + as variáveis de ambiente formam o release, e `java -jar` é o run. O mesmo artefato roda em qualquer ambiente. |
| **VI. Processes** | A aplicação é stateless: nenhum estado de sessão em memória, nenhum arquivo local. Todo estado vive no PostgreSQL, o que permite escalar horizontalmente. |
| **VIII. Concurrency** | Como consequência do stateless, escala por processo: basta subir mais instâncias atrás de um balanceador, sem afinidade de sessão. |
| **XI. Logs** | Logs vão para stdout como fluxo de eventos, sem gerenciar arquivos nem rotação — quem coleta e roteia é o ambiente de execução. |
| **X. Dev/prod parity** | Postgres em container no dev é o mesmo Postgres de produção, mantendo o gap pequeno. O H2 é usado **apenas nos testes unitários**, de propósito: garante que a suíte rode em qualquer máquina e no CI sem Docker. |

---

## Decisões de projeto e inconsistências do enunciado

O enunciado tem alguns pontos ambíguos ou contraditórios. Abaixo o que foi decidido e por quê.

**1. A alteração não muda o valor da chave.**
O objetivo da seção "Alteração" diz *"permitir alteração do valor de uma chave registrada... alterar um e-mail, telefone, CNPJ/CPF já cadastrado"*, mas os critérios de aceite 3 e 4 da mesma seção dizem que o tipo e o valor da chave **não** podem ser alterados — e a tabela 2 (dados de entrada) sequer traz esses campos. Prevaleceram os critérios de aceite e a tabela: `PUT` altera só o vínculo (conta e titular), e `tipoChave`/`valorChave` são imutáveis na entidade (`updatable = false`). Trocar o valor de uma chave é, na prática, inativar a antiga e incluir uma nova.

**2. PF/PJ vem da entidade `Account`, não do payload.**
O limite é 5 chaves para PF e 20 para PJ, mas a tabela de entrada não traz documento do titular — não há como inferir a natureza da conta pelo payload. O banco já conhece o tipo de cada conta, então existe a entidade `Account` (agência + conta + `tipoPessoa`), semeada via `data.sql`, e o limite é lido dela. Consequência: **incluir chave numa agência+conta desconhecida devolve 422** — a conta precisa existir no banco. Não há relacionamento JPA entre `Account` e `PixKey`; a ligação é um lookup por agência + conta, o que mantém as duas tabelas independentes.

**3. Formato de data diferente entre operações — é intencional.**
As tabelas 3 e 4 (alteração e inativação) declaram `DATETIME`; a tabela 5 (consulta) declara `DATE (dd/mm/aaaa)`. Em vez de uniformizar, a implementação segue o enunciado à risca: alteração/inativação devolvem ISO-8601 (`2026-09-15T10:30:00`) e as consultas devolvem `15/09/2026`. É por isso que existem DTOs de resposta distintos — cada um espelha uma tabela do enunciado.

**4. Unicidade é definitiva, inclusive contra chaves inativas.**
Um `valorChave` já usado nunca é aceito de novo, mesmo que a chave existente esteja `INACTIVE`. A garantia é dupla: constraint `UNIQUE` no banco e checagem prévia na aplicação (para devolver 422 com mensagem amigável em vez de erro de integridade). Consequência coerente: o **limite por conta conta todas as chaves**, ativas e inativas — se uma chave inativa não libera o valor, ela também não libera a vaga.

**5. Chave inativa não é consultável — nem por ID.**
O case diz que a inativação impede que a chave seja *"alterada ou consultada"*. Então `GET /{id}` de uma chave inativa devolve **404**, e as consultas por filtro só retornam chaves `ACTIVE`. O campo `dataHoraInativacao` existe no contrato de saída da consulta (tabela 5) e é preenchido na resposta do `DELETE`, que é onde o histórico de inativação é visível.

**6. Regra "ID não combina com outros filtros".**
O case pede 422 ao combinar o ID com outros filtros. Como a consulta por ID é um endpoint próprio (`GET /{id}`), isso já é estruturalmente impossível. Ainda assim o endpoint de filtros rejeita explicitamente um parâmetro `id` com 422 e orienta o uso do endpoint correto, atendendo o critério de forma literal.

**7. Inclusão devolve 200, não 201.**
Semanticamente um `POST` que cria recurso devolveria 201. O case pede 200 explicitamente no critério de aceite 9 — seguimos o case.

**8. A alteração não revalida limite da conta de destino.**
Mudar o vínculo pode, em tese, mover uma chave para uma conta que já está no limite. Os critérios de aceite da alteração listam apenas validação de campos e o bloqueio de chave inativa, então essa revalidação ficou **fora de escopo** de propósito — é uma decisão consciente, não um esquecimento.

**9. Normalização preserva letras.**
Os `normalize` removem só máscara de digitação (espaços, parênteses, hífen, ponto, barra), nunca letras. Assim `+55abc98765432` falha com *"deve conter somente números após o '+'"* em vez de ser silenciosamente transformado num valor de tamanho errado — a mensagem de erro aponta o problema real.

**10. `poupança` com e sem cedilha.**
O enunciado escreve `poupança`; o campo é texto livre vindo do cliente. `AccountType.from` normaliza acentos (NFD) e caixa, aceitando `poupanca`, `poupança`, `POUPANCA` etc.

**11. HTTP 422 no Spring 7.**
No Spring Framework 7 a constante do 422 foi renomeada de `UNPROCESSABLE_ENTITY` para `UNPROCESSABLE_CONTENT`, acompanhando a RFC 9110. É rigorosamente o mesmo status code pedido pelo case.

---

## Estrutura

```
src/main/java/com/itau/pix/pixkeys/
  controller/   PixKeyController
  service/      PixKeyService
  domain/       PixKey, Account, KeyType, AccountType, KeyStatus, PersonType
  repository/   PixKeyRepository, AccountRepository       (métodos nomeados, sem Specification)
  validator/    PixKeyValidator + Celular/Email/Cnpj      (Strategy)
  dto/          requests e responses (um por tabela do enunciado)
  exception/    ApiExceptionHandler (@RestControllerAdvice) + exceções custom
src/main/resources/
  application.yaml        profile default → Postgres via env vars
  application-test.yaml   profile test    → H2 em memória
  data.sql                seed de contas PF e PJ
docker-compose.yml
```

O tratamento de erro fica num único `@RestControllerAdvice`: regra de negócio/validação → 422, recurso inexistente → 404, e as violações de Bean Validation são traduzidas para o mesmo corpo `{"mensagem": "..."}`.
