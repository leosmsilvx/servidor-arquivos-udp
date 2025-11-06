package br.com.web.ws;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/updates")
public class UpdateSocket {
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session s) { sessions.add(s); }

    @OnClose
    public void onClose(Session s) { sessions.remove(s); }

    public static void broadcast(String msg) {
        for (Session s : sessions) {
            try { s.getBasicRemote().sendText(msg); } catch (Exception ignored) {}
        }
    }
}
