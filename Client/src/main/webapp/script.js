let websocket = null;
let reconectarTimeout = null;

function conectarWS() {
  try {
    websocket = new WebSocket("ws://" + location.host + "/app/ws/updates");

    websocket.onmessage = function (event) {
      console.log("Atualização recebida:", event.data);
      mostrarToast(
        "info",
        "Novo arquivo disponível",
        "A lista foi atualizada automaticamente"
      );
      carregarLista();
    };

    websocket.onclose = function () {
      console.log("WebSocket desconectado");

      clearTimeout(reconectarTimeout);
      reconectarTimeout = setTimeout(function () {
        console.log("Tentando reconectar...");
        conectarWS();
      }, 3000);
    };

    websocket.onerror = function (error) {
      console.error("Erro no WebSocket:", error);
    };
  } catch (e) {
    console.error("Erro ao conectar WebSocket:", e);
  }
}

function enviarArquivo() {
  const fileInput = document.getElementById("fileInput");
  const uploadBtn = document.getElementById("uploadBtn");
  const uploadBtnText = document.getElementById("uploadBtnText");
  const progressContainer = document.getElementById("progressContainer");
  const progressFill = document.getElementById("progressFill");

  if (!fileInput.files || fileInput.files.length === 0) {
    mostrarToast("error", "Erro", "Por favor, selecione um arquivo");
    return;
  }

  const file = fileInput.files[0];
  const formData = new FormData();
  formData.append("file", file);

  uploadBtn.disabled = true;
  uploadBtnText.textContent = "Enviando...";
  progressContainer.classList.add("active");

  let progresso = 0;
  const intervalo = setInterval(function () {
    progresso += 10;
    if (progresso >= 90) {
      clearInterval(intervalo);
    }
    progressFill.style.width = progresso + "%";
    progressFill.textContent = progresso + "%";
  }, 100);

  fetch("/app/api/upload", {
    method: "POST",
    body: formData,
  })
    .then((response) => response.json())
    .then((data) => {
      clearInterval(intervalo);
      progressFill.style.width = "100%";
      progressFill.textContent = "100%";

      setTimeout(function () {
        if (data.status === "success") {
          mostrarToast("success", "Sucesso!", data.message);

          fileInput.value = "";
          const label = document.getElementById("fileLabel");
          label.classList.remove("has-file");
          label.innerHTML =
            '<div style="font-weight: 600; margin-bottom: 5px;">Clique para selecionar um arquivo</div>' +
            '<div style="font-size: 0.85rem; color: #777;">Formatos aceitos: .txt, .pdf, .jpg, .png, .docx</div>';

          setTimeout(carregarLista, 500);
        } else {
          mostrarToast(
            "error",
            "Erro no upload",
            data.message || "Erro desconhecido"
          );
        }

        uploadBtn.disabled = true;
        uploadBtnText.textContent = "Enviar Arquivo";
        progressContainer.classList.remove("active");
        progressFill.style.width = "0%";
        progressFill.textContent = "0%";
      }, 500);
    })
    .catch((error) => {
      clearInterval(intervalo);
      console.error("Erro:", error);
      mostrarToast(
        "error",
        "Erro de conexão",
        "Não foi possível enviar o arquivo"
      );

      uploadBtn.disabled = false;
      uploadBtnText.textContent = "Enviar Arquivo";
      progressContainer.classList.remove("active");
      progressFill.style.width = "0%";
      progressFill.textContent = "0%";
    });
}

