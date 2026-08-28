package estoque;

import estoque.db.DatabaseConnection;
import estoque.db.SqlUtils;
import estoque.exceptions.BadRequestException;
import estoque.exceptions.ConflictException;
import estoque.exceptions.InternalErrorException;
import estoque.exceptions.NotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProdutoService {

    private static final Logger LOGGER = Logger.getLogger(ProdutoService.class.getName());

    public void cadastrar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BadRequestException("Nome do produto e obrigatorio.");
        }
        String sql = "INSERT INTO produto (nome) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome.trim());
            stmt.executeUpdate();
            LOGGER.info(() -> "Produto cadastrado: " + nome);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao cadastrar produto", e);
            throw new InternalErrorException("Erro ao cadastrar produto.");
        }
    }

    public ArrayList<Produto> getTodos() {
        ArrayList<Produto> lista = new ArrayList<>();
        String sql = "SELECT id, nome FROM produto ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Produto(rs.getInt("id"), rs.getString("nome")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao listar produtos", e);
            throw new InternalErrorException("Erro ao listar produtos.");
        }
        return lista;
    }

    public Produto buscar(int id) {
        String sql = "SELECT id, nome FROM produto WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Produto(rs.getInt("id"), rs.getString("nome"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar produto", e);
            throw new InternalErrorException("Erro ao buscar produto.");
        }
        return null;
    }

    public Produto buscarOuFalhar(int id) {
        Produto produto = buscar(id);
        if (produto == null) {
            throw new NotFoundException("Produto #" + id + " nao encontrado.");
        }
        return produto;
    }

    public void editar(int id, String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new BadRequestException("Nome do produto e obrigatorio.");
        }
        String sql = "UPDATE produto SET nome = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoNome.trim());
            stmt.setInt(2, id);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new NotFoundException("Produto #" + id + " nao encontrado.");
            }
            LOGGER.info(() -> "Produto #" + id + " editado.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao editar produto", e);
            throw new InternalErrorException("Erro ao editar produto.");
        }
    }

    public void remover(int id) {
        if (possuiDependencias(id)) {
            throw new ConflictException(
                    "Produto #" + id + " possui estoque, movimentacoes, reposicoes ou conferencias associadas "
                            + "e nao pode ser removido.");
        }
        String sql = "DELETE FROM produto WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new NotFoundException("Produto #" + id + " nao encontrado.");
            }
            LOGGER.info(() -> "Produto #" + id + " removido.");
        } catch (SQLException e) {
            if (SqlUtils.isIntegrityViolation(e)) {
                throw new ConflictException("Produto #" + id + " possui dependencias e nao pode ser removido.");
            }
            LOGGER.log(Level.SEVERE, "Erro ao remover produto", e);
            throw new InternalErrorException("Erro ao remover produto.");
        }
    }

    private boolean possuiDependencias(int produtoId) {
        String sql = """
                SELECT
                  EXISTS(SELECT 1 FROM estoque WHERE produto_id = ?)
                  OR EXISTS(SELECT 1 FROM movimentacao WHERE produto_id = ?)
                  OR EXISTS(SELECT 1 FROM reposicao WHERE produto_id = ?)
                  OR EXISTS(SELECT 1 FROM conferencia WHERE produto_id = ?) AS possui
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, produtoId);
            stmt.setInt(2, produtoId);
            stmt.setInt(3, produtoId);
            stmt.setInt(4, produtoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("possui");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar dependencias do produto", e);
            throw new InternalErrorException("Erro ao verificar dependencias do produto.");
        }
    }
}
