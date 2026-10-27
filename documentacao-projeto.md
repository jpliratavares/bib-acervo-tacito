# Sistema de Gestão de Biblioteca/Acervo

## 1. Visão Geral

Este projeto implementa um **Sistema de Gestão de Biblioteca/Acervo** desenvolvido em Java como parte da disciplina de **Programação Orientada a Objetos — DCX/CCAE/UFPB**.

O sistema foi projetado de forma incremental e utiliza conceitos fundamentais de orientação a objetos, como:

- encapsulamento;
- abstração por interfaces;
- separação de responsabilidades;
- composição;
- tratamento de exceções;
- coleções;
- imutabilidade de identificadores;
- testes automatizados;
- desenvolvimento orientado a testes.

A aplicação permite representar e gerenciar itens de um acervo, seus exemplares e usuários. O contrato também prevê funcionalidades de empréstimos, reservas, persistência e geração de relatórios.

A extensão específica desenvolvida pela equipe utiliza como domínio **jogos de tabuleiro**.

---

# 2. Tecnologias Utilizadas

O projeto utiliza:

- **Java 21**
- **Maven**
- **JUnit 5**
- **Git**
- **GitHub**
- **GitHub Actions**

O projeto é empacotado como um arquivo `JAR` e utiliza Maven para gerenciamento do ciclo de compilação e testes.

---

# 3. Pré-requisitos

Para compilar e testar o projeto são necessários:

- JDK 21 ou superior compatível;
- Apache Maven;
- Git, caso seja necessário clonar o projeto.

Para verificar as versões instaladas:

```bash
java -version
mvn -version
git --version
```

---

# 4. Obtendo o projeto

Clone o repositório:

```bash
git clone https://github.com/poo-dcx-2026-2/Repo-Aquiles-JP.git
```

Entre no diretório:

```bash
cd Repo-Aquiles-JP
```

---

# 5. Compilação e testes

Para compilar o projeto e executar todos os testes:

```bash
mvn -B verify
```

Para executar somente os testes:

```bash
mvn -B test
```

Também é possível utilizar:

```bash
mvn test
```

O projeto utiliza **JUnit 5** para os testes automatizados.

---

# 6. Estrutura do projeto

A estrutura principal é:

```text
Repo-Aquiles-JP/
│
├── .github/
│   └── workflows/
│       └── build.yml
│
├── dados/
│   └── itens-exemplo.csv
│
├── docs/
│   ├── FLUXO-DE-DESENVOLVIMENTO.md
│   └── modelo.puml
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── br/ufpb/dcx/poo/biblioteca/
│   │           ├── Fabrica.java
│   │           ├── contrato/
│   │           └── inicial/
│   │
│   └── test/
│       └── java/
│           └── br/ufpb/dcx/poo/biblioteca/
│
├── DECLARACAO-DE-USO-DE-IA.md
├── README.md
└── pom.xml
```

---

# 7. Arquitetura do sistema

O projeto separa o sistema em três áreas principais:

```text
Fabrica
   |
   v
Biblioteca
   |
   +-----------------+
   |                 |
   v                 v
AcervoService   UsuarioService
   |
   +-----------------+
   |
   v
EmprestimoService
   |
   v
RelatorioService
```

A interface `Biblioteca` funciona como o ponto central de acesso aos quatro serviços.

A implementação atual é fornecida pela classe `BibliotecaInicial`.

---

# 8. Ponto de entrada: Fabrica

A classe:

```java
br.ufpb.dcx.poo.biblioteca.Fabrica
```

possui o método:

```java
public static Biblioteca novaBiblioteca()
```

Esse método cria uma nova instância do sistema:

```java
return new BibliotecaInicial();
```

Cada chamada deve retornar uma biblioteca nova e independente.

Exemplo:

```java
Biblioteca biblioteca = Fabrica.novaBiblioteca();
```

A classe `Fabrica` é especialmente importante porque é utilizada pelos testes para obter uma implementação válida da interface `Biblioteca`.

---

# 9. Pacote `contrato`

O pacote:

```text
br.ufpb.dcx.poo.biblioteca.contrato
```

define a API pública do sistema.

Esse pacote deve ser considerado **congelado**: suas interfaces, assinaturas e tipos não devem ser modificados.

Os principais contratos são:

- `Biblioteca`
- `AcervoService`
- `UsuarioService`
- `EmprestimoService`
- `RelatorioService`

