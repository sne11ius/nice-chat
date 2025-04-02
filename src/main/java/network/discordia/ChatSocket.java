package network.discordia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ServerEndpoint("/chat")
@ApplicationScoped
public class ChatSocket
{

    List<Session> sessions = new CopyOnWriteArrayList<>();

    @OnOpen
    public void onOpen(Session session)
    {
        broadcast("New user joined, say hello!");
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session)
    {
        sessions.remove(session);
        broadcast("User left");
    }

    @OnError
    public void onError(Session session, Throwable throwable)
    {
        sessions.remove(session);
        broadcast("User left on error: " + throwable);
    }

    @OnMessage
    public void conMessage(String message)
    {
        broadcast(">> " + message);
    }

    private void broadcast(String message)
    {
        sessions.forEach(s ->
        {
            s.getAsyncRemote().sendObject(message, result ->
            {
                if (result.getException() != null)
                {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}
