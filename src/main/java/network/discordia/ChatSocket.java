package network.discordia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat/{username}")
@ApplicationScoped
public class ChatSocket
{

    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username)
    {
        broadcast("New user " + username + " joined, say hello!");
        sessions.put(username, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username)
    {
        sessions.remove(username);
        broadcast("User " + username + " left");
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable)
    {
        sessions.remove(session);
        broadcast("User " + username + " left on error: " + throwable);
    }

    @OnMessage
    public void conMessage(String message, @PathParam("username") String username)
    {
        broadcast(">> " + username + ": " + message);
    }

    private void broadcast(String message)
    {
        String actualMessage = preprocessMessage(message);
        sessions.values().forEach(session ->
        {
            session.getAsyncRemote().sendObject(actualMessage, result ->
            {
                if (result.getException() != null)
                {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }

    String preprocessMessage(String message)
    {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(":heart:", "❤️");
        mapping.put(":rocket:", "\uD83D\uDE80");
        mapping.put("13256278887989457651018865901401704640", "[redacted]");
        for (Map.Entry<String, String> entry : mapping.entrySet())
        {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }
}
