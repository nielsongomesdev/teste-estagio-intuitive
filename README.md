# Monitor ANS - Intuitive Care (Desafio Técnico)

O projeto combina a segurança e robustez do **Java (Spring Boot)** no backend com a performance e reatividade do **Vue.js** no frontend, apresentando uma interface moderna alinhada à identidade visual da empresa.

---

## 🚀 Como Executar o Projeto

A arquitetura foi pensada para ser **Simples**, **Segura** e **Performática**. O código segue boas práticas de **Clean Code**, separação de responsabilidades (Repository Pattern) e otimizações de banco de dados.

### Pré-requisitos
* **Java 17** (ou superior).
* **Maven**.
* **MySQL 8.0** (Schema `intuitive_db` na porta `3306`).

### Passo a Passo
1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/nielsongomesdev/teste-estagio-intuitive.git](https://github.com/nielsongomesdev/teste-estagio-intuitive.git)
    cd teste-estagio-intuitive
    ```

2.  **Configuração do Banco:**
    * Certifique-se de que o MySQL está rodando.
    * O sistema busca as variáveis de ambiente `DB_USER` e `DB_PASSWORD`. Caso não definidas, usa `root`/`root` como padrão (fallback) para desenvolvimento local.

3.  **Executar Testes (Opcional):**
    Para validar a integridade das regras de negócio e conversores:
    ```bash
    mvn test
    ```

4.  **Executar a Aplicação:**
    Na raiz do projeto, execute:
    ```bash
    mvn spring-boot:run
    ```

5.  **Acessar:**
    Abra o navegador em: **[http://localhost:8080](http://localhost:8080)**

---

## 🛠️ Decisões Técnicas e Otimizações

Conforme solicitado no desafio, abaixo estão as justificativas para as escolhas arquiteturais e as melhorias de performance implementadas:

### 1. Backend: Java Spring Boot
* **Performance (Batch Processing):** A inserção de dados da ANS foi otimizada utilizando `JDBC Batch Updates`. Isso permite processar milhares de registros em segundos, evitando o gargalo de inserções linha a linha.
* **Integridade de Dados (Encoding):** Tratamento explícito do charset **ISO-8859-1** na leitura dos CSVs, garantindo que acentos e caracteres especiais (ex: "SAÚDE") sejam salvos corretamente no banco.
* **Clean Code:** Adoção do **Repository Pattern** e separação em camadas (*Service, Controller, Repository*), desacoplando as regras de negócio da persistência.
* **Paginação:** Estratégia **Offset-based** (Padrão JPA), escolhida pela eficiência para o volume de dados do teste.

### 2. Frontend: Vue.js (Modo CDN)
* **Decisão:** Vue.js 3 importado via `<script>` diretamente no HTML.
* **Justificativa:** Cumpre o requisito de interface web sem adicionar complexidade de build (Webpack/Vite) para o avaliador. Basta ter o Java instalado para rodar o sistema completo.
* **UX/UI:** Identidade visual fiel à marca (**Roxo Intuitive**) e uso de *tooltips* para melhor visualização de dados longos.

### 3. Testes e Qualidade
* **Testes Unitários:** Implementação de **JUnit 5** para a classe `MathUtils`, garantindo que conversões monetárias críticas e tratamentos de nulos funcionem sem falhas.

---

##  Testes de API (Postman)

Uma coleção do Postman foi incluída na raiz do projeto para validação isolada do Backend.

* **Arquivo:** `intuitive_test_collection.json`
* **Cenários Cobertos:**
    1.  Listagem Geral Paginada.
    2.  Busca Textual (Razão Social).
    3.  Busca Específica (CNPJ ou Registro ANS).
