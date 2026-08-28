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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReposicaoService {

    private static final Logger LOGGER = Logger.getLogger(ReposicaoService.class.getName());

    private static final Set<StatusReposicao> PODE_SEPARAR = EnumSet.of(StatusReposicao.SOLICITADA);
    private static final Set<StatusReposicao> PODE_CONFERIR = EnumSet.of(StatusReposicao.SEPARADA);
    private static final Set<StatusReposicao> PODE_ENTREGAR = EnumSet.of(StatusReposicao.CONFERIDA);
    private static final Set<StatusReposicao> PODE_CANCELAR =
            EnumSet.of(StatusReposicao.SOLICITADA, StatusReposicao.SEPARADA);

    private final MovimentacaoService movimentacao;

    public ReposicaoService(MovimentacaoService movimentacao) {
        this.movimentacao = movimentacao;
    }

    public void solicitar(Produto produto, Ponto ponto, Funcionario funcionario, int quantidade) {
        if (produto == null || ponto == null || funcionario == null) {
            throw new BadRequestException("Produto, ponto ou funcionario invalido.");
        }
        if (quantidade <= 0) {
            throw new BadRequestException("A quantidade deve ser positiva.");
        }

        String sql = """
                INSERT INTO reposicao (produto_id, ponto_id, funcionario_id, quantidade, status, data_hora)
                VALUES (?, ?, ?, ?, 'SOLICITADA', ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, produto.getId());
            stmt.setInt(2, ponto.getId());
            stmt.setInt(3, funcionario.getId());
            stmt.setInt(4, quantidade);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    LOGGER.info(() -> "Reposicao #" + id + " solicitada.");
                }
            }
        } catch (SQLException e) {
            if (SqlUtils.isIntegrityViolation(e)) {
                throw new BadRequestException("Produto, ponto ou funcionario informado nao existe.");
            }
            LOGGER.log(Level.SEVERE, "Erro ao solicitar reposicao", e);
            throw new InternalErrorException("Erro ao solicitar reposicao.");
        }
    }

    public ArrayList<Reposicao> getTodos() {
        ArrayList<Reposicao> lista = new ArrayList<>();
        String sql = """
                SELECT r.*, p.nome as produto_nome, pt.nome as ponto_nome, f.nome as funcionario_nome
                FROM reposicao r
                JOIN produto p ON r.produto_id = p.id
                JOIN ponto pt ON r.ponto_id = pt.id
                JOIN funcionario f ON r.funcionario_id = f.id
                ORDER BY r.id
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapReposicao(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar reposicoes", e);
            throw new InternalErrorException("Erro ao buscar reposicoes.");
        }
        return lista;
    }

    public Reposicao buscar(int id) {
        String sql = """
                SELECT r.*, p.nome as produto_nome, pt.nome as ponto_nome, f.nome as funcionario_nome
                FROM reposicao r
                JOIN produto p ON r.produto_id = p.id
                JOIN ponto pt ON r.ponto_id = pt.id
                JOIN funcionario f ON r.funcionario_id = f.id
                WHERE r.id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReposicao(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar reposicao", e);
            throw new InternalErrorException("Erro ao buscar reposicao.");
        }
        return null;
    }

    public Reposicao buscarOuFalhar(int id) {
        Reposicao reposicao = buscar(id);
        if (reposicao == null) {
            throw new NotFoundException("Reposicao #" + id + " nao encontrada.");
        }
        return reposicao;
    }

    /**
     * Atualiza o status apenas se a reposicao ainda estiver em um dos status
     * "permitidos" no momento exato do UPDATE (nao apenas no momento da leitura).
     * Isso evita que duas requisicoes concorrentes (ex: gerente e funcionario
     * agindo ao mesmo tempo) validem a mesma transicao e ambas a executem.
     *
     * @return o status anterior, para mensagens de log/erro.
     */
    private StatusReposicao atualizarStatusSeElegivel(Connection conn, int id, Set<StatusReposicao> permitidos,
            StatusReposicao novoStatus, String acaoDescricao) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE reposicao SET status = ? WHERE id = ? AND status IN (");
        for (int i = 0; i < permitidos.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, novoStatus.name());
            stmt.setInt(2, id);
            int idx = 3;
            for (StatusReposicao status : permitidos) {
                stmt.setString(idx++, status.name());
            }
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                // Ou a reposicao nao existe, ou o status mudou entre a leitura e o UPDATE.
                Reposicao atual = buscar(id);
                if (atual == null) {
                    throw new NotFoundException("Reposicao #" + id + " nao encontrada.");
                }
                throw new ConflictException("Reposicao nao pode ser " + acaoDescricao + " estando no status "
                        + atual.getStatus() + ".");
            }
        }
        return novoStatus;
    }

    public void separar(int id) {
        buscarOuFalhar(id);
        try (Connection conn = DatabaseConnection.getConnection()) {
            atualizarStatusSeElegivel(conn, id, PODE_SEPARAR, StatusReposicao.SEPARADA, "separada");
            LOGGER.info(() -> "Reposicao #" + id + " separada.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao separar reposicao", e);
            throw new InternalErrorException("Erro ao separar reposicao.");
        }
    }

    public void conferir(int id) {
        buscarOuFalhar(id);
        try (Connection conn = DatabaseConnection.getConnection()) {
            atualizarStatusSeElegivel(conn, id, PODE_CONFERIR, StatusReposicao.CONFERIDA, "conferida");
            LOGGER.info(() -> "Reposicao #" + id + " conferida.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao conferir reposicao", e);
            throw new InternalErrorException("Erro ao conferir reposicao.");
        }
    }

    public void entregar(int id) {
        Reposicao reposicao = buscarOuFalhar(id);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Atualiza o status primeiro e de forma atomica: se outra requisicao ja
                // tiver movido a reposicao para fora de PODE_ENTREGAR, falha aqui antes
                // de registrar qualquer entrada de estoque.
                atualizarStatusSeElegivel(conn, id, PODE_ENTREGAR, StatusReposicao.ENTREGUE, "entregue");
                movimentacao.registrarEntrada(conn, reposicao.getProduto(), reposicao.getPonto(),
                        reposicao.getFuncionario(), reposicao.getQuantidade());
                conn.commit();
                LOGGER.info(() -> "Reposicao #" + id + " entregue.");
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao entregar reposicao", e);
            throw new InternalErrorException("Erro ao entregar reposicao.");
        }
    }

    public void cancelar(int id) {
        buscarOuFalhar(id);
        try (Connection conn = DatabaseConnection.getConnection()) {
            atualizarStatusSeElegivel(conn, id, PODE_CANCELAR, StatusReposicao.CANCELADA, "cancelada");
            LOGGER.info(() -> "Reposicao #" + id + " cancelada.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao cancelar reposicao", e);
            throw new InternalErrorException("Erro ao cancelar reposicao.");
        }
    }

    private Reposicao mapReposicao(ResultSet rs) throws SQLException {
        Produto produto = new Produto(rs.getInt("produto_id"), rs.getString("produto_nome"));
        Ponto ponto = new Ponto(rs.getInt("ponto_id"), rs.getString("ponto_nome"));
        Funcionario funcionario = new Funcionario(rs.getInt("funcionario_id"), rs.getString("funcionario_nome"));
        return new Reposicao(
                rs.getInt("id"),
                produto,
                ponto,
                funcionario,
                rs.getInt("quantidade"),
                StatusReposicao.valueOf(rs.getString("status")),
                rs.getTimestamp("data_hora").toLocalDateTime());
    }
}
