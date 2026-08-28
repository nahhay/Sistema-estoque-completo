package estoque.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import estoque.Conferencia;
import estoque.ConferenciaService;
import estoque.EstoqueService;
import estoque.Funcionario;
import estoque.FuncionarioService;
import estoque.Movimentacao;
import estoque.MovimentacaoService;
import estoque.Ponto;
import estoque.PontoService;
import estoque.Produto;
import estoque.ProdutoService;
import estoque.Reposicao;
import estoque.ReposicaoService;
import estoque.api.requests.ConferenciaRequest;
import estoque.api.requests.FuncionarioRequest;
import estoque.api.requests.MovimentacaoRequest;
import estoque.api.requests.PontoRequest;
import estoque.api.requests.ProdutoRequest;
import estoque.api.requests.ReposicaoRequest;
import estoque.db.DatabaseConnection;
import estoque.exceptions.BadRequestException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ApiServer {

    private static final Logger LOGGER = Logger.getLogger(ApiServer.class.getName());
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    private static final ProdutoService produtoService = new ProdutoService();
    private static final FuncionarioService funcionarioService = new FuncionarioService();
    private static final PontoService pontoService = new PontoService();
    private static final EstoqueService estoqueService = new EstoqueService();
    private static final MovimentacaoService movimentacaoService = new MovimentacaoService(estoqueService);
    private static final ReposicaoService reposicaoService = new ReposicaoService(movimentacaoService);
    private static final ConferenciaService conferenciaService = new ConferenciaService(estoqueService);

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/produtos", new ProdutosHandler());
        server.createContext("/api/funcionarios", new FuncionariosHandler());
        server.createContext("/api/pontos", new PontosHandler());
        server.createContext("/api/estoque", new EstoqueHandler());
        server.createContext("/api/estoque/entrada", new EntradaHandler());
        server.createContext("/api/estoque/saida", new SaidaHandler());
        server.createContext("/api/reposicoes/solicitar", new SolicitarReposicaoHandler());
        server.createContext("/api/reposicoes", new ReposicoesHandler());
        server.createContext("/api/conferencias", new ConferenciasHandler());
        server.createContext("/api/movimentacoes", new MovimentacoesHandler());
        server.createContext("/api/historico", new HistoricoHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(16));
        server.start();

        LOGGER.info("Servidor API rodando em http://localhost:" + PORT);
        LOGGER.info("Acesse o frontend em: frontend/index.html");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Encerrando servidor...");
            server.stop(2);
            DatabaseConnection.shutdown();
        }, "server-shutdown"));
    }

    static class ProdutosHandler extends BaseHandler {
        private static final String CONTEXT = "/api/produtos";

        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            String method = exchange.getRequestMethod();
            String subPath = extractSubPath(exchange.getRequestURI().getPath(), CONTEXT);

            if (subPath.isBlank()) {
                if ("GET".equals(method)) {
                    ArrayList<Produto> produtos = produtoService.getTodos();
                    sendJson(exchange, 200, ApiResponse.ok(produtos));
                } else if ("POST".equals(method)) {
                    ProdutoRequest request = parseBody(exchange, ProdutoRequest.class);
                    validarNome(request.getNome(), "produto");
                    produtoService.cadastrar(request.getNome());
                    sendJson(exchange, 201, ApiResponse.mensagem("Produto cadastrado com sucesso!"));
                } else {
                    sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                }
                return;
            }

            int id = extractIdFromPath(exchange.getRequestURI().getPath(), CONTEXT);
            if ("PUT".equals(method)) {
                ProdutoRequest request = parseBody(exchange, ProdutoRequest.class);
                validarNome(request.getNome(), "produto");
                produtoService.editar(id, request.getNome());
                sendJson(exchange, 200, ApiResponse.mensagem("Produto editado com sucesso!"));
            } else if ("DELETE".equals(method)) {
                produtoService.remover(id);
                sendJson(exchange, 200, ApiResponse.mensagem("Produto removido com sucesso!"));
            } else {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
            }
        }
    }

    static class FuncionariosHandler extends BaseHandler {
        private static final String CONTEXT = "/api/funcionarios";

        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            String method = exchange.getRequestMethod();
            String subPath = extractSubPath(exchange.getRequestURI().getPath(), CONTEXT);

            if (subPath.isBlank()) {
                if ("GET".equals(method)) {
                    ArrayList<Funcionario> funcionarios = funcionarioService.getTodos();
                    sendJson(exchange, 200, ApiResponse.ok(funcionarios));
                } else if ("POST".equals(method)) {
                    FuncionarioRequest request = parseBody(exchange, FuncionarioRequest.class);
                    validarNome(request.getNome(), "funcionario");
                    funcionarioService.cadastrar(request.getNome());
                    sendJson(exchange, 201, ApiResponse.mensagem("Funcionario cadastrado com sucesso!"));
                } else {
                    sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                }
                return;
            }

            int id = extractIdFromPath(exchange.getRequestURI().getPath(), CONTEXT);
            if ("PUT".equals(method)) {
                FuncionarioRequest request = parseBody(exchange, FuncionarioRequest.class);
                validarNome(request.getNome(), "funcionario");
                funcionarioService.editar(id, request.getNome());
                sendJson(exchange, 200, ApiResponse.mensagem("Funcionario editado com sucesso!"));
            } else if ("DELETE".equals(method)) {
                funcionarioService.remover(id);
                sendJson(exchange, 200, ApiResponse.mensagem("Funcionario removido com sucesso!"));
            } else {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
            }
        }
    }

    static class PontosHandler extends BaseHandler {
        private static final String CONTEXT = "/api/pontos";

        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            String method = exchange.getRequestMethod();
            String subPath = extractSubPath(exchange.getRequestURI().getPath(), CONTEXT);

            if (subPath.isBlank()) {
                if ("GET".equals(method)) {
                    ArrayList<Ponto> pontos = pontoService.getTodos();
                    sendJson(exchange, 200, ApiResponse.ok(pontos));
                } else if ("POST".equals(method)) {
                    PontoRequest request = parseBody(exchange, PontoRequest.class);
                    validarNome(request.getNome(), "ponto");
                    pontoService.cadastrar(request.getNome());
                    sendJson(exchange, 201, ApiResponse.mensagem("Ponto cadastrado com sucesso!"));
                } else {
                    sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                }
                return;
            }

            int id = extractIdFromPath(exchange.getRequestURI().getPath(), CONTEXT);
            if ("PUT".equals(method)) {
                PontoRequest request = parseBody(exchange, PontoRequest.class);
                validarNome(request.getNome(), "ponto");
                pontoService.editar(id, request.getNome());
                sendJson(exchange, 200, ApiResponse.mensagem("Ponto editado com sucesso!"));
            } else if ("DELETE".equals(method)) {
                pontoService.remover(id);
                sendJson(exchange, 200, ApiResponse.mensagem("Ponto removido com sucesso!"));
            } else {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
            }
        }
    }

    static class EstoqueHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                return;
            }
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            int produtoId = parseIntParam(params, "produtoId");
            int pontoId = parseIntParam(params, "pontoId");

            Produto produto = produtoService.buscarOuFalhar(produtoId);
            Ponto ponto = pontoService.buscarOuFalhar(pontoId);

            int quantidade = estoqueService.consultarQuantidade(produto, ponto);
            sendJson(exchange, 200, ApiResponse.ok(quantidade));
        }
    }

    static class EntradaHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                return;
            }
            MovimentacaoRequest request = parseBody(exchange, MovimentacaoRequest.class);
            validarMovimentacao(request);

            Produto produto = produtoService.buscarOuFalhar(request.getProdutoId());
            Ponto ponto = pontoService.buscarOuFalhar(request.getPontoId());
            Funcionario funcionario = funcionarioService.buscarOuFalhar(request.getFuncionarioId());

            movimentacaoService.registrarEntrada(produto, ponto, funcionario, request.getQuantidade());
            sendJson(exchange, 200, ApiResponse.mensagem("Entrada registrada com sucesso!"));
        }
    }

    static class SaidaHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                return;
            }
            MovimentacaoRequest request = parseBody(exchange, MovimentacaoRequest.class);
            validarMovimentacao(request);

            Produto produto = produtoService.buscarOuFalhar(request.getProdutoId());
            Ponto ponto = pontoService.buscarOuFalhar(request.getPontoId());
            Funcionario funcionario = funcionarioService.buscarOuFalhar(request.getFuncionarioId());

            movimentacaoService.registrarSaida(produto, ponto, funcionario, request.getQuantidade());
            sendJson(exchange, 200, ApiResponse.mensagem("Saida registrada com sucesso!"));
        }
    }

    static class SolicitarReposicaoHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                return;
            }
            ReposicaoRequest request = parseBody(exchange, ReposicaoRequest.class);
            if (request.getQuantidade() <= 0) {
                throw new BadRequestException("Quantidade deve ser positiva.");
            }

            Produto produto = produtoService.buscarOuFalhar(request.getProdutoId());
            Ponto ponto = pontoService.buscarOuFalhar(request.getPontoId());
            Funcionario funcionario = funcionarioService.buscarOuFalhar(request.getFuncionarioId());

            reposicaoService.solicitar(produto, ponto, funcionario, request.getQuantidade());
            sendJson(exchange, 201, ApiResponse.mensagem("Reposicao solicitada com sucesso!"));
        }
    }

    static class ReposicoesHandler extends BaseHandler {
        private static final String CONTEXT = "/api/reposicoes";

        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            String method = exchange.getRequestMethod();
            String subPath = extractSubPath(exchange.getRequestURI().getPath(), CONTEXT);

            if (subPath.isBlank()) {
                if (!"GET".equals(method)) {
                    sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                    return;
                }
                ArrayList<Reposicao> reposicoes = reposicaoService.getTodos();
                ArrayList<Map<String, Object>> lista = new ArrayList<>();
                for (Reposicao r : reposicoes) {
                    lista.add(mapReposicao(r));
                }
                sendJson(exchange, 200, ApiResponse.ok(lista));
                return;
            }

            if (!"PUT".equals(method)) {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                return;
            }

            String[] partes = subPath.split("/");
            if (partes.length != 2) {
                throw new BadRequestException(
                        "Rota invalida. Use /api/reposicoes/{id}/separar|conferir|entregar|cancelar.");
            }
            int id = parseIntOuBadRequest(partes[0], "id");
            String acao = partes[1];

            switch (acao) {
                case "separar" -> {
                    reposicaoService.separar(id);
                    sendJson(exchange, 200, ApiResponse.mensagem("Reposicao separada com sucesso!"));
                }
                case "conferir" -> {
                    reposicaoService.conferir(id);
                    sendJson(exchange, 200, ApiResponse.mensagem("Reposicao conferida com sucesso!"));
                }
                case "entregar" -> {
                    reposicaoService.entregar(id);
                    sendJson(exchange, 200, ApiResponse.mensagem("Reposicao entregue com sucesso!"));
                }
                case "cancelar" -> {
                    reposicaoService.cancelar(id);
                    sendJson(exchange, 200, ApiResponse.mensagem("Reposicao cancelada com sucesso!"));
                }
                default -> throw new BadRequestException("Transicao desconhecida: '" + acao + "'.");
            }
        }
    }

    static class ConferenciasHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            String method = exchange.getRequestMethod();
            if ("GET".equals(method)) {
                ArrayList<Conferencia> conferencias = conferenciaService.getTodos();
                ArrayList<Map<String, Object>> lista = new ArrayList<>();
                for (Conferencia c : conferencias) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", c.getId());
                    item.put("produtoId", c.getProduto().getId());
                    item.put("produtoNome", c.getProduto().getNome());
                    item.put("pontoId", c.getPonto().getId());
                    item.put("pontoNome", c.getPonto().getNome());
                    item.put("funcionarioId", c.getResponsavel().getId());
                    item.put("funcionarioNome", c.getResponsavel().getNome());
                    item.put("estoqueEsperado", c.getEstoqueEsperado());
                    item.put("estoqueFisico", c.getEstoqueFisico());
                    item.put("divergencia", c.getDivergencia());
                    item.put("data", c.getData().toString());
                    lista.add(item);
                }
                sendJson(exchange, 200, ApiResponse.ok(lista));
            } else if ("POST".equals(method)) {
                ConferenciaRequest request = parseBody(exchange, ConferenciaRequest.class);
                if (request.getQuantidadeFisica() < 0) {
                    throw new BadRequestException("Quantidade fisica invalida.");
                }

                Ponto ponto = pontoService.buscarOuFalhar(request.getPontoId());
                Funcionario funcionario = funcionarioService.buscarOuFalhar(request.getFuncionarioId());
                Produto produto = produtoService.buscarOuFalhar(request.getProdutoId());

                conferenciaService.realizarConferencia(ponto, funcionario, produto, request.getQuantidadeFisica());
                sendJson(exchange, 201, ApiResponse.mensagem("Conferencia realizada com sucesso!"));
            } else {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
            }
        }
    }

    static class MovimentacoesHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                return;
            }

            ArrayList<Movimentacao> movimentacoes = movimentacaoService.getTodos();
            ArrayList<Map<String, Object>> lista = new ArrayList<>();
            for (Movimentacao m : movimentacoes) {
                lista.add(mapMovimentacao(m));
            }
            sendJson(exchange, 200, ApiResponse.ok(lista));
        }
    }

    static class HistoricoHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, ApiResponse.erro("Metodo nao permitido."));
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            int produtoId = parseIntParam(params, "produtoId");
            int pontoId = parseIntParam(params, "pontoId");

            Produto produto = produtoService.buscarOuFalhar(produtoId);
            Ponto ponto = pontoService.buscarOuFalhar(pontoId);

            java.util.List<Movimentacao> historico = movimentacaoService.historicoProduto(produto, ponto);
            ArrayList<Map<String, Object>> movs = new ArrayList<>();
            for (Movimentacao m : historico) {
                movs.add(mapMovimentacao(m));
            }

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("saldo", estoqueService.consultarQuantidade(produto, ponto));
            resultado.put("movimentacoes", movs);
            sendJson(exchange, 200, ApiResponse.ok(resultado));
        }
    }

    private static void validarNome(String nome, String entidade) {
        if (nome == null || nome.isBlank()) {
            throw new BadRequestException("Nome do " + entidade + " e obrigatorio.");
        }
    }

    private static void validarMovimentacao(MovimentacaoRequest request) {
        if (request.getQuantidade() <= 0) {
            throw new BadRequestException("Quantidade deve ser positiva.");
        }
    }

    private static Map<String, Object> mapReposicao(Reposicao r) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", r.getId());
        item.put("produtoId", r.getProduto().getId());
        item.put("produtoNome", r.getProduto().getNome());
        item.put("pontoId", r.getPonto().getId());
        item.put("pontoNome", r.getPonto().getNome());
        item.put("funcionarioId", r.getFuncionario().getId());
        item.put("funcionarioNome", r.getFuncionario().getNome());
        item.put("quantidade", r.getQuantidade());
        item.put("status", r.getStatus().name());
        item.put("dataHora", r.getDataHora().toString());
        return item;
    }

    private static Map<String, Object> mapMovimentacao(Movimentacao m) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", m.getId());
        item.put("produtoId", m.getProduto().getId());
        item.put("produtoNome", m.getProduto().getNome());
        item.put("pontoId", m.getPonto().getId());
        item.put("pontoNome", m.getPonto().getNome());
        item.put("funcionarioId", m.getFuncionario().getId());
        item.put("funcionarioNome", m.getFuncionario().getNome());
        item.put("quantidade", m.getQuantidade());
        item.put("tipo", m.getTipo().name());
        item.put("dataHora", m.getDataHora().toString());
        return item;
    }
}