Também fazem parte do contrato:

- objetos de visualização (`*View`);
- enums de status;
- exceções da aplicação.

---

# 10. Biblioteca

A interface `Biblioteca` oferece acesso aos quatro módulos:

```java
AcervoService acervo();

UsuarioService usuarios();

EmprestimoService emprestimos();

RelatorioService relatorios();
```

Também define as operações de persistência:

```java
void salvar();

void carregar();
```

Na implementação atual, `salvar()` e `carregar()` ainda fazem parte das funcionalidades previstas para etapas posteriores.

---

# 11. Gerenciamento do acervo

O serviço de acervo é definido pela interface:

```java
AcervoService
```

e atualmente implementado por:

```java
AcervoEmMemoria
```

O acervo utiliza duas estruturas principais:

```java
Map<String, Item> itensPorCodigo
Map<String, Exemplar> exemplaresPorTombo
```

Isso permite acesso direto aos objetos pelos seus identificadores.

---

## 11.1 Cadastro de itens

Um item pode ser cadastrado através de:

```java
cadastrarItem(
    String codigo,
    String titulo,
    String autoria,
    String categoria,
    int ano
)
```

O código funciona como identificador único.

Exemplo conceitual:

```java
biblioteca.acervo().cadastrarItem(
    "JG001",
    "Catan",
    "Klaus Teuber",
    "Estratégia",
    1995
);
```

O sistema rejeita códigos duplicados através de:

```java
RecursoDuplicadoException
```

Código e título também não podem ser vazios ou nulos.

---

## 11.2 Busca de item

A busca utiliza:

```java
buscarItem(String codigo)
```

Caso o código não exista:

```java
RecursoNaoEncontradoException
```

é lançada.

As consultas não devolvem diretamente a entidade interna `Item`. Elas devolvem:

```java
ItemView
```

Isso evita que código externo modifique diretamente o estado interno do domínio.

---

## 11.3 Listagem de itens

A operação:

```java
listarItens()
```

retorna os itens ordenados alfabeticamente por título, ignorando diferenças entre letras maiúsculas e minúsculas.

---

## 11.4 Pesquisa por título

A operação:

```java
buscarPorTitulo(String trecho)
```

realiza busca parcial e sem diferenciação entre maiúsculas e minúsculas.

Assim, uma pesquisa como:

```text
catan
```

pode localizar um título contendo:

```text
Catan
```

---

## 11.5 Pesquisa por categoria

O método:

```java
buscarPorCategoria(String categoria)
```

retorna os itens que pertencem à categoria informada.

A comparação também ignora diferenças de caixa.

---

# 12. Exemplares

Um `Item` representa a obra ou produto existente no catálogo.

Um `Exemplar`, por sua vez, representa uma unidade física específica daquele item.

Por exemplo:

```text
Item
Catan

Exemplares
├── TOMBO-001
├── TOMBO-002
└── TOMBO-003
```

Essa separação permite possuir várias unidades do mesmo título.

---

## 12.1 Adição de exemplar

Um exemplar é cadastrado usando:

```java
adicionarExemplar(String codigoDoItem, String tombo)
```

O tombo deve ser globalmente único.

O sistema mantém um mapa específico:

```java
Map<String, Exemplar> exemplaresPorTombo
```

para realizar essa verificação de forma eficiente.

---

## 12.2 Listagem de exemplares

Os exemplares podem ser consultados através de:

```java
listarExemplares(String codigoDoItem)
```

O resultado utiliza:

```java
ExemplarView
```

e é ordenado pelo tombo.

---

# 13. Gerenciamento de usuários

O serviço:

```java
UsuarioService
```

é implementado atualmente por:

```java
UsuariosEmMemoria
```

Os usuários são armazenados através de:

```java
Map<String, Usuario> usuariosPorMatricula
```

A matrícula funciona como identificador único.

---

## 13.1 Cadastro de usuário

Um usuário pode ser cadastrado utilizando:

```java
cadastrarUsuario(String matricula, String nome)
```

Matrícula e nome são obrigatórios.

Também não é permitido cadastrar dois usuários com a mesma matrícula.

---

## 13.2 Consulta

A consulta individual utiliza:

```java
buscarUsuario(String matricula)
```

e devolve um:

```java
UsuarioView
```

Também é possível obter todos os usuários através de:

```java
listarUsuarios()
```

A lista é ordenada alfabeticamente por nome.

