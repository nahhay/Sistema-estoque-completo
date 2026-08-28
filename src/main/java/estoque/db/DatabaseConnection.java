package estoque.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static volatile HikariDataSource dataSource;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            init();
        }
        return dataSource.getConnection();
    }

    private static synchronized void init() {
        if (dataSource != null) {
            return;
        }

        Properties props = loadProperties();

        String url = getConfig(props, "db.url", "DB_URL", "jdbc:postgresql://localhost:5432/estoque");
        String user = getConfig(props, "db.user", "DB_USER", "postgres");
        String password = getConfig(props, "db.password", "DB_PASSWORD", "postgres");
        int maxPoolSize = Integer.parseInt(getConfig(props, "db.pool.maxSize", "DB_POOL_MAX_SIZE", "10"));
        int minIdle = Integer.parseInt(getConfig(props, "db.pool.minIdle", "DB_POOL_MIN_IDLE", "2"));
        long connectionTimeoutMs = Long.parseLong(
                getConfig(props, "db.pool.connectionTimeoutMs", "DB_POOL_CONN_TIMEOUT_MS", "30000"));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeoutMs);
        config.setPoolName("EstoquePool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        LOGGER.info(() -> "Pool de conexoes '" + config.getPoolName() + "' inicializado (max=" + maxPoolSize
                + ", minIdle=" + minIdle + ").");
        // Sem shutdown hook proprio aqui: quem chama shutdown() e o ApiServer, que
        // primeiro para de aceitar requisicoes HTTP e so depois fecha o pool.
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Pool de conexoes encerrado.");
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                LOGGER.log(Level.CONFIG, "config.properties nao encontrado no classpath; usando variaveis de "
                        + "ambiente e valores padrao.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao carregar config.properties, usando variaveis de ambiente e "
                    + "valores padrao.", e);
        }
        return props;
    }

    private static String getConfig(Properties props, String propKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String propValue = props.getProperty(propKey);
        if (propValue != null && !propValue.isBlank()) {
            return propValue;
        }
        return defaultValue;
    }
}
