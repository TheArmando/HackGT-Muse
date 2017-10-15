/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2016
 */

package com.choosemuse.example.libmuse;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.choosemuse.libmuse.AnnotationData;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.MessageType;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConfiguration;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseFileFactory;
import com.choosemuse.libmuse.MuseFileReader;
import com.choosemuse.libmuse.MuseFileWriter;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseVersion;
import com.choosemuse.libmuse.Result;
import com.choosemuse.libmuse.ResultLevel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;


import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.w3c.dom.Text;

/**
 * This example will illustrate how to connect to a Muse headband,
 * register for and receive EEG data and disconnect from the headband.
 * Saving EEG data to a .muse file is also covered.
 * <p>
 * For instructions on how to pair your headband with your Android device
 * please see:
 * http://developer.choosemuse.com/hardware-firmware/bluetooth-connectivity/developer-sdk-bluetooth-connectivity-2
 * <p>
 * Usage instructions:
 * 1. Pair your headband if necessary.
 * 2. Run this project.
 * 3. Turn on the Muse headband.
 * 4. Press "Refresh". It should display all paired Muses in the Spinner drop down at the
 * top of the screen.  It may take a few seconds for the headband to be detected.
 * 5. Select the headband you want to connect to and press "Connect".
 * 6. You should see EEG and accelerometer data as well as connection status,
 * version information and relative alpha values appear on the screen.
 * 7. You can pause/resume data transmission with the button at the bottom of the screen.
 * 8. To disconnect from the headband, press "Disconnect"
 */
public class MainActivity extends Activity implements OnClickListener {

    /**
     * Tag used for logging purposes.
     */
    private final String TAG = "TestLibMuseAndroid";

    private MediaPlayer mediaPlayer;

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     * <p>
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     * <p>
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

    /**
     * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
     * depending on the type of headband and the preset configuration.  We buffer the
     * data that is read until we can update the UI.
     * <p>
     * The stale flags indicate whether or not new data has been received and the buffers
     * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
     * and ACCELEROMETER values in this example.
     * <p>
     * Note: the array lengths of the buffers are taken from the comments in
     * MuseDataPacketType, which specify 3 values for accelerometer and 6
     * values for EEG and EEG-derived packets.
     */
    private final double[] eegBuffer = new double[6];
    private boolean eegStale;
    private final double[] alphaBuffer = new double[6];
    private boolean alphaStale;
    //    private final double[] accelBuffer = new double[3];
//    private boolean accelStale;
    private final double[] betaBuffer = new double[6];
    private boolean betaStale;

    // The average of all channels for Alpha_Relative and Beta_Relative
    private double avgAlpha;
    private double avgBeta;

    private double avgAlphaLong;
    private double avgBetaLong;

    private double eegLeft;
    private double eegRight;

    // For sampling
    private double relativeX = 0.0;
    private double relativeY = 0.0;
    private int noOfTime = 0;
    private double totalX = 0.0;
    private double totalY = 0.0;

    private final int SHORT_INTERVAL = 1000;
    private final int LONG_INTERVAL = 10000;
    private final int NUM_OF_TIMES = LONG_INTERVAL / SHORT_INTERVAL;

    private long startingTime;
    private long endTime;

    // Four emotions
    private enum Moods {
        HAPPY, ANGRY, RELAXED, SAD, NEUTRAL;

        public String toString() {
            switch (this) {
                case HAPPY:
                    return "Happy";
                case ANGRY:
                    return "Angry";
                case SAD:
                    return "Sad";
                case RELAXED:
                    return "Relaxed";
            }
            return "Neutral";
        }
    }

    /**
     * We will be updating the UI using a handler instead of in packet handlers because
     * packets come in at a very high frequency and it only makes sense to update the UI
     * at about 60fps. The update functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Handler handler = new Handler();

    /**
     * In the UI, the list of Muses you can connect to is displayed in a Spinner object for this example.
     * This spinner adapter contains the MAC addresses of all of the headbands we have discovered.
     */
    private ArrayAdapter<String> spinnerAdapter;

