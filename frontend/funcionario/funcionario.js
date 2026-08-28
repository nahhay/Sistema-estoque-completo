const API_URL = "http://localhost:8080/api";

function mostrarAlert(mensagem) {
  const alert = document.getElementById("customAlert");
  const msg = document.getElementById("alertMessage");
  msg.textContent = mensagem;
  alert.classList.add("show");
}

function fecharAlert() {
  document.getElementById("customAlert").classList.remove("show");
}

async function handleResponse(response) {
  const text = await response.text();
  if (!text) return "";
  try {
    const data = JSON.parse(text);
    if (data.erro) return data.erro;
    if (data.mensagem) return data.mensagem;
    return data;
  } catch {
    return text;
  }
}

function extrairDados(data) {
  if (data && data.dados) return data.dados;
  return data;
}

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", function () {
    document
      .querySelectorAll(".tab")
      .forEach((t) => t.classList.remove("active"));
    document
      .querySelectorAll(".tab-content")
      .forEach((c) => c.classList.remove("active"));

    this.classList.add("active");
    const target = document.getElementById("tab-" + this.dataset.tab);
    if (target) target.classList.add("active");

    const tabName = this.dataset.tab;
    if (tabName === "reposicoes") listarReposicoes();
    else if (tabName === "conferencias") listarConferencias();
  });
});

async function registrarEntrada() {
  const produtoId = document.getElementById("estoqueProdutoId").value;
  const pontoId = document.getElementById("estoquePontoId").value;
  const funcionarioId = document.getElementById("estoqueFuncionarioId").value;
  const quantidade = document.getElementById("estoqueQuantidade").value;

  if (!produtoId || !pontoId || !funcionarioId || !quantidade) {
    return mostrarAlert("Preencha todos os campos!");
  }

  try {
    const res = await fetch(`${API_URL}/estoque/entrada`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        produtoId: parseInt(produtoId),
        pontoId: parseInt(pontoId),
        funcionarioId: parseInt(funcionarioId),
        quantidade: parseInt(quantidade),
      }),
    });
    const msg = await handleResponse(res);
    mostrarAlert(msg);
    limparCamposEstoque();
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

async function registrarSaida() {
  const produtoId = document.getElementById("estoqueProdutoId").value;
  const pontoId = document.getElementById("estoquePontoId").value;
  const funcionarioId = document.getElementById("estoqueFuncionarioId").value;
  const quantidade = document.getElementById("estoqueQuantidade").value;

  if (!produtoId || !pontoId || !funcionarioId || !quantidade) {
    return mostrarAlert("Preencha todos os campos!");
  }

  try {
    const res = await fetch(`${API_URL}/estoque/saida`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        produtoId: parseInt(produtoId),
        pontoId: parseInt(pontoId),
        funcionarioId: parseInt(funcionarioId),
        quantidade: parseInt(quantidade),
      }),
    });
    const msg = await handleResponse(res);
    mostrarAlert(msg);
    limparCamposEstoque();
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

