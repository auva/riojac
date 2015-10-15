package es.auva.android.riojamet;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class GraphActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Log.d("ExampleWidget", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //http://androidtrainningcenter.blogspot.in/2012/11/android-webview-loading-custom-html-and.html
        WebView webView = (WebView)findViewById(R.id.webView);



        webView.setWebViewClient(new WebViewClient());
        webView.setClickable(true);

        webView.setInitialScale(1);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        /**
         * Add Java Interface Class
         */
        webView.getSettings().setJavaScriptEnabled(true);
        JsInterface jsinterface = new JsInterface(this);
        jsinterface.setString(getExtraString());
        webView.addJavascriptInterface(jsinterface, "Android");
        // Log.d("ExampleWidget", "Checking extras");
        webView.loadUrl("file:///android_asset/graph_view.html");
    }

    private String getExtraString()
    {
        String newString;
        Bundle extras;
        extras = this.getIntent().getExtras();
        if(extras == null) {
            newString = "";
        } else
        {
            newString = extras.getString("variable_string");
        }

        return newString;
    }

    @Override
    public void onStop() {
        super.onStop();
        finish();
    }
}