    /**
     * It is possible to pause the data transmission from the headband.  This boolean tracks whether
     * or not the data transmission is enabled as we allow the user to pause transmission in the UI.
     */
    private boolean dataTransmission = true;

    /**
     * To save data to a file, you should use a MuseFileWriter.  The MuseFileWriter knows how to
     * serialize the data packets received from the headband into a compact binary format.
     * To read the file back, you would use a MuseFileReader.
     */
    private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();

    /**
     * We don't want file operations to slow down the UI, so we will defer those file operations
     * to a handler on a separate thread.
     */
    private final AtomicReference<Handler> fileHandler = new AtomicReference<>();

    private TextView song;
    private TextView artist;
    private TextView emotion;
    private TextView oppEmotion;

    private boolean connected = false;

    private long beginTime;
    private long stopTime;


    //--------------------------------------
    // Lifecycle / Connection code


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeMap();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("choose-music");

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> databaseMap = (Map<String, Object>) dataSnapshot.getValue();
                Log.d("DATABASE", databaseMap.toString());
                for (Map.Entry<String, Object> entry : databaseMap.entrySet()) {
                    List<String> songList = new ArrayList<>();
                    for (Map.Entry<String, Map<String, String>> songEntry : ((Map<String, Map<String, String>>) entry.getValue()).entrySet()) {
                        String link = songEntry.getValue().get("link");
//                        Log.d("DATABASE", link);
                        songList.add(link);
                    }
                    songMap.put(entry.getKey(), songList);
                }
                Log.d("DATABASE", songMap.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        beginTime = System.currentTimeMillis();
        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<MainActivity> weakActivity =
                new WeakReference<MainActivity>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);
        // Register a listener to receive notifications of what Muse headbands
        // we can connect to.
        manager.setMuseListener(new MuseL(weakActivity));

        // Refresh the listener
        manager.stopListening();

        manager.startListening();

        // Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
        // simplify the connection process.  This requires access to the COARSE_LOCATION
        // or FINE_LOCATION permissions.  Make sure we have these permissions before
        // proceeding.
        ensurePermissions();

        // Load and initialize our UI.
        initUI();

        // Start up a thread for asynchronous file operations.
        // This is only needed if you want to do File I/O.
        fileThread.start();