---

## 13.3 Ativação e desativação

O contrato oferece:

```java
desativarUsuario(String matricula)
```

e:

```java
reativarUsuario(String matricula)
```

A implementação atual delega essas alterações para o próprio objeto `Usuario`.

A matrícula continua imutável durante todo o ciclo de vida do usuário.

---

# 14. Empréstimos

O contrato de empréstimos é definido por:

```java
EmprestimoService
```

Ele estabelece duas constantes importantes:

```java
LIMITE_DE_EMPRESTIMOS = 3
PRAZO_EM_DIAS = 14
```

Portanto, o modelo prevê no máximo **três empréstimos simultâneos por usuário** e um prazo padrão de **14 dias**.

---

## 14.1 Operações previstas

O serviço define:

```java
emprestar(...)
devolver(...)
listarEmprestimosDoUsuario(...)
listarAtrasados(...)
reservar(...)
listarReservas(...)
cancelarReserva(...)
```

Também existem exceções específicas para regras desse domínio, como:

```java
ExemplarIndisponivelException
UsuarioInativoException
LimiteDeEmprestimosExcedidoException
OperacaoNaoPermitidaException
```

Na Entrega 2 esse módulo é implementado por `EmprestimosEmMemoria`, que aplica o limite,
o prazo, a devolução, os atrasos e a fila FIFO de reservas.

---

# 15. Reservas

O contrato também prevê reserva de itens:

```java
String reservar(
    String codigoDoItem,
    String matricula,
    LocalDate data
)
```

e consulta das reservas:

```java
List<ReservaView> listarReservas(String codigoDoItem)
```

Uma reserva pode ser cancelada através do seu identificador:

```java
cancelarReserva(String idDaReserva)
```

---

# 16. Relatórios

O serviço:

```java
RelatorioService
```

define consultas de maior nível sobre os dados da biblioteca.

Entre elas:

```java
itensMaisEmprestados(int n)
```

Retorna os itens com maior número de empréstimos.

```java
emprestimosPorCategoria()
```

Agrupa o número de empréstimos por categoria.

```java
usuariosComAtraso(LocalDate data)
```

Localiza usuários que possuem empréstimos atrasados.

```java
taxaDeOcupacaoDoAcervo()
```

Calcula a proporção de exemplares que estão ocupados.

```java
importarItensEmLote(Path arquivo)
```

Permite importar itens a partir de um arquivo.

A implementação atual é:

```java
RelatoriosNaoImplementados
```

e essas funcionalidades ainda serão desenvolvidas.

---

# 17. Objetos `View`

O contrato disponibiliza objetos específicos para retornar informações ao código cliente:

- `ItemView`
- `ExemplarView`
- `UsuarioView`
- `EmprestimoView`
- `ReservaView`

Esse padrão cria uma separação entre:

```text
Entidade interna
       ↓
   conversão
       ↓
Objeto View
       ↓
código cliente
```

Dessa maneira, os objetos internos do sistema não são expostos diretamente.

Isso reduz o acoplamento e protege o estado das entidades.

---

# 18. Estados do domínio

O sistema possui tipos específicos para representar estados.

Entre eles:

```java
StatusExemplar
```

e:

```java
StatusEmprestimo
```

Esses enums permitem representar explicitamente diferentes situações de exemplares e empréstimos sem depender de textos arbitrários.

---

# 19. Tratamento de exceções

O projeto possui uma hierarquia própria de erros.

A exceção-base é:

```java
BibliotecaException
```

Entre as exceções especializadas estão:

```text
DadosInvalidosException
ExemplarIndisponivelException
LimiteDeEmprestimosExcedidoException
OperacaoNaoPermitidaException
PersistenciaException
RecursoDuplicadoException
RecursoNaoEncontradoException
UsuarioInativoException
```

O objetivo dessas exceções é expressar claramente qual regra de negócio impediu determinada operação.

Exemplo:

```java
try {
    biblioteca.acervo().buscarItem("INEXISTENTE");
} catch (RecursoNaoEncontradoException e) {
    System.out.println(e.getMessage());
}
```

---

# 20. Modelo de dados

Atualmente, as principais entidades são:

```text
Item
Exemplar
Usuario
JogoDeTabuleiro
```

Uma representação simplificada é:

