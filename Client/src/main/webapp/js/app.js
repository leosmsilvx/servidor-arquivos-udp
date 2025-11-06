function enviar() {
    let file = document.getElementById("file").files[0];
    let form = new FormData();
    form.append("file", file);

    fetch("/app/api/upload", {
        method: "POST",
        body: form
    }).then(r => r.text())
      .then(t => {
          document.getElementById("status").innerText = t;
      });
}

function conectarWS() {
    let ws = new WebSocket("ws://" + location.host + "/app/ws/updates");

    ws.onmessage = (event) => {
        document.getElementById("listaArquivos").innerText =
          "Novo arquivo disponível (" + new Date().toLocaleTimeString() + ")";
    };
}
