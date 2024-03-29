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
    private static final String MIME_TEXT_PLAIN = "text/plain";
    private ImageView nfcLogo;
    private TextView lblPassword;
    private EditText pwdField, emailField;
    private Button authBtn;
    private NfcAdapter mNfcAdapter;
    private boolean NFCTagValidity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        this.initFields();
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
        this.initFields();
        this.logout();
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

    public void fullAuthentication(View view){
        if(this.NFCTagValidity){
            String  code = this.pwdField.getText().toString(),
                    email = this.emailField.getText().toString();
            if(email.equals("a@a.a") && code.equals("426789")){
                Toast.makeText(this, "Vous voilà authentifié", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoggedInActivity.class);
                startActivity(intent);
            } else {
                this.logout();
                Toast.makeText(this, "Erreur dans l'adresse ou le mot de passe ", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Erreur : il faut d'abord valider le tag ", Toast.LENGTH_LONG).show();
        }
    }

    private void initFields(){
        this.nfcLogo = findViewById(R.id.nfcLogo);
        this.lblPassword= findViewById(R.id.txtInstructionsMdp);
        this.pwdField   = findViewById(R.id.pwdField);
        this.emailField = findViewById(R.id.emailField);
        this.authBtn    = findViewById(R.id.btnAuth);
    }

    public void logout(){
        this.nfcLogo.setColorFilter(null);
        this.emailField.setVisibility(View.GONE);
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
            this.emailField.setVisibility(View.VISIBLE);
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
