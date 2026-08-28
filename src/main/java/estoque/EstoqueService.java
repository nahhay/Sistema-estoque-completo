package estoque;

import estoque.db.DatabaseConnection;
import estoque.db.SqlUtils;
import estoque.exceptions.BadRequestException;
import estoque.exceptions.ConflictException;
import estoque.exceptions.InternalErrorException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EstoqueService {

    private static final Logger LOGGER = Logger.getLogger(EstoqueService.class.getName());

    void adicionarEntrada(Connection conn, Produto produto, Ponto ponto, int quantidade) throws SQLException {
        String sql = """
                INSERT INTO estoque (produto_id, ponto_id, quantidade)
                VALUES (?, ?, ?)
                ON CONFLICT (produto_id, ponto_id)
                DO UPDATE SET quantidade = estoque.quantidade + ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, produto.getId());
            stmt.setInt(2, ponto.getId());
            stmt.setInt(3, quantidade);
            stmt.setInt(4, quantidade);
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (SqlUtils.isIntegrityViolation(e)) {
                throw new BadRequestException("Produto ou ponto informado nao existe.");
            }
            throw e;
        }
    }

    public int consultarQuantidade(Produto produto, Ponto ponto) {
        String sql = "SELECT quantidade FROM estoque WHERE produto_id = ? AND ponto_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, produto.getId());
            stmt.setInt(2, ponto.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantidade");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao consultar estoque", e);
            throw new InternalErrorException("Erro ao consultar estoque.");
        }
        return 0;
    }

    void diminuir(Connection conn, Produto produto, Ponto ponto, int quantidade) throws SQLException {
        String sql = """
                UPDATE estoque
                SET quantidade = quantidade - ?
                WHERE produto_id = ? AND ponto_id = ? AND quantidade >= ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantidade);
            stmt.setInt(2, produto.getId());
            stmt.setInt(3, ponto.getId());
            stmt.setInt(4, quantidade);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new ConflictException("Estoque insuficiente ou produto nao encontrado nesse ponto.");
            }
        }
    }

    public List<Estoque> listarTodos() {
        List<Estoque> lista = new ArrayList<>();
        String sql = """
                SELECT p.id as produto_id, p.nome as produto_nome,
                       pt.id as ponto_id, pt.nome as ponto_nome, e.quantidade
                FROM estoque e
                JOIN produto p ON e.produto_id = p.id
                JOIN ponto pt ON e.ponto_id = pt.id
                ORDER BY p.nome
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Produto produto = new Produto(rs.getInt("produto_id"), rs.getString("produto_nome"));
                Ponto ponto = new Ponto(rs.getInt("ponto_id"), rs.getString("ponto_nome"));
                lista.add(new Estoque(produto, ponto, rs.getInt("quantidade")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao listar estoque", e);
            throw new InternalErrorException("Erro ao listar estoque.");
        }
        return lista;
    }
}
