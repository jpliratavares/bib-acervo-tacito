package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.inicial.JogoDeTabuleiro;

/** Testes da regra de negócio autoral do acervo de jogos. */
class JogoDeTabuleiroTest {

    @Test
    void faixaDeJogadoresValida() {
        assertDoesNotThrow(() -> new JogoDeTabuleiro("J-01", 2, 4));
    }

    @Test
    void maximoDeJogadoresNaoPodeSerMenorQueMinimo() {
        assertThrows(DadosInvalidosException.class,
                () -> new JogoDeTabuleiro("J-01", 4, 2));
    }
}
