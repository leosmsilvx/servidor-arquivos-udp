package br.com.web.notify;

import br.com.web.ws.UpdateSocket;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.concurrent.atomic.AtomicLong;

@Path("/notify")
public class NotifyResource {
    private static final AtomicLong ultimaNotificacao = new AtomicLong(0);
    private static final long INTERVALO_DEBOUNCE = 1000;
    
    @POST
    public Response notifyChange() {
        long agora = System.currentTimeMillis();
        long ultima = ultimaNotificacao.get();
        
        if (agora - ultima > INTERVALO_DEBOUNCE) {
            if (ultimaNotificacao.compareAndSet(ultima, agora)) {
                System.out.println("Broadcast enviado aos clientes WebSocket");
                UpdateSocket.broadcast("NOVO_ARQUIVO");
            }
        } else {
            System.out.println("Notificação ignorada (debounce ativo - aguarde " + 
                             (INTERVALO_DEBOUNCE - (agora - ultima)) + "ms)");
        }
        
        return Response.ok().build();
    }
}
