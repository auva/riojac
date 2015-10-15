package es.auva.android.riojamet;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeteoData {

    static final Map<String, Integer> METEO_STATIONS_LA_RIOJA;
    static {
        Map<String, Integer> tmp = new LinkedHashMap<String, Integer>();

        tmp.put("Aguilar del Río Alhama", 12);
        tmp.put("Alfaro", 1352);
        tmp.put("Arnedo", 3);
        tmp.put("Calahorra", 13);
        tmp.put("Cervera del Río Alhama", 2366);
        tmp.put("Ezcaray", 1);
        tmp.put("Haro", 2);
        tmp.put("Logroño", 9);
        tmp.put("Moncalvillo", 11);
        tmp.put("Nájera", 4);
        tmp.put("Ocón", 7);
        tmp.put("San Román de Cameros", 6);
        tmp.put("Santa Marina", 1183);
        tmp.put("Santo Domingo de la Calzada", 2922);
        tmp.put("Torrecilla en Cameros", 888);
        tmp.put("Ventrosa", 10);
        tmp.put("Villoslada de Cameros", 5);
        tmp.put("Yerga", 8);

        METEO_STATIONS_LA_RIOJA = Collections.unmodifiableMap(tmp);
    }


    private static final String LOG_TAG = "ExampleWidget";

    private static final DateFormat df = new SimpleDateFormat("hh:mm:ss");

    protected int meteoStationId;
    protected String meteoStation;

    public String uri = null;
    public String meanTemp = null;
    public String minTemp = null;
    public String maxTemp = null;
    public String time = null;
    public String date = null;
    public Date measurement_time = null;

    public String arrayJSON = null;

    static private SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyyhh:mm:ss");


    //static String meteoUri="http://ias1.larioja.org/estaciones/estaciones/mapa/informes/resultados.jsp?codOrg=1&codigo=1183&codigoP=6&Seleccion=D&Ano=2015&Mes=1&DiaD=9&DiaH=9&FechaD=09-01-2015&FechaH=09-01-2015&HoraD=00:00:00&HoraH=23:59:59&forma=L";
    static String meteoUri="http://ias1.larioja.org/estaciones/estaciones/mapa/informes/resultados.jsp?codOrg=1&codigo=%d&codigoP=6&Seleccion=D&Ano=%d&Mes=%d&DiaD=%d&DiaH=%d&FechaD=%d-%d-%d&FechaH=%d-%d-%d&HoraD=%s&HoraH=%s&forma=L";
    // year, month, day, day, day, month, year, time_start, time_end

    public MeteoData(String _meteoStation)
    {
        meteoStation = _meteoStation;
        meteoStationId = METEO_STATIONS_LA_RIOJA.get(meteoStation);

    }
    public static InputStream recoverMeteoStream(String meteoUriRequest) {
        try {
            HttpResponse response;
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(meteoUriRequest));
            response = client.execute(request);
            // Log.d(LOG_TAG, "Meteo inputStream recovered.");
            return response.getEntity().getContent();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Log.d(LOG_TAG, "Meteo inputStream failed.");
        return null;
    }


    public static String convertStreamToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();
            int maxLen = 1024;

            char[] buffer = new char[maxLen];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),maxLen);
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    // private static String temps_reg_exp = "<tr class=\"papelpijamaclaro\">\\s*<td align=\"center\"><font (?:color=\"\\#\\d{6}\")?>(\\d\\d\\-\\d\\d\\-\\d\\d\\d\\d)\\s*<\\/font><\\/td>\\s*<td width=\"1\" bgcolor=\"white\">\\&nbsp;<\\/td>\\s*<td align=\"center\"><font (?:color=\"\\#\\d{6}\")?>(\\d\\d\\:\\d\\d\\:\\d\\d)<\\/font><\\/td>\\s*(?:<td width=\"1\" bgcolor=\"white\">&nbsp;<\\/td>\\s*<td align=\"center\">\\s*(\\d\\d?,\\d)\\s*<\\/td>\\s*){3}\\s*<\\/tr>";
    // time_reg_exp = "\\s*<font (?:color=\"#\\d{6}\")>(\\d\\d\\-\\d\\d\\-\\d\\d\\d\\d)</font></\\w*>[\\s.]+?(\\d\\d:\\d\\d:\\d\\d)";

    //"<font color="#990000">09-01-2015</font></td>"
    private static String time_reg_exp =
            "\\s*<font (?:color=\"#\\d{6}\")>(\\d\\d\\-\\d\\d\\-\\d\\d\\d\\d)" +  // Date
             "[.\\S\\s]+?(\\d\\d:\\d\\d:\\d\\d)" +    // Time
            "(?:[.\\S\\s]+?(-?\\d\\d?,\\d\\d?))" +    // Mean temp
            "(?:[.\\S\\s]+?(-?\\d\\d?,\\d\\d?))" +    // Max temp
            "(?:[.\\S\\s]+?(-?\\d\\d?,\\d\\d?))";     // Min temp

    private static Pattern p = Pattern.compile(time_reg_exp);

    public Boolean recoverMeteoData(Date now)
    {
        Boolean ok = false;
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH)+1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        String meteoData = "";
        InputStream meteoStream = null;
        // year, month, day, day, day, month, year, time_start, time_end
        String time_start = "00:00:00";
        String time_end = "23:59:59";
        //&Ano=%d&Mes=%d&DiaD=%d&DiaH=%d&FechaD=%d-%d-%d&FechaH=%d-%d-%d&HoraD=%s&HoraH=%s&forma=L";

        String uri = String.format(meteoUri,
                meteoStationId,
                year, month, day, day,
                day, month, year,
                day, month, year,
                time_start, time_end);
        // Log.d(LOG_TAG, uri);

        try {
            meteoStream = recoverMeteoStream(uri);
            if (meteoStream != null)
                meteoData = convertStreamToString(meteoStream);
        }
        catch (IOException e) {
            e.printStackTrace();
            // Log.d(LOG_TAG,"Error parsing meteoStream");
        }
        // Log.d(LOG_TAG, "Starting to parse the data string.");

        Matcher m = p.matcher(meteoData);
        String date_str = null;
        String time_str = null;
        String mean_str = null;
        String max_str = null;
        String min_str = null;
        // Log.d(LOG_TAG, "After matcher was built.");
        String jsonString = "";
        JSONArray arr = new JSONArray();
        JSONObject tmp;
        try {

            while (m.find()) { // Find each match in turn; String can't do this.
                // #1 date, #2 time, #3 mean #4 max #5 min
                // Log.d(LOG_TAG, String.format("Found one match with %d groups.", m.groupCount()));
                tmp = new JSONObject();

                date_str = m.group(1);
                time_str = m.group(2);
                mean_str = m.group(3);
                max_str = m.group(4);
                min_str = m.group(5);
                Date measurementTime;
                try {
                    measurementTime = formatter.parse(date_str + time_str);
                }catch (ParseException e)
                {
                    e.printStackTrace();
                    measurementTime = new Date();
                }

                tmp.put("date_time", measurementTime);
                tmp.put("date", date_str);
                tmp.put("time", time_str);
                tmp.put("tempMean", mean_str);
                tmp.put("tempMax", max_str);
                tmp.put("tempMin",min_str);
                tmp.put("location", meteoStation);

                arr.put(tmp);


                // Log.d(LOG_TAG, String.format("Group: %s %s %s° %s° %s°", date_str,
    //                time_str,
    //                mean_str,
    //                max_str,
    //                min_str));
            }
            jsonString = arr.toString();
        } catch(JSONException e){
                jsonString = "JSON error";
        }

        if (time_str != null)
        {
            // Log.d(LOG_TAG, "Creating MeteoData instance.");

            ok = true;
            maxTemp = max_str;
            meanTemp = mean_str;
            minTemp = min_str;
            date = date_str;
            time = time_str;
            arrayJSON = jsonString;

            try {
                measurement_time = formatter.parse(date_str + time_str);
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }

        }
        //else
            // Log.d(LOG_TAG, "No MeteoData available.");
        return ok;
    }
}
