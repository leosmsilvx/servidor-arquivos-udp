<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mini Servidor de Arquivos UDP</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <script src="script.js"></script>

    <div class="toast-container" id="toastContainer"></div>

    <div class="container">
        <div class="header">
            <h1>Mini Servidor de Arquivos UDP</h1>
        </div>

        <div class="card">
            <div class="upload-section">
                <h2>Upload do arquivo</h2>
                
                <div class="file-input-wrapper">
                    <input type="file" id="fileInput" accept=".txt,.pdf,.jpg,.jpeg,.png,.docx,.doc,.xlsx,.zip">
                    <label for="fileInput" class="file-input-label" id="fileLabel">
                        <div style="font-weight: 600; margin-bottom: 5px;">Clique para selecionar um arquivo</div>
                        <div style="font-size: 0.85rem; color: #666;">Formatos aceitos: .txt, .pdf, .jpg, .png, .docx</div>
                    </label>
                </div>

                <button class="btn btn-primary" onclick="enviarArquivo()" id="uploadBtn" disabled>
                    <span id="uploadBtnText">Enviar Arquivo</span>
                </button>

                <div class="progress-container" id="progressContainer">
                    <div class="progress-bar">
                        <div class="progress-fill" id="progressFill">0%</div>
                    </div>
                </div>
            </div>

            <div class="files-section">
                <h2>
                    Arquivos Disponíveis
                </h2>
                <div class="files-list" id="filesList">
                    <div class="empty-state">
                        <p>Carregando lista de arquivos...</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
