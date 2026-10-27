# Sistema de Gestão de Biblioteca/Acervo

Projeto incremental da disciplina de **Programação Orientada a Objetos** — DCX/CCAE/UFPB.

Este repositório é o ponto de partida do seu projeto. Ele compila, executa e tem testes passando, mas está **deliberadamente incompleto** e contém decisões de design questionáveis. Encontrá-las, justificá-las e corrigi-las faz parte da disciplina.

---

## Começando

Você precisa de **JDK 21** e **Maven**. No IntelliJ IDEA, abra a pasta do projeto e ele reconhece o `pom.xml` sozinho.

```bash
mvn -B verify      # compila e roda os testes
mvn -B test        # só os testes
```

Na Entrega 2 a suíte local termina com `Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`.

Os relatórios e a importação em lote continuam para a Entrega 3 e ainda lançam `UnsupportedOperationException`.

---

## Mapa do projeto

```
src/main/java/br/ufpb/dcx/poo/biblioteca/
├── contrato/          ← CONGELADO. Não edite nada aqui.
│   ├── Biblioteca.java              ponto único de acesso aos serviços
│   ├── AcervoService.java           itens e exemplares
│   ├── UsuarioService.java          usuários
│   ├── EmprestimoService.java       empréstimos, devoluções e reservas
│   ├── RelatorioService.java        consultas e importação em lote
│   ├── *View.java                   o que as consultas devolvem
│   ├── Status*.java                 estados de exemplar e de empréstimo
│   └── excecoes/                    a hierarquia de erros do sistema
│
├── Fabrica.java       ← nome e assinatura congelados; o corpo é seu
│
└── inicial/           ← implementação da equipe
    ├── Item.java
    ├── JogoDeTabuleiro.java       subtipo de Item, preservado na persistência
    ├── Exemplar.java
    ├── Usuario.java
    ├── Emprestimo.java
    ├── Reserva.java
    ├── EstadoBiblioteca.java      fotografia serializada do estado
    ├── AcervoEmMemoria.java
    ├── UsuariosEmMemoria.java
    ├── EmprestimosEmMemoria.java
    ├── RelatoriosNaoImplementados.java
    └── BibliotecaInicial.java     agrega os serviços e persiste o estado

src/test/java/…       ← seus testes; comece pelos que já estão aqui
dados/                ← arquivos de exemplo
.github/workflows/    ← a integração contínua, já configurada
```

---

## As duas regras

**1. O pacote `contrato` é congelado.** Não renomeie, não altere assinaturas, não acrescente nem remova métodos. Os testes de correção são escritos contra esses tipos: se você mudar qualquer coisa ali, eles não compilam e a entrega não pode ser avaliada.

**2. `Fabrica.novaBiblioteca()` precisa continuar funcionando.** É por esse método que os testes obtêm o seu sistema. Você vai trocar o que ele devolve — não troque o nome, o pacote nem a assinatura. Cada chamada precisa devolver uma instância nova e independente.

Fora isso, **tudo é seu**. O pacote `inicial` não é modelo: é matéria-prima. Você pode reescrevê-lo inteiro.

---

## O que já funciona e o que falta

| Serviço | Situação |
|---|---|
| `AcervoService` | Cadastro, busca, exemplares e baixa. A baixa marca o exemplar como `StatusExemplar.INDISPONIVEL`: o contrato congelado não tem o valor `BAIXADO`. O exemplar baixado continua em `totalDeExemplares` e sai de `exemplaresDisponiveis`. |
| `UsuarioService` | Cadastro, busca, ativação e bloqueio da desativação quando existe empréstimo em aberto. `emprestimosAtivos` conta empréstimos ainda não devolvidos. |
| `EmprestimoService` | Empréstimo, devolução, atrasos e fila FIFO de reservas, com limite de 3 e prazo de 14 dias. |
| `RelatorioService` | Esqueleto. Entrega 3. |
| `Biblioteca.salvar/carregar` | Serialização de itens (inclusive `JogoDeTabuleiro`), exemplares, usuários, empréstimos e reservas. O construtor padrão usa `target/biblioteca-estado.dat`; testes isolados recebem um `Path`. |

`RelatorioService` ainda lança `UnsupportedOperationException`. Falha de leitura, arquivo ausente ou conteúdo inválido em `salvar`/`carregar` lança `PersistenciaException`.

## Como executar

Pré-requisitos: JDK 21 e Maven.

```bash
mvn -B verify       # compila, executa os testes e valida o projeto
mvn -B test         # executa somente a suíte JUnit 5
```

