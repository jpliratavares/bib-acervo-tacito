package br.ufpb.dcx.poo.biblioteca.inicial;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.RelatorioService;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioService;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;

/**
 * Reúne os quatro serviços em um único ponto de acesso.
 *
 * <p>Guarde esta classe na memória: na Aula 13 você vai descobrir que ela tem nome
 * e que a decisão de existir ou não tem consequências.</p>
 */
public class BibliotecaInicial implements Biblioteca {

    private final Path arquivoPersistencia;
    private final AcervoEmMemoria acervo = new AcervoEmMemoria();
    private final UsuariosEmMemoria usuarios = new UsuariosEmMemoria();
    private final EmprestimosEmMemoria emprestimos;
    private final RelatoriosNaoImplementados relatorios = new RelatoriosNaoImplementados();

    public BibliotecaInicial() {
        this(Path.of("target", "biblioteca-estado.dat"));
    }

    public BibliotecaInicial(Path arquivoPersistencia) {
        if (arquivoPersistencia == null) {
            throw new DadosInvalidosException("O arquivo de persistência é obrigatório.");
        }
        this.arquivoPersistencia = arquivoPersistencia;
        this.emprestimos = new EmprestimosEmMemoria(acervo, usuarios);
        this.usuarios.vincularEmprestimos(emprestimos::emprestimosNaoDevolvidos);
    }

    @Override
    public AcervoService acervo() { return acervo; }

    @Override
    public UsuarioService usuarios() { return usuarios; }

    @Override
    public EmprestimoService emprestimos() { return emprestimos; }

    @Override
    public RelatorioService relatorios() { return relatorios; }

    @Override
    public void salvar() throws PersistenciaException {
        try {
            Path diretorio = arquivoPersistencia.getParent();
            if (diretorio != null) {
                Files.createDirectories(diretorio);
            }
            EstadoBiblioteca estado = new EstadoBiblioteca(
                    acervo.copiarItens(),
                    usuarios.copiarUsuarios(),
                    emprestimos.copiarEmprestimos(),
                    emprestimos.copiarReservas(),
                    emprestimos.proximoIdEmprestimo(),
                    emprestimos.proximoIdReserva());
            try (ObjectOutputStream saida = new ObjectOutputStream(Files.newOutputStream(arquivoPersistencia))) {
                saida.writeObject(estado);
            }
        } catch (IOException e) {
            throw new PersistenciaException("Falha ao salvar o estado da biblioteca: " + e.getMessage());
        }
    }

    @Override
    public void carregar() throws PersistenciaException {
        if (!Files.isRegularFile(arquivoPersistencia)) {
            throw new PersistenciaException(
                    "Arquivo de persistência não encontrado: " + arquivoPersistencia);
        }
        try (ObjectInputStream entrada = new ObjectInputStream(Files.newInputStream(arquivoPersistencia))) {
            Object lido = entrada.readObject();
            if (!(lido instanceof EstadoBiblioteca estado) || !estado.integro()) {
                throw new PersistenciaException("Conteúdo inválido no arquivo de persistência.");
            }
            acervo.restaurar(estado.itens());
            usuarios.restaurar(estado.usuarios());
            emprestimos.restaurar(estado.emprestimos(), estado.reservas(),
                    estado.proximoIdEmprestimo(), estado.proximoIdReserva());
        } catch (PersistenciaException e) {
            throw e;
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenciaException("Falha ao carregar o estado da biblioteca: " + e.getMessage());
        }
    }
}