        // Start our asynchronous updates of the UI.
        handler.post(tickUi);
//        stopTime = System.currentTimeMillis();
//        if (connected) {
//            handler.post(getMood);
//        } else if (stopTime - beginTime >= 20000) {
//            Intent songIntent = new Intent(Intent.ACTION_VIEW);
//            songIntent.setData(Uri.parse(getSong("happy")));
//            startActivity(songIntent);
//        }
        handler.post(getMood);


    }

    private Map<String, List<String>> songMap = new HashMap<>();

    //open.spotify.com/track
    private void initializeMap() {
        List<String> happyList = new ArrayList<>();
        happyList.add("6NPVjNh8Jhru9xOmyQigds");
        happyList.add("3jp7Ryj1sX3riA7NQaVlLd");
        happyList.add("22XNWA1302bLyc25GyvdP3");
        happyList.add("5z8qQvLYEehH19vNOoFAPb");
        happyList.add("3Ar4cp3V0SeWXEuRelC86p");
        happyList.add("5sTC1imYc0QzNe3i5snLy7");

        List<String> sadList = new ArrayList<>();
        sadList.add("https://open.spotify.com/track/" + "7LVHVU3tWfcxj5aiPFEW4Q");
        sadList.add("https://open.spotify.com/track/" + "6N7JzrteJv8lsr1GWYyu0b");
        sadList.add("https://open.spotify.com/track/" + "3QCPCz4cU4LxHL4e0Y7Kpy");
        sadList.add("https://open.spotify.com/track/" + "6zeE5tKyr8Nu882DQhhSQI");
        sadList.add("https://open.spotify.com/track/" + "6mFkJmJqdDVQ1REhVfGgd1");

        List<String> angryList = new ArrayList<>();
        angryList.add("https://open.spotify.com/track/" + "6RJdYpFQwLyNfDc5FbjkgV");
        angryList.add("https://open.spotify.com/track/" + "3VZWVvHjzkG60FyVUkTcy5");
        angryList.add("https://open.spotify.com/track/" + "2QiqwOVUctPRVggO9G1Zs5");
        angryList.add("https://open.spotify.com/track/" + "1hR0fIFK2qRG3f3RF70pb7");

        List<String> relaxedList = new ArrayList<>();
        relaxedList.add("https://open.spotify.com/track/" + "1vG6jMgSoqT3zG9tuDrL2E");
        relaxedList.add("https://open.spotify.com/track/" + "1WwAqeweh8B5WVO041pRFf");

        List<String> neutralList = new ArrayList<>();
        neutralList.add("https://open.spotify.com/track/" + "7j4rAHvJaQLbxstJ1TnHu9");
        neutralList.add("https://open.spotify.com/track/" + "6lanRgr6wXibZr8KgzXxBl");
        neutralList.addAll(relaxedList);
        neutralList.addAll(happyList);

        songMap.put("happy", happyList);
        songMap.put("sad", sadList);
        songMap.put("angry", angryList);
        songMap.put("sad", sadList);
        songMap.put("neutral", neutralList);
    }

    private String getSong(String key) {
        int index = (int) (Math.random() * songMap.get(key).size());
        return songMap.get(key).get(index);
    }

    protected void onPause() {
        super.onPause();
        // It is important to call stopListening when the Activity is paused
        // to avoid a resource leak from the LibMuse library.
        manager.stopListening();
    }

    public boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.refresh) {
            // The user has pressed the "Refresh" button.
            // Start listening for nearby or paired Muse headbands. We call stopListening
            // first to make sure startListening will clear the list of headbands and start fresh.
            manager.stopListening();

            manager.startListening();

        } else if (v.getId() == R.id.connect) {

            // The user has pressed the "Connect" button to connect to
            // the headband in the spinner.

            // Listening is an expensive operation, so now that we know
            // which headband the user wants to connect to we can stop
            // listening for other headbands.
            manager.stopListening();

            List<Muse> availableMuses = manager.getMuses();
            Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);

            // Check that we actually have something to connect to.
            if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
                Log.w(TAG, "There is nothing to connect to");
            } else {

                // Connect to the first Muse by default
                muse = availableMuses.get(0);
                // Unregister all prior listeners and register our data listener to
                // receive the MuseDataPacketTypes we are interested in.  If you do
                // not register a listener for a particular data type, you will not
                // receive data packets of that type.
                muse.unregisterAllListeners();
                muse.registerConnectionListener(connectionListener);
                muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
                muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_ABSOLUTE);
                muse.registerDataListener(dataListener, MuseDataPacketType.BETA_ABSOLUTE);
                muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
                muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
                muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
                muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);

                // Initiate a connection to the headband and stream the data asynchronously.
                muse.runAsynchronously();
            }

        } else if (v.getId() == R.id.disconnect) {

            // The user has pressed the "Disconnect" button.
            // Disconnect from the selected Muse.
            if (muse != null) {
                muse.disconnect();
            }

        } else if (v.getId() == R.id.rescan) {

            // The user has pressed the "Pause/Resume" button to either pause or
            // resume data transmission.  Toggle the state and pause or resume the
            // transmission on the headband.
            playingMusic = false;
        }
    }

    //--------------------------------------
    // Permissions

    /**
     * The ACCESS_COARSE_LOCATION permission is required to use the
     * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
     * On an Android 6.0 device, the following code will display 2 dialogs,
     * one to provide context and the second to request the permission.
     * On an Android device running an earlier version, nothing is displayed
     * as the permission is granted from the manifest.
     * <p>
     * If the permission is not granted, then Muse 2016 (MU-02) headbands will
     * not be discovered and a SecurityException will be thrown.
     */
    private void ensurePermissions() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
            // the user to grant us the permission.

            DialogInterface.OnClickListener buttonListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    0);
                        }
                    };

            // This is the context dialog which explains to the user the reason we are requesting
            // this permission.  When the user presses the positive (I Understand) button, the
            // standard Android permission dialog will be displayed (as defined in the button
            // listener above).
            AlertDialog introDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_description)
                    .setPositiveButton(R.string.permission_dialog_understand, buttonListener)
                    .create();
            introDialog.show();
        }
    }


    //--------------------------------------
    // Listeners

    /**
     * You will receive a callback to this method each time a headband is discovered.
     * In this example, we update the spinner with the MAC address of the headband.
     */
    public void museListChanged() {
        final List<Muse> list = manager.getMuses();
        spinnerAdapter.clear();
        for (Muse m : list) {
            spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
        }
    }

    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     *
     * @param p    A packet containing the current and prior connection states
     * @param muse The headband whose state changed.
     */
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {

        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        final String status = p.getPreviousConnectionState() + " -> " + current;
        Log.i(TAG, status);

        // Update the UI with the change in connection state.
        handler.post(new Runnable() {
            @Override
            public void run() {

                final TextView statusText = (TextView) findViewById(R.id.con_status);
                statusText.setText(status);

                final MuseVersion museVersion = muse.getMuseVersion();
                final TextView museVersionText = (TextView) findViewById(R.id.version);
                // If we haven't yet connected to the headband, the version information
                // will be null.  You have to connect to the headband before either the
                // MuseVersion or MuseConfiguration information is known.
                if (museVersion != null) {
                    final String version = museVersion.getFirmwareType() + " - "
                            + museVersion.getFirmwareVersion() + " - "
                            + museVersion.getProtocolVersion();
                    museVersionText.setText(version);
                } else {
                    museVersionText.setText(R.string.undefined);
                }
            }
        });

        if (current == ConnectionState.CONNECTED) {
            connected = true;
        }

        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());
            // Save the data file once streaming has stopped.
            saveFile();
            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;
        }
    }

    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     *
     * @param p    The data packet containing the data from the headband (eg. EEG data)
     * @param muse The headband that sent the information.
     */
    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        writeDataPacketToFile(p);

        /**
         * Things I am trying to do:
         * Left brain & Right brain
         * Agitated or not (Alpha-beta)
         *
         * If alpha is above a threshold, is calm
         * If beta is above a threshold, is alert
         */

        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert (eegBuffer.length >= n);
                getEegChannelValues(eegBuffer, p);
                eegStale = true;
                break;
