package com.nitramite.apcupsdmonitor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@SuppressWarnings("FieldCanBeLocal")
public class UpsViewer extends AppCompatActivity implements ConnectorInterface {

    // Logging
    private final static String TAG = "UpsViewer";

    // Views
    private ProgressBar progressBar;
    private LinearLayout upsDetailsView, upsStatisticsView;
    private CardView eventsCardView;
    private TextView statisticsNotEnoughData;

    // Chart
    private BarChart chart;
    private BarData barData;
    private BarDataSet barDataSet;
    private ArrayList<BarEntry> barEntries;
    private ArrayList<String> barLabels;

    // Variables
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);
    private String upsId = null;
    private SharedPreferences sharedPreferences;
    private ArrayList<String> eventsArrayList = null;
    private String rawStatusOutput = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ups_viewer);

        // Shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // New or existing package
        Intent intent = getIntent();
        upsId = intent.getStringExtra("UPS_ID");

        if (upsId == null) {
            Toast.makeText(this, "Error, provided UPS id was undefined!", Toast.LENGTH_SHORT).show();
            UpsViewer.this.finish();
        }


        // Find views and set defaults
        upsDetailsView = findViewById(R.id.upsDetailsView);
        upsDetailsView.setVisibility(View.VISIBLE);
        eventsCardView = findViewById(R.id.eventsCardView);
        eventsCardView.setVisibility(View.VISIBLE);
        upsStatisticsView = findViewById(R.id.upsStatisticsView);
        upsStatisticsView.setVisibility(View.GONE);
        chart = findViewById(R.id.chart);
        chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        statisticsNotEnoughData = findViewById(R.id.statisticsNotEnoughData);
        statisticsNotEnoughData.setVisibility(View.GONE);



        Button returnFromStatisticsBtn = findViewById(R.id.returnFromStatisticsBtn);
        returnFromStatisticsBtn.setOnClickListener(view -> toggleStatisticsMode());

        getUpsData();
    } // End of onCreate()


    private void getUpsData() {
        try {
            UPS ups = databaseHelper.getAllUps(upsId).get(0);
            drawData(ups, ups.getUPS_STATUS_STR());
            rawStatusOutput = ups.getUPS_STATUS_STR();
            eventsArrayList = databaseHelper.getAllEvents(upsId);
            drawEventsData(ups.getUpsLoadEvents(), eventsArrayList);
            closeProgressDialog();
        } catch (IndexOutOfBoundsException e) {
            UpsViewer.this.finish();
        }
    }


    // ---------------------------------------------------------------------------------------------


    // Get's UPS data
    private void startConnectorTask() {
        progressBar.setScaleY(3f);
        progressBar.setVisibility(View.VISIBLE);
        new ConnectorTask(this, this, TaskMode.MODE_ACTIVITY, this.upsId);
    }


    @Override
    public void noUpsConfigured() {
    }

    @Override
    public void onAskToTrustKey(String upsId, String hostName, String hostFingerPrint, String hostKey) {
    }

    @Override
    public void onRefreshList() {
    }

    @Override
    public void onTaskCompleted() {
        runOnUiThread(this::getUpsData);
    }

    @Override
    public void onMissingPreferences() {
    }

    @Override
    public void onConnectionError() {
    }

    @Override
    public void onCommandError(String errorStr) {
    }

    // Close progressbar in controlled way
    private void closeProgressDialog() {
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }


    // ---------------------------------------------------------------------------------------------

    private void drawData(UPS ups, String serverOutput) {
        try {


            // Set title
            if (sharedPreferences.getBoolean(Constants.SP_SET_UPS_AS_ACTIVITY_TITLE, true)) {
                setTitle(ups.getUPS_NAME());
            }


            // Set status (Always shown)
            TextView status = findViewById(R.id.status);
            status.setText(ups.getSTATUS());
            if (ups.isOnline()) {
                status.setBackgroundColor(ContextCompat.getColor(UpsViewer.this, R.color.bootStrapSuccess));
            } else {
                status.setBackgroundColor(ContextCompat.getColor(UpsViewer.this, R.color.bootStrapDanger));
            }


            // Set model
            LinearLayout upsModelLayout = findViewById(R.id.upsModelLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_UPS_MODEL, true)) {
                upsModelLayout.setVisibility(View.VISIBLE);
                TextView model = (TextView) findViewById(R.id.model);
                model.setText(ups.getMODEL());
            } else {
                upsModelLayout.setVisibility(View.GONE);
            }


            // Set line voltage
            LinearLayout lineVoltageLayout = findViewById(R.id.lineVoltageLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_LINE_VOLTAGE, true)) {
                lineVoltageLayout.setVisibility(View.VISIBLE);
                // TextView lineVoltage = (TextView) findViewById(R.id.lineVoltage);
                // lineVoltage.setText(ups.getLineVoltageStr());
                TextView lineVoltageOnly = (TextView) findViewById(R.id.lineVoltageOnly);
                lineVoltageOnly.setText(ups.getLineVoltageOnlyStr());
            } else {
                lineVoltageLayout.setVisibility(View.GONE);
            }


            // Set battery voltage
            LinearLayout batteryVoltageLayout = findViewById(R.id.batteryVoltageLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_BATTERY_VOLTAGE, true)) {
                batteryVoltageLayout.setVisibility(View.VISIBLE);
                // TextView batteryVoltage = (TextView) findViewById(R.id.batteryVoltage);
                // batteryVoltage.setText(ups.getBatteryVoltageStr());
                TextView batteryVoltageOnly = (TextView) findViewById(R.id.batteryVoltageOnly);
                batteryVoltageOnly.setText(ups.getBatteryVoltageOnlyStr());
            } else {
                batteryVoltageLayout.setVisibility(View.GONE);
            }


            // Set internal temperature //    Int. Temp
            LinearLayout internalTemperatureLayout = findViewById(R.id.internalTemperatureLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_INTERNAL_TEMPERATURE, true)) {
                internalTemperatureLayout.setVisibility(View.VISIBLE);
                TextView internalTemperature = (TextView) findViewById(R.id.internalTemperature);
                internalTemperature.setText(ups.getITEMP());
            } else {
                internalTemperatureLayout.setVisibility(View.GONE);
            }


            // Set load percentage
            LinearLayout batteryLoadPercentageLayout = findViewById(R.id.batteryLoadPercentageLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_LOAD_PERCENTAGE, true)) {
                batteryLoadPercentageLayout.setVisibility(View.VISIBLE);
                TextView loadPercent = (TextView) findViewById(R.id.loadPercent);
                loadPercent.setText(ups.getLoadPercentStr());
                ProgressBar loadPercentPB = (ProgressBar) findViewById(R.id.loadPercentPB);
                if (ups.getLoadPercentInteger() != null) {
                    loadPercentPB.setProgress(ups.getLoadPercentInteger());
                    loadPercentPB.setVisibility(View.VISIBLE);
                } else {
                    loadPercentPB.setVisibility(View.GONE);
                }
            } else {
                batteryLoadPercentageLayout.setVisibility(View.GONE);
            }


            // Set battery charge level
            LinearLayout batteryChargeLevelLayout = findViewById(R.id.batteryChargeLevelLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_PERCENT_BATTERY_CHARGE, true)) {
                batteryChargeLevelLayout.setVisibility(View.VISIBLE);
                TextView batteryChargeLevel = (TextView) findViewById(R.id.batteryChargeLevel);
                batteryChargeLevel.setText(ups.getBatteryChargeLevelStr());
                ProgressBar chargePB = (ProgressBar) findViewById(R.id.chargePB);
                if (ups.getBatteryChargeLevelInteger() != null) {
                    chargePB.setVisibility(View.VISIBLE);
                    chargePB.setProgress(ups.getBatteryChargeLevelInteger());
                } else {
                    chargePB.setVisibility(View.GONE);
                }
            } else {
                batteryChargeLevelLayout.setVisibility(View.GONE);
            }


            // Set transfer reason
            LinearLayout lastTransferReasonLayout = findViewById(R.id.lastTransferReasonLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_LAST_TRANSFER_REASON, true)) {
                lastTransferReasonLayout.setVisibility(View.VISIBLE);
                TextView lastTransferReason = (TextView) findViewById(R.id.lastTransferReason);
                lastTransferReason.setText(ups.getLastTransferReasonStr());
            } else {
                lastTransferReasonLayout.setVisibility(View.GONE);
            }


            // Set battery time left
            LinearLayout batteryTimeLeftLayout = findViewById(R.id.batteryTimeLeftLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_BATTERY_TIME_LEFT, true)) {
                batteryTimeLeftLayout.setVisibility(View.VISIBLE);
                TextView batteryTimeLeft = (TextView) findViewById(R.id.batteryTimeLeft);
                batteryTimeLeft.setText(ups.getBATTERY_TIME_LEFT());
            } else {
                batteryTimeLeftLayout.setVisibility(View.GONE);
            }


            // Set battery date
            LinearLayout batteryDateLayout = findViewById(R.id.batteryDateLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_BATTERY_DATE, true)) {
                batteryDateLayout.setVisibility(View.VISIBLE);
                TextView batteryDate = (TextView) findViewById(R.id.batteryDate);
                batteryDate.setText(ups.getBATTERY_DATE());
            } else {
                batteryDateLayout.setVisibility(View.GONE);
            }


            // Set firmware
            LinearLayout batteryFirmwareLayout = findViewById(R.id.batteryFirmwareLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_FIRMWARE_VERSION, true)) {
                batteryFirmwareLayout.setVisibility(View.VISIBLE);
                TextView firmware = (TextView) findViewById(R.id.firmware);
                firmware.setText(ups.getFIRMWARE());
            } else {
                batteryFirmwareLayout.setVisibility(View.GONE);
            }


            // Set start time
            LinearLayout startTimeLayout = findViewById(R.id.startTimeLayout);
            if (sharedPreferences.getBoolean(Constants.SP_SHOW_START_TIME, false)) {
                startTimeLayout.setVisibility(View.VISIBLE);
                TextView startTime = (TextView) findViewById(R.id.startTime);
                startTime.setText(ups.getSTART_TIME());
            } else {
                startTimeLayout.setVisibility(View.GONE);
            }


        } catch (NullPointerException e) {
            e.printStackTrace();
            genericErrorDialog("Error", "Result from your server was invalid. Do you have apcupsd installed and running?\n\n" +
                    "This could be caused by app expecting data what it's not receiving from your ssh apcaccess status command output. " +
                    "You can help me via sending this message output to my email nitramite@outlook.com"
                    + "\n\nError description: " + e.toString()
                    + "\n\nServer raw output: " + serverOutput
            );
        }
    }


    private void drawEventsData(final boolean loadEvents, final ArrayList<String> eventsArray) {
        if (eventsArray != null) {
            CardView eventsCardView = findViewById(R.id.eventsCardView);
            if (eventsArray.size() > 0 && loadEvents) {
                eventsCardView.setVisibility(View.VISIBLE);
                ListView eventsList = findViewById(R.id.eventsList);
                CustomEventsAdapter customEventsAdapter = new CustomEventsAdapter(UpsViewer.this, eventsArray, sharedPreferences.getBoolean(Constants.SP_EVENTS_COLORING, true));
                eventsList.setAdapter(customEventsAdapter);
            } else {
                eventsCardView.setVisibility(View.GONE);
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // Helpers


    /**
     * Parse outage events for statistics
     */
    private void parseOutageEventsStatistics() {
        try {
            ArrayList<OutageEventObj> outageEventObjs = new ArrayList<>();

            // Get default date format
            final String dateFormatStr = sharedPreferences.getString(Constants.SP_STATISTICS_DATE_FORMAT, "yyyy-MM-dd");
            final String timeFormatStr = sharedPreferences.getString(Constants.SP_STATISTICS_TIME_FORMAT, "HH:mm:ss");

            @SuppressLint("SimpleDateFormat") DateFormat timeFormat = new SimpleDateFormat(timeFormatStr);

            String powerFailureDate = null;
            Date powerFailureTime = null;

            // Parse events
            for (int i = 0; i < eventsArrayList.size(); i++) {

                final String eventRow = eventsArrayList.get(i);

                // We are only interested on actual power events
                if (eventRow.contains("Power failure") || eventRow.contains("Power is back")) {

                    final String date = eventRow.substring(0, dateFormatStr.length());
                    final String time = eventRow.substring(dateFormatStr.length() + 1, dateFormatStr.length() + 1 + timeFormatStr.length());

                    // Log.i(TAG, "Event date: " + date + "  and time: " + time);

                    if (eventRow.contains("Power failure")) {
                        powerFailureDate = date;
                        powerFailureTime = timeFormat.parse(time);
                    } else if (eventRow.contains("Power is back")) {
                        if (date.equals(powerFailureDate)) {
                            Date powerIsBackTime = timeFormat.parse(time);

                            long seconds = ((powerIsBackTime.getTime() - powerFailureTime.getTime()) / 1000);

                            // Log.i(TAG, date + " had " + String.valueOf(seconds) + " seconds of power outage");
                            outageEventObjs.add(new OutageEventObj(
                                    date, (int) seconds
                            ));

                        }
                        powerFailureDate = null; // reset
                    }
                }
            }
            drawOutageEventStatistics(outageEventObjs);
        } catch (ParseException e) {
            genericErrorDialog("Error", e.toString());
        }
    }


    /**
     * Draw outage events
     *
     * @param outageEventObj individual events
     */
    private void drawOutageEventStatistics(final ArrayList<OutageEventObj> outageEventObj) {

        barEntries = new ArrayList<>();
        barLabels = new ArrayList<>();

        Integer totalSeconds = 0;

        String currentYearMothPart = null;
        Integer monthSeconds = 0;

        int entryCount = 0;

        for (int i = 0; i < outageEventObj.size(); i++) {

            // Set base
            if (currentYearMothPart == null) {
                currentYearMothPart = outageEventObj.get(i).getYearMonthPart();
            }

            if (outageEventObj.get(i).getYearMonthPart().equals(currentYearMothPart)) {
                monthSeconds = monthSeconds + outageEventObj.get(i).getOutageSeconds();
            } else {

                // Add month seconds point
                barEntries.add(new BarEntry(entryCount, monthSeconds));
                entryCount++; // Increment entry count
                barLabels.add(currentYearMothPart);
                totalSeconds = totalSeconds + monthSeconds;

                Log.i(TAG, currentYearMothPart + " " + monthSeconds);

                // Reset for next month
                currentYearMothPart = outageEventObj.get(i).getYearMonthPart();
                monthSeconds = outageEventObj.get(i).getOutageSeconds();
            }

        }

        // Set total time out
        TextView totalTimeOutTV = findViewById(R.id.totalTimeOutTV);
        final String totalStr = totalSeconds + "s (" + (int) (totalSeconds / 60) + " min)";
        totalTimeOutTV.setText(totalStr);

        // Average time out
        if (totalSeconds > 0) {
            TextView averageTimeOutTV = findViewById(R.id.averageTimeOutTV);
            final String averageStr = totalSeconds / entryCount + "s (" + (int) (totalSeconds / entryCount / 60) + " min)";
            averageTimeOutTV.setText(averageStr);
        }

        // Set chart data
        barDataSet = new BarDataSet(barEntries, "Seconds /month");
        barDataSet.setColor(ContextCompat.getColor(this, R.color.bootStrapDanger));
        barData = new BarData(barDataSet);
        chart.setData(barData);
        barDataSet.setValueTextSize(18f);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(barLabels));
        chart.getDescription().setText("Showing power outage total seconds for each available month");

        // No data warning if no data
        if (barEntries.size() == 0) {
            statisticsNotEnoughData.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Toggles between statistics and normal details mode
     */
    private void toggleStatisticsMode() {
        if (eventsArrayList != null) {
            if (upsDetailsView.getVisibility() == View.VISIBLE) {
                upsDetailsView.setVisibility(View.GONE);
                eventsCardView.setVisibility(View.GONE);
                upsStatisticsView.setVisibility(View.VISIBLE);
                parseOutageEventsStatistics();
            } else {
                upsDetailsView.setVisibility(View.VISIBLE);
                eventsCardView.setVisibility(View.VISIBLE);
                upsStatisticsView.setVisibility(View.GONE);
            }
        } else {
            genericErrorDialog("Note", "You don't yet have events to use for calculating statistics.");
        }
    }


    // Generic use error dialog
    private void genericErrorDialog(final String title, final String description) {
        //if (activityActive && !this.isFinishing()) {
        new AlertDialog.Builder(UpsViewer.this)
                .setTitle(title)
                .setMessage(description)
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNeutralButton("Copy content", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("", description);
                            assert clipboard != null;
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(UpsViewer.this, "Content copied to clipboard", Toast.LENGTH_SHORT).show();
                        } catch (IndexOutOfBoundsException e) {
                            Toast.makeText(UpsViewer.this, "There was nothing to copy", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setIcon(R.drawable.ic_error_small)
                .show();
        //}
    }


    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ups_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            startConnectorTask();
            return true;
        }
        if (id == R.id.action_statistics) {
            toggleStatisticsMode();
            return true;
        }
        if (id == R.id.action_debug_output) {
            if (rawStatusOutput != null) {
                genericErrorDialog("Debug output", rawStatusOutput);
            } else {
                Toast.makeText(this, "Debug output is null", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // ---------------------------------------------------------------------------------------------


}
