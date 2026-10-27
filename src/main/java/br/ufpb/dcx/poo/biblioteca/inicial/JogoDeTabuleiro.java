package br.ufpb.dcx.poo.biblioteca.inicial;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;

/** Extensão autoral: item de acervo com a faixa de jogadores de um jogo de tabuleiro. */
public final class JogoDeTabuleiro extends Item {

    private static final long serialVersionUID = 1L;

    private final int jogadoresMinimos;
    private final int jogadoresMaximos;

    public JogoDeTabuleiro(String codigoDoItem, int jogadoresMinimos, int jogadoresMaximos) {
        this(codigoObrigatorio(codigoDoItem), codigoObrigatorio(codigoDoItem), "", "jogo", 0,
                jogadoresMinimos, jogadoresMaximos);
    }

    public JogoDeTabuleiro(String codigo, String titulo, String autoria, String categoria, int ano,
                           int jogadoresMinimos, int jogadoresMaximos) {
        super(codigo, titulo, autoria, categoria, ano);
        if (jogadoresMinimos < 1 || jogadoresMaximos < jogadoresMinimos) {
            throw new DadosInvalidosException("A faixa de jogadores deve ser válida.");
        }
        this.jogadoresMinimos = jogadoresMinimos;
        this.jogadoresMaximos = jogadoresMaximos;
    }

    public String codigoDoItem() {
        return codigo();
    }

    public int jogadoresMinimos() { return jogadoresMinimos; }
    public int jogadoresMaximos() { return jogadoresMaximos; }

    private static String codigoObrigatorio(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new DadosInvalidosException("O código do item é obrigatório.");
        }
        return codigo;
    }
}
