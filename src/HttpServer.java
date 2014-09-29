/**
 * Created by megge on 10.09.14.
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.URLDecoder;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;


public class HttpServer {

    public static void main(String[] args) throws Throwable {
        ServerSocket serverSocket = new ServerSocket(8080);
        WorkQueue workQueue = new WorkQueue(1);
        while (true) {
            Socket socket = serverSocket.accept();
            workQueue.execute(new SocketProcessor(socket));
        }
    }

    private static class SocketProcessor implements Runnable {

        private Socket socket;
        private InputStream in;
        private OutputStream out;
        private static final String DEFAULT_FILES_DIR = "/Users/megge/IdeaProjects/TP_server";

        private SocketProcessor(Socket socket) throws Throwable {
            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        public void run() {
            try {
                String header = readHeader();
                String method = getMethodFromHeader(header);
                String path = getURIFromHeader(header);
                if (!method.equals("GET") && !method.equals("HEAD")){
                    getHeader(405, "", 0);
                }
                if (path.contains("../") && (!new File(path).getCanonicalPath().contains(DEFAULT_FILES_DIR))){
                    getHeader(403, "", 0);
                } else {
                    path = URLDecoder.decode(path);
                    File file = new File(path);
                    if (!file.exists()){
                        getHeader(404, "", 0);
                    } else {
                        if (path.endsWith("/")){
                            path += "index.html";
                            file = new File(path);
                        }
                        if (!file.exists()){
                            getHeader(403, "", 0);
                            out.flush();
                        } else {
                            String typeParse[] = path.split("\\.");
                            String type = typeParse[typeParse.length-1];
                            String typeName;
                            if (type.equals("html")){
                                typeName = "text/html";
                            } else if (type.equals("css")){
                                typeName = "text/css";
                            } else if (type.equals("js")){
                                typeName = "text/javascript";
                            } else if (type.equals("jpg") || type.equals("jpeg")){
                                typeName = "image/jpeg";
                            } else if (type.equals("png")){
                                typeName = "image/png";
                            } else if (type.equals("gif")){
                                typeName = "image/gif";
                            } else if (type.equals("swf")){
                                typeName = "application/x-shockwave-flash";
                            } else {
                                typeName = "text/plain";
                            }
                            //System.out.println(path.toString());
                            getHeader(200, typeName, file.length());
                            if (method.equals("GET")){
                                Path pathForRead = Paths.get(path);
                                byte[] byteArray = Files.readAllBytes(pathForRead);
                                out.write(byteArray);
                            }
                            out.flush();
                        }

                    }
                }
                socket.close();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            //System.err.println("Client processing finished");
        }

        /**
         * Считывает заголовок сообщения от клиента.
         */
        private String readHeader() throws Throwable {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder builder = new StringBuilder();
            String ln = null;
            while (true) {
                ln = reader.readLine();
                if (ln == null || ln.isEmpty()) {
                    break;
                }
                builder.append(ln + System.getProperty("line.separator"));
            }
            return builder.toString();
        }

        /**
         * Вытаскивает метод из заголовка сообщения от
         * клиента.
         */
        private String getMethodFromHeader(String header) {
            int to = header.indexOf(" ");
            String method = header.substring(0, to);
            return method;
        }

        /**
         * Вытаскивает идентификатор запрашиваемого ресурса из заголовка сообщения от
         * клиента.
         */
        private String getURIFromHeader(String header) {
            int from = header.indexOf(" ") + 1;
            int to = header.indexOf(" ", from);
            String uri = header.substring(from, to);
            // тут мы отрезаем параметры запроса
            int paramIndex = uri.indexOf("?");
            if (paramIndex != -1) {
                uri = uri.substring(0, paramIndex);
            }
            return DEFAULT_FILES_DIR + uri;
        }

        /**
         * Возвращает http заголовок ответа.
         */
        private void getHeader(int code, String type, long length) throws Throwable{
            String result = "HTTP/1.1 " + code + getAnswer(code);
            result += "\r\n";
            result += "Date: ";
            Date date = new Date();
            DateFormat dateFormat =
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            result += dateFormat.format(date) + "\r\n";
            result += "Server: server\r\n";
            result += "Content-Length: " + length + "\r\n";
            result += "Content-Type: " + type + "\r\n";
            result += "Connection: close\r\n\r\n";
            //System.out.println(result);
            out.write(result.getBytes());
        }

        /**
         * Возвращает комментарий к коду результата отправки.
         */
        private String getAnswer(int code) {
            switch (code) {
                case 200:
                    return " OK";
                case 404:
                    return " Not Found";
                case 405:
                    return " Method Not Allowed";
                case 403:
                    return " Forbidden";
                default:
                    return " Internal Server Error";
            }
        }
    }
}