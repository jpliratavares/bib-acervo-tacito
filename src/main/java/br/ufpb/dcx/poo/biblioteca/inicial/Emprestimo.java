package br.ufpb.dcx.poo.biblioteca.inicial;

import java.time.LocalDate;

/** Empréstimo de um exemplar para um usuário, aberto enquanto não houver devolução. */
final class Emprestimo {

    private final String id;
    private final String tombo;
    private final String matricula;
    private final LocalDate dataDoEmprestimo;
    private final LocalDate dataPrevistaDeDevolucao;
    private LocalDate dataDaDevolucao;

    Emprestimo(String id, String tombo, String matricula, LocalDate dataDoEmprestimo,
               LocalDate dataPrevistaDeDevolucao) {
        this.id = id;
        this.tombo = tombo;
        this.matricula = matricula;
        this.dataDoEmprestimo = dataDoEmprestimo;
        this.dataPrevistaDeDevolucao = dataPrevistaDeDevolucao;
    }

    String id() { return id; }
    String tombo() { return tombo; }
    String matricula() { return matricula; }
    LocalDate dataDoEmprestimo() { return dataDoEmprestimo; }
    LocalDate dataPrevistaDeDevolucao() { return dataPrevistaDeDevolucao; }
    LocalDate dataDaDevolucao() { return dataDaDevolucao; }

    boolean emAberto() {
        return dataDaDevolucao == null;
    }

    void registrarDevolucao(LocalDate data) {
        dataDaDevolucao = data;
    }
}
