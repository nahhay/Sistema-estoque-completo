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

public class PontoService {

    private static final Logger LOGGER = Logger.getLogger(PontoService.class.getName());

    public void cadastrar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BadRequestException("Nome do ponto e obrigatorio.");
        }
        String sql = "INSERT INTO ponto (nome) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome.trim());
            stmt.executeUpdate();
            LOGGER.info(() -> "Ponto cadastrado: " + nome);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao cadastrar ponto", e);
            throw new InternalErrorException("Erro ao cadastrar ponto.");
        }
    }

    public ArrayList<Ponto> getTodos() {
        ArrayList<Ponto> lista = new ArrayList<>();
        String sql = "SELECT id, nome FROM ponto ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Ponto(rs.getInt("id"), rs.getString("nome")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao listar pontos", e);
            throw new InternalErrorException("Erro ao listar pontos.");
        }
        return lista;
    }

    public Ponto buscar(int id) {
        String sql = "SELECT id, nome FROM ponto WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Ponto(rs.getInt("id"), rs.getString("nome"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar ponto", e);
            throw new InternalErrorException("Erro ao buscar ponto.");
        }
        return null;
    }

    public Ponto buscarOuFalhar(int id) {
        Ponto ponto = buscar(id);
        if (ponto == null) {
            throw new NotFoundException("Ponto #" + id + " nao encontrado.");
        }
        return ponto;
    }

    public void editar(int id, String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new BadRequestException("Nome do ponto e obrigatorio.");
        }
        String sql = "UPDATE ponto SET nome = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoNome.trim());
            stmt.setInt(2, id);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new NotFoundException("Ponto #" + id + " nao encontrado.");
            }
            LOGGER.info(() -> "Ponto #" + id + " editado.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao editar ponto", e);
            throw new InternalErrorException("Erro ao editar ponto.");
        }
    }

    public void remover(int id) {
        if (possuiDependencias(id)) {
            throw new ConflictException(
                    "Ponto #" + id + " possui estoque, movimentacoes, reposicoes ou conferencias associadas "
                            + "e nao pode ser removido.");
        }
        String sql = "DELETE FROM ponto WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new NotFoundException("Ponto #" + id + " nao encontrado.");
            }
            LOGGER.info(() -> "Ponto #" + id + " removido.");
        } catch (SQLException e) {
            if (SqlUtils.isIntegrityViolation(e)) {
                throw new ConflictException("Ponto #" + id + " possui dependencias e nao pode ser removido.");
            }
            LOGGER.log(Level.SEVERE, "Erro ao remover ponto", e);
            throw new InternalErrorException("Erro ao remover ponto.");
        }
    }

    private boolean possuiDependencias(int pontoId) {
        String sql = """
                SELECT
                  EXISTS(SELECT 1 FROM estoque WHERE ponto_id = ?)
                  OR EXISTS(SELECT 1 FROM movimentacao WHERE ponto_id = ?)
                  OR EXISTS(SELECT 1 FROM reposicao WHERE ponto_id = ?)
                  OR EXISTS(SELECT 1 FROM conferencia WHERE ponto_id = ?) AS possui
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pontoId);
            stmt.setInt(2, pontoId);
            stmt.setInt(3, pontoId);
            stmt.setInt(4, pontoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("possui");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar dependencias do ponto", e);
            throw new InternalErrorException("Erro ao verificar dependencias do ponto.");
        }
    }
}
