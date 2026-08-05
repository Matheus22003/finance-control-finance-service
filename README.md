# Finance Control - Finance Service

Microserviço responsável pelo domínio financeiro pessoal: receitas, despesas, categorias e resumos mensais. Ele é consumido somente pelo BFF dentro da arquitetura do Finance Control.

## Stack

- Java `21`
- Spring Boot `4.0.3`
- Spring MVC
- Spring Data JPA com Hibernate `7.2.4.Final`
- PostgreSQL JDBC `42.7.10`
- Flyway `11.14.1`
- Spring Boot Actuator
- springdoc OpenAPI `3.0.3`
- Maven `3.9.12`
- Docker

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| `GET` | `/health` | Estado da aplicação |
| `GET` | `/api/v1/finance/incomes` | Lista receitas |
| `GET` | `/api/v1/finance/incomes/{id}` | Consulta uma receita |
| `POST` | `/api/v1/finance/incomes` | Cria uma receita |
| `PUT` | `/api/v1/finance/incomes/{id}` | Atualiza uma receita |
| `DELETE` | `/api/v1/finance/incomes/{id}` | Exclui uma receita |
| `GET` | `/api/v1/finance/expenses` | Lista despesas |
| `GET` | `/api/v1/finance/expenses/{id}` | Consulta uma despesa |
| `POST` | `/api/v1/finance/expenses` | Cria uma despesa |
| `PUT` | `/api/v1/finance/expenses/{id}` | Atualiza uma despesa |
| `DELETE` | `/api/v1/finance/expenses/{id}` | Exclui uma despesa |
| `GET` | `/api/v1/finance/recurring-transactions` | Lista regras recorrentes |
| `POST` | `/api/v1/finance/recurring-transactions` | Cria uma recorrência semanal, mensal ou anual |
| `PUT` | `/api/v1/finance/recurring-transactions/{id}` | Atualiza, pausa ou reativa uma recorrência |
| `DELETE` | `/api/v1/finance/recurring-transactions/{id}` | Exclui a regra e mantém ocorrências já geradas |
| `GET` | `/api/v1/finance/budgets?month=yyyy-MM` | Retorna orçamento e consumo por categoria |
| `PUT` | `/api/v1/finance/budgets/{category}?month=yyyy-MM` | Define o limite mensal da categoria |
| `DELETE` | `/api/v1/finance/budgets/{category}?month=yyyy-MM` | Remove o limite da categoria |
| `GET` | `/api/v1/finance/categories` | Lista categorias de despesa |
| `GET` | `/api/v1/finance/summary?month=yyyy-MM` | Calcula o resumo mensal |
| `GET` | `/openapi/v1.json` | Documento OpenAPI |
| `GET` | `/swagger` | Swagger UI |

O endpoint interno `DELETE /api/v1/internal/account-data` exclui todas as receitas e despesas privadas do usuário do header interno.

O parâmetro `month` é opcional. Quando omitido, o resumo usa o mês atual em UTC.

As listagens aceitam filtros inclusivos: `from` e `to` no formato `yyyy-MM-dd`; despesas também aceitam `category`. Regras recorrentes materializam ocorrências vencidas de forma idempotente ao serem criadas, ao consultar dados do usuário e diariamente às `00:05`.

O Finance Service não emite nem valida JWT. A autenticação é responsabilidade exclusiva do BFF, e no Docker Compose este serviço permanece em rede interna.
O endpoint de ciclo de vida é idempotente e recebe `X-Finance-Control-User-Id` somente pela rede interna.

## Persistência

O schema é controlado exclusivamente pelo Flyway. O Hibernate usa `ddl-auto: validate` e não cria ou modifica tabelas automaticamente.

Migration atual:

- `V1__create_finance_tables.sql`: tabelas `incomes` e `expenses`, constraints e índices.
- `V2__scope_finance_data_by_user.sql`: isolamento das receitas e despesas pelo proprietário.
- `V3__add_budgets_and_recurring_transactions.sql`: regras recorrentes, ocorrências e orçamentos mensais.

Variáveis de conexão:

| Variável | Valor local padrão |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/finance_control_finance` |
| `SPRING_DATASOURCE_USERNAME` | `finance_app` |
| `SPRING_DATASOURCE_PASSWORD` | `local_finance_password` |

## Execução local

Requer JDK `21`, Maven `3.9.12` e um PostgreSQL disponível com as credenciais configuradas.

```powershell
mvn --batch-mode --no-transfer-progress clean verify
mvn spring-boot:run
```

A aplicação escuta em `http://localhost:8081`. A porta pode ser alterada por `SERVER_PORT`.

O arquivo `finance-control-finance-service.http` contém exemplos completos para IntelliJ IDEA.

## Docker Compose

O ambiente integrado deve ser iniciado pelo repositório `finance-control-infra`:

```powershell
Set-Location ..\finance-control-infra
Copy-Item .env.example .env
docker compose up --build --detach --wait
```

O Finance Service não publica uma porta no host nesse ambiente. As chamadas da aplicação passam pelo BFF.

## Testes

Os testes usam H2 `2.4.240` em modo de compatibilidade PostgreSQL e executam a mesma migration Flyway usada em produção. O build da imagem também executa `mvn clean verify`.

```powershell
mvn --batch-mode --no-transfer-progress clean verify
docker build --tag finance-control-finance-service:local .
```

## Versões diretas

Dependências:

- `org.springframework.boot:spring-boot-starter-web:4.0.3`
- `org.springframework.boot:spring-boot-starter-actuator:4.0.3`
- `org.springframework.boot:spring-boot-starter-validation:4.0.3`
- `org.springframework.boot:spring-boot-starter-data-jpa:4.0.3`
- `org.springframework.boot:spring-boot-starter-flyway:4.0.3`
- `org.flywaydb:flyway-database-postgresql:11.14.1`
- `org.postgresql:postgresql:42.7.10`
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3`
- `org.springframework.boot:spring-boot-starter-test:4.0.3`
- `com.h2database:h2:2.4.240`

Plugins Maven:

- `org.apache.maven.plugins:maven-compiler-plugin:3.14.1`
- `org.apache.maven.plugins:maven-surefire-plugin:3.5.4`
- `org.springframework.boot:spring-boot-maven-plugin:4.0.3`

Imagens Docker:

- Build: `maven:3.9.12-eclipse-temurin-21-alpine`
- Runtime: `eclipse-temurin:21.0.11_10-jre-alpine-3.23`

As imagens também estão fixadas por digest no Dockerfile.