function carregarLista() {
  const filesList = document.getElementById("filesList");

  fetch("/app/api/list")
    .then((response) => response.json())
    .then((data) => {
      if (data.status === "success") {
        if (data.arquivos && data.arquivos.length > 0) {
          let html = "";
          data.arquivos.forEach((arquivo) => {
            html +=
              '<div class="file-item">' +
              '<div class="file-info">' +
              '<div class="file-name">' +
              " " +
              arquivo.nome +
              "</div>" +
              '<div class="file-meta">' +
              "Tamanho: " +
              arquivo.tamanhoFormatado +
              " - " +
              "Modificado: " +
              arquivo.dataModificacao +
              "</div>" +
              "</div>" +
              '<button class="btn btn-download" onclick="baixarArquivo(\'' +
              arquivo.nome +
              "')\">" + "Download" +
              "</button>" +
              "</div>";
          });
          filesList.innerHTML = html;
        } else {
          filesList.innerHTML =
            '<div class="empty-state">' +
            "<p>Nenhum arquivo disponível no servidor</p>" +
            '<p style="font-size: 0.9rem; margin-top: 10px; color: #999;">' +
            "Faça upload de um arquivo para começar" +
            "</p>" +
            "</div>";
        }
      } else {
        mostrarToast(
          "error",
          "Erro ao listar",
          data.message || "Erro desconhecido"
        );
        filesList.innerHTML =
          '<div class="empty-state">' +
          "<p>Erro ao carregar a lista de arquivos</p>" +
          "</div>";
      }
    })
    .catch((error) => {
      console.error("Erro ao carregar lista:", error);
      mostrarToast(
        "error",
        "Erro de conexão",
        "Não foi possível carregar a lista de arquivos"
      );
      filesList.innerHTML =
        '<div class="empty-state">' +
        "<p>Erro de conexão com o servidor</p>" +
        '<p style="font-size: 0.9rem; margin-top: 10px; color: #999;">' +
        "Verifique se o servidor está em execução" +
        "</p>" +
        "</div>";
    });
}

function baixarArquivo(nomeArquivo) {
  mostrarToast("info", "Download iniciado", "Baixando " + nomeArquivo + "...");

  fetch("/app/api/download/" + encodeURIComponent(nomeArquivo))
    .then((response) => {
      if (!response.ok) {
        throw new Error("Arquivo não encontrado");
      }
      return response.blob();
    })
    .then((blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = nomeArquivo;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    })
    .catch((error) => {
      console.error("Erro ao baixar:", error);
      mostrarToast(
        "error",
        "Erro no download",
        "Não foi possível baixar o arquivo"
      );
    });
}

function mostrarToast(tipo, titulo, mensagem) {
  const container = document.getElementById("toastContainer");

  const toast = document.createElement("div");
  toast.className = "toast " + tipo;
  toast.innerHTML =
    '<div class="toast-icon">' +
    "</div>" +
    '<div class="toast-content">' +
    '<div class="toast-title">' +
    titulo +
    "</div>" +
    '<div class="toast-message">' +
    mensagem +
    "</div>" +
    "</div>";

  container.appendChild(toast);

  setTimeout(function () {
    toast.style.animation = "slideIn 0.3s ease reverse";
    setTimeout(function () {
      container.removeChild(toast);
    }, 300);
  }, 5000);
}

function formatarTamanho(bytes) {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + " KB";
  return (bytes / (1024 * 1024)).toFixed(2) + " MB";
}

document.addEventListener("DOMContentLoaded", function () {
  conectarWS();
  carregarLista();

  document.getElementById("fileInput").addEventListener("change", function (e) {
    const label = document.getElementById("fileLabel");
    const btn = document.getElementById("uploadBtn");

    if (this.files && this.files.length > 0) {
      const file = this.files[0];
      label.classList.add("has-file");
      label.innerHTML =
        '<div style="font-weight: 600; margin-bottom: 5px;">' +
        file.name +
        "</div>" +
        '<div style="font-size: 0.85rem; color: #999;">Tamanho: ' +
        formatarTamanho(file.size) +
        "</div>";
      btn.disabled = false;
    } else {
      label.classList.remove("has-file");
      label.innerHTML =
        '<div style="font-weight: 600; margin-bottom: 5px;">Clique para selecionar um arquivo</div>' +
        '<div style="font-size: 0.85rem; color: #777;">Formatos aceitos: .txt, .pdf, .jpg, .png, .docx</div>';
      btn.disabled = true;
    }
  });
});
