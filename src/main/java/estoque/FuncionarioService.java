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

public class FuncionarioService {

    private static final Logger LOGGER = Logger.getLogger(FuncionarioService.class.getName());

    public void cadastrar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BadRequestException("Nome do funcionario e obrigatorio.");
        }
        String sql = "INSERT INTO funcionario (nome) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome.trim());
            stmt.executeUpdate();
            LOGGER.info(() -> "Funcionario cadastrado: " + nome);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao cadastrar funcionario", e);
            throw new InternalErrorException("Erro ao cadastrar funcionario.");
        }
    }

    public ArrayList<Funcionario> getTodos() {
        ArrayList<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT id, nome FROM funcionario ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Funcionario(rs.getInt("id"), rs.getString("nome")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao listar funcionarios", e);
            throw new InternalErrorException("Erro ao listar funcionarios.");
        }
        return lista;
    }

    public Funcionario buscar(int id) {
        String sql = "SELECT id, nome FROM funcionario WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Funcionario(rs.getInt("id"), rs.getString("nome"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar funcionario", e);
            throw new InternalErrorException("Erro ao buscar funcionario.");
        }
        return null;
    }

    public Funcionario buscarOuFalhar(int id) {
        Funcionario funcionario = buscar(id);
        if (funcionario == null) {
            throw new NotFoundException("Funcionario #" + id + " nao encontrado.");
        }
        return funcionario;
    }

    public void editar(int id, String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new BadRequestException("Nome do funcionario e obrigatorio.");
        }
        String sql = "UPDATE funcionario SET nome = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoNome.trim());
            stmt.setInt(2, id);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new NotFoundException("Funcionario #" + id + " nao encontrado.");
            }
            LOGGER.info(() -> "Funcionario #" + id + " editado.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao editar funcionario", e);
            throw new InternalErrorException("Erro ao editar funcionario.");
        }
    }

    public void remover(int id) {
        if (possuiDependencias(id)) {
            throw new ConflictException(
                    "Funcionario #" + id + " possui movimentacoes, reposicoes ou conferencias associadas "
                            + "e nao pode ser removido.");
        }
        String sql = "DELETE FROM funcionario WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new NotFoundException("Funcionario #" + id + " nao encontrado.");
            }
            LOGGER.info(() -> "Funcionario #" + id + " removido.");
        } catch (SQLException e) {
            if (SqlUtils.isIntegrityViolation(e)) {
                throw new ConflictException("Funcionario #" + id + " possui dependencias e nao pode ser removido.");
            }
            LOGGER.log(Level.SEVERE, "Erro ao remover funcionario", e);
            throw new InternalErrorException("Erro ao remover funcionario.");
        }
    }

    private boolean possuiDependencias(int funcionarioId) {
        String sql = """
                SELECT
                  EXISTS(SELECT 1 FROM movimentacao WHERE funcionario_id = ?)
                  OR EXISTS(SELECT 1 FROM reposicao WHERE funcionario_id = ?)
                  OR EXISTS(SELECT 1 FROM conferencia WHERE funcionario_id = ?) AS possui
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, funcionarioId);
            stmt.setInt(2, funcionarioId);
            stmt.setInt(3, funcionarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("possui");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar dependencias do funcionario", e);
            throw new InternalErrorException("Erro ao verificar dependencias do funcionario.");
        }
    }
}
