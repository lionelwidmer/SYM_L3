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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import ch.heig.sym_labo3.utils.NdefReaderTask;

public class NfcActivity extends AppCompatActivity {
    // Instance variables
    private static final String TAG = NfcActivity.class.getSimpleName();
    private static  final int AUTHENTICATE_MAX = 10;
    private ImageView nfcLogo;
    private TextView lblPassword;
    private EditText pwdField;
    private Button authBtn;
    private NfcAdapter mNfcAdapter;
    private boolean NFCTagValidity;
    private static final String MIME_TEXT_PLAIN = "text/plain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        this.nfcLogo = findViewById(R.id.nfcLogo);
        this.lblPassword= findViewById(R.id.txtInstructionsMdp);
        this.pwdField   = findViewById(R.id.pwdField);
        this.authBtn    = findViewById(R.id.btnAuth);
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
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }

    public void fullAuthentication(View view){
        if(this.NFCTagValidity){
            String code = this.pwdField.getText().toString();
            if(code.equals("426789")){
                Toast.makeText(this, "Vous voilà authentifié", Toast.LENGTH_LONG).show();
            } else {
                this.logout();
                Toast.makeText(this, "Erreur dans le mot de passe : " + code, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Erreur : il faut d'abord valider le tag ", Toast.LENGTH_LONG).show();
        }
    }

    public void logout(){
        this.nfcLogo.setColorFilter(null);
        this.pwdField.setVisibility(View.GONE);
        this.lblPassword.setVisibility(View.GONE);
        this.authBtn.setVisibility(View.GONE);
        this.pwdField.setText("");
        NFCTagValidity = false;
    }

    void readTag(String msg) {
        if(msg.equals("1^b\"`0]|^!H~<C#;|Ri&.)oB &.tXOAm")){
            //tag auth ok!
            this.nfcLogo.setColorFilter(Color.parseColor("#689F38"));
            Toast.makeText(this, "Tag NFC Validé !", Toast.LENGTH_LONG).show();

            this.pwdField.setVisibility(View.VISIBLE);
            this.lblPassword.setVisibility(View.VISIBLE);
            this.authBtn.setVisibility(View.VISIBLE);
            NFCTagValidity = true;
        } else {
            this.nfcLogo.setColorFilter(Color.parseColor("#B71C1C"));
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
                new NdefReaderTask().execute(tag);

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
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }
    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                Toast.makeText(NfcActivity.this, "Invalid Tag or not NDEF capable", Toast.LENGTH_LONG).show();
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
            /*
             * See NFC forum specification for "Text Record Type Definition" at 3.2.1
             *
             * http://www.nfc-forum.org/specs/
             *
             * bit_7 defines encoding
             * bit_6 reserved for future use, must be 0
             * bit_5..0 length of IANA language code
             */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                NfcActivity.this.readTag(result);
            }
        }
    }
}
