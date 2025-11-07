package br.com.web.udp;

import java.io.*;
import java.net.*;
import java.util.*;

public class UdpClient {
    private static final String SERVIDOR_IP = "localhost";
    private static final int SERVIDOR_PORTA = 9998;
    private static final int TIMEOUT = 5000;
    private static final int TAMANHO_FRAGMENTO = 60000;
    
    public String enviarArquivo(String nomeArquivo, byte[] dados) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            InetAddress addr = InetAddress.getByName(SERVIDOR_IP);
            
            if (dados.length > TAMANHO_FRAGMENTO) {
                return enviarFragmentado(socket, addr, nomeArquivo, dados);
            } else {
                return enviarSimples(socket, addr, nomeArquivo, dados);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERRO|" + e.getMessage();
        }
    }
    
    private String enviarSimples(DatagramSocket socket, InetAddress addr, 
                                String nomeArquivo, byte[] dados) throws Exception {
        String comando = "UPLOAD|" + nomeArquivo + "|";
        byte[] mensagem = new byte[comando.length() + dados.length];
        System.arraycopy(comando.getBytes(), 0, mensagem, 0, comando.length());
        System.arraycopy(dados, 0, mensagem, comando.length(), dados.length);
        
        DatagramPacket packet = new DatagramPacket(mensagem, mensagem.length, addr, SERVIDOR_PORTA);
        socket.send(packet);
        
        byte[] resp = new byte[1024];
        DatagramPacket respPacket = new DatagramPacket(resp, resp.length);
        socket.receive(respPacket);
        
        return new String(respPacket.getData(), 0, respPacket.getLength());
    }
    
    private String enviarFragmentado(DatagramSocket socket, InetAddress addr, 
                                    String nomeArquivo, byte[] dados) throws Exception {
        int totalFragmentos = (int) Math.ceil((double) dados.length / TAMANHO_FRAGMENTO);
        System.out.println("Enviando arquivo em " + totalFragmentos + " fragmentos...");
        
        for (int i = 0; i < totalFragmentos; i++) {
            int inicio = i * TAMANHO_FRAGMENTO;
            int fim = Math.min(inicio + TAMANHO_FRAGMENTO, dados.length);
            byte[] fragmento = Arrays.copyOfRange(dados, inicio, fim);
            
            String comando = "UPLOAD_FRAGMENTO|" + nomeArquivo + "|" + i + "|" + totalFragmentos + "|";
            byte[] mensagem = new byte[comando.length() + fragmento.length];
            System.arraycopy(comando.getBytes(), 0, mensagem, 0, comando.length());
            System.arraycopy(fragmento, 0, mensagem, comando.length(), fragmento.length);
            
            DatagramPacket packet = new DatagramPacket(mensagem, mensagem.length, addr, SERVIDOR_PORTA);
            socket.send(packet);
            
            byte[] resp = new byte[1024];
            DatagramPacket respPacket = new DatagramPacket(resp, resp.length);
            socket.receive(respPacket);
            
            String resposta = new String(respPacket.getData(), 0, respPacket.getLength());
            System.out.println("Fragmento " + (i + 1) + "/" + totalFragmentos + ": " + resposta);
        }
        
        return "OK|Upload fragmentado concluído";
    }
    
    public List<ArquivoInfo> listarArquivos() {
        List<ArquivoInfo> arquivos = new ArrayList<>();
        
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            InetAddress addr = InetAddress.getByName(SERVIDOR_IP);
            
            String comando = "LIST";
            DatagramPacket packet = new DatagramPacket(comando.getBytes(), 
                                                      comando.length(), addr, SERVIDOR_PORTA);
            socket.send(packet);
            
            byte[] resp = new byte[65000];
            DatagramPacket respPacket = new DatagramPacket(resp, resp.length);
            socket.receive(respPacket);
            
            String resposta = new String(respPacket.getData(), 0, respPacket.getLength());
            
            if (resposta.startsWith("LIST_RESPONSE|")) {
                String dados = resposta.substring("LIST_RESPONSE|".length());
                
                if (!dados.equals("VAZIO")) {
                    String[] listaArquivos = dados.split("\\|");
                    for (String info : listaArquivos) {
                        if (!info.isEmpty()) {
                            String[] partes = info.split(";");
                            if (partes.length >= 3) {
                                arquivos.add(new ArquivoInfo(
                                    partes[0], 
                                    Long.parseLong(partes[1]), 
                                    Long.parseLong(partes[2])
                                ));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return arquivos;
    }
    
    public byte[] baixarArquivo(String nomeArquivo) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            InetAddress addr = InetAddress.getByName(SERVIDOR_IP);
            
            String comando = "DOWNLOAD|" + nomeArquivo;
            DatagramPacket packet = new DatagramPacket(comando.getBytes(), 
                                                      comando.length(), addr, SERVIDOR_PORTA);
            socket.send(packet);
            
            byte[] resp = new byte[65000];
            DatagramPacket respPacket = new DatagramPacket(resp, resp.length);
            socket.receive(respPacket);
            
            String resposta = new String(respPacket.getData(), 0, respPacket.getLength());
            
            if (resposta.startsWith("ERRO|")) {
                System.out.println("Erro: " + resposta);
                return null;
            } else if (resposta.startsWith("DOWNLOAD_OK|")) {
                String[] partes = resposta.split("\\|", 3);
                int inicioDados = partes[0].length() + partes[1].length() + 2;
                return Arrays.copyOfRange(respPacket.getData(), inicioDados, respPacket.getLength());
                
            } else if (resposta.startsWith("DOWNLOAD_FRAGMENTADO|")) {
                String[] info = resposta.split("\\|");
                int totalFragmentos = Integer.parseInt(info[2]);
                int tamanhoTotal = Integer.parseInt(info[3]);
                
                System.out.println("Recebendo arquivo em " + totalFragmentos + " fragmentos...");
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream(tamanhoTotal);
                
                for (int i = 0; i < totalFragmentos; i++) {
                    socket.receive(respPacket);
                    String fragmentoMsg = new String(respPacket.getData(), 0, 
                                                    Math.min(100, respPacket.getLength()));
                    
                    if (fragmentoMsg.startsWith("FRAGMENTO|")) {
                        String[] fragInfo = fragmentoMsg.split("\\|", 3);
                        int inicioDados = fragInfo[0].length() + fragInfo[1].length() + 2;
                        int tamanhoDados = respPacket.getLength() - inicioDados;
                        
                        baos.write(respPacket.getData(), inicioDados, tamanhoDados);
                        System.out.println("Fragmento " + (i + 1) + "/" + totalFragmentos + " recebido");
                    }
                }
                
                return baos.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static class ArquivoInfo {
        public String nome;
        public long tamanho;
        public long dataModificacao;
        
        public ArquivoInfo(String nome, long tamanho, long dataModificacao) {
            this.nome = nome;
            this.tamanho = tamanho;
            this.dataModificacao = dataModificacao;
        }
    }
}
