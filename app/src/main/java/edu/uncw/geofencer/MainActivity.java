package edu.uncw.geofencer;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.objectbox.Box;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "edu.uncw.geofencer";
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    private static final int MINIMUM_DWELL_DURATION_IN_SECS = 5;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;

    private Box<Building> buildingBox;
    private TextView infoView;
    private static final String TEXT_KEY="info_text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoView = findViewById(R.id.info);

        if(savedInstanceState != null) {
            infoView.setText(savedInstanceState.getCharSequence(TEXT_KEY));
        }

        buildingBox = ((App) getApplication()).getBoxStore().boxFor(Building.class);
        mGeofencingClient = LocationServices.getGeofencingClient(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(TEXT_KEY, infoView.getText());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // We are going to register a custom BroadcastReceiver to listen for Intents that capture
        // Geofence updates. The custom receiver is located at the bottom of the class.
        registerReceiver(receiver, new IntentFilter(GEOFENCE_NOTIFICATION_ACTION));
        addGeofences();
    }

    private void addGeofences() {
        final Context context = this;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        } else {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            String msg = "Geofences successfully added";
                            infoView.setText(msg+"\n"+infoView.getText());
                            Toast.makeText( context, msg, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, msg);
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String msg = "Failed to add Geofences! Please restart the app!";
                            infoView.setText(msg+"\n"+infoView.getText());
                            Toast.makeText( context, msg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, msg);
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addGeofences();
                } else {
                    String error = "I can't do anything because I don't have permission to access location!";
                    infoView.setText(error+"\n"+infoView.getText());
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    Log.e(TAG,error);
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // We should stop listening for Geofence updates if we're not in the app
        unregisterReceiver(receiver);
        mGeofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        String msg = "Geofences successfully removed!";
                        infoView.setText(msg+"\n"+infoView.getText());
                        Log.d(TAG, msg);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to remove Geofences!");
                    }
                });
    }

    /**
     * Specify all the parameters for the Geofencing API, like which Geofences and transitions to listen for
     *
     * @return
     */
    private GeofencingRequest getGeofencingRequest() {
        // First create a list of geofencing objects from the database
        // The objects are specified with a unique name, lat, lon, and the radius from the point
        // These Geofences are set to never expire, and will generate events when the user stays in
        // the Geofence for 5 seconds and when the user leaves the fence
        List<Geofence> geofenceList = new ArrayList<>();
        for (Building building : buildingBox.getAll()) {
            Geofence g = new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(building.getName())
                    .setCircularRegion(
                            building.getLat(),
                            building.getLon(),
                            building.getRadius()
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setLoiteringDelay(MINIMUM_DWELL_DURATION_IN_SECS)
                    .build();
            geofenceList.add(g);
        }

        // Now constuct a request object that will be given to the Google APIs to implement the
        // and monitor the geofences
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    /**
     * This method specifies the Geofencing API from Google should do when Geofence transitions happen.
     * In our case, we want it to send a Broadcast that we can listen for and handle.
     *
     * @return
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent();
        intent.setAction(GEOFENCE_NOTIFICATION_ACTION);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private static final String GEOFENCE_NOTIFICATION_ACTION = "edu.uncw.geofencer.MY_GEOFENCE_NOTIFICATION";
    private static final SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy HH:mm:ss");

    /**
     * This special BroadcastReceiver listens for Intents sent out by hte Geofences API. This is
     * the code that actually updates the UI
     *      */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), "Geofence update received!", Toast.LENGTH_SHORT).show();

            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            Log.d(TAG, geofencingEvent.toString());

            if (geofencingEvent.hasError()) {
                Log.e(TAG, GeofenceStatusCodes.getStatusCodeString(
                        geofencingEvent.getErrorCode()));
                return;
            }

            // Get the transition type.
            int geofenceTransition = geofencingEvent.getGeofenceTransition();

            // Test that the reported transition was of interest.
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ||
                    geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

                // Get the geofences that were triggered. A single event can trigger
                // multiple geofences.
                List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();


                // Get the transition details as a String.
                String text = "";
                for (Geofence f: triggeringGeofences) {
                    text += "("+formatter.format(new Date())+") ";
                    text += geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ? "Entered " +f.getRequestId() : "Exited " + f.getRequestId();
                    text += "\n";
                }
                text += infoView.getText().toString();
                infoView.setText(text);

                Log.i(TAG, text);
            }
        }
    };

}
