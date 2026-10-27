package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.LimiteDeEmprestimosExcedidoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;

/** Empréstimos, devoluções, atrasos e fila de reservas da Entrega 2. */
class EmprestimoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 1);

    private Biblioteca biblioteca;

    @BeforeEach
    void criarBibliotecaVazia() {
        biblioteca = Fabrica.novaBiblioteca();
    }

    @Test
    @DisplayName("empréstimo marca o exemplar, aplica o prazo e a devolução o libera")
    void emprestarEDevolver() throws BibliotecaException {
        prepararItem("L1", "T-001");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        String id = biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);

        assertFalse(id.isBlank());
        assertEquals(StatusExemplar.EMPRESTADO, statusDe("T-001"));
        assertEquals(1, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());

        EmprestimoView aberto = emprestimoDoUsuario("2026001", id);
        assertEquals("T-001", aberto.tombo());
        assertEquals("2026001", aberto.matricula());
        assertEquals(HOJE, aberto.dataDoEmprestimo());
        assertEquals(HOJE.plusDays(EmprestimoService.PRAZO_EM_DIAS), aberto.dataPrevistaDeDevolucao());
        assertNull(aberto.dataDaDevolucao());
        assertEquals(StatusEmprestimo.ATIVO, aberto.status());

        LocalDate devolucao = HOJE.plusDays(3);
        biblioteca.emprestimos().devolver("T-001", devolucao);

        assertEquals(StatusExemplar.DISPONIVEL, statusDe("T-001"));
        EmprestimoView fechado = emprestimoDoUsuario("2026001", id);
        assertEquals(devolucao, fechado.dataDaDevolucao());
        assertEquals(StatusEmprestimo.DEVOLVIDO, fechado.status());
        assertEquals(0, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
    }

    @Test
    @DisplayName("parâmetros nulos ou em branco são rejeitados no empréstimo e na devolução")
    void parametrosInvalidos() throws BibliotecaException {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar(null, "2026001", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("  ", "2026001", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", null, HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", " ", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "2026001", null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver(null, HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver("  ", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver("T-001", null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarEmprestimosDoUsuario(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarEmprestimosDoUsuario(" "));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarAtrasados(null));
    }

    @Test
    @DisplayName("tombo ou matrícula inexistentes impedem o empréstimo e a consulta")
    void recursosInexistentes() throws BibliotecaException {
        prepararItem("L1", "T-001");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().emprestar("NAO-EXISTE", "2026001", HOJE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "0000", HOJE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().devolver("NAO-EXISTE", HOJE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().listarEmprestimosDoUsuario("0000"));
    }

    @Test
    @DisplayName("usuário inativo não empresta e o exemplar permanece disponível")
    void usuarioInativoNaoEmpresta() throws BibliotecaException {
        prepararItem("L1", "T-001");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");
        biblioteca.usuarios().desativarUsuario("2026001");

        assertThrows(UsuarioInativoException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE));
        assertEquals(StatusExemplar.DISPONIVEL, statusDe("T-001"));
    }

    @Test
    @DisplayName("exemplar emprestado ou baixado não pode ser emprestado de novo")
    void exemplarIndisponivel() throws BibliotecaException {
        prepararItem("L1", "T-001", "T-002");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");
        biblioteca.usuarios().cadastrarUsuario("2026002", "Bruno Lima");
        biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        biblioteca.acervo().baixarExemplar("T-002");

        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "2026002", HOJE));
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-002", "2026002", HOJE));
    }

    @Test
    @DisplayName("o quarto empréstimo em aberto é recusado e o exemplar continua disponível")
    void limiteDeTresEmprestimos() throws BibliotecaException {
        prepararItem("L1", "T-001", "T-002", "T-003", "T-004");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");
        biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        biblioteca.emprestimos().emprestar("T-002", "2026001", HOJE);
        biblioteca.emprestimos().emprestar("T-003", "2026001", HOJE);

        assertEquals(EmprestimoService.LIMITE_DE_EMPRESTIMOS,
                biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
        assertThrows(LimiteDeEmprestimosExcedidoException.class,
                () -> biblioteca.emprestimos().emprestar("T-004", "2026001", HOJE));
        assertEquals(StatusExemplar.DISPONIVEL, statusDe("T-004"));

        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(1));
        String novo = biblioteca.emprestimos().emprestar("T-004", "2026001", HOJE.plusDays(1));
        assertFalse(novo.isBlank());
        assertEquals(EmprestimoService.LIMITE_DE_EMPRESTIMOS,
                biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
    }

    @Test
    @DisplayName("a listagem do usuário preserva a ordem e distingue empréstimo aberto de devolvido")
    void listarEmprestimosDoUsuario() throws BibliotecaException {
        prepararItem("L1", "T-001", "T-002");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");
        biblioteca.usuarios().cadastrarUsuario("2026002", "Bruno Lima");
        String primeiro = biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        String segundo = biblioteca.emprestimos().emprestar("T-002", "2026002", HOJE.plusDays(1));
        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(2));

        List<EmprestimoView> daAna = biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001");
        assertEquals(List.of(primeiro), daAna.stream().map(EmprestimoView::id).toList());
        assertEquals(StatusEmprestimo.DEVOLVIDO, daAna.get(0).status());
        assertThrows(UnsupportedOperationException.class, () -> daAna.add(daAna.get(0)));

        List<EmprestimoView> doBruno = biblioteca.emprestimos().listarEmprestimosDoUsuario("2026002");
        assertEquals(List.of(segundo), doBruno.stream().map(EmprestimoView::id).toList());
        assertEquals(StatusEmprestimo.ATIVO, doBruno.get(0).status());
        assertNotEquals(primeiro, segundo);
    }

    @Test
    @DisplayName("atraso exige data posterior à prevista e ignora empréstimo já devolvido")
    void listarAtrasados() throws BibliotecaException {
        prepararItem("L1", "T-001", "T-002");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");
        String atrasado = biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        biblioteca.emprestimos().emprestar("T-002", "2026001", HOJE.plusDays(10));
        LocalDate prevista = HOJE.plusDays(EmprestimoService.PRAZO_EM_DIAS);

        assertEquals(List.of(), biblioteca.emprestimos().listarAtrasados(prevista));

        List<EmprestimoView> noDiaSeguinte = biblioteca.emprestimos().listarAtrasados(prevista.plusDays(1));
        assertEquals(List.of(atrasado), noDiaSeguinte.stream().map(EmprestimoView::id).toList());
        assertEquals(StatusEmprestimo.ATRASADO, noDiaSeguinte.get(0).status());
        assertNull(noDiaSeguinte.get(0).dataDaDevolucao());
        assertEquals(StatusEmprestimo.ATIVO,
                biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").get(0).status());

        biblioteca.emprestimos().devolver("T-001", prevista.plusDays(2));
        assertEquals(List.of(), biblioteca.emprestimos().listarAtrasados(prevista.plusDays(3)));
    }

    @Test
    @DisplayName("não se devolve exemplar que não está emprestado nem com data anterior ao empréstimo")
    void devolucaoInvalida() throws BibliotecaException {
        prepararItem("L1", "T-001");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().devolver("T-001", HOJE));

        biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().devolver("T-001", HOJE.minusDays(1)));
        assertEquals(StatusExemplar.EMPRESTADO, statusDe("T-001"));
        assertNull(biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").get(0).dataDaDevolucao());

        biblioteca.emprestimos().devolver("T-001", HOJE);
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(1)));
        assertEquals(StatusExemplar.DISPONIVEL, statusDe("T-001"));
    }

    @Test
    @DisplayName("biblioteca sem empréstimos devolve listas vazias")
    void listasVazias() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        assertEquals(List.of(), biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001"));
        assertEquals(List.of(), biblioteca.emprestimos().listarAtrasados(HOJE));
    }

    @Test
    @DisplayName("reserva só é aceita quando não há exemplar livre e entra no fim da fila")
    void reservarQuandoNaoHaExemplarLivre() throws BibliotecaException {
        prepararItem("L1", "T-001");
        cadastrarUsuarios("2026001", "Ana", "2026002", "Bruno");

        assertOperacaoNaoPermitida(
                () -> biblioteca.emprestimos().reservar("L1", "2026001", HOJE));

        biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        String doBruno = biblioteca.emprestimos().reservar("L1", "2026002", HOJE);
        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(1));
        String daAna = biblioteca.emprestimos().reservar("L1", "2026001", HOJE.plusDays(1));

        List<ReservaView> fila = biblioteca.emprestimos().listarReservas("L1");
        assertEquals(List.of(doBruno, daAna), fila.stream().map(ReservaView::id).toList());
        assertEquals(1, fila.get(0).posicaoNaFila());
        assertEquals(2, fila.get(1).posicaoNaFila());
        assertEquals("L1", fila.get(0).codigoDoItem());
        assertEquals("2026002", fila.get(0).matricula());
        assertEquals(HOJE, fila.get(0).dataDaReserva());
        assertEquals(StatusExemplar.DISPONIVEL, statusDe("T-001"));
        assertThrows(UnsupportedOperationException.class, () -> fila.add(fila.get(0)));
    }

    @Test
    @DisplayName("cancelar reserva reordena a fila e identificador inválido é rejeitado")
    void cancelarReservaReordenaFila() throws BibliotecaException {
        prepararItem("L1", "T-001");
        cadastrarUsuarios("2026001", "Ana", "2026002", "Bruno", "2026003", "Carla", "2026004", "Diego");
        biblioteca.emprestimos().emprestar("T-001", "2026004", HOJE);
        String ana = biblioteca.emprestimos().reservar("L1", "2026001", HOJE);
        String bruno = biblioteca.emprestimos().reservar("L1", "2026002", HOJE.plusDays(1));
        String carla = biblioteca.emprestimos().reservar("L1", "2026003", HOJE.plusDays(2));

        biblioteca.emprestimos().cancelarReserva(bruno);

        List<ReservaView> fila = biblioteca.emprestimos().listarReservas("L1");
        assertEquals(List.of(ana, carla), fila.stream().map(ReservaView::id).toList());
        assertEquals(List.of(1, 2), fila.stream().map(ReservaView::posicaoNaFila).toList());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().cancelarReserva(bruno));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().cancelarReserva("RESERVA-INEXISTENTE"));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().cancelarReserva(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().cancelarReserva("  "));
    }

    @Test
    @DisplayName("reserva recusa item sem exemplar circulável, duplicidade, inativo e quem já tem o item")
    void reservasNaoPermitidas() throws BibliotecaException {
        prepararItem("L1", "T-001");
        prepararItem("L2");
        cadastrarUsuarios("2026001", "Ana", "2026002", "Bruno");
        biblioteca.acervo().baixarExemplar("T-001");

        assertOperacaoNaoPermitida(() -> biblioteca.emprestimos().reservar("L1", "2026001", HOJE));
        assertOperacaoNaoPermitida(() -> biblioteca.emprestimos().reservar("L2", "2026001", HOJE));

        prepararItem("L3", "T-003");
        biblioteca.emprestimos().emprestar("T-003", "2026001", HOJE);
        assertOperacaoNaoPermitida(() -> biblioteca.emprestimos().reservar("L3", "2026001", HOJE));

        biblioteca.usuarios().desativarUsuario("2026002");
        Exception inativo = assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().reservar("L3", "2026002", HOJE));
        assertEquals(OperacaoNaoPermitidaException.class, inativo.getClass());

        biblioteca.usuarios().cadastrarUsuario("2026003", "Carla");
        String reserva = biblioteca.emprestimos().reservar("L3", "2026003", HOJE);
        assertOperacaoNaoPermitida(() -> biblioteca.emprestimos().reservar("L3", "2026003", HOJE.plusDays(1)));
        assertEquals(List.of(reserva), biblioteca.emprestimos().listarReservas("L3").stream()
                .map(ReservaView::id).toList());
    }

    @Test
    @DisplayName("parâmetros e cadastros inválidos são rejeitados na reserva")
    void reservaComDadosInvalidos() throws BibliotecaException {
        prepararItem("L1", "T-001");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar(null, "2026001", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar(" ", "2026001", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar("L1", null, HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar("L1", " ", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar("L1", "2026001", null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarReservas(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarReservas(" "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().reservar("NAO-EXISTE", "2026001", HOJE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().reservar("L1", "0000", HOJE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().listarReservas("NAO-EXISTE"));
        assertEquals(List.of(), biblioteca.emprestimos().listarReservas("L1"));
    }

    @Test
    @DisplayName("quem está na vez da fila leva o exemplar e quem está atrás não fura a fila")
    void prioridadeDaFilaNoEmprestimo() throws BibliotecaException {
        prepararItem("L1", "T-001");
        cadastrarUsuarios("2026001", "Ana", "2026002", "Bruno", "2026003", "Carla", "2026004", "Diego");
        biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        String bruno = biblioteca.emprestimos().reservar("L1", "2026002", HOJE);
        String carla = biblioteca.emprestimos().reservar("L1", "2026003", HOJE.plusDays(1));
        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(2));

        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "2026004", HOJE.plusDays(2)));
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "2026003", HOJE.plusDays(2)));

        biblioteca.emprestimos().emprestar("T-001", "2026002", HOJE.plusDays(2));

        List<ReservaView> fila = biblioteca.emprestimos().listarReservas("L1");
        assertEquals(List.of(carla), fila.stream().map(ReservaView::id).toList());
        assertEquals(1, fila.get(0).posicaoNaFila());
        assertEquals(StatusExemplar.EMPRESTADO, statusDe("T-001"));
        assertFalse(fila.stream().anyMatch(reserva -> reserva.id().equals(bruno)));
    }

    @Test
    @DisplayName("cópias disponíveis além da fila podem ser emprestadas por quem já está nela")
    void copiasDisponiveisAtendemAFilaNaOrdem() throws BibliotecaException {
        prepararItem("L1", "T-001", "T-002");
        cadastrarUsuarios("2026001", "Ana", "2026002", "Bruno", "2026003", "Carla", "2026004", "Diego");
        biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        biblioteca.emprestimos().emprestar("T-002", "2026002", HOJE);
        biblioteca.emprestimos().reservar("L1", "2026003", HOJE);
        biblioteca.emprestimos().reservar("L1", "2026004", HOJE.plusDays(1));
        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(2));
        biblioteca.emprestimos().devolver("T-002", HOJE.plusDays(2));

        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE.plusDays(3)));
        biblioteca.emprestimos().emprestar("T-001", "2026004", HOJE.plusDays(3));
        biblioteca.emprestimos().emprestar("T-002", "2026003", HOJE.plusDays(3));

        assertEquals(List.of(), biblioteca.emprestimos().listarReservas("L1"));
        assertEquals(0, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
        assertEquals(1, biblioteca.usuarios().buscarUsuario("2026003").emprestimosAtivos());
        assertEquals(1, biblioteca.usuarios().buscarUsuario("2026004").emprestimosAtivos());
    }

    @Test
    @DisplayName("limite de empréstimos é verificado sem consumir a reserva de quem não pode levar")
    void limiteNaoConsomeReserva() throws BibliotecaException {
        prepararItem("L1", "T-001", "T-002", "T-003");
        prepararItem("L2", "T-010");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");
        biblioteca.usuarios().cadastrarUsuario("2026002", "Bruno Lima");
        biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        biblioteca.emprestimos().emprestar("T-002", "2026001", HOJE);
        biblioteca.emprestimos().emprestar("T-003", "2026001", HOJE);
        biblioteca.emprestimos().emprestar("T-010", "2026002", HOJE);
        String reserva = biblioteca.emprestimos().reservar("L2", "2026001", HOJE);
        biblioteca.emprestimos().devolver("T-010", HOJE.plusDays(1));

        assertThrows(LimiteDeEmprestimosExcedidoException.class,
                () -> biblioteca.emprestimos().emprestar("T-010", "2026001", HOJE.plusDays(1)));

        List<ReservaView> fila = biblioteca.emprestimos().listarReservas("L2");
        assertEquals(List.of(reserva), fila.stream().map(ReservaView::id).toList());
        assertEquals(1, fila.get(0).posicaoNaFila());
        assertEquals(StatusExemplar.DISPONIVEL, statusDoItem("L2", "T-010"));
    }

    @Test
    @DisplayName("filas de itens diferentes não interferem entre si")
    void filasIndependentesPorItem() throws BibliotecaException {
        prepararItem("L1", "T-001");
        prepararItem("L2", "T-002");
        cadastrarUsuarios("2026001", "Ana", "2026002", "Bruno", "2026003", "Carla");
        biblioteca.emprestimos().emprestar("T-001", "2026001", HOJE);
        biblioteca.emprestimos().emprestar("T-002", "2026002", HOJE);
        String primeira = biblioteca.emprestimos().reservar("L1", "2026003", HOJE);
        String segunda = biblioteca.emprestimos().reservar("L2", "2026003", HOJE);

        assertEquals(List.of(primeira), idsDasReservas("L1"));
        assertEquals(List.of(segunda), idsDasReservas("L2"));
        assertEquals(1, biblioteca.emprestimos().listarReservas("L1").get(0).posicaoNaFila());
        assertEquals(1, biblioteca.emprestimos().listarReservas("L2").get(0).posicaoNaFila());
    }

    private void prepararItem(String codigo, String... tombos) throws BibliotecaException {
        biblioteca.acervo().cadastrarItem(codigo, "Java Efetivo", "Bloch", "livro", 2019);
        for (String tombo : tombos) {
            biblioteca.acervo().adicionarExemplar(codigo, tombo);
        }
    }

    private void cadastrarUsuarios(String... matriculaENome) throws BibliotecaException {
        for (int indice = 0; indice < matriculaENome.length; indice += 2) {
            biblioteca.usuarios().cadastrarUsuario(matriculaENome[indice], matriculaENome[indice + 1]);
        }
    }

    private void assertOperacaoNaoPermitida(org.junit.jupiter.api.function.Executable acao) {
        Exception erro = assertThrows(OperacaoNaoPermitidaException.class, acao);
        assertEquals(OperacaoNaoPermitidaException.class, erro.getClass());
    }

    private List<String> idsDasReservas(String codigoDoItem) throws BibliotecaException {
        return biblioteca.emprestimos().listarReservas(codigoDoItem).stream()
                .map(ReservaView::id)
                .toList();
    }

    private StatusExemplar statusDoItem(String codigo, String tombo) throws BibliotecaException {
        return biblioteca.acervo().listarExemplares(codigo).stream()
                .filter(exemplar -> exemplar.tombo().equals(tombo))
                .findFirst()
                .orElseThrow()
                .status();
    }

    private StatusExemplar statusDe(String tombo) throws BibliotecaException {
        return biblioteca.acervo().listarExemplares("L1").stream()
                .filter(exemplar -> exemplar.tombo().equals(tombo))
                .findFirst()
                .orElseThrow()
                .status();
    }

    private EmprestimoView emprestimoDoUsuario(String matricula, String id) throws BibliotecaException {
        return biblioteca.emprestimos().listarEmprestimosDoUsuario(matricula).stream()
                .filter(emprestimo -> emprestimo.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
