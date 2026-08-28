package estoque;

import estoque.db.DatabaseConnection;
import estoque.db.SqlUtils;
import estoque.exceptions.BadRequestException;
import estoque.exceptions.InternalErrorException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConferenciaService {

    private static final Logger LOGGER = Logger.getLogger(ConferenciaService.class.getName());
    private final EstoqueService estoque;

    public ConferenciaService(EstoqueService estoque) {
        this.estoque = estoque;
    }

    public void realizarConferencia(Ponto ponto, Funcionario funcionario, Produto produto, int quantidadeFisica) {
        if (ponto == null || funcionario == null || produto == null) {
            throw new BadRequestException("Ponto, funcionario ou produto invalido.");
        }
        if (quantidadeFisica < 0) {
            throw new BadRequestException("Quantidade fisica invalida.");
        }

        int estoqueEsperado = estoque.consultarQuantidade(produto, ponto);
        int divergencia = quantidadeFisica - estoqueEsperado;

        String sql = """
                INSERT INTO conferencia (produto_id, ponto_id, funcionario_id, estoque_esperado, estoque_fisico,
                                          divergencia, data)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, produto.getId());
            stmt.setInt(2, ponto.getId());
            stmt.setInt(3, funcionario.getId());
            stmt.setInt(4, estoqueEsperado);
            stmt.setInt(5, quantidadeFisica);
            stmt.setInt(6, divergencia);
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    LOGGER.info(() -> "Conferencia #" + id + " realizada (divergencia=" + divergencia + ").");
                }
            }
        } catch (SQLException e) {
            if (SqlUtils.isIntegrityViolation(e)) {
                throw new BadRequestException("Produto, ponto ou funcionario informado nao existe.");
            }
            LOGGER.log(Level.SEVERE, "Erro ao realizar conferencia", e);
            throw new InternalErrorException("Erro ao realizar conferencia.");
        }
    }

    public ArrayList<Conferencia> getTodos() {
        ArrayList<Conferencia> lista = new ArrayList<>();
        String sql = """
                SELECT c.*, p.nome as produto_nome, pt.nome as ponto_nome, f.nome as funcionario_nome
                FROM conferencia c
                JOIN produto p ON c.produto_id = p.id
                JOIN ponto pt ON c.ponto_id = pt.id
                JOIN funcionario f ON c.funcionario_id = f.id
                ORDER BY c.id
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Ponto ponto = new Ponto(rs.getInt("ponto_id"), rs.getString("ponto_nome"));
                Funcionario funcionario = new Funcionario(rs.getInt("funcionario_id"), rs.getString("funcionario_nome"));
                Produto produto = new Produto(rs.getInt("produto_id"), rs.getString("produto_nome"));

                lista.add(new Conferencia(
                        rs.getInt("id"),
                        ponto,
                        funcionario,
                        produto,
                        rs.getInt("estoque_esperado"),
                        rs.getInt("estoque_fisico"),
                        rs.getTimestamp("data").toLocalDateTime()));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar conferencias", e);
            throw new InternalErrorException("Erro ao buscar conferencias.");
        }
        return lista;
    }
}
