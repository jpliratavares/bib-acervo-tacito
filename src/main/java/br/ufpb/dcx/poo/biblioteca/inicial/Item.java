package br.ufpb.dcx.poo.biblioteca.inicial;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Entidade do acervo cuja identidade é o código imutável. */
class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String codigo;
    private final String titulo;
    private final String autoria;
    private final String categoria;
    private final int ano;
    private final List<Exemplar> exemplares = new ArrayList<>();

    Item(String codigo, String titulo, String autoria, String categoria, int ano) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.autoria = autoria;
        this.categoria = categoria;
        this.ano = ano;
    }

    String codigo() { return codigo; }
    String titulo() { return titulo; }
    String autoria() { return autoria; }
    String categoria() { return categoria; }
    int ano() { return ano; }

    void adicionarExemplar(Exemplar exemplar) {
        exemplares.add(exemplar);
    }

    List<Exemplar> exemplares() {
        return List.copyOf(exemplares);
    }
}
