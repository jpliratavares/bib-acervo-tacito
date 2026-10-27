package br.ufpb.dcx.poo.biblioteca.inicial;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

/** Empréstimos, devoluções e atrasos em memória. A fila de reservas entra no ciclo seguinte. */
public class EmprestimosEmMemoria implements EmprestimoService {

    private final AcervoEmMemoria acervo;
    private final UsuariosEmMemoria usuarios;
    private final List<Emprestimo> emprestimos = new ArrayList<>();
    private long proximoId = 1;

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
        if (emprestimosNaoDevolvidos(matricula) >= LIMITE_DE_EMPRESTIMOS) {
            throw new LimiteDeEmprestimosExcedidoException(
                    "O usuário " + matricula + " já atingiu o limite de " + LIMITE_DE_EMPRESTIMOS
                            + " empréstimos.");
        }

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
        throw new UnsupportedOperationException("Entrega 2: implementar reservar");
    }

    @Override
    public List<ReservaView> listarReservas(String codigoDoItem) throws RecursoNaoEncontradoException {
        throw new UnsupportedOperationException("Entrega 2: implementar listarReservas");
    }

    @Override
    public void cancelarReserva(String idDaReserva) throws RecursoNaoEncontradoException {
        throw new UnsupportedOperationException("Entrega 2: implementar cancelarReserva");
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
