package br.drummond.provador3d;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provador 3D para Android.
 * O editor roda embutido (offline). A foto é gerada no próprio celular pelo motor do Local Dream,
 * que expõe um servidor local em 127.0.0.1:8081 enquanto um modelo está carregado.
 */
public class MainActivity extends Activity {
    static final String ENGINE = "http://127.0.0.1:8081";
    static final String ENGINE_PACKAGE = "io.github.xororz.localdream";
    static final String ENGINE_DOWNLOAD = "https://github.com/xororz/local-dream/releases/latest";
    static final String HOST = "https://appassets.androidplatform.net";

    private WebView web;
    private File photosDir;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        photosDir = new File(getFilesDir(), "fotos");
        photosDir.mkdirs();

        WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/fotos/", new WebViewAssetLoader.InternalStoragePathHandler(this, photosDir))
                .build();

        web = findViewById(R.id.web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        web.setBackgroundColor(0xFF121315);
        web.addJavascriptInterface(new Bridge(), "ProvadorNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return assets.shouldInterceptRequest(r.getUrl());
            }
        });
        if (saved != null) web.restoreState(saved);
        else web.loadUrl(HOST + "/assets/offline/index.html");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    private boolean backArmed = false;

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.__back ? window.__back() : false", v -> {
            if ("true".equals(v)) return;
            if (backArmed) { finish(); return; }
            backArmed = true;
            Toast.makeText(this, "Toque voltar de novo para sair", Toast.LENGTH_SHORT).show();
            ui.postDelayed(() -> backArmed = false, 2000);
        });
    }

    private void js(String code) {
        ui.post(() -> web.evaluateJavascript(code, null));
    }

    // ---------- motor de imagens ----------
    private final EngineClient engine = new EngineClient(ENGINE);

    void runGeneration(String requestJson) {
        engine.generate(requestJson, new EngineClient.Listener() {
            @Override public void progress(int step, int total) {
                js("window.__phoneProgress(" + step + "," + total + ")");
            }
            @Override public void complete(byte[] img, String fmt, long seed) {
                try {
                    String name = "provador_" + System.currentTimeMillis() + ("png".equals(fmt) ? ".png" : ".jpg");
                    File f = new File(photosDir, name);
                    try (FileOutputStream fo = new FileOutputStream(f)) { fo.write(img); }
                    copyToGallery(f, "png".equals(fmt) ? "image/png" : "image/jpeg");
                    js("window.__phoneDone(" + JSONObject.quote(HOST + "/fotos/" + name) + "," + seed + ")");
                } catch (Exception e) {
                    fail("Não consegui salvar a foto: " + e.getMessage());
                }
            }
            @Override public void error(String message) { fail(message); }
        });
    }

    private void fail(String msg) {
        js("window.__phoneError(" + JSONObject.quote(msg) + ")");
    }

    /** Copia a foto para a galeria, em Imagens/Provador3D. */
    private void copyToGallery(File f, String mime) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, f.getName());
                v.put(MediaStore.Images.Media.MIME_TYPE, mime);
                v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Provador3D");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return;
                try (OutputStream os = getContentResolver().openOutputStream(uri);
                     InputStream in = new java.io.FileInputStream(f)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Provador3D");
                dir.mkdirs();
                File dest = new File(dir, f.getName());
                try (InputStream in = new java.io.FileInputStream(f); OutputStream os = new FileOutputStream(dest)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
            }
        } catch (Exception ignored) {
            // a foto continua no app mesmo se a galeria recusar
        }
    }

    // ---------- ponte com a página ----------
    private class Bridge {
        @JavascriptInterface
        public String engine() { return "phone"; }

        @JavascriptInterface
        public void health(String callback) {
            new Thread(() -> js("window[" + JSONObject.quote(callback) + "] && window[" + JSONObject.quote(callback) + "](" + engine.alive(1500) + ")")).start();
        }

        @JavascriptInterface
        public void generate(String requestJson) {
            new Thread(() -> runGeneration(requestJson)).start();
        }

        @JavascriptInterface
        public void openEngine() {
            ui.post(() -> {
                Intent i = getPackageManager().getLaunchIntentForPackage(ENGINE_PACKAGE);
                if (i == null) {
                    Toast.makeText(MainActivity.this, "Instale o Local Dream (o motor de imagens) e volte.", Toast.LENGTH_LONG).show();
                    i = new Intent(Intent.ACTION_VIEW, Uri.parse(ENGINE_DOWNLOAD));
                }
                startActivity(i);
            });
        }

        @JavascriptInterface
        public void save(String url, String name) {
            ui.post(() -> Toast.makeText(MainActivity.this, "Essa foto já está na galeria, em Imagens/Provador3D", Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void reconnect() { }
    }
}
