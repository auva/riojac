package es.auva.android.riojamet;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;

import com.luckycatlabs.sunrisesunset.dto.Location;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class JsInterface {
    private static final String LOG_TAG = "RiojaMet";
    private Context con;
    private String stringFromWidget = "Not initialized";

    public JsInterface(Context con) {
        this.con = con;
    }

    @JavascriptInterface
    public void setString(String newString) {
        stringFromWidget = newString;
    }

    @JavascriptInterface
    public String meteoDataJSON() {

        Log.d(LOG_TAG, "Android.meteoDataJSON-> " + stringFromWidget);
        return stringFromWidget;
    }
    @JavascriptInterface
    public String androidWindowSize() {
        WindowManager wm = (WindowManager) this.con.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        // Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        // since SDK_INT = 1;
        int widthPixels = metrics.widthPixels;
        int heightPixels;
        heightPixels = metrics.heightPixels;
        try {
            // used when 17 > SDK_INT >= 14; includes window decorations (statusbar bar/menu bar)
            widthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
            heightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
        } catch (Exception ignored) {
        }
        try {
            // used when SDK_INT >= 17; includes window decorations (statusbar bar/menu bar)
            Point realSize = new Point();
            Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
            widthPixels = realSize.x;
            heightPixels = realSize.y;
        } catch (Exception ignored) {
        }

        JSONObject tmp = new JSONObject();
        String jsonString;
        try {
            // Log.d("ExampleWidget", "Screen size (wxh): ("+widthPixels+"pix, "+heightPixels+"pix)");
            tmp.put("widthPixels", widthPixels);
            tmp.put("heightPixels", heightPixels);
            jsonString = tmp.toString();
        }
        catch(JSONException e){
            jsonString = new String();
            jsonString = "{\"witdh\": 300, \"height\": 300}";
        }
        return jsonString;

    }

    @JavascriptInterface
    public String androidSunrise()
    {
        Location location = new Location("42.245790", "-2.358500");
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, "Europe/Madrid");

        // Then call the method for the type of sunrise/sunset you want to calculate:

        String officialSunrise = calculator.getOfficialSunriseForDate(Calendar.getInstance());
       // Calendar officialSunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());
        return officialSunrise;
    }

    @JavascriptInterface
    public String androidSunset()
    {
        Location location = new Location("42.245790", "-2.358500");
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, "Europe/Madrid");

        // Then call the method for the type of sunrise/sunset you want to calculate:

        String officialSunset = calculator.getOfficialSunsetForDate(Calendar.getInstance());
        // Calendar officialSunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());
        return officialSunset;
    }
}
// Santa Marina 42.245790, -2.358500