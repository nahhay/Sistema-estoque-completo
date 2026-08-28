package estoque.api.requests;

public class ReposicaoRequest {
    private int produtoId;
    private int pontoId;
    private int funcionarioId;
    private int quantidade;

    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }
    public int getPontoId() { return pontoId; }
    public void setPontoId(int pontoId) { this.pontoId = pontoId; }
    public int getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(int funcionarioId) { this.funcionarioId = funcionarioId; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
}
