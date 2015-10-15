
package es.auva.android.riojamet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Math.max;


public class RiojaCWidgetProvider extends AppWidgetProvider {
	private static final String LOG_TAG = "RiojaMet";
    private static final String prefLastSuccessfulUpdate = "LastSuccessfulUpdate";
    private static final String prefLastDataJSON = "LastDataJSON";

	private static final DateFormat df = new SimpleDateFormat("hh:mm:ss");
    /// When connection is OK, next connection try at 15 min.
    private static final int measurementIntervalMillis = 15*60*1000;
    /// When connection is KO, next connection try at 1.5 min.
    private static final int updateIntervalMillis = 90*1000;

 	/**
	 * Custom Intent name that is used by the AlarmManager to tell us to update the widget periodically.
	 */
	public static String CLOCK_WIDGET_UPDATE = "es.auva.android.riojamet.RIOJAMET_WIDGET_UPDATE";

    public Date lastConnectionTry = null;
    private MeteoData lastMeasurementData = null;
    private static Set<Integer> updatingWidgetIds = new HashSet<Integer>();

    public class MeteoDataUpdater extends AsyncTask<String, Void, MeteoData> {

        private RemoteViews views;
        private int widgetID;
        private AppWidgetManager widgetManager;
        private Context context;

        public MeteoDataUpdater(Context _context,
                                RemoteViews _views, int appWidgetID, AppWidgetManager appWidgetManager){
            this.context = _context;
            this.views = _views;
            this.widgetID = appWidgetID;
            this.widgetManager = appWidgetManager;
            updatingWidgetIds.add(widgetID);
            this.views.setImageViewResource(R.id.imageButton, R.drawable.ic_refresh_grey600_48dp);
            widgetManager.updateAppWidget(widgetID, views);
        }


        public Boolean recoverMeteoData(String meteoStation) {

            lastConnectionTry = new Date();
            String currentTime = df.format(lastConnectionTry);

            Log.d(LOG_TAG, "Trying connection at " + currentTime);

            MeteoData lastMeas = new MeteoData(meteoStation);

            if (lastMeas.recoverMeteoData(lastConnectionTry)) {
                Log.d(LOG_TAG, "Connection successful!");
                SetLastSuccessfulUpdate(context, lastConnectionTry);
                lastMeasurementData = lastMeas;
                Log.d(LOG_TAG, "Connection OK @ " + lastConnectionTry.toString());
                return true;
            }
            Log.d(LOG_TAG, "Connection KO");
            return false;
        }


        @Override
        protected MeteoData doInBackground(String... meteoStation) {
            try {

                if (recoverMeteoData(meteoStation[0]))
                    return lastMeasurementData;
                else
                    return null;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Falló la descarga de datos: " + e.getMessage());
            }
            return null;
        }

        //.....
        public void onPostExecute(MeteoData _lastMeasurementData){
            try {
                if (_lastMeasurementData != null) {
                    // Set temp
                    views.setTextViewText(R.id.tempTextView, _lastMeasurementData.meanTemp + "°");
                    views.setTextViewText(R.id.meteoStationTextView, _lastMeasurementData.meteoStation);
                    // Remove seconds from last time and set them
                    int last_colon = max(_lastMeasurementData.time.length()-3,-1);
                    String time_wo_seconds = _lastMeasurementData.time.substring(0, last_colon);
                    views.setTextViewText(R.id.timeTextView, time_wo_seconds);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putString(prefLastDataJSON, _lastMeasurementData.arrayJSON);
                    ed.apply();
                }
                views.setImageViewResource(R.id.imageButton, R.drawable.ic_refresh_white_48dp);
                // Update the widget view
                widgetManager.updateAppWidget(widgetID, views);

            }
            catch (Exception e)
            {
                Log.e(LOG_TAG, "Falló la actualización del widget: " + e.getMessage());
            }
            updatingWidgetIds.remove(Integer.valueOf(widgetID));

        }
    }





