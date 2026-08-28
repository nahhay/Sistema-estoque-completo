package estoque.api;

public final class CorsConfig {

    private static final String ALLOWED_ORIGIN;

    static {
        String env = System.getenv("CORS_ALLOWED_ORIGIN");
        ALLOWED_ORIGIN = (env != null && !env.isBlank()) ? env.trim() : "*";
    }

    private CorsConfig() {
    }

    public static String allowedOrigin() {
        return ALLOWED_ORIGIN;
    }
}
