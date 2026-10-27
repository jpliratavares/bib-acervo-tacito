# Fluxo de desenvolvimento — análise inicial

**Data:** 29/09/2026  
**Etapa:** levantamento técnico antes da implementação  
**Objetivo:** demonstrar o entendimento da base fornecida, planejar a evolução sem
alterar o contrato congelado e registrar o uso responsável de IA.

## 1. Escopo e restrições identificados

1. O pacote `br.ufpb.dcx.poo.biblioteca.contrato` é a API pública congelada. Seus
   métodos, tipos de retorno, estados e exceções não devem ser modificados.
2. `Fabrica.novaBiblioteca()` é a porta de entrada dos testes. Ela deve manter nome,
   pacote e assinatura, além de produzir uma biblioteca nova e independente em cada
   chamada.
3. O pacote `inicial` é somente a base de partida. Portanto, será possível refatorar
   suas classes ou substituí-las por implementações de domínio mais adequadas, desde
   que as interfaces do contrato continuem sendo atendidas.
4. A integração contínua executa `mvn -B verify` com Java 21. O mesmo comando será
   usado localmente antes de cada entrega.

## 2. Leitura da arquitetura atual

| Camada/componente | Papel atual | Situação observada |
|---|---|---|
| `contrato` | Define os quatro serviços, *views*, estados e exceções. | Congelado; será usado como especificação de implementação e testes. |
| `Fabrica` | Cria o objeto que implementa `Biblioteca`. | Já devolve uma instância nova de `BibliotecaInicial`. |
| `BibliotecaInicial` | Agrega acervo, usuários, empréstimos e relatórios. | Acervo e usuários estão isolados; empréstimos e relatórios ainda não recebem as dependências necessárias para aplicar regras integradas. |
| `AcervoEmMemoria` | Mantém itens em memória e produz `ItemView`. | Cadastro, busca e listagem simples existem; busca por título e gestão de exemplares estão pendentes. |
| `UsuariosEmMemoria` | Mantém usuários em memória. | Cadastro, busca e listagem simples existem; estado ativo e empréstimos ativos ainda não são persistidos em um objeto de domínio. |
| Serviços de empréstimo/relatório | Expõem as operações previstas no contrato. | São esqueletos que lançam `UnsupportedOperationException`, previstos respectivamente para as Entregas 2 e 3. |

## 3. Achados técnicos que orientam a próxima implementação

### 3.1 Defeito reproduzível no acervo

O método privado de localização compara `String` com `==`. Essa comparação verifica
identidade de objetos, não igualdade textual; portanto, uma busca por um código com o
mesmo conteúdo, mas instância diferente, pode não encontrar o item cadastrado. A
correção planejada é usar igualdade de valor (`equals`) de forma segura e criar um
teste de regressão que use `new String(...)` ou outro valor equivalente não internado.

### 3.2 Modelagem e encapsulamento

- `Item` e `Exemplar` possuem *setters* para atributos que representam identidade e
  associação. Isso fragiliza invariantes como código/tombo únicos e o vínculo de um
  exemplar ao item.
- `Item#getExemplares()` devolve a lista mutável interna, permitindo que qualquer
  consumidor altere o acervo sem as regras do serviço.
- `UsuariosEmMemoria` usa listas paralelas de matrículas e nomes. A evolução para
  usuário ativo, empréstimos ativos e validações torna mais segura a criação de uma
  entidade `Usuario` e o uso de estruturas indexadas por identificador.

### 3.3 Regras a preservar e testar

- item: código e título obrigatórios; código não pode repetir;
- usuário: matrícula e nome obrigatórios; matrícula não pode repetir;
- listagens: itens por título e usuários por nome, sem retornar `null`;
- exemplar: inicia disponível e seu tombo deve ser único no acervo inteiro;
- empréstimo: respeitar o limite de três empréstimos e o prazo de 14 dias definidos
  no contrato;
- erros: utilizar as exceções específicas do pacote congelado, não exceções genéricas.

## 4. Estratégia de qualidade: Test-Driven Development (TDD)

O projeto adotará **TDD como prática de engenharia**. Isso significa que uma alteração
de comportamento não será considerada pronta apenas por compilar: ela deverá ser
especificada por um teste automatizado antes da implementação e permanecer protegida
contra regressões na suíte.

