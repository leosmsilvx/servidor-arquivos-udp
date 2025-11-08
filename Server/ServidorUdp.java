import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServidorUdp {
    private static final int PORTA = 9998;
    private static final int TAMANHO_BUFFER = 65000;
    private static final int TAMANHO_FRAGMENTO = 60000;
    private static final String PASTA_ARQUIVOS = "Server/arquivos_servidor";
    private static final String ARQUIVO_LOG = "Server/log_servidor.txt";
    
    private static Map<String, List<byte[]>> uploadsParciais = new HashMap<>();
    private static List<ClienteInfo> clientesConectados = new ArrayList<>();
    
    static class ClienteInfo {
        String ip;
        int porta;
        
        ClienteInfo(String ip, int porta) {
            this.ip = ip;
            this.porta = porta;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClienteInfo that = (ClienteInfo) o;
            return porta == that.porta && ip.equals(that.ip);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(ip, porta);
        }
    }
    
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(PASTA_ARQUIVOS));
        
        DatagramSocket socket = new DatagramSocket(PORTA);
        byte[] buffer = new byte[TAMANHO_BUFFER];
        
        log("INFO", "0.0.0.0", "Servidor UDP iniciado na porta " + PORTA);
        System.out.println("Servidor UDP aguardando conexões na porta " + PORTA + "...");
        System.out.println("Pasta de arquivos: " + new File(PASTA_ARQUIVOS).getAbsolutePath());

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String clienteIP = packet.getAddress().getHostAddress();
                int clientePorta = packet.getPort();
                
                String mensagem = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                String[] partes = mensagem.split("\\|", 2);
                String comando = partes[0];
                
                registrarCliente(clienteIP, clientePorta);
                
                System.out.println("Comando recebido: " + comando + " de " + clienteIP);
                
                switch (comando) {
                    case "UPLOAD":
                        processarUpload(socket, packet, partes, clienteIP, clientePorta);
                        break;
                        
                    case "UPLOAD_FRAGMENTO":
                        processarFragmento(socket, packet, partes, clienteIP, clientePorta);
                        break;
                        
                    case "LIST":
                        processarList(socket, packet.getAddress(), clientePorta, clienteIP);
                        break;
                        
                    case "DOWNLOAD":
                        processarDownload(socket, packet, partes, clienteIP, clientePorta);
                        break;
                        
                    default:
                        log("ERRO", clienteIP, "Comando desconhecido: " + comando);
                        enviarResposta(socket, "ERRO|Comando desconhecido", 
                                     packet.getAddress(), clientePorta);
                }
                
            } catch (Exception e) {
                log("ERRO", "SISTEMA", "Erro no servidor: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private static void registrarCliente(String clienteIP, int clientePorta) {
        ClienteInfo novoCliente = new ClienteInfo(clienteIP, clientePorta);
        
        boolean jaExiste = false;
        for (ClienteInfo cliente : clientesConectados) {
            if (cliente.equals(novoCliente)) {
                jaExiste = true;
                break;
            }
        }
        
        if (!jaExiste) {
            clientesConectados.add(novoCliente);
            System.out.println("Novo cliente registrado: " + clienteIP + ":" + clientePorta + 
                             " (Total: " + clientesConectados.size() + ")");
            log("INFO", clienteIP, "Cliente registrado automaticamente");
        }
    }
    
    private static void processarUpload(DatagramSocket socket, DatagramPacket packet, 
                                       String[] partes, String clienteIP, int clientePorta) throws Exception {
        if (partes.length < 2) {
            enviarResposta(socket, "ERRO|Formato inválido", packet.getAddress(), clientePorta);
            return;
        }
        
        String resto = partes[1];
        int indicePipe = resto.indexOf("|");
        String nomeArquivo;
        int inicioBytes;
        
        if (indicePipe > 0) {
            nomeArquivo = resto.substring(0, indicePipe);
            String cabecalho = "UPLOAD|" + nomeArquivo + "|";
            inicioBytes = cabecalho.length();
        } else {
            nomeArquivo = resto.trim();
            inicioBytes = packet.getLength();
        }
        
        byte[] conteudo = Arrays.copyOfRange(packet.getData(), inicioBytes, packet.getLength());
        
        salvarArquivo(nomeArquivo, conteudo, clienteIP);
        enviarResposta(socket, "OK|Upload concluído", packet.getAddress(), clientePorta);
        notificarClientes();
    }
    
    private static void processarFragmento(DatagramSocket socket, DatagramPacket packet, 
                                          String[] partes, String clienteIP, int clientePorta) throws Exception {
        String[] info = partes[1].split("\\|", 4);
        String nomeArquivo = info[0];
        int fragmentoAtual = Integer.parseInt(info[1]);
        int totalFragmentos = Integer.parseInt(info[2]);
        
        int inicioBytes = partes[0].length() + 1 + info[0].length() + 1 + 
                         info[1].length() + 1 + info[2].length() + 1;
        byte[] fragmentoBytes = Arrays.copyOfRange(packet.getData(), inicioBytes, packet.getLength());
        
        String chaveUpload = clienteIP + ":" + nomeArquivo;
        
        if (fragmentoAtual == 0) {
            uploadsParciais.put(chaveUpload, new ArrayList<>());
        }
        
        List<byte[]> fragmentos = uploadsParciais.get(chaveUpload);
        if (fragmentos != null) {
            fragmentos.add(fragmentoBytes);
            
            log("INFO", clienteIP, "Fragmento " + (fragmentoAtual + 1) + "/" + 
                totalFragmentos + " recebido para " + nomeArquivo);
            
            if (fragmentos.size() == totalFragmentos) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (byte[] frag : fragmentos) {
                    baos.write(frag);
                }
                
                salvarArquivo(nomeArquivo, baos.toByteArray(), clienteIP);
                uploadsParciais.remove(chaveUpload);
                
                enviarResposta(socket, "OK|Upload concluído - " + nomeArquivo, 
                             packet.getAddress(), clientePorta);
                notificarClientes();
            } else {
                enviarResposta(socket, "OK|Fragmento " + (fragmentoAtual + 1) + 
                             "/" + totalFragmentos + " recebido", 
                             packet.getAddress(), clientePorta);
            }
        }
    }
    
    private static void processarList(DatagramSocket socket, InetAddress endereco, 
                                      int porta, String clienteIP) throws Exception {
        log("INFO", clienteIP, "Solicitação LIST");
        
        File pasta = new File(PASTA_ARQUIVOS);
        File[] arquivos = pasta.listFiles();
        
        StringBuilder lista = new StringBuilder("LIST_RESPONSE|");
        if (arquivos != null && arquivos.length > 0) {
            for (File arquivo : arquivos) {
                if (arquivo.isFile()) {
                    lista.append(arquivo.getName())
                         .append(";")
                         .append(arquivo.length())
                         .append(";")
                         .append(arquivo.lastModified())
                         .append("|");
                }
            }
        } else {
            lista.append("VAZIO");
        }
        
        enviarResposta(socket, lista.toString(), endereco, porta);
    }
    
    private static void processarDownload(DatagramSocket socket, DatagramPacket packet, 
                                         String[] partes, String clienteIP, int clientePorta) throws Exception {
        if (partes.length < 2) {
            enviarResposta(socket, "ERRO|Formato inválido", packet.getAddress(), clientePorta);
            return;
        }
        
        String nomeArquivo = partes[1];
        File arquivo = new File(PASTA_ARQUIVOS, nomeArquivo);
        
        if (!arquivo.exists() || !arquivo.isFile()) {
            log("ERRO", clienteIP, "Download falhou - arquivo não encontrado: " + nomeArquivo);
            enviarResposta(socket, "ERRO|Arquivo não encontrado", packet.getAddress(), clientePorta);
            return;
        }
        
        log("INFO", clienteIP, "Download iniciado: " + nomeArquivo + " (" + arquivo.length() + " bytes)");
        
        byte[] conteudo = Files.readAllBytes(arquivo.toPath());
        
        if (conteudo.length > TAMANHO_FRAGMENTO) {
            int totalFragmentos = (int) Math.ceil((double) conteudo.length / TAMANHO_FRAGMENTO);
            
            String info = "DOWNLOAD_FRAGMENTADO|" + nomeArquivo + "|" + totalFragmentos + "|" + conteudo.length;
            enviarResposta(socket, info, packet.getAddress(), clientePorta);
            
            Thread.sleep(100);
            
            for (int i = 0; i < totalFragmentos; i++) {
                int inicio = i * TAMANHO_FRAGMENTO;
                int fim = Math.min(inicio + TAMANHO_FRAGMENTO, conteudo.length);
                byte[] fragmento = Arrays.copyOfRange(conteudo, inicio, fim);
                
                String cabecalho = "FRAGMENTO|" + i + "|";
                byte[] cabecalhoBytes = cabecalho.getBytes("UTF-8");
                byte[] mensagemCompleta = new byte[cabecalhoBytes.length + fragmento.length];
                System.arraycopy(cabecalhoBytes, 0, mensagemCompleta, 0, cabecalhoBytes.length);
                System.arraycopy(fragmento, 0, mensagemCompleta, cabecalhoBytes.length, fragmento.length);
                
                DatagramPacket pacoteFragmento = new DatagramPacket(mensagemCompleta, 
                    mensagemCompleta.length, packet.getAddress(), clientePorta);
                socket.send(pacoteFragmento);
                
                Thread.sleep(10);
            }
            
            log("INFO", clienteIP, "Download concluído: " + nomeArquivo + 
                " (" + totalFragmentos + " fragmentos)");
        } else {
            String resposta = "DOWNLOAD_OK|" + nomeArquivo + "|";
            byte[] respostaBytes = resposta.getBytes("UTF-8");
            byte[] mensagemCompleta = new byte[respostaBytes.length + conteudo.length];
            System.arraycopy(respostaBytes, 0, mensagemCompleta, 0, respostaBytes.length);
            System.arraycopy(conteudo, 0, mensagemCompleta, respostaBytes.length, conteudo.length);
            
            DatagramPacket pacoteResposta = new DatagramPacket(mensagemCompleta, 
                mensagemCompleta.length, packet.getAddress(), clientePorta);
            socket.send(pacoteResposta);
            
            log("INFO", clienteIP, "Download concluído: " + nomeArquivo);
        }
    }
    
    private static void salvarArquivo(String nomeArquivo, byte[] conteudo, String clienteIP) throws Exception {
        String nomeSanitizado = new File(nomeArquivo).getName();
        Path caminho = Paths.get(PASTA_ARQUIVOS, nomeSanitizado);
        
        Files.write(caminho, conteudo);
        
        log("UPLOAD", clienteIP, "Arquivo salvo: " + nomeSanitizado + " (" + conteudo.length + " bytes)");
        System.out.println("Arquivo salvo: " + caminho.toAbsolutePath());
    }
    
    private static void enviarResposta(DatagramSocket socket, String mensagem, 
                                      InetAddress endereco, int porta) throws Exception {
        byte[] resposta = mensagem.getBytes("UTF-8");
        DatagramPacket pacote = new DatagramPacket(resposta, resposta.length, endereco, porta);
        socket.send(pacote);
    }
    
    private static void notificarClientes() {
        if (clientesConectados.isEmpty()) {
            System.out.println("Aviso: Nenhum cliente conectado para notificar");
            return;
        }
        
        System.out.println("Notificando " + clientesConectados.size() + " cliente(s)...");
        
        for (ClienteInfo cliente : clientesConectados) {
            try {
                String url = "http://" + cliente.ip + ":8080/app/api/notify";
                
                HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                );
                
                System.out.println("Cliente notificado: " + cliente.ip + ":8080");
                
            } catch (Exception e) {
                System.out.println("Aviso: Não foi possível notificar " + cliente.ip + 
                                 ": " + e.getMessage());
            }
        }
    }
    
    private static void log(String tipo, String clienteIP, String mensagem) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String linha = String.format("[%s] [%s] [%s] %s%n", 
                sdf.format(new Date()), tipo, clienteIP, mensagem);
            
            Files.write(Paths.get(ARQUIVO_LOG), linha.getBytes("UTF-8"), 
                       StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("Erro ao gravar log: " + e.getMessage());
        }
    }
}
