package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

public class ClientHandler {
    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    public static final int SO_TIMEOUT = 120000;
    Socket socket;
    Server server;
    DataInputStream in;
    DataOutputStream out;

    private String nickname;
    private String login;

    public ClientHandler(Socket socket, Server server, ExecutorService es) {
        try {
            this.socket = socket;
            this.server = server;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            es.execute(() -> serverThread(socket, server));
        } catch (IOException e)  {
            logger.error("{ }",e);
        }
    }

    private void serverThread(Socket socket, Server server) {
        try {
            if (authenticateClient(socket, server))
                collaborateWithClient(server);
        } catch (SocketTimeoutException e) {
            logger.error("disconnect by timeout");
            sendMsg("/end");
        } catch (IOException e) {
            logger.error("{ }",e);
        } finally {
            server.unsubscribe(this);
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Unsubscribe exception ",e);
            }
        }
    }

    private boolean authenticateClient(Socket socket, Server server) throws IOException {
        try {
            socket.setSoTimeout(SO_TIMEOUT);
            // цикл аутентификации
            while (true) {
                String str = in.readUTF();
                logger.info("u > {}", str);
                if (endCommand(str)) break;
                if (authenticationCommand(server, str)) return true;
                registrationCommand(server, str);
            }
            return false;
        } finally {
            socket.setSoTimeout(0);
        }
    }

    private boolean authenticationCommand(Server server, String str) {
        if (str.startsWith("/auth ")) {
            String[] token = str.split("\\s+");
            nickname = server.getAuthService()
                    .getNicknameByLoginAndPassword(token[1], token[2]);
            login = token[1];
            if (nickname != null) {
                if (!server.isLoginAuthenticated(login)) {
                    sendMsg("/authok " + nickname);
                    server.subscribe(this);
                    logger.info("Client {} connected",nickname);
                    return true;
                } else {
                    sendMsg("С логином " + login + " уже вошли");
                }
            } else {
                sendMsg("Неверный логин / пароль");
            }
        }
        logger.info("Client {} not connected",nickname);
        return false;
    }

    private void registrationCommand(Server server, String str) {
        if (str.startsWith("/reg ")) {
            String[] token = str.split("\\s+");
            if (token.length < 4) {
                return;
            }

            boolean regOk = server.getAuthService().
                    registration(token[1], token[2], token[3]);
            if (regOk) {
                logger.info("Client {} registered",nickname);
                sendMsg("/regok");
            } else {
                sendMsg("/regno");
            }
        }
    }

    private boolean endCommand(String str) {
        if (str.equals("/end")) {
            sendMsg("/end");
            logger.info("Client {} disconnected",nickname);
            return true;
        }
        return false;
    }

    private void collaborateWithClient(Server server) throws IOException {
        // цикл работы
        while (!Thread.currentThread().isInterrupted()) {
            String str = in.readUTF();
            if (str.startsWith("/")) {
                if (endCommand(str))
                    return;
                if (renameNickCommand(server, str))
                    continue;
                privateMessageCommand(server, str);
            } else {
                server.broadcastMsg(this, str);
                logger.info("broadcastMsg {}",str);

            }
        }
    }

    private void privateMessageCommand(Server server, String str) {
        if (str.startsWith("/w")) {
            String[] token = str.split("\\s+", 3);
            if (token.length < 3) {
                return;
            }
            server.privateMsg(this, token[1], token[2]);
            logger.info("privateMsg {}",str);
        }
    }

    private boolean renameNickCommand(Server server, String str) {
        if (str.startsWith("/ren ")) {
            String[] token = str.split("\\s+");
            if (token.length < 4) {
                sendMsg("/renno Неправильное количество аргументов " + str);
                return true;
            }
            if (!login.equals(token[1])) {
                sendMsg("/renno Для ника " + token[3] + " указан некорректный логин " + token[1]);
                return true;
            }
            boolean regOk = server.getAuthService().
                    changeNick(token[1], token[2], token[3]);
            if (regOk) {
                String oldNickname = nickname;
                nickname = token[3];
                sendMsg("/renok Ник " + oldNickname + " успешно изменен на " + nickname);
                server.broadcastClientList();
            } else {
                sendMsg("/renno Ник " + token[3] + " уже занят");
            }
        }
        return false;
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            logger.error("sendMsg exception ",e);
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
