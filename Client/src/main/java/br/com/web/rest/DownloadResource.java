package br.com.web.rest;

import br.com.web.udp.UdpClient;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

@Path("/download")
public class DownloadResource {

    private UdpClient udp = new UdpClient();

    @GET
    @Path("/{nomeArquivo}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("nomeArquivo") String nomeArquivo) {
        try {
            byte[] dados = udp.baixarArquivo(nomeArquivo);
            
            if (dados == null || dados.length == 0) {
                return Response.status(404)
                    .entity("{\"status\":\"error\",\"message\":\"Arquivo não encontrado\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
            }
            
            return Response.ok(dados)
                .header("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"")
                .header("Content-Type", "application/octet-stream")
                .build();
                
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"status\":\"error\",\"message\":\"Erro ao baixar arquivo: " + 
                       e.getMessage() + "\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }
}
