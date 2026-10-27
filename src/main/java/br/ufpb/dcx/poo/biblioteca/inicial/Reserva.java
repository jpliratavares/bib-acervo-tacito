package br.ufpb.dcx.poo.biblioteca.inicial;

import java.io.Serializable;
import java.time.LocalDate;

/** Reserva ativa na fila FIFO de um item. */
final class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String codigoDoItem;
    private final String matricula;
    private final LocalDate dataDaReserva;

    Reserva(String id, String codigoDoItem, String matricula, LocalDate dataDaReserva) {
        this.id = id;
        this.codigoDoItem = codigoDoItem;
        this.matricula = matricula;
        this.dataDaReserva = dataDaReserva;
    }

    String id() { return id; }
    String codigoDoItem() { return codigoDoItem; }
    String matricula() { return matricula; }
    LocalDate dataDaReserva() { return dataDaReserva; }
}