```text
Item
├── codigo
├── titulo
├── autoria
├── categoria
├── ano
└── exemplares
       |
       +---- Exemplar
             ├── tombo
             ├── codigoDoItem
             └── status

Usuario
├── matricula
├── nome
└── ativo
```

---

# 21. Identificadores imutáveis

Alguns atributos representam a identidade das entidades:

| Entidade | Identificador |
|---|---|
| Item | código |
| Exemplar | tombo |
| Usuário | matrícula |

Esses identificadores são definidos durante a criação e não possuem setters públicos.

Essa decisão impede que a identidade de um objeto mude arbitrariamente depois de seu cadastro.

---

# 22. Coleções utilizadas

O projeto utiliza principalmente `Map` e `List`.

## `Map<String, Item>`

Armazena itens indexados pelo código:

```java
Map<String, Item> itensPorCodigo
```

Permite busca direta e facilita a verificação de duplicidade.

## `Map<String, Exemplar>`

Armazena exemplares indexados pelo tombo:

```java
Map<String, Exemplar> exemplaresPorTombo
```

Garante que o mesmo tombo não seja reutilizado.

## `Map<String, Usuario>`

Armazena usuários pela matrícula:

```java
Map<String, Usuario> usuariosPorMatricula
```

## `List<Exemplar>`

Mantém os exemplares associados a determinado item.

---

# 23. Extensão autoral: jogos de tabuleiro

A extensão própria da equipe é representada por:

```java
JogoDeTabuleiro
```

Ela adiciona informações específicas sobre a quantidade suportada de jogadores.

Os atributos são:

```java
codigoDoItem
jogadoresMinimos
jogadoresMaximos
```

---

## 23.1 Regra de negócio

A faixa de jogadores deve ser válida.

O sistema exige:

```text
jogadoresMinimos >= 1
```

e:

```text
jogadoresMaximos >= jogadoresMinimos
```

Assim:

```java
new JogoDeTabuleiro("JG001", 2, 4);
```

é válido.

Por outro lado:

```java
new JogoDeTabuleiro("JG001", 4, 2);
```

é inválido.

Nesse caso é lançada:

```java
DadosInvalidosException
```

---

# 24. Testes automatizados

Os testes estão localizados em:

```text
src/test/java/br/ufpb/dcx/poo/biblioteca/
```

Entre os testes atualmente presentes estão:

```text
AcervoTest.java
AcervoTddTest.java
FabricaTest.java
JogoDeTabuleiroTest.java
UsuarioTest.java
```

Eles validam aspectos como:

- criação independente das bibliotecas;
- cadastro de itens;
- prevenção de duplicidade;
- busca de itens;
- exemplares;
- usuários;
- validações;
- extensão de jogos de tabuleiro;
- regressões encontradas durante o desenvolvimento.

---

# 25. Desenvolvimento orientado a testes

O projeto contém um exemplo de correção orientada a testes.

Um defeito existente anteriormente comparava objetos `String` com:

```java
==
```

Em Java, essa operação compara a identidade das referências, e não necessariamente seu conteúdo.

O comportamento correto para identificação textual depende de igualdade de conteúdo.

A implementação atual utiliza o código como chave de:

```java
Map<String, Item>
```

o que permite realizar a busca utilizando a igualdade definida por `String`.

Um teste de regressão mantém esse comportamento protegido contra futuras alterações.

---

# 26. Integração contínua

O repositório possui um workflow GitHub Actions em:

```text
.github/workflows/build.yml
```

A integração contínua executa a validação do projeto a cada alteração configurada pelo workflow.

O comando principal utilizado pelo projeto é:

```bash
mvn -B verify
```

Isso permite detectar automaticamente erros de compilação e regressões nos testes.

---

# 27. Persistência

O contrato define:

```java
salvar()
carregar()
```

na interface `Biblioteca`.

Também existe:

```java
PersistenciaException
```

para representar falhas relacionadas à leitura ou escrita dos dados.

`BibliotecaInicial` persiste o estado em arquivo com serialização Java. O construtor
padrão usa `target/biblioteca-estado.dat` e `BibliotecaInicial(Path)` permite testes
isolados. Arquivo ausente, conteúdo inválido ou falha de escrita lançam
`PersistenciaException`. O subtipo `JogoDeTabuleiro` é preservado.

---

# 28. Importação em lote

O projeto contém:

```text
dados/itens-exemplo.csv
```

e o contrato de relatórios define:

```java
int importarItensEmLote(Path arquivo)
```

