package pl.niezgodnosci.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        // Na ekranie raportu "wstecz" wraca do wpisu, na ekranie wpisu zamyka aplikację
        web.evaluateJavascript("(function(){if(typeof pg!=='undefined'&&pg===1){go(0);return '1'}return '0'})()",
                value -> { if (value == null || !value.contains("1")) finish(); });
    }

    private class Bridge {
        @JavascriptInterface
        public void saveCsv(String name, String data) {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                v.put(MediaStore.Downloads.RELATIVE_PATH, "Download");
                final Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                OutputStream o = getContentResolver().openOutputStream(uri);
                o.write(data.getBytes(StandardCharsets.UTF_8));
                o.close();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Zapisano w Pobranych: " + name, Toast.LENGTH_LONG).show();
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/csv");
                    i.putExtra(Intent.EXTRA_STREAM, uri);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(i, "Wyślij / otwórz raport"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Błąd zapisu pliku", Toast.LENGTH_LONG).show());
            }
        }
    }
}
