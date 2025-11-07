package br.com.web.rest;

import br.com.web.udp.UdpClient;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.media.multipart.*;

import java.io.InputStream;

@Path("/upload")
public class UploadResource {

    private UdpClient udp = new UdpClient();

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response upload(
            @FormDataParam("file") InputStream fileStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {
        try {
            if (fileStream == null || fileDetail == null) {
                return Response.status(400)
                    .entity("{\"status\":\"error\",\"message\":\"Nenhum arquivo selecionado\"}")
                    .build();
            }
            
            String nomeArquivo = fileDetail.getFileName();
            byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(fileStream);
            
            if (bytes.length == 0) {
                return Response.status(400)
                    .entity("{\"status\":\"error\",\"message\":\"Arquivo vazio\"}")
                    .build();
            }
            
            String resultado = udp.enviarArquivo(nomeArquivo, bytes);
            
            if (resultado.startsWith("OK|") || resultado.startsWith("RECEBIDO")) {
                return Response.ok()
                    .entity("{\"status\":\"success\",\"message\":\"Arquivo enviado com sucesso: " + 
                           nomeArquivo + " (" + formatarTamanho(bytes.length) + ")\"}")
                    .build();
            } else {
                return Response.status(500)
                    .entity("{\"status\":\"error\",\"message\":\"Erro no servidor: " + resultado + "\"}")
                    .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"status\":\"error\",\"message\":\"Erro ao processar arquivo: " + 
                       e.getMessage() + "\"}")
                .build();
        }
    }
    
    private String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