A funcionalidade permitirá cadastrar vários itens a partir de um arquivo externo.

Essa operação faz parte das funcionalidades previstas para evolução do sistema.

---

# 29. Funcionalidades atuais

A Entrega 2 cobre acervo, usuários, circulação e persistência.

### Acervo

- cadastro de item;
- busca por código;
- listagem;
- busca por trecho do título;
- busca por categoria;
- adição de exemplar;
- listagem de exemplares;
- baixa de exemplar (`StatusExemplar.INDISPONIVEL`).

### Usuários

- cadastro;
- consulta;
- listagem;
- desativação bloqueada quando há empréstimo em aberto;
- reativação;
- contagem real de empréstimos ativos.

### Circulação

- empréstimo com prazo de 14 dias e limite de 3;
- devolução;
- listagem de empréstimos e de atrasos;
- fila FIFO de reservas, cancelamento e prioridade no empréstimo.

### Persistência

- `salvar` e `carregar`, inclusive o subtipo `JogoDeTabuleiro`.

### Extensão

- representação de jogos de tabuleiro como subtipo de `Item`;
- validação da faixa mínima e máxima de jogadores.

---

# 30. Funcionalidades em evolução

A Entrega 3 ainda cobre:

- importação em lote;
- relatórios;
- estatísticas do acervo.

Métodos ainda não concluídos utilizam `UnsupportedOperationException` para indicar explicitamente que aquela funcionalidade pertence a uma etapa futura.

---

# 31. Exemplo de utilização

Um fluxo básico do sistema pode ser escrito da seguinte forma:

```java
Biblioteca biblioteca = Fabrica.novaBiblioteca();

biblioteca.acervo().cadastrarItem(
    "JG001",
    "Catan",
    "Klaus Teuber",
    "Estratégia",
    1995
);

biblioteca.acervo().adicionarExemplar(
    "JG001",
    "TOMBO-001"
);

biblioteca.usuarios().cadastrarUsuario(
    "20260001",
    "João"
);

ItemView item = biblioteca.acervo().buscarItem("JG001");

System.out.println(item.titulo());

biblioteca.acervo()
        .listarExemplares("JG001")
        .forEach(System.out::println);

biblioteca.usuarios()
        .listarUsuarios()
        .forEach(System.out::println);
```

---

# 32. Princípios de projeto adotados

A implementação atual procura preservar alguns princípios importantes.

### Encapsulamento

As entidades internas não são devolvidas diretamente aos consumidores do sistema.

### Separação de responsabilidades

Cada serviço é responsável por uma área:

```text
AcervoService       → catálogo e exemplares
UsuarioService      → usuários
EmprestimoService   → circulação do acervo
RelatorioService    → consultas agregadas
```

### Identidade imutável

Códigos, tombos e matrículas não mudam após a criação.

### Contratos estáveis

As interfaces do pacote `contrato` representam a API utilizada pelos testes e consumidores.

### Testabilidade

`Fabrica.novaBiblioteca()` gera instâncias independentes, permitindo que os testes não compartilhem estado entre execuções.

---

# 33. Observações para desenvolvimento

Ao continuar o desenvolvimento do projeto:

1. não modificar as assinaturas existentes no pacote `contrato`;
2. manter `Fabrica.novaBiblioteca()` funcional;
3. garantir que cada chamada à fábrica gere uma instância independente;
4. executar os testes antes de realizar commits;
5. adicionar testes para novas regras de negócio;
6. utilizar as exceções específicas do contrato;
7. evitar expor diretamente entidades mutáveis;
8. manter identificadores únicos consistentes;
9. atualizar esta documentação quando novas entregas forem implementadas.

---

# 34. Comandos úteis

Executar testes:

```bash
mvn test
```

Validar o projeto:

```bash
mvn verify
```

Limpar arquivos de build:

```bash
mvn clean
```

Limpar e validar novamente:

```bash
mvn clean verify
```

Gerar Javadoc:

```bash
mvn javadoc:javadoc
```

---

# 35. Status do projeto

O projeto encontra-se em desenvolvimento incremental.

O núcleo de acervo, usuários, empréstimos, reservas e persistência já possui implementação funcional. Relatórios e importação em lote permanecem para a Entrega 3.

A arquitetura baseada em contratos permite que essas implementações sejam substituídas ou ampliadas sem alterar a API principal utilizada pelos consumidores do sistema.
