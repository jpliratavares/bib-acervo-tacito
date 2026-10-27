# Declaração de uso de ferramentas de IA

Preencha e mantenha atualizado até a entrega final. A ausência da declaração, ou uma
declaração que não corresponde ao histórico do repositório, é tratada como problema de
autoria.

Usar IA não reduz a nota. Não conseguir explicar, testar e adaptar o que foi entregue,
sim.

## Equipe

| Nome | Matrícula |
|---|---|
| João Pedro de Lira Tavares | 20250104501 |
| Aquiles Francisco da Silva | 20250104421 |

## Uso declarado

Uma linha por uso relevante. Se não houve uso, escreva "Não houve uso de ferramentas de IA".

| Data | Ferramenta | Finalidade | Arquivos/trechos afetados | O que foi revisado e alterado por vocês |
|---|---|---|---|---|
| 29/09/2026 | Codex (OpenAI, gpt-5.6-terra) | Analisar a estrutura inicial, identificar responsabilidades, pendências e riscos técnicos; organizar o registro do fluxo de desenvolvimento e formalizar o uso de Test-Driven Development (TDD). | `DECLARACAO-DE-USO-DE-IA.md` e `docs/FLUXO-DE-DESENVOLVIMENTO.md`. A análise consultou contratos, implementação inicial, testes, dados, CI e configuração Maven, sem alterar o pacote `contrato`. | O escopo foi limitado à documentação da análise inicial e da estratégia TDD. As conclusões foram verificadas pela leitura do código e dos testes públicos; cada implementação futura deverá nascer de um teste, respeitar o contrato e passar por validação da equipe. |
| 29/09/2026 | Codex (OpenAI, gpt-5.6-terra) | Analisar a estrutura inicial, formalizar TDD e apoiar a implementação da Entrega 1. | `README.md`, `docs/modelo.puml`, implementação e testes fora de `contrato`. O diagrama PNG foi gerado localmente, mas não é versionado porque arquivos binários não são compatíveis com a PR. | A equipe definiu as estruturas de dados, revisou a correção para igualdade de conteúdo de códigos, adotou o ciclo Red–Green–Refactor e deve compreender e validar cada regra, entidade e teste antes da entrega. |
| 01/10/2026 | Codex (OpenAI, gpt-5.6-terra) | Conduzir o primeiro ciclo TDD da Entrega 2 para o estado de ativação de usuários. | `Usuario.java`, `UsuariosEmMemoria.java`, `UsuarioTest.java` e `docs/FLUXO-DE-DESENVOLVIMENTO.md`, sem alterar `contrato`. | O teste foi registrado antes da implementação; a equipe deve verificar que o estado pertence à entidade `Usuario`, que a *view* o expõe corretamente e que a regra de empréstimos ativos será integrada antes de impedir a desativação. |
| 27/10/2026 | Cursor (Grok 4.7) | Implementar a Entrega 2 em ciclos Red–Green: baixa de exemplar, empréstimos e atrasos, fila de reservas e persistência, além da documentação correspondente. | Testes `AcervoTest`, `AcervoTddTest`, `UsuarioTest`, `EmprestimoTest` e `PersistenciaTest`; classes de `inicial` (`EmprestimosEmMemoria`, `BibliotecaInicial`, `EstadoBiblioteca`, `JogoDeTabuleiro` e entidades de domínio); `README.md`, `docs/modelo.puml`, `docs/FLUXO-DE-DESENVOLVIMENTO.md` e esta declaração. O pacote `contrato` não foi modificado. | Cada comportamento novo foi especificado por um teste que falhava antes do código de produção. A baixa usa `StatusExemplar.INDISPONIVEL`, estado do contrato para fora de circulação. A fila é FIFO por item e a persistência serializa o subtipo `JogoDeTabuleiro`. A equipe conferiu as regras com `mvn -B verify` e precisa explicar limite, prazo, fila e restauração do estado. |

## Compromisso

Ao entregar, a equipe declara que:

- entende cada trecho do código entregue e consegue explicá-lo oralmente;
- testou o que foi gerado, e não apenas verificou que compila;
- adaptou o que foi gerado ao contrato e às decisões de design do projeto;
- está ciente de que cada integrante fará uma alteração individual em sala, sem consulta,
  na defesa da Entrega 3.

Assinaturas (nome e data):
