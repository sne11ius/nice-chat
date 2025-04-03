package network.discordia;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ServerEndpoint("/chat/{username}")
@ApplicationScoped
public class ChatSocket
{

    List<Session> sessions = new CopyOnWriteArrayList<>();

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    private void runPing()
    {
        executor.scheduleAtFixedRate(() ->
        {
            sessions.forEach(session ->
            {
                try
                {
                    ByteBuffer payload = ByteBuffer.wrap("ping".getBytes());
                    session.getAsyncRemote().sendPing(payload);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
        }, 30, 30, TimeUnit.SECONDS);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username)
    {
        broadcast("New user " + username + " joined, say hello!");
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username)
    {
        sessions.remove(session);
        broadcast("User left " + username);
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("username") String username)
    {
        sessions.remove(session);
        broadcast("User " + username + " left on error: " + throwable);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username)
    {
        broadcast(">> " + username + "<br>" + message);
    }

    private void broadcast(String message)
    {
        String echteNachricht = message;
        echteNachricht = echteNachricht.replace(":heart:", "\u2764\uFE0F");
        final String finaleNachricht = echteNachricht;
        sessions.forEach(s ->
        {
            s.getAsyncRemote().sendObject(finaleNachricht, result ->
            {
                if (result.getException() != null)
                {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}
