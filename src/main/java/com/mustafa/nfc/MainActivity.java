package com.mustafa.nfc;


import static android.content.ContentValues.TAG;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;
    NfcManager nfcManager;
    TextView nfcText, infoText;
    private SwipeRefreshLayout swipeRefreshLayout;
    PendingIntent pendingIntent;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.container);
        swipeRefreshLayout.setOnRefreshListener(this::refreshPage);

        nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        nfcText = findViewById(R.id.nfcText);
        infoText = findViewById(R.id.infoText);
        button = findViewById(R.id.nfcBtn);

        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            // adapter exists and is enabled.
            Toast.makeText(this, "NFC Cihazı Destekleniyor.", Toast.LENGTH_SHORT).show();
            nfcText.setText("NFC Cihazı Bağlı.");
            button.setText("Kapatmak için Ayarları Aç");
        } else {
            // adapter not exists and is not enabled.
            Toast.makeText(this, "NFC Cihazı Bulunmuyor!", Toast.LENGTH_SHORT).show();
            nfcText.setText("NFC Cihazı Bağlı Değil!");
            button.setText("NFC Ayarları Aç");
        }

    }

    public void checkNFC(View view) {

        if (nfcAdapter == null || !(nfcAdapter.isEnabled()) || nfcManager.getDefaultAdapter() == null || nfcAdapter != null) {

            AlertDialog.Builder alertbox = new AlertDialog.Builder(view.getContext());
            alertbox.setTitle("NFC");
            alertbox.setMessage("NFC Ayarları Açmak ister misiniz?");
            alertbox.setPositiveButton("Aç", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    {
                        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                        startActivity(intent);
                    }
                }
            });
            alertbox.setNegativeButton("Kapat", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            alertbox.show();

        }


    }

    public void refreshPage() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                    // adapter exists and is enabled.
                    Toast.makeText(getApplicationContext(), "NFC Cihazı Destekleniyor.", Toast.LENGTH_SHORT).show();
                    nfcText.setText("NFC Cihazı Bağlı.");
                } else {
                    // adapter not exists and is not enabled.
                    Toast.makeText(getApplicationContext(), "NFC Cihazı Bulunmuyor!", Toast.LENGTH_SHORT).show();
                    nfcText.setText("NFC Cihazı Bağlı Değil!");
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 2000);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        setIntent(intent);
        resolveIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        infoText.setText("ID: " + Arrays.toString(tag.getId()));
        super.onNewIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            assert tag != null;
            byte[] payload = detectTagData(tag).getBytes();
        }
    }

    public void writeTag(MifareUltralight mifareUlTag) {
        try {
            mifareUlTag.connect();
            mifareUlTag.writePage(4, "get ".getBytes(Charset.forName("US-ASCII")));
            mifareUlTag.writePage(5, "fast".getBytes(Charset.forName("US-ASCII")));
            mifareUlTag.writePage(6, " NFC".getBytes(Charset.forName("US-ASCII")));
            mifareUlTag.writePage(7, " now".getBytes(Charset.forName("US-ASCII")));
        } catch (IOException e) {
            Log.e(TAG, "IOException while writing MifareUltralight...", e);
        } finally {
            try {
                mifareUlTag.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException while closing MifareUltralight...", e);
            }
        }
    }
    public String readTag(MifareUltralight mifareUlTag) {
        try {
            mifareUlTag.connect();
            byte[] payload = mifareUlTag.readPages(4);
            return new String(payload, Charset.forName("US-ASCII"));
        } catch (IOException e) {
            Log.e(TAG, "IOException while reading MifareUltralight message...", e);
        } finally {
            if (mifareUlTag != null) {
                try {
                    mifareUlTag.close();
                }
                catch (IOException e) {
                    Log.e(TAG, "Error closing tag...", e);
                }
            }
        }
        return null;
    }


    private String detectTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        sb.append("NFC ID (dec): ").append(toDec(id)).append('\n');
        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareUltralight.class.getName())) {
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                readTag(mifareUlTag);
                writeTag(mifareUlTag);
            }
        }

        Intent intent = new Intent(MainActivity.this,CardService.class);
        intent.putExtra("id",id);
        startActivity(intent);

        infoText.setText(Arrays.toString(id));
        return sb.toString();
}

    private long toDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

}