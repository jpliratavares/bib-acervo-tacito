package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.ExemplarView;
import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

/** Cenários de fronteira adicionados pela equipe com o ciclo TDD. */
class AcervoTddTest {

    private Biblioteca biblioteca;

    @BeforeEach
    void criarBibliotecaVazia() {
        biblioteca = Fabrica.novaBiblioteca();
    }

    @Test
    @DisplayName("busca por título mantém a ordenação alfabética dos resultados")
    void buscaPorTituloOrdenada() throws BibliotecaException {
        biblioteca.acervo().cadastrarItem("J2", "Zombicide", "Guillotin", "jogo", 2012);
        biblioteca.acervo().cadastrarItem("J1", "Azul", "Kiesling", "jogo", 2017);

        List<ItemView> resultado = biblioteca.acervo().buscarPorTitulo("z");

        assertEquals(List.of("Azul", "Zombicide"), resultado.stream().map(ItemView::titulo).toList());
    }

    @Test
    @DisplayName("tombo em branco é rejeitado antes de alterar o item")
    void tomboEmBrancoNaoDeixaResiduo() throws BibliotecaException {
        biblioteca.acervo().cadastrarItem("J1", "Azul", "Kiesling", "jogo", 2017);

        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.acervo().adicionarExemplar("J1", " "));

        assertEquals(0, biblioteca.acervo().buscarItem("J1").totalDeExemplares());
    }

    @Test
    @DisplayName("lista de exemplares é devolvida ordenada por tombo")
    void listarExemplaresOrdenada() throws BibliotecaException {
        biblioteca.acervo().cadastrarItem("J1", "Azul", "Kiesling", "jogo", 2017);
        biblioteca.acervo().adicionarExemplar("J1", "T-002");
        biblioteca.acervo().adicionarExemplar("J1", "T-001");

        List<ExemplarView> exemplares = biblioteca.acervo().listarExemplares("J1");

        assertEquals(List.of("T-001", "T-002"), exemplares.stream().map(ExemplarView::tombo).toList());
    }

    @Test
    @DisplayName("baixar tombo inexistente lança RecursoNaoEncontradoException")
    void baixarTomboInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.acervo().baixarExemplar("NAO-EXISTE"));
    }

    @Test
    @DisplayName("tombo nulo ou em branco é rejeitado na baixa")
    void baixarTomboInvalido() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().baixarExemplar(null));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().baixarExemplar("   "));
    }

    @Test
    @DisplayName("exemplar já baixado não pode ser baixado outra vez")
    void baixarExemplarJaBaixado() throws BibliotecaException {
        biblioteca.acervo().cadastrarItem("J1", "Azul", "Kiesling", "jogo", 2017);
        biblioteca.acervo().adicionarExemplar("J1", "T-001");
        marcarStatus("T-001", StatusExemplar.INDISPONIVEL);

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.acervo().baixarExemplar("T-001"));
        assertEquals(StatusExemplar.INDISPONIVEL, biblioteca.acervo().listarExemplares("J1").get(0).status());
        assertEquals(1, biblioteca.acervo().buscarItem("J1").totalDeExemplares());
        assertEquals(0, biblioteca.acervo().buscarItem("J1").exemplaresDisponiveis());
    }

    @Test
    @DisplayName("exemplar emprestado não pode ser baixado")
    void baixarExemplarEmprestado() throws BibliotecaException {
        biblioteca.acervo().cadastrarItem("J1", "Azul", "Kiesling", "jogo", 2017);
        biblioteca.acervo().adicionarExemplar("J1", "T-001");
        marcarStatus("T-001", StatusExemplar.EMPRESTADO);

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.acervo().baixarExemplar("T-001"));
        assertEquals(StatusExemplar.EMPRESTADO, biblioteca.acervo().listarExemplares("J1").get(0).status());
    }

    /**
     * Prepara o status interno do exemplar. O contrato não oferece outra forma de
     * simular EMPRESTADO ou INDISPONIVEL antes do serviço de empréstimos existir.
     * A baixa do contrato congelado usa {@link StatusExemplar#INDISPONIVEL}.
     */
    @SuppressWarnings("unchecked")
    private void marcarStatus(String tombo, StatusExemplar status) {
        try {
            Field mapa = biblioteca.acervo().getClass().getDeclaredField("exemplaresPorTombo");
            mapa.setAccessible(true);
            Map<String, Object> exemplares = (Map<String, Object>) mapa.get(biblioteca.acervo());
            Object exemplar = exemplares.get(tombo);
            Field campoStatus = exemplar.getClass().getDeclaredField("status");
            campoStatus.setAccessible(true);
            campoStatus.set(exemplar, status);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível preparar o status do exemplar.", e);
        }
    }
}
