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
    public Response upload(@FormDataParam("file") InputStream fileStream) {
        try {
            byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(fileStream);
            udp.send(bytes);
            return Response.ok("Upload enviado ao servidor UDP").build();
        } catch (Exception e) {
            return Response.status(500).entity("Erro ao ler arquivo").build();
        }
    }
}
