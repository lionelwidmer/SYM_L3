package ch.heig.sym_labo3;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import ch.heig.sym_labo3.utils.NdefReaderTask;

public class LoggedInActivity extends AppCompatActivity {

    private static  final int AUTHENTICATE_MAX = 10, AUTHENTICATE_MED = 30, AUTHENTICATE_MIN = 60;
    private static final String TAG = NfcActivity.class.getSimpleName();
    private static final String MIME_TEXT_PLAIN = "text/plain";
    private Long lastTimeStamp;
    private NfcAdapter mNfcAdapter;
    private boolean NFCTagValidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);
        refreshLogin();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "NFC : OK!", Toast.LENGTH_LONG).show();
        }
        this.NFCTagValidity = false;


        handleIntent(getIntent());
    }
    @Override
    protected void onResume() {
        super.onResume();
        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void refreshLogin(){
        this.lastTimeStamp = System.currentTimeMillis()/1000;
    }

    public void testMinimumSec(View view){
        if( AUTHENTICATE_MIN + lastTimeStamp  >= System.currentTimeMillis()/1000){
            Toast.makeText(this, "Runned minimum security task", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Access denied", Toast.LENGTH_LONG).show();
        }
    }

    public void testMediumSec(View view){
        if( lastTimeStamp + AUTHENTICATE_MED >= System.currentTimeMillis()/1000){
            Toast.makeText(this, "Runned medium security task", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Access denied", Toast.LENGTH_LONG).show();
        }
    }

    public void testMaximumSec(View view){
        if( lastTimeStamp + AUTHENTICATE_MAX >= System.currentTimeMillis()/1000){
            Toast.makeText(this, "Runned maximum security task", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Access denied", Toast.LENGTH_LONG).show();
        }
    }

    void readTag(String msg) {
        if(msg.equals("1^b\"`0]|^!H~<C#;|Ri&.)oB &.tXOAm")){
            //tag auth ok!
            this.refreshLogin();
            Toast.makeText(this, "Refreshed security", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Wrong tag ", Toast.LENGTH_LONG).show();
        }

        //Toast.makeText(this, "Read content: " + msg, Toast.LENGTH_LONG).show();
    }

    private static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask(p -> readTag(p)).execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask(p -> readTag(p)).execute(tag);
                    break;
                }
            }
        }
    }
}
