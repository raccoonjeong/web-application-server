package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            DataOutputStream dos = new DataOutputStream(out);

            String line = br.readLine();
            byte[] body = getBody(line);

            response200Header(dos, body.length);
            responseBody(dos, body);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private byte[] getBody(String line) throws IOException {
        String url = HttpRequestUtils.parseURL(line);
        String[] pathAndParams = HttpRequestUtils.parsePathAndParams(url);
        route(pathAndParams);
        byte[] body;
        if("".equals(url) || "/".equals(url)) {
            body = "Hello World".getBytes();
        }else {
            body = Files.readAllBytes(new File("./webapp" + url).toPath());
        }
        return body;
    }

    private void route(String[] pathAndParams) {
        if (pathAndParams == null || pathAndParams.length < 2) return; // TODO: params가 없는데 경로 처리하는 경우?

        String requestPath = pathAndParams[0];
        String params = pathAndParams[1];

        if("/user/create".equals(requestPath)) createUser(params);

    }

    private void createUser(String params) {
        final String[] mustUserInfo = {"userId", "password", "name", "email"};
        for(int i = 0; i < mustUserInfo.length; i++) {
            if (!params.contains(mustUserInfo[i])) {
                log.debug("회원 가입 실패! 필수값 없음 : {}", mustUserInfo[i]);
                return;
            }
        }
        Map<String, String> userInfoMap = HttpRequestUtils.parseQueryString(params);

        User user = new User(userInfoMap.get(mustUserInfo[0]),
                             userInfoMap.get(mustUserInfo[1]),
                             userInfoMap.get(mustUserInfo[2]),
                             userInfoMap.get(mustUserInfo[3]));

        log.debug("회원 가입! userId : {}, password : {}, name : {}, email : {}",
                  userInfoMap.get(mustUserInfo[0]),
                  userInfoMap.get(mustUserInfo[1]),
                  userInfoMap.get(mustUserInfo[2]),
                  userInfoMap.get(mustUserInfo[3]));
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
