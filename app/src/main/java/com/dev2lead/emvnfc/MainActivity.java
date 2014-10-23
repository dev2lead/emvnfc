package com.dev2lead.emvnfc;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.TextView;
import android.content.Intent;
import android.util.Log;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MainActivity extends Activity {

    private TextView view;
    private Intent intent;
    private Tag tag;
    private IsoDep isodep;
    private StringBuilder sb = new StringBuilder();

    public byte[] tlv(byte[] data, byte id) {
        for (int i = 0; i != data.length; i = i) {
            if (data[i++] == id) {
                return Arrays.copyOfRange(data, i + 1, i + 1 + data[i]);
            }
        }
        return null;
    }

    public byte[] transceive(byte[] data) {
        sb.setLength(0);
        for (byte element : data) sb.append(String.format("%02X ", element));
        Log.w("NFC", "REQ >> " + sb.toString());
        try {
            data = isodep.transceive(data);
        } catch (Exception e) {
            data = null;
        }
        sb.setLength(0);
        for (byte element : data) sb.append(String.format("%02X ", element));
        Log.w("NFC", "RES << " + sb.toString());
        return data;
    }

    public String hex(byte[] data, int offset, int length) {
        sb.setLength(0);
        for (byte element : Arrays.copyOfRange(data, offset, offset + length)) sb.append(String.format("%02X", element));
        return sb.toString();
    }

    public byte[] handle() {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            for (String tech : tag.getTechList()) {
                if (tech.equals("android.nfc.tech.IsoDep")) {
                    isodep = IsoDep.get(tag);
                    try {
                        isodep.connect();

                        ByteArrayOutputStream aStream = new ByteArrayOutputStream();
                        aStream.write(0x00);
                        aStream.write(0xA4);
                        aStream.write(0x04);
                        aStream.write(0x00);
                        aStream.write(new String("1PAY.SYS.DDF01").length());
                        aStream.write(new String("1PAY.SYS.DDF01").getBytes());
                        byte[] a = transceive(aStream.toByteArray());

                        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                        bStream.write(0x00);
                        bStream.write(0xB2);
                        bStream.write(tlv(a, (byte) 0x88)[0]);
                        bStream.write(tlv(a, (byte) 0x88)[0] << 3 | 4);
                        bStream.write(0x00);
                        byte[] b = transceive(bStream.toByteArray());

                        ByteArrayOutputStream cStream = new ByteArrayOutputStream();
                        cStream.write(0x00);
                        cStream.write(0xA4);
                        cStream.write(0x04);
                        cStream.write(0x00);
                        cStream.write(tlv(b, (byte) 0x4F).length);
                        cStream.write(tlv(b, (byte) 0x4F));
                        byte[] c = transceive(cStream.toByteArray());

                        ByteArrayOutputStream dStream = new ByteArrayOutputStream();
                        dStream.write(0x80);
                        dStream.write(0xA8);
                        dStream.write(0x00);
                        dStream.write(0x00);
                        dStream.write(0x23);
                        dStream.write(0x83);
                        dStream.write(0x21);
                        dStream.write(0xFF);                                                        // TTQ
                        dStream.write(0xFF);                                                        // TTQ
                        dStream.write(0xFF);                                                        // TTQ
                        dStream.write(0xFF);                                                        // TTQ
                        dStream.write(new byte[6]);                                                 // PURCHASE AMOUNT
                        dStream.write(new byte[6]);                                                 // CASHBACK AMOUNT
                        dStream.write(0x02);                                                        // TERMINAL COUNTRY : FR (250)
                        dStream.write(0x50);                                                        // TERMINAL COUNTRY : FR (250)
                        dStream.write(new byte[5]);                                                 // TERMINAL VERIFICATION
                        dStream.write(0x09);                                                        // TRANSACTION CURRENCY : EUR (978)
                        dStream.write(0x78);                                                        // TRANSACTION CURRENCY : EUR (978)
                        dStream.write(0x14);                                                        // TRANSACTION DATE : 2014-12-10
                        dStream.write(0x12);                                                        // TRANSACTION DATE : 2014-12-10
                        dStream.write(0x10);                                                        // TRANSACTION DATE : 2014-12-10
                        dStream.write(0x00);                                                        // TRANSACTION TYPE : PURCHASE
                        dStream.write(new byte[4]);                                                 // RANDOM NUMBER
                        byte[] d = transceive(dStream.toByteArray());

                        view = (TextView) (findViewById(R.id.ccnumb));
                        view.setText(hex(tlv(d, (byte) 0x57), 0, 8));
                        view = (TextView) (findViewById(R.id.ccdate));
                        view.setText(hex(tlv(d, (byte) 0x57), 8, 5).substring(1, 5));

                        isodep.close();
                    } catch (Exception e) {
                        Log.e("NFC", "ERR");
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = (TextView) (findViewById(R.id.ccnumb));
        view.setText("XXXXXXXXXXXXXXXX");
        view = (TextView) (findViewById(R.id.ccdate));
        view.setText("XXXX");
        intent = getIntent();
        handle();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
