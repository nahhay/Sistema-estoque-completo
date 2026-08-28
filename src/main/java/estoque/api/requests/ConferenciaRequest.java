package estoque.api.requests;

public class ConferenciaRequest {
    private int produtoId;
    private int pontoId;
    private int funcionarioId;
    private int quantidadeFisica;

    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }
    public int getPontoId() { return pontoId; }
    public void setPontoId(int pontoId) { this.pontoId = pontoId; }
    public int getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(int funcionarioId) { this.funcionarioId = funcionarioId; }
    public int getQuantidadeFisica() { return quantidadeFisica; }
    public void setQuantidadeFisica(int quantidadeFisica) { this.quantidadeFisica = quantidadeFisica; }
}
