package com.forestgps.navigator;

import android.app.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.*;
import android.widget.*;
import android.view.*;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.net.Uri;
import android.content.Intent;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import java.io.*;
import java.util.*;

public class MainActivity extends ComponentActivity {
    TextView info;
    LocationManager gps;
    ActivityResultLauncher<String[]> picker;
    ActivityResultLauncher<String> permission;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setPadding(20,20,20,20);

        TextView title = new TextView(this);
        title.setText("FOREST GPS NAVIGATOR");
        title.setTextSize(24);
        layout.addView(title);

        Button map = new Button(this);
        map.setText("IMPORT PDF FOREST MAP");
        layout.addView(map);

        info = new TextView(this);
        info.setText("GPS: Waiting...");
        info.setTextSize(18);
        layout.addView(info);

        Button locate = new Button(this);
        locate.setText("GET GPS LOCATION");
        layout.addView(locate);

        setContentView(layout);

        picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if(uri != null)
                    info.setText("PDF Map selected: " + uri.getLastPathSegment());
            }
        );

        map.setOnClickListener(v ->
            picker.launch(new String[]{"application/pdf"})
        );

        gps = (LocationManager)getSystemService(LOCATION_SERVICE);

        locate.setOnClickListener(v -> getGPS());
    }

    void getGPS() {
        if(ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            return;
        }

        try {
            Location l = gps.getLastKnownLocation(
                LocationManager.GPS_PROVIDER);
            if(l != null)
                info.setText("Latitude: "+l.getLatitude()+
                    "\nLongitude: "+l.getLongitude());
            else
                info.setText("Waiting for GPS fix...");
        } catch(Exception e) {
            info.setText(e.getMessage());
        }
    }
}
