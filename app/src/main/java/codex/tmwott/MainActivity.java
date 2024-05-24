package codex.tmwott;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import codex.tmwott.services.ApplicationService;
import codex.tmwott.utilities.AdBlockUtilities;
import codex.tmwott.utilities.Utilities;
import io.github.edsuns.adfilter.FilterResult;

public class MainActivity extends AppCompatActivity {
    //public static final String URL = "https://tmwott.top/server/";

    //public static final String URL = "https://google.com";
    public static final String URL = "http://192.168.0.106:3000/";

    //public static final String URL = "https://bdtechexpert.xyz/TMW/";

    public static boolean isSupperBack = false;
    private ProgressBar progressBar;
    @SuppressLint("StaticFieldLeak")
    public static WebView webview;
    private MenuItem refreshBtn;
    private BottomNavigationView bottomNav;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utilities.init(this);
        AdBlockUtilities.init(this);

        webview = findViewById(R.id.custom_webview);
        progressBar = findViewById(R.id.progress_bar);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnApplyWindowInsetsListener(null);
        refreshBtn = bottomNav.getMenu().findItem(R.id.nav_refresh);

        bottomNav.setOnItemSelectedListener(item -> {
            if(item.getItemId() == R.id.nav_home){
                //runJS("window.location.replace('https://tmwott.top/server/#/active')");
                runJS("window.location.replace('http://192.168.0.106:3000/#/active')");
            } else if(item.getItemId() == R.id.nav_time){
                //runJS("window.location.replace('https://tmwott.top/server/#/profile')");
                runJS("window.location.replace('http://192.168.0.106:3000/#/profile')");

            } else if(item.getItemId() == R.id.nav_back){
                if(webview.canGoBack()){
                    webview.goBack();
                } else {
                    Utilities.showToast("No back page found");
                }
            } else if(item.getItemId() == R.id.nav_refresh){
                if(webview.getProgress() < 100){
                    webview.stopLoading();
                } else {
                    webview.reload();
                }
            } else if(item.getItemId() == R.id.nav_forward){
                if(webview.canGoForward()){
                    webview.goForward();
                } else {
                    Utilities.showToast("No forward page found");
                }
            }
            return false;
        });
        bottomNav.post(() -> bottomNav.getMenu().getItem(2).setChecked(true));

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true);

        webview.setWebViewClient(new MyWebViewClient());
        webview.setWebChromeClient(new MyWebChromeClient());
        webview.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> askForPermissionAndDownload(url, contentDisposition, mimetype));


        AdBlockUtilities.getFilter().setupWebView(webview);

        AdBlockUtilities.getFilterViewModel().getOnDirty().observe(this, aBoolean -> webview.clearCache(false));

        webview.addJavascriptInterface(new Utilities(), "App");

        webview.loadUrl(URL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(MainActivity.this, ApplicationService.class));
        } else {
            startService(new Intent(MainActivity.this, ApplicationService.class));
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if(isSupperBack){
            if(webview.canGoBack()) {
                webview.goBack();
            } else {
                showExitDialog();
            }
            isSupperBack = false;
            return;
        }
        runJS("try{ window.onBackPressed(); } catch (e) { window.App.onBackPressed(); }");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Utilities.isInternetAvailable()){
            Utilities.showNoInternetDialog();
            return;
        }
        if(webview != null) {
            webview.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(webview != null) {
            webview.onPause();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webview.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webview.restoreState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainActivity.this, ApplicationService.class));
    }

    private class MyWebViewClient extends WebViewClient {
        MyWebViewClient(){}

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //return request.getUrl().toString().contains("ak.jocauzee.net")
            boolean isOverride = false;
            for (String link : Utilities.getPrefs("override", "ak.jocauzee.net").split(",")) {
                if(request.getUrl().toString().contains(link)){
                    isOverride = true;
                    break;
                }
            }
            return isOverride;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            FilterResult result = AdBlockUtilities.getFilter().shouldIntercept(view, request);
            return result.getResourceResponse();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            AdBlockUtilities.getFilter().performScript(view, url);
            runJS();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(ProgressBar.GONE);
            refreshBtn.setIcon(R.drawable.ic_refresh);
            if (Utilities.isInternetAvailable()){
                Utilities.showNoInternetDialog();
                return;
            }
            runJS();
            Utilities.startApp();
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        private int mOriginalSystemUiVisibility;
        private final Handler handler = new Handler();
        private static final long interval = 3000;

        private final Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                handler.postDelayed(this, interval);
            }
        };

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            if(newProgress >= 90) runJS();

            if (newProgress == 100) {
                progressBar.setVisibility(ProgressBar.GONE);
                refreshBtn.setIcon(R.drawable.ic_refresh);
            } else {
                progressBar.setVisibility(ProgressBar.VISIBLE);
                refreshBtn.setIcon(R.drawable.ic_close);
            }
        }

        @SuppressLint("SourceLockedOrientationActivity")
        public void onHideCustomView() {
            handler.removeCallbacks(runnableCode);
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback)
        {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }

            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));


            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            handler.post(runnableCode);
        }
    }

    private void askForPermissionAndDownload(String url, String contentDisposition, String mimetype) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            String downloadDirectory = Environment.DIRECTORY_DOWNLOADS;

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            request.setTitle(fileName);
            request.setDescription("Downloading...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(downloadDirectory, fileName);

            DownloadManager downloadManager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);

            if (downloadManager != null) {
                downloadManager.enqueue(request);
            }

            Utilities.showToast("Please wait, downloading...");
        }
    }

    public static void runJS(String js){
        if(webview != null)
            webview.evaluateJavascript("try{" + js + "}catch(e){}",null);
    }

    public void runJS(){
        String js;
        String url = webview.getUrl();

        if (url != null && (url.equalsIgnoreCase(URL) || Utilities.getPrefs("urls", "").equalsIgnoreCase(URL))){
            return;
        }

        js = "try { document.getElementsByClassName('logo')[0].remove(); } catch(e) {}" +
                "try { document.getElementsByClassName('heading-archive')[0].remove(); } catch(e) {}" +
                "try { document.getElementsByClassName('comments-area')[0].remove(); } catch(e) {}" +
                "try { document.getElementById('st-2').remove(); } catch(e) {}" +
                "try { document.getElementById('logo').remove(); } catch(e) {}" +
                "try { document.getElementsByClassName('happy-under-player')[0].remove(); } catch(e) {}" +
                "try { document.getElementsByClassName('happy-under-player-mobile')[0].remove(); } catch(e) {}" +
                "try { document.getElementsByClassName('under-video-block')[0].remove(); } catch(e) {}" +
                "try { document.getElementsByTagName('aside')[0].remove(); } catch(e) {}" +
                "try { document.getElementsByTagName('footer')[0].remove(); } catch(e) {}";

        js = Utilities.getPrefs("js", js);

        if(webview != null)
            webview.evaluateJavascript("try{" + js + "}catch(e){}",null);
    }

    private void showExitDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.exit_dialog_title));
        builder.setMessage(getResources().getString(R.string.exit_dialog_descriptions));
        builder.setPositiveButton(getResources().getString(R.string.yes_btn), (dialog, which) -> finish());
        builder.setNegativeButton(getResources().getString(R.string.no_btn), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}