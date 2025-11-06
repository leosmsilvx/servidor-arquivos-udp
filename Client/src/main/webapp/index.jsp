<html>
<head>
    <script src="js/app.js"></script>
</head>
<body>
<h1>Upload para Servidor UDP</h1>

<form id="formUpload" enctype="multipart/form-data">
    <input type="file" id="file">
    <button type="button" onclick="enviar()">Enviar</button>
</form>

<div id="status"></div>
<div id="listaArquivos"></div>

<script>
conectarWS();
</script>
</body>
</html>
