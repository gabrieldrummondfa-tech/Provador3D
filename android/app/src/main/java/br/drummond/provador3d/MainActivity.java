package br.drummond.provador3d;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Casca Android do Provador 3D.
 * Procura o ComfyUI do PC no Wi-Fi (portas 8000 e 8188, caminho /provador/api/models) e abre o app servido por ele.
 * Sem PC, abre o editor embutido (mesmo código), que funciona offline.
 */
public class MainActivity extends Activity {
    private static final int[] PORTS = {8000, 8188};
    private static final String OFFLINE_URL = "https://appassets.androidplatform.net/assets/offline/index.html";

    private WebView web;
    private View connect;
    private TextView status, hint;
    private ProgressBar spin;
    private EditText addr;
    private SharedPreferences prefs;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile int scanId = 0;
    private WebViewAssetLoader assets;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("provador", MODE_PRIVATE);
        web = findViewById(R.id.web);
        connect = findViewById(R.id.connect);
        status = findViewById(R.id.status);
        hint = findViewById(R.id.hint);
        spin = findViewById(R.id.spin);
        addr = findViewById(R.id.addr);

        assets = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        web.setBackgroundColor(0xFF121315);
        web.addJavascriptInterface(new Bridge(), "ProvadorNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return assets.shouldInterceptRequest(r.getUrl());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return false;
            }
        });

        ((Button) findViewById(R.id.retry)).setOnClickListener(v -> startScan());
        ((Button) findViewById(R.id.offline)).setOnClickListener(v -> openOffline());
        ((Button) findViewById(R.id.connectAddr)).setOnClickListener(v -> tryTyped());
        addr.setOnEditorActionListener((v, id, e) -> { if (id == EditorInfo.IME_ACTION_GO) { tryTyped(); return true; } return false; });
        String last = prefs.getString("server", "");
        if (!last.isEmpty()) addr.setText(last.replace("http://", ""));

        startScan();
    }

    // ---------- busca do PC ----------
    private void setStatus(String main, boolean busy) {
        status.setText(main);
        spin.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    private void startScan() {
        final int id = ++scanId;
        showConnect();
        setStatus("Procurando o PC no Wi-Fi…", true);
        new Thread(() -> {
            String found = null;
            String last = prefs.getString("server", "");
            if (!last.isEmpty() && probe(last, 1500)) found = last;
            if (found == null) found = scanSubnet(id);
            final String result = found;
            ui.post(() -> {
                if (id != scanId) return;
                if (result != null) openServer(result);
                else setStatus("Não achei o PC. Confira se o ComfyUI está aberto e no mesmo Wi-Fi.", false);
            });
        }).start();
    }

    private void tryTyped() {
        String t = addr.getText().toString().trim();
        if (t.isEmpty()) return;
        if (!t.startsWith("http")) t = "http://" + t;
        if (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        final String base = t;
        final int id = ++scanId;
        setStatus("Conectando em " + base.replace("http://", "") + "…", true);
        new Thread(() -> {
            boolean ok = probe(base, 3000);
            ui.post(() -> {
                if (id != scanId) return;
                if (ok) openServer(base);
                else setStatus("Esse endereço não respondeu. Confira o IP e a porta (8000 no Desktop, 8188 no portátil).", false);
            });
        }).start();
    }

    private static boolean probe(String base, int timeoutMs) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(base + "/provador/api/models").openConnection();
            c.setConnectTimeout(timeoutMs);
            c.setReadTimeout(timeoutMs);
            int code = c.getResponseCode();
            if (code != 200) return false;
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[64];
                int n = in.read(buf);
                return n > 0 && new String(buf, 0, n).contains("models");
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private String scanSubnet(int id) {
        List<String> prefixes = localPrefixes();
        if (prefixes.isEmpty()) return null;
        ExecutorService pool = Executors.newFixedThreadPool(48);
        AtomicReference<String> hit = new AtomicReference<>(null);
        for (String pre : prefixes) {
            for (int h = 1; h < 255; h++) {
                for (int port : PORTS) {
                    final String base = "http://" + pre + h + ":" + port;
                    pool.execute(() -> {
                        if (hit.get() != null || id != scanId) return;
                        if (probe(base, 700)) hit.compareAndSet(null, base);
                    });
                }
            }
        }
        pool.shutdown();
        try {
            long end = System.currentTimeMillis() + 25000;
            while (!pool.isTerminated() && hit.get() == null && System.currentTimeMillis() < end) {
                pool.awaitTermination(200, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException ignored) { }
        pool.shutdownNow();
        return hit.get();
    }

    private List<String> localPrefixes() {
        List<String> out = new ArrayList<>();
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            LinkProperties lp = n != null ? cm.getLinkProperties(n) : null;
            if (lp != null) for (LinkAddress la : lp.getLinkAddresses()) {
                if (la.getAddress() instanceof Inet4Address && !la.getAddress().isLoopbackAddress()) {
                    String ip = la.getAddress().getHostAddress();
                    out.add(ip.substring(0, ip.lastIndexOf('.') + 1));
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    // ---------- telas ----------
    private void showConnect() {
        web.setVisibility(View.GONE);
        connect.setVisibility(View.VISIBLE);
    }

    private void openServer(String base) {
        prefs.edit().putString("server", base).apply();
        addr.setText(base.replace("http://", ""));
        connect.setVisibility(View.GONE);
        web.setVisibility(View.VISIBLE);
        web.loadUrl(base + "/provador/");
        Toast.makeText(this, "Conectado ao PC", Toast.LENGTH_SHORT).show();
    }

    private void openOffline() {
        ++scanId;
        connect.setVisibility(View.GONE);
        web.setVisibility(View.VISIBLE);
        web.loadUrl(OFFLINE_URL);
    }

    @Override
    public void onBackPressed() {
        if (web.getVisibility() == View.VISIBLE) {
            web.evaluateJavascript("window.__back ? window.__back() : false", v -> {
                if (!"true".equals(v)) showConnectOrExit();
            });
        } else {
            super.onBackPressed();
        }
    }

    private boolean backArmed = false;
    private void showConnectOrExit() {
        if (backArmed) { finish(); return; }
        backArmed = true;
        Toast.makeText(this, "Toque voltar de novo para sair", Toast.LENGTH_SHORT).show();
        ui.postDelayed(() -> backArmed = false, 2000);
    }

    // ---------- ponte com a página ----------
    private class Bridge {
        @JavascriptInterface
        public void reconnect() {
            ui.post(MainActivity.this::startScan);
        }

        @JavascriptInterface
        public void save(String url, String name) {
            ui.post(() -> {
                try {
                    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                    String file = (name == null || name.isEmpty()) ? "provador.png" : name.replaceAll("[^A-Za-z0-9._-]", "_");
                    r.setTitle("Provador 3D: " + file);
                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Provador3D/" + file);
                    ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
                    Toast.makeText(MainActivity.this, "Salvando em Imagens/Provador3D", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Não consegui salvar a foto.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
