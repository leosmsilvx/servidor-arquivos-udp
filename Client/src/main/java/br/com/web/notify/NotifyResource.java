package br.com.web.notify;

import br.com.web.ws.UpdateSocket;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/notify")
public class NotifyResource {
    @POST
    public Response notifyChange() {
        UpdateSocket.broadcast("NOVO_ARQUIVO");
        return Response.ok().build();
    }
}
