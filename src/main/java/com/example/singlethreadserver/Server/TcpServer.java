package com.example.singlethreadserver.Server;


import com.example.singlethreadserver.Connection.Connection;
import com.example.singlethreadserver.Connection.TcpConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TcpServer implements Server, Connection.Listener {
    private static Log logger = LogFactory.getLog(TcpServer.class);
    private ServerSocket serverSocket;
    private volatile boolean isStop;
    private List<Connection> connections = new ArrayList<>();
    private List<Connection.Listener> listeners = new ArrayList<>();

    @Override
    public int getConnectionsCount() {
        return connections.size();
    }

    @Override
    public void setPort(Integer port) {
        try {
            if (port == null) {
                logger.info("Server port not found. Use default port 1025");
                port = 1025;
            }
            serverSocket = new ServerSocket(port);
            logger.info("Server start at port " + port);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("May be port " + port + " busy.");
        }
    }

    @Override
    public void start() {
        new Thread(() -> {
            while (!isStop) {
                try {
                    Socket socket = serverSocket.accept();
                    if (socket.isConnected()) {
                        TcpConnection tcpConnection = new TcpConnection(socket);
                        tcpConnection.start();
                        tcpConnection.addListener(this);
                        connected(tcpConnection);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void stop() {
        isStop = true;
    }

    @Override
    public List<Connection> getConnections() {
        return connections;
    }

    @Override
    public void addListener(Connection.Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void messageReceived(Connection connection, Object message) {
        logger.trace("Received new message from " + connection.getAddress().getCanonicalHostName());
        logger.trace("Class name: " + message.getClass().getCanonicalName() + ", toString: " + message.toString());
        for (Connection.Listener listener : listeners) {
            listener.messageReceived(connection, message);
        }
    }

    @Override
    public void connected(Connection connection) {
        logger.info("New connection! Ip: " + connection.getAddress().getCanonicalHostName() + ".");
        connections.add(connection);
        logger.info("Current connections count: " + connections.size());
        for (Connection.Listener listener : listeners) {
            listener.connected(connection);
        }
    }

    @Override
    public void disconnected(Connection connection) {
        logger.info("Disconnect! Ip: " + connection.getAddress().getCanonicalHostName() + ".");
        connections.remove(connection);
        logger.info("Current connections count: " + connections.size());
        for (Connection.Listener listener : listeners) {
            listener.disconnected(connection);
        }
    }
}
