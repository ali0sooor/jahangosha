package com.arisar.jahangosha;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryListener;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final String GAME_URL = "file:///android_asset/game.html";

    // شناسه‌های ادیوری (تبلیغات یکتانت) - از پنل ناشرین ادیوری
    private static final String ADIVERY_APP_ID = "defac452-1108-46ce-b770-b9017e0fea29";
    private static final String ADIVERY_REWARDED_PLACEMENT = "d8017e86-9337-40af-b61f-c0068084a457";

    private static final int REQ_FILE = 101;
    private static final int REQ_SAVE = 102;

    private WebView web;

    private String lastAdLog;

    private ValueCallback<Uri[]> fileCallback;
    private String pendingText;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0E1E30"));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // ذخیره بازی در localStorage
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);                    // بزرگی فونت سیستم چیدمان بازی را به هم نریزد

        web.addJavascriptInterface(new AdsBridge(), "AndroidAds");
        web.addJavascriptInterface(new FilesBridge(), "AndroidFiles");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) { }
                return true;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                runOnUiThread(MainActivity.this::recreate);
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            // «ورود از فایل» در منوی ذخیره‌ها
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        goImmersive();
        Adivery.configure(getApplication(), ADIVERY_APP_ID);
        Adivery.addPlacementListener(ADIVERY_REWARDED_PLACEMENT, new AdiveryListener() {
            @Override
            public void onRewardedAdLoaded(String placementId) {
                lastAdLog = null;
            }

            @Override
            public void onRewardedAdClosed(String placementId, boolean isRewarded) {
                sendAdEvent(isRewarded ? "rewarded" : "closed");
            }

            @Override
            public void log(String placementId, String message) {
                lastAdLog = message;   // فقط برای نمایش علت خطا
            }
        });
        Adivery.prepareRewardedAd(this, ADIVERY_REWARDED_PLACEMENT);
        web.loadUrl(GAME_URL);
    }

    // ---------------------------------------------------------------- تمام‌صفحه

    @SuppressWarnings("deprecation")
    private void goImmersive() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) goImmersive();
    }

    // ---------------------------------------------------------------- چرخه عمر

    @Override
    protected void onPause() {
        // ذخیره فوری بازی هنگام رفتن به پس‌زمینه
        web.evaluateJavascript("try{save()}catch(e){}", null);
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        goImmersive();
    }

    @Override
    protected void onDestroy() {
        Adivery.removePlacementListener(ADIVERY_REWARDED_PLACEMENT);
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    // دکمه بازگشت: بستن پنجره باز، وگرنه منوی بازی، وگرنه خروج
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        String js = "(function(){try{"
                + "var m=document.getElementById('modal');"
                + "if(m&&m.classList.contains('open')){m.classList.remove('open');return 'x';}"
                + "if(typeof started!=='undefined'&&started&&typeof gameMenu==='function'){gameMenu();return 'x';}"
                + "}catch(e){}return 'exit';})()";
        web.evaluateJavascript(js, value -> {
            if (value != null && value.contains("exit")) finish();
        });
    }

    // ---------------------------------------------------------------- نتیجه انتخاب/ذخیره فایل

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                fileCallback = null;
            }
        } else if (requestCode == REQ_SAVE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingText != null) {
                try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                    os.write(pendingText.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, "فایل ذخیره شد", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "ذخیره فایل ممکن نشد", Toast.LENGTH_SHORT).show();
                }
            }
            pendingText = null;
        }
    }

    // ---------------------------------------------------------------- پل ذخیره فایل

    private class FilesBridge {
        @JavascriptInterface
        public void save(final String name, final String text) {
            runOnUiThread(() -> {
                pendingText = text;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, name);
                try {
                    startActivityForResult(i, REQ_SAVE);
                } catch (Exception e) {
                    pendingText = null;
                    Toast.makeText(MainActivity.this, "ذخیره فایل ممکن نشد", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ---------------------------------------------------------------- پل تبلیغ پاداشی
    // بازی window.AndroidAds.show() را صدا می‌زند و جواب را با window.onAdEvent('rewarded' | 'closed' | 'notready') می‌گیرد.

    private class AdsBridge {
        @JavascriptInterface
        public void show() {
            runOnUiThread(MainActivity.this::showRewardedAd);
        }
    }

    private void showRewardedAd() {
        if (Adivery.isLoaded(ADIVERY_REWARDED_PLACEMENT)) {
            Adivery.showAd(ADIVERY_REWARDED_PLACEMENT);   // نتیجه از onRewardedAdClosed می‌آید
            return;
        }
        String why = lastAdLog != null
                ? "خطای تبلیغ: " + lastAdLog
                : "تبلیغ هنوز آماده نیست؛ چند ثانیه بعد دوباره امتحان کن";
        Toast.makeText(this, why, Toast.LENGTH_LONG).show();
        Adivery.prepareRewardedAd(this, ADIVERY_REWARDED_PLACEMENT);
        sendAdEvent("notready");
    }

    private void sendAdEvent(final String event) {
        if (web == null) return;
        web.post(() -> {
            if (web != null) {
                web.evaluateJavascript("window.onAdEvent&&window.onAdEvent('" + event + "')", null);
            }
        });
    }
}
