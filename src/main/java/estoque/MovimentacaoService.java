package estoque;

import estoque.db.DatabaseConnection;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovimentacaoService {

    private static final Logger LOGGER = Logger.getLogger(MovimentacaoService.class.getName());
    private final EstoqueService estoque;

    public MovimentacaoService(EstoqueService estoque) {
        this.estoque = estoque;
    }

    public void registrarEntrada(Produto produto, Ponto ponto, Funcionario funcionario, int quantidade) {
        if (quantidade <= 0) {
            throw new BadRequestException("Quantidade deve ser positiva.");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                registrarEntrada(conn, produto, ponto, funcionario, quantidade);
                conn.commit();
                LOGGER.info(() -> "Entrada registrada: produto=" + produto.getId() + " ponto=" + ponto.getId()
                        + " qtd=" + quantidade);
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao registrar entrada", e);
            throw new InternalErrorException("Erro ao registrar entrada.");
        }
    }

    public void registrarEntrada(Connection conn, Produto produto, Ponto ponto, Funcionario funcionario, int quantidade)
            throws SQLException {
        if (quantidade <= 0) {
            throw new BadRequestException("Quantidade deve ser positiva.");
        }
        estoque.adicionarEntrada(conn, produto, ponto, quantidade);
        inserirMovimentacao(conn, produto, ponto, funcionario, quantidade, TipoMovimentacao.ENTRADA);
    }

    public void registrarSaida(Produto produto, Ponto ponto, Funcionario funcionario, int quantidade) {
        if (quantidade <= 0) {
            throw new BadRequestException("Quantidade deve ser positiva.");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                registrarSaida(conn, produto, ponto, funcionario, quantidade);
                conn.commit();
                LOGGER.info(() -> "Saida registrada: produto=" + produto.getId() + " ponto=" + ponto.getId()
                        + " qtd=" + quantidade);
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao registrar saida", e);
            throw new InternalErrorException("Erro ao registrar saida.");
        }
    }

    public void registrarSaida(Connection conn, Produto produto, Ponto ponto, Funcionario funcionario, int quantidade)
            throws SQLException {
        if (quantidade <= 0) {
            throw new BadRequestException("Quantidade deve ser positiva.");
        }
        estoque.diminuir(conn, produto, ponto, quantidade);
        inserirMovimentacao(conn, produto, ponto, funcionario, quantidade, TipoMovimentacao.SAIDA);
    }

    private void inserirMovimentacao(Connection conn, Produto produto, Ponto ponto, Funcionario funcionario,
            int quantidade, TipoMovimentacao tipo) throws SQLException {
        String sql = """
                INSERT INTO movimentacao (produto_id, ponto_id, funcionario_id, quantidade, tipo, data_hora)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, produto.getId());
            stmt.setInt(2, ponto.getId());
            stmt.setInt(3, funcionario.getId());
            stmt.setInt(4, quantidade);
            stmt.setString(5, tipo.name());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }

    public ArrayList<Movimentacao> getTodos() {
        ArrayList<Movimentacao> lista = new ArrayList<>();
        String sql = """
                SELECT m.*, p.nome as produto_nome, pt.nome as ponto_nome, f.nome as funcionario_nome
                FROM movimentacao m
                JOIN produto p ON m.produto_id = p.id
                JOIN ponto pt ON m.ponto_id = pt.id
                JOIN funcionario f ON m.funcionario_id = f.id
                ORDER BY m.id
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapMovimentacao(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar movimentacoes", e);
            throw new InternalErrorException("Erro ao buscar movimentacoes.");
        }
        return lista;
    }

    public List<Movimentacao> historicoProduto(Produto produto, Ponto ponto) {
        String sql = """
                SELECT m.*, p.nome as produto_nome, pt.nome as ponto_nome, f.nome as funcionario_nome
                FROM movimentacao m
                JOIN produto p ON m.produto_id = p.id
                JOIN ponto pt ON m.ponto_id = pt.id
                JOIN funcionario f ON m.funcionario_id = f.id
                WHERE m.produto_id = ? AND m.ponto_id = ?
                ORDER BY m.data_hora
                """;
        List<Movimentacao> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, produto.getId());
            stmt.setInt(2, ponto.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapMovimentacao(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar historico", e);
            throw new InternalErrorException("Erro ao buscar historico.");
        }
        return lista;
    }

    private Movimentacao mapMovimentacao(ResultSet rs) throws SQLException {
        Produto produto = new Produto(rs.getInt("produto_id"), rs.getString("produto_nome"));
        Ponto ponto = new Ponto(rs.getInt("ponto_id"), rs.getString("ponto_nome"));
        Funcionario funcionario = new Funcionario(rs.getInt("funcionario_id"), rs.getString("funcionario_nome"));
        return new Movimentacao(
                rs.getInt("id"),
                produto,
                ponto,
                funcionario,
                rs.getInt("quantidade"),
                TipoMovimentacao.valueOf(rs.getString("tipo")),
                rs.getTimestamp("data_hora").toLocalDateTime());
    }
}
