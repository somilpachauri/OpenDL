import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class LocalServer {

    public static void startServer(Consumer<String> onUrlReceived) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 12345), 0);

            server.createContext("/catch", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    String capturedUrl = new String(is.readAllBytes());

                    System.out.println("Browser Extension sent URL: " + capturedUrl);

                    onUrlReceived.accept(capturedUrl);

                    String response = "URL Received";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("OpenDL Local Server listening on port 12345...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}