//            case ACCELEROMETER:
//                assert(accelBuffer.length >= n);
//                getAccelValues(p);
//                accelStale = true;
//                break;
            case ALPHA_ABSOLUTE:
                assert (alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer, p);
                alphaStale = true;
                break;
            case BETA_ABSOLUTE:
                assert (betaBuffer.length >= n);
                getEegChannelValues(betaBuffer, p);
                betaStale = true;
                break;
            case BATTERY:
            case DRL_REF:
            case QUANTIZATION:
            default:
                break;
        }
    }

    /**
     * You will receive a callback to this method each time an artifact packet is generated if you
     * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
     * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
     *
     * @param p    The artifact packet with the data from the headband.
     * @param muse The headband that sent the information.
     */
    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
    }

    /**
     * Helper methods to get different packet values.  These methods simply store the
     * data in the buffers for later display in the UI.
     * <p>
     * getEegChannelValue can be used for any EEG or EEG derived data packet type
     * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
     * of MuseDataPacketType for all of the available values.
     * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
     * getValue methods.
     */
    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }

//    private void getAccelValues(MuseDataPacket p) {
//        accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
//        accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
//        accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
//    }


    //--------------------------------------
    // UI Specific methods

    /**
     * Initializes the UI of the example application.
     */
    private void initUI() {
        setContentView(R.layout.activity_listener);
        final Button recorderButton = (Button) findViewById(R.id.recorder);
        recorderButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorderButton.getText().toString().equals("Start Recording")) {
                    recorderButton.setText("Stop Recording");
                    startingTime = System.currentTimeMillis();
                    noOfTime++;
                } else {
                    recorderButton.setText("Start Recording");
                    endTime = System.currentTimeMillis();
                    long elapse = endTime - startingTime;
                    // Arousal
                    totalY += (avgBeta - avgAlpha) / (elapse / SHORT_INTERVAL);
                    // Valence
                    totalX += (eegLeft - eegRight) / (elapse / SHORT_INTERVAL);
                    relativeX = totalX / noOfTime;
                    relativeY = totalY / noOfTime;
                }
            }
        });

        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        Button disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);
        Button rescanButton = (Button) findViewById(R.id.rescan);
        rescanButton.setOnClickListener(this);

        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);
    }

    /**
     * The runnable that is used to update the UI at 60Hz.
     * <p>
     * We update the UI from this Runnable instead of in packet handlers
     * because packets come in at high frequency -- 220Hz or more for raw EEG
     * -- and it only makes sense to update the UI at about 60fps. The update
     * functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            if (eegStale) {
                updateEeg();
            }
//            if (accelStale) {
//                updateAccel();
//            }
            if (alphaStale) {
                updateAlpha();
            }
            if (betaStale) {
                updateBeta();
            }

            handler.postDelayed(tickUi, SHORT_INTERVAL);
        }
    };


    private boolean playingMusic = false;
    /**
     * The following runnable deals with updating the mood
     */
    private Runnable getMood = new Runnable() {
        @Override
        public void run() {
            avgAlphaLong = avgAlphaLong / NUM_OF_TIMES;
            avgBetaLong = avgBetaLong / NUM_OF_TIMES;
            eegLeft = eegLeft / NUM_OF_TIMES;
            eegRight = eegRight / NUM_OF_TIMES;

            //ImageView background = (ImageView) findViewById(R.id.background_img);
            if (!playingMusic) {
                stopTime = System.currentTimeMillis();
                if (connected) {
                    String url = getSong(determineMood().toLowerCase());
                    TextView mood = (TextView) findViewById(R.id.mood);
                    mood.setText("You are feeling " + determineMood().toLowerCase());
                    Intent songIntent = new Intent(Intent.ACTION_VIEW);
                    songIntent.setData(Uri.parse(url));
                    startActivity(songIntent);
                    playingMusic = true;
                } else if (stopTime - beginTime >= 25000) {
                    playingMusic = true;
                    TextView mood = (TextView) findViewById(R.id.mood);
                    mood.setText("You are feeling happy");
                    Intent songIntent = new Intent(Intent.ACTION_VIEW);
                    songIntent.setData(Uri.parse(getSong("happy")));
                    startActivity(songIntent);
                }
            }


//            artist.setText(currentSong.artist);
//            song.setText(currentSong.name);
//
//            // If no other song is playing, update the whole thing
//            if (!mediaPlayer.isPlaying()) {
//
//            }
//            if(mood.toLowerCase().equals("happy")) {
//                emotion.setText("happy");
//                oppEmotion.setText("sad");
//                background.setImageResource(R.drawable.happy2);
//            }
//            else if(mood.toLowerCase().equals("sad")){
//                emotion.setText("sad");
//                oppEmotion.setText("happy");
//                background.setImageResource(R.drawable.sad2);
//            }
//            else if(mood.toLowerCase().equals("angry")) {
//                emotion.setText("angry");
//                oppEmotion.setText("relaxed");
//                background.setImageResource(R.drawable.angry1);
//            }
//            else if(mood.toLowerCase().equals("relaxed")) {
//                emotion.setText("relaxed");
//                oppEmotion.setText("angry");
//                background.setImageResource(R.drawable.neutral1);
//            }
//            else {
//                emotion.setText("neutral");
//                oppEmotion.setText("neutral");
//                background.setImageResource(R.drawable.neutral2);
//            }
            avgAlphaLong = 0;
            avgBetaLong = 0;
            handler.postDelayed(getMood, LONG_INTERVAL);
        }
    };

    /**
     * The following methods update the TextViews in the UI with the data
     * from the buffers.
     */