async function consultarEstoque() {
  const produtoId = document.getElementById("estoqueProdutoId").value;
  const pontoId = document.getElementById("estoquePontoId").value;

  if (!produtoId || !pontoId) {
    return mostrarAlert("Preencha ID do produto e do ponto!");
  }

  try {
    const res = await fetch(
      `${API_URL}/estoque?produtoId=${produtoId}&pontoId=${pontoId}`,
    );
    const text = await res.text();
    const data = JSON.parse(text);
    const quantidade = data.dados !== undefined ? data.dados : data;
    document.getElementById("resultadoEstoque").innerHTML =
      `<div class="saldo">Quantidade em estoque: <strong>${quantidade}</strong></div>`;
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

function limparCamposEstoque() {
  document.getElementById("estoqueProdutoId").value = "";
  document.getElementById("estoquePontoId").value = "";
  document.getElementById("estoqueFuncionarioId").value = "";
  document.getElementById("estoqueQuantidade").value = "";
}

async function solicitarReposicao() {
  const produtoId = document.getElementById("reposicaoProdutoId").value;
  const pontoId = document.getElementById("reposicaoPontoId").value;
  const funcionarioId = document.getElementById("reposicaoFuncionarioId").value;
  const quantidade = document.getElementById("reposicaoQuantidade").value;

  if (!produtoId || !pontoId || !funcionarioId || !quantidade) {
    return mostrarAlert("Preencha todos os campos!");
  }

  try {
    const res = await fetch(`${API_URL}/reposicoes/solicitar`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        produtoId: parseInt(produtoId),
        pontoId: parseInt(pontoId),
        funcionarioId: parseInt(funcionarioId),
        quantidade: parseInt(quantidade),
      }),
    });
    const msg = await handleResponse(res);
    mostrarAlert(msg);
    document.getElementById("reposicaoProdutoId").value = "";
    document.getElementById("reposicaoPontoId").value = "";
    document.getElementById("reposicaoFuncionarioId").value = "";
    document.getElementById("reposicaoQuantidade").value = "";
    listarReposicoes();
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

async function separarReposicao() {
  const id = document.getElementById("reposicaoId").value;
  if (!id) return mostrarAlert("Digite o ID da reposição!");

  try {
    const res = await fetch(`${API_URL}/reposicoes/${id}/separar`, {
      method: "PUT",
    });
    const msg = await handleResponse(res);
    mostrarAlert(msg);
    document.getElementById("reposicaoId").value = "";
    listarReposicoes();
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

async function conferirReposicao() {
  const id = document.getElementById("reposicaoId").value;
  if (!id) return mostrarAlert("Digite o ID da reposição!");

  try {
    const res = await fetch(`${API_URL}/reposicoes/${id}/conferir`, {
      method: "PUT",
    });
    const msg = await handleResponse(res);
    mostrarAlert(msg);
    document.getElementById("reposicaoId").value = "";
    listarReposicoes();
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

async function entregarReposicao() {
  const id = document.getElementById("reposicaoId").value;
  if (!id) return mostrarAlert("Digite o ID da reposição!");

  try {
    const res = await fetch(`${API_URL}/reposicoes/${id}/entregar`, {
      method: "PUT",
    });
    const msg = await handleResponse(res);
    mostrarAlert(msg);
    document.getElementById("reposicaoId").value = "";
    listarReposicoes();
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

async function cancelarReposicao() {
  const id = document.getElementById("reposicaoId").value;
  if (!id) return mostrarAlert("Digite o ID da reposição!");

  try {
    const res = await fetch(`${API_URL}/reposicoes/${id}/cancelar`, {
      method: "PUT",
    });
    const msg = await handleResponse(res);
    mostrarAlert(msg);
    document.getElementById("reposicaoId").value = "";
    listarReposicoes();
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

async function listarReposicoes() {
  try {
    const res = await fetch(`${API_URL}/reposicoes`);
    const data = await res.json();
    const reposicoes = extrairDados(data);
    const tbody = document.getElementById("listaReposicoes");

    if (!reposicoes || reposicoes.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="6" class="empty-msg">Nenhuma reposição</td></tr>';
      return;
    }

    tbody.innerHTML = reposicoes
      .map(
        (r) => `
            <tr>
                <td><strong>#${r.id}</strong></td>
                <td>${r.produtoNome}</td>
                <td>${r.pontoNome}</td>
                <td>${r.quantidade}</td>
                <td><span class="status-badge ${r.status.toLowerCase()}">${r.status}</span></td>
                <td>${new Date(r.dataHora).toLocaleDateString("pt-BR")}</td>
            </tr>
        `,
      )
      .join("");
  } catch (error) {
    console.error("Erro ao listar reposições:", error);
  }
}

async function realizarConferencia() {
  const produtoId = document.getElementById("conferenciaProdutoId").value;
  const pontoId = document.getElementById("conferenciaPontoId").value;
  const funcionarioId = document.getElementById(
    "conferenciaFuncionarioId",
  ).value;
  const quantidadeFisica = document.getElementById(
    "conferenciaQuantidadeFisica",
  ).value;

  if (!produtoId || !pontoId || !funcionarioId || !quantidadeFisica) {
    return mostrarAlert("Preencha todos os campos!");
  }

  try {
    const res = await fetch(`${API_URL}/conferencias`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        produtoId: parseInt(produtoId),
        pontoId: parseInt(pontoId),
        funcionarioId: parseInt(funcionarioId),
        quantidadeFisica: parseInt(quantidadeFisica),
      }),
    });
    const msg = await handleResponse(res);
    mostrarAlert(msg);
    document.getElementById("conferenciaProdutoId").value = "";
    document.getElementById("conferenciaPontoId").value = "";
    document.getElementById("conferenciaFuncionarioId").value = "";
    document.getElementById("conferenciaQuantidadeFisica").value = "";
    listarConferencias();
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

async function listarConferencias() {
  try {
    const res = await fetch(`${API_URL}/conferencias`);
    const data = await res.json();
    const conferencias = extrairDados(data);
    const tbody = document.getElementById("listaConferencias");

    if (!conferencias || conferencias.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="6" class="empty-msg">Nenhuma conferência</td></tr>';
      return;
    }

    tbody.innerHTML = conferencias
      .map((c) => {
        let divClass = "divergencia-zero";
        let divText = c.divergencia;
        if (c.divergencia < 0) {
          divClass = "divergencia-negativa";
          divText = c.divergencia;
        } else if (c.divergencia > 0) {
          divClass = "divergencia-positiva";
          divText = "+" + c.divergencia;
        }

        return `
                <tr>
                    <td><strong>#${c.id}</strong></td>
                    <td>${c.produtoNome}</td>
                    <td>${c.pontoNome}</td>
                    <td>${c.estoqueEsperado}</td>
                    <td>${c.estoqueFisico}</td>
                    <td class="${divClass}">${divText}</td>
                </tr>
            `;
      })
      .join("");
  } catch (error) {
    console.error("Erro ao listar conferências:", error);
  }
}

async function verHistorico() {
  const produtoId = document.getElementById("historicoProdutoId").value;
  const pontoId = document.getElementById("historicoPontoId").value;

  if (!produtoId || !pontoId) {
    return mostrarAlert("Preencha ID do produto e do ponto!");
  }

  try {
    const res = await fetch(
      `${API_URL}/historico?produtoId=${produtoId}&pontoId=${pontoId}`,
    );
    const data = await res.json();

    const div = document.getElementById("resultadoHistorico");

    if (data.erro) {
      div.innerHTML = `<div style="color: var(--danger);">${data.erro}</div>`;
      return;
    }

    let html = `<div class="saldo">Saldo atual: <strong>${data.dados.saldo}</strong></div>`;

    if (data.dados.movimentacoes && data.dados.movimentacoes.length > 0) {
      html += '<div style="margin-top: 12px;">';
      data.dados.movimentacoes.forEach((m) => {
        const cor = m.tipo === "ENTRADA" ? "#22c55e" : "#ef4444";
        html += `
                    <div class="mov-item">
                        <strong>${new Date(m.dataHora).toLocaleString("pt-BR")}</strong>
                        <span style="color: ${cor}; font-weight:600;">${m.tipo}</span>
                        ${m.quantidade} - ${m.funcionarioNome}
                    </div>
                `;
      });
      html += "</div>";
    } else {
      html +=
        '<p style="color: var(--gray-400); margin-top: 12px;">Nenhuma movimentação encontrada</p>';
    }

    div.innerHTML = html;
  } catch (error) {
    mostrarAlert("Erro: " + error.message);
  }
}

document.addEventListener("DOMContentLoaded", function () {
  listarReposicoes();
  listarConferencias();
});
