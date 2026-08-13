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
| `GET` | `/api/v1/finance/incomes/{id}/goal-allocations` | Detalha as metas e os aportes que consomem a receita |
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
| `POST` | `/api/v1/finance/categories` | Cria uma categoria personalizada |
| `PUT` | `/api/v1/finance/categories/{id}` | Renomeia uma categoria personalizada |
| `DELETE` | `/api/v1/finance/categories/{id}` | Exclui uma categoria personalizada sem vínculos |
| `GET` | `/api/v1/finance/summary?month=yyyy-MM` | Calcula o resumo mensal |
| `GET` | `/api/v1/finance/trends?month=yyyy-MM&months=6` | Retorna a evolução de receitas, despesas e saldo de 2 a 12 meses |
| `GET` | `/api/v1/finance/goals` | Lista as metas financeiras do usuário |
| `POST` | `/api/v1/finance/goals` | Cria uma meta financeira |
| `GET` | `/api/v1/finance/goals/{id}` | Consulta uma meta financeira |
| `PUT` | `/api/v1/finance/goals/{id}` | Atualiza nome, objetivo e prazo |
| `DELETE` | `/api/v1/finance/goals/{id}` | Exclui uma meta financeira |
| `GET` | `/api/v1/finance/goals/{id}/contributions` | Lista o histórico de aportes da meta |
| `POST` | `/api/v1/finance/goals/{id}/contributions` | Registra um aporte manual ou vinculado a uma receita e recalcula o progresso |
| `DELETE` | `/api/v1/finance/goals/{id}/contributions/{contributionId}` | Remove um aporte e recalcula o progresso |
| `GET` | `/api/v1/finance/projections/cash-flow?months=6` | Projeta de 1 a 12 meses usando lançamentos registrados e recorrências futuras |
| `GET` | `/openapi/v1.json` | Documento OpenAPI |
| `GET` | `/swagger` | Swagger UI |

O endpoint interno `DELETE /api/v1/internal/account-data` exclui receitas, despesas, recorrências, orçamentos e metas privadas do usuário do header interno.

O parâmetro `month` é opcional. Quando omitido, resumo, tendência e orçamento usam o mês atual em UTC. A tendência termina no mês de referência e aceita de 2 a 12 meses consecutivos.

As listagens aceitam filtros inclusivos: `from` e `to` no formato `yyyy-MM-dd`; despesas também aceitam `category`. Regras recorrentes materializam ocorrências vencidas de forma idempotente ao serem criadas, ao consultar dados do usuário e diariamente às `00:05`.

O valor atual de uma meta é controlado pelo ledger de aportes. A criação aceita um saldo inicial; depois disso, alterações de progresso usam os endpoints de `contributions`, preservando data e observação de cada movimentação. Um aporte pode informar `sourceIncomeId`; o serviço valida que a receita pertence ao mesmo usuário e grava um snapshot da descrição, valor e data para manter a rastreabilidade mesmo se a receita for alterada ou removida posteriormente.

Receitas retornam `goalAllocatedAmount` e `goalAvailableAmount`. A soma dos aportes vinculados nunca pode ultrapassar o valor da receita, inclusive em requisições concorrentes, e a receita não pode ser reduzida para um valor inferior ao que já está reservado em metas. Excluir um aporte libera novamente o respectivo valor.

Cada usuário possui as seis categorias padrão e pode criar categorias próprias. Os códigos são estáveis para preservar relatórios ao renomear; categorias padrão são protegidas e categorias personalizadas só podem ser excluídas quando não possuem despesas, orçamentos ou recorrências vinculadas.

O Finance Service não emite nem valida JWT. A autenticação é responsabilidade exclusiva do BFF, e no Docker Compose este serviço permanece em rede interna.
O endpoint de ciclo de vida é idempotente e recebe `X-Finance-Control-User-Id` somente pela rede interna.

## Observabilidade

Cada requisição recebe um UUID no header `X-Correlation-ID`. O serviço preserva
um valor válido propagado pelo BFF ou gera um novo, devolve-o na resposta e o
inclui nas respostas ProblemDetails. Os logs de console usam JSON no formato
Logstash e registram o ID de correlação, método, caminho, status HTTP e duração,
sem registrar payloads ou identificadores financeiros.

## Persistência

O schema é controlado exclusivamente pelo Flyway. O Hibernate usa `ddl-auto: validate` e não cria ou modifica tabelas automaticamente.

Migration atual:

- `V1__create_finance_tables.sql`: tabelas `incomes` e `expenses`, constraints e índices.
- `V2__scope_finance_data_by_user.sql`: isolamento das receitas e despesas pelo proprietário.
- `V3__add_budgets_and_recurring_transactions.sql`: regras recorrentes, ocorrências e orçamentos mensais.
- `V4__add_financial_goals.sql`: metas financeiras isoladas por usuário, constraints e índice por prazo.
- `V5__add_financial_goal_contributions.sql`: ledger de aportes com backfill do saldo inicial, isolamento por usuário e atualização segura do progresso.
- `V6__link_goal_contributions_to_incomes.sql`: vínculo opcional com receitas e snapshot auditável da origem do aporte.
- `V7__add_custom_finance_categories.sql`: catálogo de categorias por usuário, backfill dos códigos padrão e integridade referencial para despesas, orçamentos e recorrências.

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

## Integração contínua

O workflow `.github/workflows/ci.yml` é executado em pushes e pull requests para
`main` e `develop`, além de permitir execução manual. A pipeline usa o próprio
Dockerfile, que fixa Maven `3.9.12` e Java `21`, para compilar o serviço, executar
os testes e validar a imagem final.

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
