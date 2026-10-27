package br.ufpb.dcx.poo.biblioteca.inicial;

import java.io.Serializable;
import java.util.List;

/** Fotografia serializável de todo o estado necessário para reabrir a biblioteca. */
final class EstadoBiblioteca implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Item> itens;
    private final List<Usuario> usuarios;
    private final List<Emprestimo> emprestimos;
    private final List<Reserva> reservas;
    private final long proximoIdEmprestimo;
    private final long proximoIdReserva;

    EstadoBiblioteca(List<Item> itens, List<Usuario> usuarios, List<Emprestimo> emprestimos,
                     List<Reserva> reservas, long proximoIdEmprestimo, long proximoIdReserva) {
        this.itens = itens;
        this.usuarios = usuarios;
        this.emprestimos = emprestimos;
        this.reservas = reservas;
        this.proximoIdEmprestimo = proximoIdEmprestimo;
        this.proximoIdReserva = proximoIdReserva;
    }

    boolean integro() {
        return itens != null && usuarios != null && emprestimos != null && reservas != null
                && proximoIdEmprestimo > 0 && proximoIdReserva > 0;
    }

    List<Item> itens() { return itens; }
    List<Usuario> usuarios() { return usuarios; }
    List<Emprestimo> emprestimos() { return emprestimos; }
    List<Reserva> reservas() { return reservas; }
    long proximoIdEmprestimo() { return proximoIdEmprestimo; }
    long proximoIdReserva() { return proximoIdReserva; }
}
