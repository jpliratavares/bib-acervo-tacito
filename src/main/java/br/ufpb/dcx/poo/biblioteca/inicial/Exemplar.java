package br.ufpb.dcx.poo.biblioteca.inicial;

import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;

/** Cópia física identificada de forma imutável pelo tombo. */
final class Exemplar {

    private final String tombo;
    private final String codigoDoItem;
    private StatusExemplar status = StatusExemplar.DISPONIVEL;

    Exemplar(String tombo, String codigoDoItem) {
        this.tombo = tombo;
        this.codigoDoItem = codigoDoItem;
    }

    String tombo() { return tombo; }
    String codigoDoItem() { return codigoDoItem; }
    StatusExemplar status() { return status; }

    /** Baixa retira o exemplar de circulação. O contrato congelado representa isso com INDISPONIVEL. */
    void baixar() {
        status = StatusExemplar.INDISPONIVEL;
    }

    void marcarEmprestado() {
        status = StatusExemplar.EMPRESTADO;
    }

    void disponibilizar() {
        status = StatusExemplar.DISPONIVEL;
    }
}
