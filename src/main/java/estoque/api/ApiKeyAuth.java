package estoque.api;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public final class ApiKeyAuth {

    private static final Logger LOGGER = Logger.getLogger(ApiKeyAuth.class.getName());
    private static final Set<String> CHAVES_VALIDAS = new HashSet<>();
    private static final boolean HABILITADA;

    static {
        String chavesEnv = System.getenv("API_KEYS");
        if (chavesEnv == null || chavesEnv.isBlank()) {
            chavesEnv = lerChaveDoConfigProperties();
        }

        if (chavesEnv != null && !chavesEnv.isBlank()) {
            Arrays.stream(chavesEnv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(CHAVES_VALIDAS::add);
        }

        HABILITADA = !CHAVES_VALIDAS.isEmpty();
        if (!HABILITADA) {
            LOGGER.warning("Nenhuma API key configurada (API_KEYS ou api.keys) - autenticacao DESABILITADA. "
                    + "Configure antes de colocar em producao.");
        }
    }

    private ApiKeyAuth() {
    }

    public static boolean autorizado(HttpExchange exchange) {
        if (!HABILITADA) {
            return true;
        }
        String chave = exchange.getRequestHeaders().getFirst("X-API-Key");
        return chave != null && CHAVES_VALIDAS.contains(chave.trim());
    }

    private static String lerChaveDoConfigProperties() {
        Properties props = new Properties();
        try (InputStream in = ApiKeyAuth.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
                return props.getProperty("api.keys");
            }
        } catch (IOException e) {
            LOGGER.warning("Erro ao ler api.keys de config.properties: " + e.getMessage());
        }
        return null;
    }
}
