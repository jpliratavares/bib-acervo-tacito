package br.ufpb.dcx.poo.biblioteca.inicial;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.LimiteDeEmprestimosExcedidoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;

/** Empréstimos, devoluções, atrasos e fila de reservas em memória. */
public class EmprestimosEmMemoria implements EmprestimoService {

    private final AcervoEmMemoria acervo;
    private final UsuariosEmMemoria usuarios;
    private final List<Emprestimo> emprestimos = new ArrayList<>();
    private final Map<String, List<Reserva>> filasPorItem = new HashMap<>();
    private final Map<String, Reserva> reservasPorId = new HashMap<>();
    private long proximoId = 1;
    private long proximoIdReserva = 1;

    EmprestimosEmMemoria(AcervoEmMemoria acervo, UsuariosEmMemoria usuarios) {
        this.acervo = acervo;
        this.usuarios = usuarios;
    }

    @Override
    public String emprestar(String tombo, String matricula, LocalDate data)
            throws RecursoNaoEncontradoException, ExemplarIndisponivelException,
                   UsuarioInativoException, LimiteDeEmprestimosExcedidoException {
        exigirTexto(tombo, "O tombo é obrigatório.");
        exigirTexto(matricula, "A matrícula é obrigatória.");
        exigirData(data);

        Exemplar exemplar = acervo.localizarExemplar(tombo);
        Usuario usuario = usuarios.localizarUsuario(matricula);
        if (!usuario.ativo()) {
            throw new UsuarioInativoException("O usuário " + matricula + " está inativo.");
        }
        if (exemplar.status() != StatusExemplar.DISPONIVEL) {
            throw new ExemplarIndisponivelException("O exemplar " + tombo + " não está disponível.");
        }
        garantirVezNaFila(exemplar.codigoDoItem(), matricula);
        if (emprestimosNaoDevolvidos(matricula) >= LIMITE_DE_EMPRESTIMOS) {
            throw new LimiteDeEmprestimosExcedidoException(
                    "O usuário " + matricula + " já atingiu o limite de " + LIMITE_DE_EMPRESTIMOS
                            + " empréstimos.");
        }
        consumirReserva(exemplar.codigoDoItem(), matricula);

        String id = "E-" + proximoId++;
        emprestimos.add(new Emprestimo(id, tombo, matricula, data, data.plusDays(PRAZO_EM_DIAS)));
        exemplar.marcarEmprestado();
        return id;
    }

    @Override
    public void devolver(String tombo, LocalDate data)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        exigirTexto(tombo, "O tombo é obrigatório.");
        exigirData(data);