//    private void updateAccel() {
//        TextView acc_x = (TextView)findViewById(R.id.acc_x);
//        TextView acc_y = (TextView)findViewById(R.id.acc_y);
//        TextView acc_z = (TextView)findViewById(R.id.acc_z);
//        acc_x.setText(String.format("%6.2f", accelBuffer[0]));
//        acc_y.setText(String.format("%6.2f", accelBuffer[1]));
//        acc_z.setText(String.format("%6.2f", accelBuffer[2]));
//    }
    private void updateEeg() {
        TextView tp9 = (TextView) findViewById(R.id.eeg_tp9);
        TextView fp1 = (TextView) findViewById(R.id.eeg_af7);
        TextView fp2 = (TextView) findViewById(R.id.eeg_af8);
        TextView tp10 = (TextView) findViewById(R.id.eeg_tp10);
        tp9.setText(String.format("%6.2f", eegBuffer[0]));
        fp1.setText(String.format("%6.2f", eegBuffer[1]));
        fp2.setText(String.format("%6.2f", eegBuffer[2]));
        tp10.setText(String.format("%6.2f", eegBuffer[3]));

        eegLeft += eegBuffer[0] + eegBuffer[1];
        eegRight += eegBuffer[2] + eegBuffer[3];
    }

    private void updateAlpha() {
        TextView elem1 = (TextView) findViewById(R.id.elem1);
        elem1.setText(String.format("%6.2f", alphaBuffer[0]));
        TextView elem2 = (TextView) findViewById(R.id.elem2);
        elem2.setText(String.format("%6.2f", alphaBuffer[1]));
        TextView elem3 = (TextView) findViewById(R.id.elem3);
        elem3.setText(String.format("%6.2f", alphaBuffer[2]));
        TextView elem4 = (TextView) findViewById(R.id.elem4);
        elem4.setText(String.format("%6.2f", alphaBuffer[3]));

        avgAlpha = getAvg(alphaBuffer[0], alphaBuffer[1], alphaBuffer[2], alphaBuffer[3]);

        avgAlphaLong += avgAlpha;
    }

    private void updateBeta() {
        TextView beta1 = (TextView) findViewById(R.id.beta1);
        beta1.setText(String.format("%6.2f", betaBuffer[0]));
        TextView beta2 = (TextView) findViewById(R.id.beta2);
        beta2.setText(String.format("%6.2f", betaBuffer[1]));
        TextView beta3 = (TextView) findViewById(R.id.beta3);
        beta3.setText(String.format("%6.2f", betaBuffer[2]));
        TextView beta4 = (TextView) findViewById(R.id.beta4);
        beta4.setText(String.format("%6.2f", betaBuffer[3]));

        avgBeta = getAvg(betaBuffer[0], betaBuffer[1], betaBuffer[2], betaBuffer[3]);

        avgBetaLong += avgBeta;
    }

    private double getAvg(double... nums) {
        double sum = 0;
        int count = 0;
        for (double d : nums) {
            if (d > 0) {
                sum += d;
                count++;
            }
        }
        return (count > 0) ? (sum / count) : 0;
    }

    // Determine which mood the user is in
    private String determineMood() {
        // Change valence values
        double valence = Math.round(eegRight / 50 - eegLeft / 50 - relativeX / 50);
        double arousal = Math.round(avgBetaLong * 100 - avgAlphaLong * 100 - relativeY * 100);
        if ((arousal > 0) && (valence > 0)) {
            return (Moods.HAPPY.toString());
        } else if ((arousal > 0) && (valence < 0)) {
            return (Moods.ANGRY.toString());
        } else if ((arousal < 0) && (valence > 0)) {
            return (Moods.RELAXED.toString());
        } else if ((arousal < 0) && (valence < 0)) {
            return (Moods.SAD.toString());
        }
        return (Moods.NEUTRAL.toString());
    }


    //--------------------------------------
    // File I/O

    /**
     * We don't want to block the UI thread while we write to a file, so the file
     * writing is moved to a separate thread.
     */
    private final Thread fileThread = new Thread() {
        @Override
        public void run() {
            Looper.prepare();
            fileHandler.set(new Handler());
            final File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            final File file = new File(dir, "new_muse_file.muse");
            // MuseFileWriter will append to an existing file.
            // In this case, we want to start fresh so the file
            // if it exists.
            if (file.exists()) {
                file.delete();
            }
            Log.i(TAG, "Writing data to: " + file.getAbsolutePath());
            fileWriter.set(MuseFileFactory.getMuseFileWriter(file));
            Looper.loop();
        }
    };

    /**
     * Writes the provided MuseDataPacket to the file.  MuseFileWriter knows
     * how to write all packet types generated from LibMuse.
     *
     * @param p The data packet to write.
     */
    private void writeDataPacketToFile(final MuseDataPacket p) {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    fileWriter.get().addDataPacket(0, p);
                }
            });
        }
    }

    /**
     * Flushes all the data to the file and closes the file writer.
     */
    private void saveFile() {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    MuseFileWriter w = fileWriter.get();
                    // Annotation strings can be added to the file to
                    // give context as to what is happening at that point in
                    // time.  An annotation can be an arbitrary string or
                    // may include additional AnnotationData.
                    w.addAnnotationString(0, "Disconnected");
                    w.flush();
                    w.close();
                }
            });
        }
    }

    /**
     * Reads the provided .muse file and prints the data to the logcat.
     *
     * @param name The name of the file to read.  The file in this example
     *             is assumed to be in the Environment.DIRECTORY_DOWNLOADS
     *             directory.
     */
    private void playMuseFile(String name) {

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, name);

        final String tag = "Muse File Reader";

        if (!file.exists()) {
            Log.w(tag, "file doesn't exist");
            return;
        }

        MuseFileReader fileReader = MuseFileFactory.getMuseFileReader(file);

        // Loop through each message in the file.  gotoNextMessage will read the next message
        // and return the result of the read operation as a Result.
        Result res = fileReader.gotoNextMessage();
        while (res.getLevel() == ResultLevel.R_INFO && !res.getInfo().contains("EOF")) {

            MessageType type = fileReader.getMessageType();
            int id = fileReader.getMessageId();
            long timestamp = fileReader.getMessageTimestamp();

            Log.i(tag, "type: " + type.toString() +
                    " id: " + Integer.toString(id) +
                    " timestamp: " + String.valueOf(timestamp));

            switch (type) {
                // EEG messages contain raw EEG data or DRL/REF data.
                // EEG derived packets like ALPHA_RELATIVE and artifact packets
                // are stored as MUSE_ELEMENTS messages.
                case EEG:
                case BATTERY:
                case ACCELEROMETER:
                case QUANTIZATION:
                case GYRO:
                case MUSE_ELEMENTS:
                    MuseDataPacket packet = fileReader.getDataPacket();
                    Log.i(tag, "data packet: " + packet.packetType().toString());
                    break;
                case VERSION:
                    MuseVersion version = fileReader.getVersion();
                    Log.i(tag, "version" + version.getFirmwareType());
                    break;
                case CONFIGURATION:
                    MuseConfiguration config = fileReader.getConfiguration();
                    Log.i(tag, "config" + config.getBluetoothMac());
                    break;
                case ANNOTATION:
                    AnnotationData annotation = fileReader.getAnnotation();
                    Log.i(tag, "annotation" + annotation.getData());
                    break;
                default:
                    break;
            }

            // Read the next message.
            res = fileReader.gotoNextMessage();
        }
    }

    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.
    class MuseL extends MuseListener {
        final WeakReference<MainActivity> activityRef;

        MuseL(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged() {
            activityRef.get().museListChanged();
        }
    }

    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<MainActivity> activityRef;

        ConnectionListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<MainActivity> activityRef;

        DataListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p, muse);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            activityRef.get().receiveMuseArtifactPacket(p, muse);
        }
    }
}
