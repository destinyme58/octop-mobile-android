package com.octop.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String HOME_PATH = "/chat";
    private static final String MOBILE_USER_AGENT = "OctopMobile/" + BuildConfig.VERSION_NAME;

    private MaterialToolbar toolbar;
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private View loadingOverlay;
    private View errorContainer;
    private TextView errorTitle;
    private TextView errorMessage;
    private BottomNavigationView bottomNavigation;

    private AppPreferences preferences;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean networkAvailable = true;
    private boolean firstPageLoaded;
    private boolean pageFailed;
    private long lastBackPressAt;

    private ValueCallback<Uri[]> filePathCallback;
    private PermissionRequest pendingPermissionRequest;
    private Dialog popupDialog;
    private WebView popupWebView;

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (filePathCallback == null) {
                            return;
                        }
                        Uri[] selected = null;
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            ClipData clipData = data.getClipData();
                            if (clipData != null) {
                                selected = new Uri[clipData.getItemCount()];
                                for (int index = 0; index < clipData.getItemCount(); index++) {
                                    selected[index] = clipData.getItemAt(index).getUri();
                                }
                            } else if (data.getData() != null) {
                                selected = new Uri[]{data.getData()};
                            }
                        }
                        filePathCallback.onReceiveValue(selected);
                        filePathCallback = null;
                    });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    granted -> {
                        PermissionRequest request = pendingPermissionRequest;
                        pendingPermissionRequest = null;
                        if (request == null) {
                            return;
                        }

                        List<String> approved = new ArrayList<>();
                        for (String resource : request.getResources()) {
                            String permission = permissionForResource(resource);
                            if (permission == null
                                    || ContextCompat.checkSelfPermission(this, permission)
                                    == PackageManager.PERMISSION_GRANTED) {
                                approved.add(resource);
                            }
                        }

                        if (approved.isEmpty()) {
                            request.deny();
                            Snackbar.make(
                                    webView,
                                    R.string.permission_needed,
                                    Snackbar.LENGTH_LONG
                            ).setAnchorView(bottomNavigation).show();
                        } else {
                            request.grant(approved.toArray(new String[0]));
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new AppPreferences(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        applyWindowInsets(findViewById(R.id.root_container));
        applySystemBarAppearance();

        toolbar = findViewById(R.id.toolbar);
        webView = findViewById(R.id.web_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        loadingOverlay = findViewById(R.id.loading_overlay);
        errorContainer = findViewById(R.id.error_container);
        errorTitle = findViewById(R.id.error_title);
        errorMessage = findViewById(R.id.error_message);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        setupToolbar();
        setupWebView();
        setupSwipeRefresh();
        setupBottomNavigation();
        setupErrorActions();
        setupBackNavigation();
        registerNetworkMonitor();
        loadHome();
    }

    private void applyWindowInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void applySystemBarAppearance() {
        boolean lightBars = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                != android.content.res.Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        );
        controller.setAppearanceLightStatusBars(lightBars);
        controller.setAppearanceLightNavigationBars(lightBars);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setLogo(R.drawable.octop_toolbar_logo);
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(ServerUrl.hostLabel(preferences.getServerUrl()));
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setTitleTextAppearance(this, R.style.TextAppearance_Octop_Toolbar);
        toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        toolbar.setNavigationOnClickListener(view -> openServerSettings());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.octop_red),
                ContextCompat.getColor(this, R.color.octop_red_dark)
        );
        swipeRefresh.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(this, R.color.surface)
        );
        swipeRefresh.setOnRefreshListener(() -> {
            if (webView.getUrl() == null) {
                loadHome();
            } else {
                webView.reload();
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setUserAgentString(settings.getUserAgentString() + " " + MOBILE_USER_AGENT);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.setWebViewClient(new OctopWebViewClient());
        webView.setWebChromeClient(new OctopChromeClient());
        webView.setDownloadListener(this::startDownload);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                navigateTo("/chat");
                return true;
            }
            if (itemId == R.id.nav_experts) {
                navigateTo("/experts");
                return true;
            }
            if (itemId == R.id.nav_tasks) {
                navigateTo("/tasks");
                return true;
            }
            if (itemId == R.id.nav_more) {
                showMoreSheet();
                return true;
            }
            return false;
        });
    }

    private void setupErrorActions() {
        findViewById(R.id.retry_button).setOnClickListener(view -> {
            errorContainer.setVisibility(View.GONE);
            loadingOverlay.setVisibility(View.VISIBLE);
            if (webView.getUrl() == null) {
                loadHome();
            } else {
                webView.reload();
            }
        });
        findViewById(R.id.error_settings_button).setOnClickListener(
                view -> openServerSettings()
        );
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (popupDialog != null && popupDialog.isShowing()) {
                    popupDialog.dismiss();
                    return;
                }
                if (webView.canGoBack()) {
                    webView.goBack();
                    return;
                }

                String path = ServerUrl.pathOf(webView.getUrl());
                if (!NavigationRoute.isHome(path)) {
                    navigateTo(HOME_PATH);
                    return;
                }

                long now = System.currentTimeMillis();
                if (now - lastBackPressAt < 2200) {
                    finish();
                    return;
                }
                lastBackPressAt = now;
                Snackbar.make(
                        webView,
                        R.string.press_again_exit,
                        Snackbar.LENGTH_SHORT
                ).setAnchorView(bottomNavigation).show();
            }
        });
    }

    private void loadHome() {
        pageFailed = false;
        firstPageLoaded = false;
        errorContainer.setVisibility(View.GONE);
        loadingOverlay.setVisibility(View.VISIBLE);
        navigateTo(HOME_PATH);
    }

    private void navigateTo(String path) {
        String url = preferences.getServerUrl() + path;
        webView.loadUrl(url);
    }

    private void updateChromeForUrl(String url) {
        String path = ServerUrl.pathOf(url);
        boolean hidePrimaryNavigation = NavigationRoute.hidesPrimaryNavigation(path);
        bottomNavigation.setVisibility(
                hidePrimaryNavigation ? View.GONE : View.VISIBLE
        );

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            String subtitle = networkAvailable
                    ? ServerUrl.hostLabel(preferences.getServerUrl())
                    : getString(R.string.connection_offline);
            actionBar.setSubtitle(subtitle);
        }

        if (hidePrimaryNavigation) {
            return;
        }
        int menuId;
        switch (NavigationRoute.tabFor(path)) {
            case CHAT:
                menuId = R.id.nav_chat;
                break;
            case EXPERTS:
                menuId = R.id.nav_experts;
                break;
            case TASKS:
                menuId = R.id.nav_tasks;
                break;
            default:
                menuId = R.id.nav_more;
                break;
        }
        MenuItem item = bottomNavigation.getMenu().findItem(menuId);
        if (item != null) {
            item.setChecked(true);
        }
    }

    private void showLoadingForPage() {
        if (!firstPageLoaded) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void finishPageLoad() {
        firstPageLoaded = true;
        swipeRefresh.setRefreshing(false);
        loadingOverlay.setVisibility(View.GONE);
        if (!pageFailed) {
            errorContainer.setVisibility(View.GONE);
        }
        updateChromeForUrl(webView.getUrl());
    }

    private void showPageError(String message) {
        pageFailed = true;
        firstPageLoaded = true;
        swipeRefresh.setRefreshing(false);
        loadingOverlay.setVisibility(View.GONE);
        errorTitle.setText(R.string.error_title);
        errorMessage.setText(message);
        errorContainer.setVisibility(View.VISIBLE);
    }

    private void showHttpError(int statusCode) {
        if (statusCode >= 500) {
            showPageError(getString(R.string.error_server));
        }
    }

    private void showMoreSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = getLayoutInflater().inflate(
                R.layout.dialog_more_sheet,
                bottomNavigation,
                false
        );
        dialog.setContentView(content);

        content.findViewById(R.id.more_personalization).setOnClickListener(view -> {
            dialog.dismiss();
            navigateTo("/personalization");
        });
        content.findViewById(R.id.more_knowledge).setOnClickListener(view -> {
            dialog.dismiss();
            navigateTo("/knowledge-bases");
        });
        content.findViewById(R.id.more_workbench).setOnClickListener(view -> {
            dialog.dismiss();
            navigateTo("/workbench");
        });
        content.findViewById(R.id.more_usage).setOnClickListener(view -> {
            dialog.dismiss();
            navigateTo("/token-usage");
        });
        dialog.show();
    }

    private void openServerSettings() {
        View content = getLayoutInflater().inflate(
                R.layout.dialog_server_settings,
                null,
                false
        );
        TextInputLayout inputLayout = content.findViewById(R.id.server_url_layout);
        TextInputEditText input = content.findViewById(R.id.server_url_input);
        input.setText(preferences.getServerUrl());
        input.setSelection(input.length());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.server_settings)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    try {
                        String normalized = ServerUrl.normalize(String.valueOf(
                                input.getText()
                        ));
                        inputLayout.setError(null);
                        preferences.setServerUrl(normalized);
                        dialog.dismiss();
                        loadHome();
                    } catch (IllegalArgumentException exception) {
                        inputLayout.setError(getString(R.string.server_url_invalid));
                    }
                }));
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void openCurrentPageInBrowser() {
        String url = webView.getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException exception) {
            Snackbar.make(
                    webView,
                    R.string.open_external_failed,
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    private void shareCurrentPage() {
        String url = webView.getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }
        try {
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(share, getString(R.string.menu_share)));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showClearSessionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_session_title)
                .setMessage(R.string.clear_session_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear, (dialog, which) -> clearSession())
                .show();
    }

    private void clearSession() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        WebStorage.getInstance().deleteAllData();
        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();
        loadHome();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_message, BuildConfig.VERSION_NAME))
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void startDownload(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType,
            long contentLength
    ) {
        if (url == null || url.startsWith("blob:") || url.startsWith("data:")) {
            Snackbar.make(
                    webView,
                    R.string.download_blob_unsupported,
                    Snackbar.LENGTH_LONG
            ).setAnchorView(bottomNavigation).show();
            return;
        }

        try {
            String guessedName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            File directory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (directory == null) {
                throw new IllegalStateException("Download directory is unavailable");
            }
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException("Cannot create download directory");
            }

            File destination = uniqueFile(directory, guessedName);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDestinationUri(Uri.fromFile(destination));
            request.setTitle(destination.getName());
            request.setDescription(getString(R.string.app_name));
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            if (!TextUtils.isEmpty(mimeType)) {
                request.setMimeType(mimeType);
            }
            if (!TextUtils.isEmpty(userAgent)) {
                request.addRequestHeader("User-Agent", userAgent);
            }
            if (!TextUtils.isEmpty(webView.getUrl())) {
                request.addRequestHeader("Referer", webView.getUrl());
            }
            String cookies = CookieManager.getInstance().getCookie(url);
            if (!TextUtils.isEmpty(cookies)) {
                request.addRequestHeader("Cookie", cookies);
            }

            DownloadManager manager = (DownloadManager) getSystemService(
                    Context.DOWNLOAD_SERVICE
            );
            manager.enqueue(request);
            Snackbar.make(
                    webView,
                    getString(R.string.download_started, destination.getName()),
                    Snackbar.LENGTH_LONG
            ).setAnchorView(bottomNavigation).show();
        } catch (RuntimeException exception) {
            Snackbar.make(
                    webView,
                    R.string.download_failed,
                    Snackbar.LENGTH_LONG
            ).setAnchorView(bottomNavigation).show();
        }
    }

    private File uniqueFile(File directory, String fileName) {
        File candidate = new File(directory, fileName);
        if (!candidate.exists()) {
            return candidate;
        }

        int dot = fileName.lastIndexOf('.');
        String stem = dot <= 0 ? fileName : fileName.substring(0, dot);
        String extension = dot <= 0 ? "" : fileName.substring(dot);
        int index = 1;
        while (candidate.exists()) {
            candidate = new File(directory, stem + "-" + index + extension);
            index += 1;
        }
        return candidate;
    }

    private boolean handleExternalScheme(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        if (scheme.equals("http")
                || scheme.equals("https")
                || scheme.equals("about")
                || scheme.equals("javascript")
                || scheme.equals("blob")
                || scheme.equals("data")) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException exception) {
            Snackbar.make(
                    webView,
                    R.string.open_external_failed,
                    Snackbar.LENGTH_LONG
            ).show();
        }
        return true;
    }

    private void registerNetworkMonitor() {
        connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        networkAvailable = hasNetwork(connectivityManager);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    networkAvailable = true;
                    updateChromeForUrl(webView.getUrl());
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    networkAvailable = hasNetwork(connectivityManager);
                    updateChromeForUrl(webView.getUrl());
                });
            }
        };
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (RuntimeException ignored) {
            // The connection still works if status monitoring is unavailable.
        }
    }

    private boolean hasNetwork(ConnectivityManager manager) {
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(
                manager.getActiveNetwork()
        );
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private String permissionForResource(String resource) {
        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
            return Manifest.permission.CAMERA;
        }
        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
            return Manifest.permission.RECORD_AUDIO;
        }
        return null;
    }

    private void configurePopupWebView(WebView target) {
        WebSettings settings = target.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " " + MOBILE_USER_AGENT);
        CookieManager.getInstance().setAcceptThirdPartyCookies(target, true);
        target.setBackgroundColor(ContextCompat.getColor(this, R.color.surface));
    }

    private void showPopupWindow(Message resultMessage) {
        View content = getLayoutInflater().inflate(
                R.layout.dialog_web_popup,
                webView,
                false
        );
        applyWindowInsets(content.findViewById(R.id.popup_container));
        MaterialToolbar popupToolbar = content.findViewById(R.id.popup_toolbar);
        popupWebView = content.findViewById(R.id.popup_web_view);
        configurePopupWebView(popupWebView);

        popupDialog = new Dialog(this);
        popupDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        popupDialog.setContentView(content);
        popupDialog.setCancelable(true);
        popupToolbar.setNavigationOnClickListener(view -> popupDialog.dismiss());
        popupDialog.setOnDismissListener(ignored -> {
            if (popupWebView != null) {
                popupWebView.loadUrl("about:blank");
                popupWebView.destroy();
            }
            popupWebView = null;
            popupDialog = null;
        });

        popupWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                return handleExternalScheme(request.getUrl());
            }
        });
        popupWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onCloseWindow(WebView window) {
                if (popupDialog != null) {
                    popupDialog.dismiss();
                }
            }
        });

        Window window = popupDialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMessage.obj;
        transport.setWebView(popupWebView);
        resultMessage.sendToTarget();
        popupDialog.show();
        window = popupDialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            errorContainer.setVisibility(View.GONE);
            if (webView.getUrl() == null) {
                loadHome();
            } else {
                webView.reload();
            }
            return true;
        }
        if (itemId == R.id.action_server) {
            openServerSettings();
            return true;
        }
        if (itemId == R.id.action_share) {
            shareCurrentPage();
            return true;
        }
        if (itemId == R.id.action_open_browser) {
            openCurrentPageInBrowser();
            return true;
        }
        if (itemId == R.id.action_clear_session) {
            showClearSessionDialog();
            return true;
        }
        if (itemId == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (networkCallback != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (RuntimeException ignored) {
                // Callback may already be detached by the system.
            }
        }
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (popupWebView != null) {
            popupWebView.destroy();
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }

    private final class OctopWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request
        ) {
            return handleExternalScheme(request.getUrl());
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            pageFailed = false;
            errorContainer.setVisibility(View.GONE);
            showLoadingForPage();
            updateChromeForUrl(url);
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            super.onPageCommitVisible(view, url);
            loadingOverlay.setVisibility(View.GONE);
            errorContainer.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            finishPageLoad();
        }

        @Override
        public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error
        ) {
            super.onReceivedError(view, request, error);
            if (!request.isForMainFrame()) {
                return;
            }
            showPageError(getString(R.string.error_network));
        }

        @Override
        public void onReceivedHttpError(
                WebView view,
                WebResourceRequest request,
                WebResourceResponse errorResponse
        ) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (request.isForMainFrame()) {
                showHttpError(errorResponse.getStatusCode());
            }
        }

        @Override
        public void onReceivedSslError(
                WebView view,
                SslErrorHandler handler,
                android.net.http.SslError error
        ) {
            handler.cancel();
            showPageError(getString(R.string.error_network));
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            updateChromeForUrl(url);
        }
    }

    private final class OctopChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress >= 100) {
                swipeRefresh.setRefreshing(false);
            } else if (!firstPageLoaded) {
                loadingOverlay.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
        ) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
            }
            filePathCallback = callback;

            Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            String[] accepted = params.getAcceptTypes();
            if (accepted != null && accepted.length > 0) {
                List<String> mimeTypes = new ArrayList<>();
                for (String mimeType : accepted) {
                    if (!TextUtils.isEmpty(mimeType) && !mimeTypes.contains(mimeType)) {
                        mimeTypes.add(mimeType);
                    }
                }
                if (!mimeTypes.isEmpty()) {
                    picker.setType(mimeTypes.get(0));
                    if (mimeTypes.size() > 1) {
                        picker.putExtra(
                                Intent.EXTRA_MIME_TYPES,
                                mimeTypes.toArray(new String[0])
                        );
                    }
                }
            }

            try {
                fileChooserLauncher.launch(Intent.createChooser(
                        picker,
                        getString(R.string.page_loading)
                ));
            } catch (ActivityNotFoundException exception) {
                filePathCallback.onReceiveValue(null);
                filePathCallback = null;
                Snackbar.make(
                        webView,
                        R.string.open_external_failed,
                        Snackbar.LENGTH_LONG
                ).show();
            }
            return true;
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            List<String> missing = new ArrayList<>();
            for (String resource : request.getResources()) {
                String permission = permissionForResource(resource);
                if (permission != null
                        && ContextCompat.checkSelfPermission(
                        MainActivity.this,
                        permission
                ) != PackageManager.PERMISSION_GRANTED) {
                    missing.add(permission);
                }
            }

            if (missing.isEmpty()) {
                request.grant(request.getResources());
            } else {
                pendingPermissionRequest = request;
                permissionLauncher.launch(missing.toArray(new String[0]));
            }
        }

        @Override
        public boolean onCreateWindow(
                WebView view,
                boolean isDialog,
                boolean isUserGesture,
                Message resultMsg
        ) {
            if (popupDialog != null && popupDialog.isShowing()) {
                return false;
            }
            showPopupWindow(resultMsg);
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (popupDialog != null) {
                popupDialog.dismiss();
            }
        }
    }
}
