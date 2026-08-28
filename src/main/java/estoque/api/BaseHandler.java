package estoque.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import estoque.exceptions.ApiException;
import estoque.exceptions.BadRequestException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseHandler implements HttpHandler {

    protected static final Gson gson = new Gson();
    private static final Logger LOGGER = Logger.getLogger(BaseHandler.class.getName());

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            sendCors(exchange);
            return;
        }

        if (!ApiKeyAuth.autorizado(exchange)) {
            sendJson(exchange, 401, ApiResponse.erro("Nao autorizado. Informe um cabecalho X-API-Key valido."));
            return;
        }

        try {
            handleRequest(exchange);
        } catch (ApiException e) {
            LOGGER.log(Level.WARNING, () -> "Erro de negocio (" + e.getStatusCode() + "): " + e.getMessage());
            sendJson(exchange, e.getStatusCode(), ApiResponse.erro(e.getMessage()));
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "Corpo da requisicao com JSON invalido", e);
            sendJson(exchange, 400, ApiResponse.erro("Corpo da requisicao invalido (JSON malformado)."));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro interno inesperado", e);
            sendJson(exchange, 500, ApiResponse.erro("Erro interno no servidor."));
        }
    }

    protected abstract void handleRequest(HttpExchange exchange) throws Exception;

    protected static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    protected static void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        String json = gson.toJson(payload);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        applyCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected static void sendCors(HttpExchange exchange) throws IOException {
        applyCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
    }

    private static void applyCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", CorsConfig.allowedOrigin());
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-API-Key");
    }

    protected static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String param : query.split("&")) {
            if (param.isBlank()) {
                continue;
            }
            String[] pair = param.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    protected static int parseIntParam(Map<String, String> params, String nomeCampo) {
        String raw = params.get(nomeCampo);
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Parametro '" + nomeCampo + "' e obrigatorio.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Parametro '" + nomeCampo + "' deve ser um numero inteiro valido "
                    + "(recebido: '" + raw + "').");
        }
    }

    protected static int extractIdFromPath(String path, String contextPrefix) {
        String resto = semBarraInicial(path, contextPrefix);
        int barra = resto.indexOf('/');
        String idParte = barra == -1 ? resto : resto.substring(0, barra);
        if (idParte.isBlank()) {
            throw new BadRequestException("ID nao informado na URL.");
        }
        try {
            return Integer.parseInt(idParte);
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID invalido na URL (recebido: '" + idParte + "').");
        }
    }

    protected static String extractSubPath(String path, String contextPrefix) {
        return semBarraInicial(path, contextPrefix);
    }

    private static String semBarraInicial(String path, String contextPrefix) {
        String resto = path.substring(contextPrefix.length());
        while (resto.startsWith("/")) {
            resto = resto.substring(1);
        }
        return resto;
    }

    protected static <T> T parseBody(HttpExchange exchange, Class<T> classe) throws IOException {
        String body = readBody(exchange);
        T request = gson.fromJson(body, classe);
        if (request == null) {
            throw new BadRequestException("Corpo da requisicao vazio ou invalido.");
        }
        return request;
    }

    protected static int parseIntOuBadRequest(String raw, String nomeCampo) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Parametro '" + nomeCampo + "' deve ser um numero inteiro valido "
                    + "(recebido: '" + raw + "').");
        }
    }
}