### 4.1 Referências de qualidade do repositório

As seguintes concentrações de testes automatizados são a referência inicial para a
organização da suíte e para o padrão de qualidade esperado:

| Conjunto de testes | Comportamentos que estabelecem o padrão |
|---|---|
| `FabricaTest` | Disponibilização dos quatro serviços e independência entre bibliotecas criadas pela fábrica. |
| `AcervoTest` | Cadastro, consultas, validações, exceções específicas, ordenação, coleções vazias e cenários planejados de exemplares e busca por título. |
| `UsuarioTest` | Cadastro, busca, duplicidade, validação de entrada e ordenação de usuários. |
| Interfaces e exceções em `contrato` | Assinaturas, tipos de retorno, estados, limites e categorias de erro que toda implementação deve preservar. |

Os testes marcados com `@Disabled` não são descartados: representam requisitos das
próximas entregas. Eles serão habilitados quando o comportamento correspondente for
implementado e complementados por testes novos de fronteira, falha e regressão. Os
testes adicionais serão agrupados por serviço e cenário de negócio, mantendo nomes que
expressem o comportamento observável em vez de detalhes da implementação.

### 4.2 Ciclo obrigatório por requisito: Red → Green → Refactor

1. **Red — especificar:** ler a assinatura e as exceções do contrato, escolher um
   cenário dos testes de exemplo ou uma regra de negócio e escrever/ativar um teste
   JUnit que inicialmente falhe.
2. **Green — implementar o mínimo:** alterar somente a implementação fora de
   `contrato/` até o teste novo e os testes existentes passarem.
3. **Refactor — melhorar sem mudar comportamento:** remover duplicação, fortalecer
   encapsulamento e nomes, mantendo a suíte verde.
4. **Validar:** executar a suíte completa com `mvn -B verify`, revisar os casos de
   exceção, ordenação, valores vazios e independência das instâncias.

Cada defeito encontrado deverá primeiro receber um **teste de regressão** que o
reproduza. Por exemplo, a falha de comparação de códigos por identidade será coberta
com dois objetos `String` de mesmo conteúdo; só então a busca será corrigida. Assim, o
teste demonstra a causa, confirma a correção e impede o retorno do defeito.

## 5. Fluxo de trabalho a ser seguido

```text
Ler contrato, exceções e teste público de referência
        ↓
RED: escrever/ativar teste do comportamento esperado e casos-limite
        ↓
Executar teste para observar a falha inicial
        ↓
GREEN: implementar o mínimo apenas fora de contrato/
        ↓
REFACTOR: melhorar o desenho com a suíte verde
        ↓
Executar mvn -B verify e revisar toda a concentração de testes do serviço
        ↓
Revisar regra de negócio, exceções, ordenação e independência da fábrica
        ↓
Atualizar esta documentação e a declaração de uso de IA
```

## 6. Plano incremental

### Entrega 1 — acervo e modelagem

1. Criar testes de regressão para a igualdade de códigos e para validações de entrada.
2. Implementar busca por trecho de título, ignorando maiúsculas/minúsculas.
3. Implementar adição e listagem de exemplares, garantindo tombo globalmente único.
4. Refatorar as entidades para proteger invariantes e não expor coleções mutáveis.

### Entrega 2 — usuários, empréstimos e persistência

1. Modelar o estado ativo do usuário e impedir operações não permitidas.
2. Integrar acervo, usuários e empréstimos para controlar disponibilidade, devolução,
   atrasos e limite de empréstimos.
3. Implementar reservas e a respectiva fila por item.
4. Definir e testar a estratégia de salvar/carregar, convertendo erros de I/O em
   `PersistenciaException`.

### Entrega 3 — relatórios e importação

1. Implementar relatórios a partir dos dados de domínio, com ordenação e critérios
   explicitamente testados.
2. Importar o CSV de exemplo com validação, contagem de itens aceitos e tratamento de
   falhas de persistência.
3. Documentar a extensão autoral do acervo e ao menos uma regra de negócio própria.

## 7. Registro de validação desta etapa

- Foram inspecionados `README.md`, `pom.xml`, o pacote `contrato`, as classes em
  `inicial`, `Fabrica`, os testes públicos, o CSV de exemplo e o workflow de CI.
