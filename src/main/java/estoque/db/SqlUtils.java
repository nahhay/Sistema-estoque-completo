package estoque.db;

import java.sql.SQLException;

public final class SqlUtils {

    private SqlUtils() {
    }

    public static boolean isIntegrityViolation(SQLException e) {
        String state = e.getSQLState();
        return state != null && state.startsWith("23");
    }
}
