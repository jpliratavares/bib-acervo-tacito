package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

/** Testes públicos dos usuários. */
class UsuarioTest {

    private Biblioteca biblioteca;

    @BeforeEach
    void criarBibliotecaVazia() {
        biblioteca = Fabrica.novaBiblioteca();
    }

    @Test
    @DisplayName("usuário cadastrado começa ativo e sem empréstimos")
    void cadastrarEBuscar() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        UsuarioView usuario = biblioteca.usuarios().buscarUsuario("2026001");

        assertEquals("Ana Souza", usuario.nome());
        assertTrue(usuario.ativo());
        assertEquals(0, usuario.emprestimosAtivos());
    }

    @Test
    @DisplayName("matrícula repetida é recusada")
    void matriculaDuplicada() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        assertThrows(RecursoDuplicadoException.class,
                () -> biblioteca.usuarios().cadastrarUsuario("2026001", "Outro Nome"));
    }

    @Test
    @DisplayName("buscar usuário inexistente lança RecursoNaoEncontradoException")
    void buscarInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.usuarios().buscarUsuario("0000"));
    }

    @Test
    @DisplayName("nome em branco é entrada inválida")
    void nomeEmBranco() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().cadastrarUsuario("2026001", "   "));
    }

    @Test
    @DisplayName("listar devolve os usuários ordenados por nome")
    void listarOrdenado() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("3", "Carlos");
        biblioteca.usuarios().cadastrarUsuario("1", "Ana");
        biblioteca.usuarios().cadastrarUsuario("2", "Bruno");

        assertEquals("Ana", biblioteca.usuarios().listarUsuarios().get(0).nome());
        assertEquals("Carlos", biblioteca.usuarios().listarUsuarios().get(2).nome());
    }

    @Test
    @DisplayName("usuário desativado deixa de estar ativo e pode ser reativado")
    void desativarEReativarUsuario() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        biblioteca.usuarios().desativarUsuario("2026001");

        assertFalse(biblioteca.usuarios().buscarUsuario("2026001").ativo());

        biblioteca.usuarios().reativarUsuario("2026001");

        assertTrue(biblioteca.usuarios().buscarUsuario("2026001").ativo());
    }

    @Test
    @DisplayName("matrícula nula ou em branco é rejeitada ao desativar")
    void desativarMatriculaInvalida() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.usuarios().desativarUsuario(null));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.usuarios().desativarUsuario("  "));
    }

    @Test
    @DisplayName("desativar matrícula inexistente lança RecursoNaoEncontradoException")
    void desativarInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.usuarios().desativarUsuario("0000"));
    }

    @Test
    @DisplayName("usuário com empréstimo em aberto não pode ser desativado")
    void naoDesativaComEmprestimoAtivo() throws BibliotecaException {
        prepararCirculacao();
        biblioteca.emprestimos().emprestar("T-001", "2026001", LocalDate.of(2026, 10, 1));

        assertEquals(1, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.usuarios().desativarUsuario("2026001"));
        assertTrue(biblioteca.usuarios().buscarUsuario("2026001").ativo());
    }

    @Test
    @DisplayName("empréstimo atrasado continua ativo e só libera a desativação depois da devolução")
    void devolveEEntaoDesativa() throws BibliotecaException {
        prepararCirculacao();
        biblioteca.emprestimos().emprestar("T-001", "2026001", LocalDate.of(2026, 1, 1));

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.usuarios().desativarUsuario("2026001"));

        biblioteca.emprestimos().devolver("T-001", LocalDate.of(2026, 3, 1));

        assertEquals(0, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
        biblioteca.usuarios().desativarUsuario("2026001");
        assertFalse(biblioteca.usuarios().buscarUsuario("2026001").ativo());
    }

    private void prepararCirculacao() throws BibliotecaException {
        biblioteca.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        biblioteca.acervo().adicionarExemplar("L1", "T-001");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");
    }
}
