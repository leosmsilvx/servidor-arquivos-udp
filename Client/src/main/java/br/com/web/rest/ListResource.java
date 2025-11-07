package br.com.web.rest;

import br.com.web.udp.UdpClient;
import br.com.web.udp.UdpClient.ArquivoInfo;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Path("/list")
public class ListResource {

    private UdpClient udp = new UdpClient();

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response listarArquivos() {
        try {
            List<ArquivoInfo> arquivos = udp.listarArquivos();
            
            StringBuilder json = new StringBuilder("{\"status\":\"success\",\"arquivos\":[");
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            
            for (int i = 0; i < arquivos.size(); i++) {
                ArquivoInfo info = arquivos.get(i);
                if (i > 0) json.append(",");
                
                json.append("{")
                    .append("\"nome\":\"").append(escaparJson(info.nome)).append("\",")
                    .append("\"tamanho\":").append(info.tamanho).append(",")
                    .append("\"tamanhoFormatado\":\"").append(formatarTamanho(info.tamanho)).append("\",")
                    .append("\"dataModificacao\":\"").append(sdf.format(new Date(info.dataModificacao))).append("\"")
                    .append("}");
            }
            
            json.append("]}");
            
            return Response.ok(json.toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"status\":\"error\",\"message\":\"Erro ao listar arquivos: " + 
                       escaparJson(e.getMessage()) + "\"}")
                .build();
        }
    }
    
    private String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
    
    private String escaparJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}