- O comando `mvn -B verify` foi iniciado. A compilação não prosseguiu porque o Maven
  recebeu HTTP 403 ao tentar baixar `maven-resources-plugin:3.3.1` do Maven Central;
  não houve falha atribuível ao código-fonte nesta etapa.
- Nenhum arquivo do pacote `contrato` foi modificado.

## 8. Registro de uso de IA

O apoio de IA nesta etapa foi restrito à organização da análise, da documentação e da
estratégia de TDD.
O pedido foi objetivo: analisar o repositório, respeitar o mapa da disciplina, manter
uma declaração de uso de IA e produzir um log técnico do fluxo. As conclusões acima
foram confrontadas com o código e os testes locais; nenhuma sugestão deve ser aceita
sem compreensão, teste e revisão pela equipe.

O uso correspondente está declarado em
[`DECLARACAO-DE-USO-DE-IA.md`](../DECLARACAO-DE-USO-DE-IA.md).

## 9. Continuidade — Entrega 2

O primeiro incremento da Entrega 2 foi o estado de ativação do usuário. O teste
`desativarEReativarUsuario` foi escrito e registrado antes da implementação no commit
`9310753`, seguindo o passo **Red**. A entidade `Usuario` passou a manter o estado e
o serviço apenas delega a transição, enquanto `UsuarioView` passa a mostrar o valor
real. A integração com empréstimos ativos, a fila de reservas e a persistência estão
nos ciclos da seção 10.

## 10. Ciclos TDD da Entrega 2

Cada requisito abaixo teve o teste registrado antes da implementação. O comando de
validação da suíte foi `mvn -B verify`. O pacote `contrato` permaneceu intacto. A baixa
usa `StatusExemplar.INDISPONIVEL`, porque o enum congelado não declara `BAIXADO`: o
exemplar continua no total do item e deixa de contar como disponível.

| Ciclo | Red | Green |
|---|---|---|
| Baixa de exemplar | `0e755e43dee3259626f976aa66bbddc1f21c1d8c` `test: adicionar testes para baixa de exemplar no acervo` | `991107f328b4aa764d30f497a32c8b3c680f7fcd` `feat: implementar baixa de exemplar no acervo` |
| Bloqueio de desativação e empréstimos | `f2e5fb9620fa878820a7e310da0b1da61908adb6` `test: especificar bloqueio de desativacao de usuario com emprestimo ativo` | `411061c17fed7061a98b35422416f8a808b37d9d` `feat: implementar servico de emprestimos, devolucoes e atrasos` |
| Fila de reservas | `8dac2d2b1bd686ecdccf013b5e734b2062cff273` `test: adicionar cenarios de fila de reservas por item e cancelamento` | `17b68232136147d3bd3b24dfa5ca49966a624081` `feat: implementar fila de reservas e integracao com emprestimos` |
| Persistência | `4d9da6a5362fbfd53707553f1b197b6bde2739b6` `test: especificar persistencia de dados da biblioteca` | `0f032429453d9625aeb3b1869b43ae4c81babf69` `feat: implementar salvar e carregar estado da biblioteca` |

Regras fixadas por esses testes:

- baixa rejeita tombo vazio, inexistente, emprestado ou já baixado;
- usuário com empréstimo não devolvido não é desativado, e `emprestimosAtivos` reflete essa contagem;
- empréstimo exige exemplar `DISPONIVEL`, usuário ativo e no máximo 3 empréstimos em aberto;
- a devolução não aceita data anterior à do empréstimo;
- atraso é estrito: a data de consulta precisa ser posterior à data prevista, e empréstimo devolvido não entra na lista;
- reserva só ocorre quando não há exemplar livre para empréstimo imediato e existe exemplar circulável;
- a fila é FIFO por item; cancelar ou atender uma reserva renumera as posições a partir de 1;
- no empréstimo, as cópias disponíveis ficam comprometidas com quem está à frente na fila;
- `salvar`/`carregar` restauram o subtipo `JogoDeTabuleiro`, os status, o histórico e a ordem da fila;
- arquivo ausente, conteúdo inválido ou falha de escrita lançam `PersistenciaException` sem descartar o estado em memória quando a leitura falha.
