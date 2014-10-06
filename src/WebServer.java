import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class WebServer extends Thread {

    public static void main(String args[])
    {
        try
        {
            Integer port = 8080;
            ServerSocket server = new ServerSocket(port);
            WorkQueue workQueue = new WorkQueue(4);
            while(true)
            {
                Socket socket = server.accept();
                workQueue.execute(new WebSocket(socket));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static class WebSocket extends Thread {

        private Socket socket;
        private static final String OK = "HTTP/1.1 200 OK\r\n";
        private static final String METHOD_NOT_ALLOWED = "HTTP/1.1 405 Method Not Allowed\r\n";
        private static final String NOT_FOUND = "HTTP/1.1 404 Not Found\r\n";
        private static final String FORBIDDEN = "HTTP/1.1 403 Forbidden\r\n";
        private static final String DEFAULT_FILES_DIR = "/Users/megge/IdeaProjects/TP_server";

        public WebSocket(Socket socket) {
            this.socket = socket;
        }

        String getServerTime() {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormat.format(calendar.getTime());
        }

        String getDateLastModifiedFile(File file) {
            SimpleDateFormat lastModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            lastModified.setTimeZone(TimeZone.getTimeZone("GMT"));
            return lastModified.format(file.lastModified());
        }

        public void run() {
            try {

                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                byte buf[] = new byte[64 * 1024];
                int readBuf = inputStream.read(buf);

                if (readBuf < 0) {
                    socket.close();
                    return;
                }

                String request = new String(buf, 0, readBuf);
                String path = getPath(request);

                if (path == METHOD_NOT_ALLOWED || path == FORBIDDEN) {
                    String response = "";
                    if (path == METHOD_NOT_ALLOWED)
                        response = METHOD_NOT_ALLOWED;
                    else
                        response = FORBIDDEN;

                    response += "Date: " + getServerTime() + "\r\n";
                    response += "Connection: close\r\n" + "Server: WEBServer\r\n\r\n";
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                    socket.close();
                    return;
                }
                if (path.contains("../") && (new File(path).getCanonicalPath().indexOf(DEFAULT_FILES_DIR) != 0)) {
                    String response;
                    response = FORBIDDEN;
                    response += "Date: " + getServerTime() + "\r\n";
                    response += "Connection: close\r\n" + "Server: WEBServer\r\n\r\n";
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                    socket.close();
                    return;
                }
                path = URLDecoder.decode(path, "UTF-8");
                File file = new File(path);
                boolean fileIsExist = file.exists();
                boolean indexIsExist = true;

                if (fileIsExist)
                    if (file.isDirectory()) {
                        if (path.lastIndexOf("" + File.separator) == path.length() - 1)
                            path = path + "index.html";
                        else
                            path = path + File.separator + "index.html";
                        file = new File(path);
                        fileIsExist = file.exists();
                        if (!fileIsExist)
                            indexIsExist = false;
                    }

                if (!fileIsExist) {
                    String response;
                    if (!indexIsExist)
                        response = FORBIDDEN;
                    else
                        response = NOT_FOUND;

                    response += "Date: " + getServerTime() + "\r\n";
                    response += "Content-Type: text/plain\r\n"
                            + "Connection: close\r\n"
                            + "Server: WEBServer\r\n\r\n";

                    response += "File " + path + " not found!";
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                    socket.close();
                    return;
                }

                String mime = "text/plain";

                readBuf = path.lastIndexOf(".");
                if (readBuf > 0) {
                    String ext = path.substring(readBuf);
                    if (ext.equalsIgnoreCase(".html"))
                        mime = "text/html";
                    else if (ext.equalsIgnoreCase(".htm"))
                        mime = "text/html";
                    else if (ext.equalsIgnoreCase(".js"))
                        mime = "text/javascript";
                    else if (ext.equalsIgnoreCase(".css"))
                        mime = "text/css";
                    else if (ext.equalsIgnoreCase(".gif"))
                        mime = "image/gif";
                    else if (ext.equalsIgnoreCase(".jpg"))
                        mime = "image/jpeg";
                    else if (ext.equalsIgnoreCase(".jpeg"))
                        mime = "image/jpeg";
                    else if (ext.equalsIgnoreCase(".png"))
                        mime = "image/png";
                    else if (ext.equalsIgnoreCase(".swf"))
                        mime = "application/x-shockwave-flash";
                }

                String response = OK;
                response += "Date: " + getServerTime() + "\r\n";
                response += "Last-Modified: " + getDateLastModifiedFile(file) + "\r\n";
                response += "Content-Length: " + file.length() + "\r\n";
                response += "Content-Type: " + mime + "\r\n";
                response += "Connection: close\r\n"
                        + "Server: WEBServer\r\n\r\n";
                outputStream.write(response.getBytes());
                String isHead = extract(request, "HEAD ", " ");

                if (isHead == null) {
                    FileInputStream fileInputStream = new FileInputStream(path);
                    readBuf = 1;
                    while (readBuf > 0) {
                        readBuf = fileInputStream.read(buf);
                        if (readBuf > 0) outputStream.write(buf, 0, readBuf);
                    }
                    fileInputStream.close();
                }
                outputStream.flush();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String getPath(String header) {
            String URIGet = extract(header, "GET ", " HTTP");
            String URIHead = extract(header, "HEAD ", " HTTP");
            String path, URI;
            if (URIGet != null) {
                path = URIGet;
                URI = URIGet;
            } else if (URIHead != null) {
                path = URIHead;
                URI = URIHead;
            } else
                return METHOD_NOT_ALLOWED;

            path.toLowerCase();
            path = DEFAULT_FILES_DIR + path;
            if (path.indexOf("http://", 0) == 0) {
                URI = URI.substring(7);
                URI = URI.substring(URI.indexOf("/", 0));
            } else if (path.indexOf("/", 0) == 0)
                URI = URI.substring(1);

            int i = URI.indexOf("?");
            if (i > 0)
                URI = URI.substring(0, i);

            i = URI.indexOf("#");
            if (i > 0)
                URI = URI.substring(0, i);

            path = "." + File.separator;
            char a;

            for (i = 0; i < URI.length(); i++) {
                a = URI.charAt(i);
                if (a == '/')
                    path += File.separator;
                else
                    path += a;
            }
            return path;
        }

        private String extract(String str, String start, String end) {
            int resultStart = str.indexOf("\r\n\r\n", 0), resultEnd;
            if (resultStart < 0)
                resultStart = str.indexOf("\r\n\r\n", 0);
            if (resultStart > 0)
                str = str.substring(0, resultStart);
            resultStart = str.indexOf(start, 0) + start.length();
            if (resultStart < start.length())
                return null;
            resultEnd = str.indexOf(end, resultStart);
            if (resultEnd < 0)
                resultEnd = str.length();
            return (str.substring(resultStart, resultEnd)).trim();
        }
    }
}