package es.auva.android.riojamet;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AboutActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Log.d("ExampleWidget", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        WebView webView = (WebView)findViewById(R.id.webViewAbout);

        webView.setWebViewClient(new WebViewClient());
        webView.setClickable(true);

        webView.loadUrl("file:///android_asset/about.html");
    }


    @Override
    public void onStop() {
        super.onStop();
        finish();
    }
}

