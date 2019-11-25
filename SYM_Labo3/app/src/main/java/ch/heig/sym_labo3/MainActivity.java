package ch.heig.sym_labo3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void goToNFfcActivity(View view) {
        Intent intent = new Intent(this, NfcActivity.class);
        startActivity(intent);
    }

    public void goToBarcodeActivity(View view) {
        Intent intent = new Intent(this, BarcodeActivity.class);
        startActivity(intent);
    }
}
