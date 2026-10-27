package br.ufpb.dcx.poo.biblioteca.inicial;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.ExemplarView;
import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

/** Implementação em memória do acervo de jogos de tabuleiro. */
public class AcervoEmMemoria implements AcervoService {

    private final Map<String, Item> itensPorCodigo = new HashMap<>();
    private final Map<String, Exemplar> exemplaresPorTombo = new HashMap<>();

    @Override
    public void cadastrarItem(String codigo, String titulo, String autoria,
                              String categoria, int ano) throws RecursoDuplicadoException {
        exigirTextoPreenchido(codigo, "codigo");
        exigirTextoPreenchido(titulo, "titulo");
        if (itensPorCodigo.containsKey(codigo)) {
            throw new RecursoDuplicadoException("Já existe item com o código " + codigo);
        }
        itensPorCodigo.put(codigo, new Item(codigo, titulo, autoria, categoria, ano));
    }

    @Override
    public ItemView buscarItem(String codigo) throws RecursoNaoEncontradoException {
        return paraView(localizarItem(codigo));
    }

    @Override
    public List<ItemView> listarItens() {
        return itensPorCodigo.values().stream()
                .map(this::paraView)
                .sorted(Comparator.comparing(ItemView::titulo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<ItemView> buscarPorTitulo(String trecho) {
        exigirTextoPreenchido(trecho, "trecho");
        String trechoNormalizado = trecho.toLowerCase(Locale.ROOT);
        return itensPorCodigo.values().stream()
                .filter(item -> item.titulo().toLowerCase(Locale.ROOT).contains(trechoNormalizado))
                .map(this::paraView)
                .sorted(Comparator.comparing(ItemView::titulo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<ItemView> buscarPorCategoria(String categoria) {
        exigirTextoPreenchido(categoria, "categoria");
        return itensPorCodigo.values().stream()
                .filter(item -> item.categoria().equalsIgnoreCase(categoria))
                .map(this::paraView)
                .sorted(Comparator.comparing(ItemView::titulo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public void adicionarExemplar(String codigoDoItem, String tombo)
            throws RecursoNaoEncontradoException, RecursoDuplicadoException {
        Item item = localizarItem(codigoDoItem);
        exigirTextoPreenchido(tombo, "tombo");
        if (exemplaresPorTombo.containsKey(tombo)) {
            throw new RecursoDuplicadoException("Já existe exemplar com o tombo " + tombo);
        }
        Exemplar exemplar = new Exemplar(tombo, item.codigo());
        item.adicionarExemplar(exemplar);
        exemplaresPorTombo.put(tombo, exemplar);
    }

    @Override
    public List<ExemplarView> listarExemplares(String codigoDoItem)
            throws RecursoNaoEncontradoException {
        Item item = localizarItem(codigoDoItem);
        return item.exemplares().stream()
                .map(exemplar -> new ExemplarView(
                        exemplar.tombo(), exemplar.codigoDoItem(), exemplar.status()))
                .sorted(Comparator.comparing(ExemplarView::tombo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public void baixarExemplar(String tombo)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        exigirTextoPreenchido(tombo, "tombo");
        Exemplar exemplar = exemplaresPorTombo.get(tombo);
        if (exemplar == null) {
            throw new RecursoNaoEncontradoException("Exemplar não encontrado: " + tombo);
        }
        if (exemplar.status() == StatusExemplar.EMPRESTADO
                || exemplar.status() == StatusExemplar.INDISPONIVEL) {
            throw new OperacaoNaoPermitidaException(
                    "O exemplar " + tombo + " não pode ser baixado no status " + exemplar.status() + ".");
        }
        exemplar.baixar();
    }

    private Item localizarItem(String codigo) throws RecursoNaoEncontradoException {
        Item item = itensPorCodigo.get(codigo);
        if (item == null) {
            throw new RecursoNaoEncontradoException("Item não encontrado: " + codigo);
        }
        return item;
    }

    private ItemView paraView(Item item) {
        long disponiveis = item.exemplares().stream()
                .filter(exemplar -> exemplar.status() == StatusExemplar.DISPONIVEL)
                .count();
        return new ItemView(item.codigo(), item.titulo(), item.autoria(), item.categoria(),
                item.ano(), item.exemplares().size(), (int) disponiveis);
    }

    private static void exigirTextoPreenchido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo " + campo + " é obrigatório.");
        }
    }
}