O projeto usa JUnit 5 e o workflow em `.github/workflows/build.yml` executa
`mvn -B verify` em cada push e Pull Request.

## Decisões de modelagem e coleções

O domínio protege as identidades de `Item`, `Exemplar` e `Usuario`: código, tombo e
matrícula são definidos na construção e não possuem *setters*. As operações de
alteração passam pelos serviços, onde as regras de negócio são verificadas.

| Estrutura | Uso | Justificativa |
|---|---|---|
| `Map<String, Item>` | Itens indexados pelo código. | Busca e verificação de duplicidade por código são diretas e usam igualdade de conteúdo de `String`. |
| `Map<String, Exemplar>` | Exemplares indexados pelo tombo. | O tombo é globalmente único, inclusive entre itens diferentes. |
| `Map<String, Usuario>` | Usuários indexados pela matrícula. | Evita listas paralelas e mantém nome, matrícula e estado ativo no mesmo objeto. |
| `List<Exemplar>` | Exemplares pertencentes a um item. | Preserva a composição item–exemplar; o acesso externo é uma cópia imutável. |
| `List<Emprestimo>` | Histórico de empréstimos na ordem de realização. | A devolução preenche `dataDaDevolucao` sem apagar o registro. Em aberto conta como ativo, mesmo atrasado. |
| `Map<String, List<Reserva>>` | Fila FIFO de reservas por item. | A posição é o índice mais um. Cancelar ou atender uma reserva reordena o restante. |
| `Map<String, Reserva>` | Reservas ativas pelo identificador. | Permite cancelar sem varrer todas as filas. |

As consultas devolvem `ItemView`, `ExemplarView` e `UsuarioView`, não entidades
internas. Isso evita vazamento de estado do domínio.

## Relato do defeito oculto

O método de localização do código comparava textos com `==`. Em Java, `==` compara a
identidade do objeto, e não o conteúdo; portanto, dois objetos `String` com o mesmo
texto poderiam não ser reconhecidos como o mesmo código. O teste de regressão foi
registrado primeiro no commit `cceeda0` e cria duas instâncias distintas de `String`
com o valor `"L1"`. A correção substitui a comparação por uma busca no `Map`, cuja
chave usa igualdade de conteúdo. Esse fluxo mantém o defeito coberto pela suíte.

---

## Persistência

`BibliotecaInicial` grava e relê uma fotografia `EstadoBiblioteca` com serialização Java. O arquivo padrão é `target/biblioteca-estado.dat`. `new BibliotecaInicial(Path)` isola cada teste. Itens, exemplares e seus status, usuários e o flag `ativo`, o histórico de empréstimos e a ordem das filas de reserva voltam na nova instância. `JogoDeTabuleiro` estende `Item`, então o subtipo sobrevive ao `carregar()`.

## Um aviso honesto

O defeito de comparar códigos com `==` foi reproduzido, coberto e corrigido na Entrega 1. A Entrega 3 ainda não implementa relatórios nem importação em lote. Suíte verde continua sendo o piso da entrega, não a prova de que toda regra futura já está coberta.

---

## Extensão autoral

Sua equipe escolhe um acervo próprio: jogos, filmes, quadrinhos, instrumentos, obras locais, recursos de laboratório. A extensão vive **por fora** do contrato — novos tipos, novos serviços, novas regras — e precisa incluir ao menos **uma regra de negócio própria**, documentada abaixo, que os testes de correção não conhecem e que você demonstra na defesa.

### Nossa extensão

O acervo da equipe é de **jogos de tabuleiro**. `JogoDeTabuleiro` estende `Item` e
registra a faixa de participantes. A regra autoral é: o mínimo de jogadores deve ser
pelo menos 1 e o máximo não pode ser menor que o mínimo. A classe valida a regra no
construtor. `AcervoEmMemoria.cadastrarJogoDeTabuleiro` coloca esse subtipo no acervo, e
a persistência o restaura como `JogoDeTabuleiro`, não como `Item` genérico. A extensão
não altera o pacote `contrato`.

---

## Uso de ferramentas de IA

O uso é permitido como apoio, desde que declarado em [`DECLARACAO-DE-USO-DE-IA.md`](DECLARACAO-DE-USO-DE-IA.md). Você continua responsável por explicar, testar e adaptar todo o código entregue — inclusive em uma alteração feita presencialmente, sem consulta, na defesa da Entrega 3.

---

## Equipe

| Nome | Matrícula | GitHub |
|---|---|---|
| Aquiles Francisco da Silva | 20250104421 | AquilesFrancisco |
| João Pedro de Lira Tavares | 20250104501 | jpliratavares |