    @Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		if (CLOCK_WIDGET_UPDATE.equals(intent.getAction())) {
			Log.d(LOG_TAG, "Clock update");
			// Get the widget manager and ids for this widget provider, then call the shared
			// clock update method.
			ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
		    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		    int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            for (int appWidgetID: ids) {
				updateAppWidget(context, appWidgetID, appWidgetManager);
		    	
		    }
		}
	}


    private Date GetLastSuccessfulUpdate(Context context) {
        Date lastSuccessfulUpdate = null;
        //SharedPreferences sharedPref = context.getSharedPreferences("MyVariables", Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        try{
            if (sharedPref != null){
                ISO8601DateParser parser = new ISO8601DateParser();
                Log.d(LOG_TAG, "Get"+ prefLastSuccessfulUpdate + ": " + sharedPref.getString(prefLastSuccessfulUpdate, "No data"));
                lastSuccessfulUpdate = parser.parse(sharedPref.getString(prefLastSuccessfulUpdate, ""));
            }
        }catch(Exception e){
            Log.e(LOG_TAG, "No se pudo recuperar la última fecha de descarga: " + e.getMessage());
        }
        return lastSuccessfulUpdate;

    }

    private void SetLastSuccessfulUpdate(Context context, Date lastUpdate) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        try{
            if (sharedPref != null){
                ISO8601DateParser parser = new ISO8601DateParser();
                SharedPreferences.Editor ed = sharedPref.edit();
                Log.d(LOG_TAG, "Set" + prefLastSuccessfulUpdate + ": " +  parser.toString(lastUpdate));
                ed.putString(prefLastSuccessfulUpdate, parser.toString(lastUpdate));
                ed.apply();
            }
        }catch(Exception e){
            Log.e(LOG_TAG, "No se pudo almacenar la última fecha de descarga: " + e.getMessage());
        }
    }

	private PendingIntent createClockTickIntent(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        Intent intent = new Intent(CLOCK_WIDGET_UPDATE);
        // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
	}
	
	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		Log.d(LOG_TAG, "Widget Provider disabled. Turning off timer");
    	AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createClockTickIntent(context));
        updatingWidgetIds.clear();
	}

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for(int appWidgetId: appWidgetIds)
        {
            updatingWidgetIds.remove(Integer.valueOf(appWidgetId));
        }
        if (updatingWidgetIds.isEmpty()) {
            Log.d(LOG_TAG, "Widget Provider deleted. Turning off timer");
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(createClockTickIntent(context));
        }
        super.onDeleted(context, appWidgetIds);
    }


	@Override 
	public void onEnabled(Context context) {
		super.onEnabled(context);

        Log.d(LOG_TAG, String.format(
                "Widget Provider enabled. Starting timer to update widget every %d milliseconds.",
                updateIntervalMillis));
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MILLISECOND, 100);

        alarmManager.setRepeating(AlarmManager.RTC,
                calendar.getTimeInMillis(),
                updateIntervalMillis,
                createClockTickIntent(context));

	}


	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// final int N = appWidgetIds.length;
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d(LOG_TAG, "Call to onUpdate.");
        SetLastSuccessfulUpdate(context, null);
        for (int appWidgetId : appWidgetIds) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget1);

            // Create an Intent to launch AboutActivity (general info from button)
            Intent aboutIntent = new Intent(context, AboutActivity.class);
                        // FLAG_UPDATE_CURRENT is important to get any previously existing intent updated.
            aboutIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            aboutIntent.setData(Uri.parse(aboutIntent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent pendingAboutIntent = PendingIntent.getActivity(context, 0, aboutIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            views.setOnClickPendingIntent(R.id.logoButton, pendingAboutIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);



            // Create an Intent to launch ExampleActivity (the meteo plot)
            Intent graphIntent = new Intent(context, GraphActivity.class);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String lastDataJSON = prefs.getString(prefLastDataJSON, "");
            // FLAG_UPDATE_CURRENT is important to get any previously existing intent updated.
            graphIntent.putExtra("variable_string", lastDataJSON);
            graphIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            graphIntent.setData(Uri.parse(graphIntent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent pendingGraphIntent = PendingIntent.getActivity(context, 0, graphIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            views.setOnClickPendingIntent(R.id.button, pendingGraphIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);

            // Create an Intent to force the widget's update

            Intent updateIntent = new Intent(context, RiojaCWidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            updateIntent.setData(Uri.parse(updateIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pendingUpdateIntent = PendingIntent.getBroadcast(context,
                    0, updateIntent, 0);

            views.setOnClickPendingIntent(R.id.imageButton, pendingUpdateIntent);

            // Create an Intent to modify the widget's settings
            Intent settingsIntent = new Intent(context, SettingsActivity.class);
            settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            settingsIntent.setData(Uri.parse(settingsIntent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent pendingSettingsIntent = PendingIntent.getActivity(context, 0, settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.settingsButton, pendingSettingsIntent);



            appWidgetManager.updateAppWidget(appWidgetId, views);

            updateAppWidget(context, appWidgetId, appWidgetManager);
        }
	}

    /// Launches async task to download meteo data
    public void updateAppWidget(Context context, int appWidgetId, AppWidgetManager appWidgetManager) {

        if(!updatingWidgetIds.contains(new Integer(appWidgetId))) {

            Date lastSuccessfulUpdate = GetLastSuccessfulUpdate(context);
            Boolean update = (lastSuccessfulUpdate == null);
            if (!update) {
                Date now = new Date();
                Log.d(LOG_TAG, "Check time after lastSuccessfulUpdate");
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(lastSuccessfulUpdate);
                calendar.add(Calendar.MILLISECOND, max(measurementIntervalMillis - updateIntervalMillis / 2, 60000));

                update = now.after(calendar.getTime());
                Log.d(LOG_TAG, "Now: " + now.toString());
                Log.d(LOG_TAG, "Time to update: " + calendar.toString());
                Log.d(LOG_TAG, "Update?: " + update);

            } else {
                Log.d(LOG_TAG,
                        "No successfulUpdate yet, we check for new data. ");
            }

            if (update) {
                // Check last connection time and update
                Log.d(LOG_TAG, "Launching MeteoDataUpdater for widgetID " + appWidgetId);
                RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget1);
                MeteoDataUpdater updater = new MeteoDataUpdater(context, updateViews, appWidgetId, appWidgetManager);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String meteoStation = prefs.getString("meteo_list", "Logroño");
                updater.execute(meteoStation);
            } else
                Log.d(LOG_TAG, "Waiting to launch MeteoDataUpdater later");
      }
      else
           Log.d(LOG_TAG, "Process " + appWidgetId + " already being updated, we skip it.");
    }

}
