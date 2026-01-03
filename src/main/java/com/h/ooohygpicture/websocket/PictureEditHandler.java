package com.h.ooohygpicture.websocket;

import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/ws/picture/edit/{spaceId}/{userId}") // 👈 客户端连接地址
public class PictureEditHandler {

    // 存放每个空间的连接会话：Map<spaceId, Set<Session>>
    private static ConcurrentHashMap<Long, CopyOnWriteArraySet<Session>> spaceSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("spaceId") Long spaceId, @PathParam("userId") Long userId) {
        // 加入该空间的会话集合
        spaceSessions.computeIfAbsent(spaceId, k -> new CopyOnWriteArraySet<>()).add(session);
        System.out.println("用户 " + userId + " 进入了空间 " + spaceId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("spaceId") Long spaceId) {
        if (spaceSessions.containsKey(spaceId)) {
            spaceSessions.get(spaceId).remove(session);
        }
    }

    /**
     * 广播消息：当有人修改图片时调用这个方法
     */
    public void broadcast(Long spaceId, String message) {
        CopyOnWriteArraySet<Session> sessions = spaceSessions.get(spaceId);
        if (sessions != null) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(message);
                }
            }
        }
    }
}
