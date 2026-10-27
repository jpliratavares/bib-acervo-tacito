package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;
import br.ufpb.dcx.poo.biblioteca.inicial.AcervoEmMemoria;
import br.ufpb.dcx.poo.biblioteca.inicial.BibliotecaInicial;
import br.ufpb.dcx.poo.biblioteca.inicial.JogoDeTabuleiro;

/** Persistência do estado completo da biblioteca, inclusive subtipos do acervo. */
class PersistenciaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 1);

    @TempDir
    Path diretorio;

    @Test
    @DisplayName("salvar e carregar restauram itens, exemplares, usuários, empréstimos e reservas")
    void salvarECarregarRestauraEstado() throws BibliotecaException {
        Path arquivo = diretorio.resolve("aninhado/biblioteca.dat");
        BibliotecaInicial origem = new BibliotecaInicial(arquivo);
        origem.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        origem.acervo().adicionarExemplar("L1", "T-001");
        origem.acervo().adicionarExemplar("L1", "T-002");
        origem.acervo().baixarExemplar("T-002");
        ((AcervoEmMemoria) origem.acervo()).cadastrarJogoDeTabuleiro(
                "J1", "Azul", "Kiesling", "jogo", 2017, 2, 4);
        origem.acervo().adicionarExemplar("J1", "T-010");
        origem.usuarios().cadastrarUsuario("2026001", "Ana Souza");
        origem.usuarios().cadastrarUsuario("2026002", "Bruno Lima");
        origem.usuarios().cadastrarUsuario("2026003", "Carla Dias");
        origem.usuarios().cadastrarUsuario("2026004", "Diego Nunes");
        origem.usuarios().desativarUsuario("2026002");

        String devolvido = origem.emprestimos().emprestar("T-001", "2026001", DIA);
        origem.emprestimos().devolver("T-001", DIA.plusDays(4));
        String aberto = origem.emprestimos().emprestar("T-010", "2026001", DIA.plusDays(5));
        String primeiraReserva = origem.emprestimos().reservar("J1", "2026003", DIA.plusDays(6));
        String segundaReserva = origem.emprestimos().reservar("J1", "2026004", DIA.plusDays(7));
        origem.salvar();

        BibliotecaInicial destino = new BibliotecaInicial(arquivo);
        assertEquals(0, destino.acervo().listarItens().size());
        destino.carregar();

        assertEquals(List.of("Azul", "Java Efetivo"),
                destino.acervo().listarItens().stream().map(ItemView::titulo).toList());
        ItemView livro = destino.acervo().buscarItem("L1");
        assertEquals(2, livro.totalDeExemplares());
        assertEquals(1, livro.exemplaresDisponiveis());
        assertEquals(StatusExemplar.DISPONIVEL, status(destino, "L1", "T-001"));
        assertEquals(StatusExemplar.INDISPONIVEL, status(destino, "L1", "T-002"));
        assertEquals(StatusExemplar.EMPRESTADO, status(destino, "J1", "T-010"));

        JogoDeTabuleiro jogo = ((AcervoEmMemoria) destino.acervo()).buscarJogo("J1");
        assertEquals("J1", jogo.codigoDoItem());
        assertEquals(2, jogo.jogadoresMinimos());
        assertEquals(4, jogo.jogadoresMaximos());
        assertEquals("Azul", destino.acervo().buscarItem("J1").titulo());

        assertFalse(destino.usuarios().buscarUsuario("2026002").ativo());
        assertEquals(1, destino.usuarios().buscarUsuario("2026001").emprestimosAtivos());
        assertThrows(UsuarioInativoException.class,
                () -> destino.emprestimos().emprestar("T-001", "2026002", DIA.plusDays(8)));

        List<EmprestimoView> historico = destino.emprestimos().listarEmprestimosDoUsuario("2026001");
        assertEquals(List.of(devolvido, aberto), historico.stream().map(EmprestimoView::id).toList());
        assertEquals(StatusEmprestimo.DEVOLVIDO, historico.get(0).status());
        assertEquals(DIA.plusDays(4), historico.get(0).dataDaDevolucao());
        assertEquals(StatusEmprestimo.ATIVO, historico.get(1).status());
        assertEquals(DIA.plusDays(5).plusDays(EmprestimoService.PRAZO_EM_DIAS),
                historico.get(1).dataPrevistaDeDevolucao());

        List<ReservaView> fila = destino.emprestimos().listarReservas("J1");
        assertEquals(List.of(primeiraReserva, segundaReserva), fila.stream().map(ReservaView::id).toList());
        assertEquals(List.of(1, 2), fila.stream().map(ReservaView::posicaoNaFila).toList());

        String novoEmprestimo = destino.emprestimos().emprestar("T-001", "2026001", DIA.plusDays(8));
        assertNotEquals(devolvido, novoEmprestimo);
        assertNotEquals(aberto, novoEmprestimo);
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> destino.usuarios().desativarUsuario("2026001"));

        destino.emprestimos().devolver("T-010", DIA.plusDays(9));
        assertThrows(ExemplarIndisponivelException.class,
                () -> destino.emprestimos().emprestar("T-010", "2026004", DIA.plusDays(9)));
        destino.emprestimos().emprestar("T-010", "2026003", DIA.plusDays(9));
        assertEquals(List.of(segundaReserva), destino.emprestimos().listarReservas("J1").stream()
                .map(ReservaView::id).toList());
        assertEquals(1, destino.emprestimos().listarReservas("J1").get(0).posicaoNaFila());
    }

    @Test
    @DisplayName("bibliotecas com arquivos diferentes não compartilham estado")
    void arquivosIndependentes() throws BibliotecaException {
        BibliotecaInicial uma = new BibliotecaInicial(diretorio.resolve("uma.dat"));
        BibliotecaInicial outra = new BibliotecaInicial(diretorio.resolve("outra.dat"));
        uma.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);

        assertEquals(1, uma.acervo().listarItens().size());
        assertEquals(0, outra.acervo().listarItens().size());
    }

    @Test
    @DisplayName("carregar sem arquivo, com conteúdo inválido ou salvar em diretório falha sem apagar a memória")
    void falhasDePersistencia() throws BibliotecaException, java.io.IOException {
        assertThrows(DadosInvalidosException.class, () -> new BibliotecaInicial(null));

        BibliotecaInicial ausente = new BibliotecaInicial(diretorio.resolve("ausente.dat"));
        assertThrows(PersistenciaException.class, ausente::carregar);

        Path corrompido = diretorio.resolve("ruim.dat");
        Files.writeString(corrompido, "conteudo-invalido");
        BibliotecaInicial invalida = new BibliotecaInicial(corrompido);
        invalida.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        assertThrows(PersistenciaException.class, invalida::carregar);
        assertEquals("Java Efetivo", invalida.acervo().buscarItem("L1").titulo());

        BibliotecaInicial diretorioComoArquivo = new BibliotecaInicial(diretorio);
        assertThrows(PersistenciaException.class, diretorioComoArquivo::salvar);
    }

    @Test
    @DisplayName("o construtor padrão grava em target/biblioteca-estado.dat")
    void construtorPadrao() throws BibliotecaException, java.io.IOException {
        Path arquivo = Path.of("target", "biblioteca-estado.dat");
        Files.deleteIfExists(arquivo);
        try {
            BibliotecaInicial origem = new BibliotecaInicial();
            origem.acervo().cadastrarItem("P1", "Padrão", "Autoria", "livro", 2020);
            origem.salvar();
            assertTrue(Files.isRegularFile(arquivo));

            BibliotecaInicial destino = new BibliotecaInicial();
            destino.carregar();
            assertEquals("Padrão", destino.acervo().buscarItem("P1").titulo());
        } finally {
            Files.deleteIfExists(arquivo);
        }
    }

    private static StatusExemplar status(BibliotecaInicial biblioteca, String codigo, String tombo)
            throws BibliotecaException {
        return biblioteca.acervo().listarExemplares(codigo).stream()
                .filter(exemplar -> exemplar.tombo().equals(tombo))
                .findFirst()
                .orElseThrow()
                .status();
    }
}