        Exemplar exemplar = acervo.localizarExemplar(tombo);
        Emprestimo aberto = emprestimoAberto(tombo);
        if (aberto == null || exemplar.status() != StatusExemplar.EMPRESTADO) {
            throw new OperacaoNaoPermitidaException("O exemplar " + tombo + " não está emprestado.");
        }
        if (data.isBefore(aberto.dataDoEmprestimo())) {
            throw new OperacaoNaoPermitidaException(
                    "A devolução não pode ser anterior à data do empréstimo.");
        }
        aberto.registrarDevolucao(data);
        exemplar.disponibilizar();
    }

    @Override
    public List<EmprestimoView> listarEmprestimosDoUsuario(String matricula)
            throws RecursoNaoEncontradoException {
        exigirTexto(matricula, "A matrícula é obrigatória.");
        usuarios.localizarUsuario(matricula);
        return emprestimos.stream()
                .filter(emprestimo -> emprestimo.matricula().equals(matricula))
                .map(emprestimo -> paraView(emprestimo,
                        emprestimo.emAberto() ? StatusEmprestimo.ATIVO : StatusEmprestimo.DEVOLVIDO))
                .toList();
    }

    @Override
    public List<EmprestimoView> listarAtrasados(LocalDate data) {
        exigirData(data);
        return emprestimos.stream()
                .filter(emprestimo -> emprestimo.emAberto()
                        && data.isAfter(emprestimo.dataPrevistaDeDevolucao()))
                .map(emprestimo -> paraView(emprestimo, StatusEmprestimo.ATRASADO))
                .toList();
    }

    @Override
    public String reservar(String codigoDoItem, String matricula, LocalDate data)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        exigirTexto(codigoDoItem, "O código do item é obrigatório.");
        exigirTexto(matricula, "A matrícula é obrigatória.");
        exigirData(data);

        Item item = acervo.localizarItem(codigoDoItem);
        Usuario usuario = usuarios.localizarUsuario(matricula);
        if (!usuario.ativo()) {
            throw new OperacaoNaoPermitidaException("O usuário " + matricula + " está inativo.");
        }
        List<Reserva> fila = filasPorItem.getOrDefault(codigoDoItem, List.of());
        if (contar(item, StatusExemplar.DISPONIVEL) > fila.size()) {
            throw new OperacaoNaoPermitidaException(
                    "Há exemplar disponível para empréstimo imediato do item " + codigoDoItem + ".");
        }
        if (contarCirculaveis(item) == 0) {
            throw new OperacaoNaoPermitidaException(
                    "O item " + codigoDoItem + " não possui exemplar em circulação.");
        }
        if (indiceNaFila(fila, matricula) >= 0) {
            throw new OperacaoNaoPermitidaException(
                    "O usuário " + matricula + " já possui reserva ativa para o item " + codigoDoItem + ".");
        }
        if (possuiEmprestimoAbertoDoItem(matricula, codigoDoItem)) {
            throw new OperacaoNaoPermitidaException(
                    "O usuário " + matricula + " já está com um exemplar do item " + codigoDoItem
                            + " emprestado.");
        }

        String id = "R-" + proximoIdReserva++;
        Reserva reserva = new Reserva(id, codigoDoItem, matricula, data);
        filasPorItem.computeIfAbsent(codigoDoItem, chave -> new ArrayList<>()).add(reserva);
        reservasPorId.put(id, reserva);
        return id;
    }

    @Override
    public List<ReservaView> listarReservas(String codigoDoItem) throws RecursoNaoEncontradoException {
        exigirTexto(codigoDoItem, "O código do item é obrigatório.");
        acervo.localizarItem(codigoDoItem);
        List<Reserva> fila = filasPorItem.getOrDefault(codigoDoItem, List.of());
        List<ReservaView> views = new ArrayList<>();
        for (int indice = 0; indice < fila.size(); indice++) {
            Reserva reserva = fila.get(indice);
            views.add(new ReservaView(reserva.id(), reserva.codigoDoItem(), reserva.matricula(),
                    reserva.dataDaReserva(), indice + 1));
        }
        return List.copyOf(views);
    }

    @Override
    public void cancelarReserva(String idDaReserva) throws RecursoNaoEncontradoException {
        exigirTexto(idDaReserva, "O identificador da reserva é obrigatório.");
        Reserva reserva = reservasPorId.remove(idDaReserva);
        if (reserva == null) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada: " + idDaReserva);
        }
        List<Reserva> fila = filasPorItem.get(reserva.codigoDoItem());
        if (fila != null) {
            fila.remove(reserva);
        }
    }

    int emprestimosNaoDevolvidos(String matricula) {
        int total = 0;
        for (Emprestimo emprestimo : emprestimos) {
            if (emprestimo.matricula().equals(matricula) && emprestimo.emAberto()) {
                total++;
            }
        }
        return total;
    }

    private void garantirVezNaFila(String codigoDoItem, String matricula)
            throws RecursoNaoEncontradoException, ExemplarIndisponivelException {
        List<Reserva> fila = filasPorItem.getOrDefault(codigoDoItem, List.of());
        if (fila.isEmpty()) {
            return;
        }
        int disponiveis = contar(acervo.localizarItem(codigoDoItem), StatusExemplar.DISPONIVEL);
        if (disponiveis <= pessoasNaFrente(fila, matricula)) {
            throw new ExemplarIndisponivelException(
                    "Os exemplares disponíveis do item " + codigoDoItem
                            + " estão comprometidos com a fila de reserva.");
        }
    }

    private void consumirReserva(String codigoDoItem, String matricula) {
        List<Reserva> fila = filasPorItem.get(codigoDoItem);
        if (fila == null) {
            return;
        }
        Reserva encontrada = null;
        for (Reserva reserva : fila) {
            if (reserva.matricula().equals(matricula)) {
                encontrada = reserva;
                break;
            }
        }
        if (encontrada == null) {
            return;
        }
        fila.remove(encontrada);
        reservasPorId.remove(encontrada.id());
    }

    private boolean possuiEmprestimoAbertoDoItem(String matricula, String codigoDoItem)
            throws RecursoNaoEncontradoException {
        for (Emprestimo emprestimo : emprestimos) {
            if (emprestimo.emAberto() && emprestimo.matricula().equals(matricula)
                    && acervo.localizarExemplar(emprestimo.tombo()).codigoDoItem().equals(codigoDoItem)) {
                return true;
            }
        }
        return false;
    }

    private static int pessoasNaFrente(List<Reserva> fila, String matricula) {
        int indice = indiceNaFila(fila, matricula);
        return indice >= 0 ? indice : fila.size();
    }

    private static int indiceNaFila(List<Reserva> fila, String matricula) {
        for (int indice = 0; indice < fila.size(); indice++) {
            if (fila.get(indice).matricula().equals(matricula)) {
                return indice;
            }
        }
        return -1;
    }

    private static int contar(Item item, StatusExemplar status) {
        int total = 0;
        for (Exemplar exemplar : item.exemplares()) {
            if (exemplar.status() == status) {
                total++;
            }
        }
        return total;
    }

    private static int contarCirculaveis(Item item) {
        int total = 0;
        for (Exemplar exemplar : item.exemplares()) {
            if (exemplar.status() != StatusExemplar.INDISPONIVEL) {
                total++;
            }
        }
        return total;
    }

    private Emprestimo emprestimoAberto(String tombo) {
        for (int indice = emprestimos.size() - 1; indice >= 0; indice--) {
            Emprestimo emprestimo = emprestimos.get(indice);
            if (emprestimo.tombo().equals(tombo) && emprestimo.emAberto()) {
                return emprestimo;
            }
        }
        return null;
    }

    private static EmprestimoView paraView(Emprestimo emprestimo, StatusEmprestimo status) {
        return new EmprestimoView(emprestimo.id(), emprestimo.tombo(), emprestimo.matricula(),
                emprestimo.dataDoEmprestimo(), emprestimo.dataPrevistaDeDevolucao(),
                emprestimo.dataDaDevolucao(), status);
    }

    private static void exigirTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException(mensagem);
        }
    }

    private static void exigirData(LocalDate data) {
        if (data == null) {
            throw new DadosInvalidosException("A data é obrigatória.");
        }
    }
}
