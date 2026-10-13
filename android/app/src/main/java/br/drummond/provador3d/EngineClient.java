package br.drummond.provador3d;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cliente do servidor local do Local Dream (POST /generate, resposta em Server-Sent Events).
 * Sem dependências do Android para poder ser testado fora do aparelho.
 */
public class EngineClient {
    public interface Listener {
        void progress(int step, int total);
        void complete(byte[] image, String format, long seed);
        void error(String message);
    }

    private final String base;

    public EngineClient(String base) { this.base = base; }

    public boolean alive(int timeoutMs) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(base + "/health").openConnection();
            c.setConnectTimeout(timeoutMs);
            c.setReadTimeout(timeoutMs);
            return c.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    public void generate(String requestJson, Listener l) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(base + "/generate").openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(5000);
            c.setReadTimeout(10 * 60 * 1000);
            c.setRequestProperty("Content-Type", "application/json");
            byte[] body = requestJson.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = c.getOutputStream()) { os.write(body); }

            int code = c.getResponseCode();
            if (code != 200) {
                String msg = readAll(c.getErrorStream());
                String nice = "O motor recusou o pedido.";
                try { nice = new JSONObject(msg).getJSONObject("error").getString("message"); } catch (Exception ignored) { }
                l.error(nice);
                return;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    JSONObject ev = new JSONObject(line.substring(5).trim());
                    String type = ev.optString("type");
                    if ("progress".equals(type)) {
                        l.progress(ev.optInt("step"), ev.optInt("total_steps"));
                    } else if ("complete".equals(type)) {
                        String fmt = ev.optString("format", "jpeg");
                        if (!"jpeg".equals(fmt) && !"png".equals(fmt)) { l.error("O motor devolveu um formato inesperado (" + fmt + ")."); return; }
                        l.complete(Base64.getMimeDecoder().decode(ev.getString("image")), fmt, ev.optLong("seed"));
                        return;
                    } else if ("error".equals(type)) {
                        l.error(ev.optString("message", "Erro no motor de imagens."));
                        return;
                    }
                }
            }
            l.error("O motor encerrou sem devolver a foto.");
        } catch (Exception e) {
            l.error(alive(1500) ? "A geração foi interrompida: " + e.getMessage()
                                : "O motor de imagens fechou. Abra o Local Dream e carregue o modelo de novo.");
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readAll(InputStream in) {
        if (in == null) return "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = r.readLine()) != null) sb.append(s);
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
