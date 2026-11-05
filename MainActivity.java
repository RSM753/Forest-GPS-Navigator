package com.forestgps.navigator;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.location.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends ComponentActivity {
    LinearLayout root, toolbar;
    TextView status, coords, trackStatus;
    ImageView pageView;
    PdfRenderer renderer;
    ParcelFileDescriptor pdfFd;
    int pageIndex = 0;
    LocationManager locationManager;
    Location lastLocation;
    boolean recording = false;
    File trackFile;
    BufferedWriter trackWriter;
    ActivityResultLauncher<String[]> pdfPicker;
    ActivityResultLauncher<String[]> permissionRequest;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(234,241,233));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        pdfPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) openPdf(uri);
        });
        permissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> updateLocation());
        updateLocation();
    }

    void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247,249,246)); root.setPadding(16,12,16,12);
        setContentView(root);
        TextView title = new TextView(this); title.setText("🌲 Forest GPS Navigator");
        title.setTextSize(22); title.setTextColor(Color.rgb(28,65,42)); title.setTypeface(null,1);
        root.addView(title);
        TextView sub = new TextView(this); sub.setText("Offline Forest Mapping & Patrolling"); sub.setTextColor(Color.DKGRAY);
        root.addView(sub);
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        addButton(actions,"Import PDF Map",v -> pdfPicker.launch(new String[]{"application/pdf"}));
        addButton(actions,"GPS",v -> updateLocation());
        root.addView(actions);
        status = new TextView(this); status.setText("No map imported. Select a georeferenced or ordinary PDF map.");
        status.setPadding(4,10,4,10); root.addView(status);
        pageView = new ImageView(this); pageView.setBackgroundColor(Color.WHITE);
        pageView.setAdjustViewBounds(true); pageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1,0,1); pp.setMargins(0,8,0,8); root.addView(pageView,pp);
        LinearLayout pages = new LinearLayout(this);
        addButton(pages,"◀ Previous",v -> showPage(pageIndex-1));
        addButton(pages,"Next ▶",v -> showPage(pageIndex+1)); root.addView(pages);
        coords = new TextView(this); coords.setText("GPS: Waiting for location permission…");
        coords.setTextSize(16); coords.setPadding(4,8,4,8); root.addView(coords);
        trackStatus = new TextView(this); trackStatus.setText("Track: stopped"); root.addView(trackStatus);
        LinearLayout trackButtons = new LinearLayout(this);
        addButton(trackButtons,"Start Track",v -> startTrack());
        addButton(trackButtons,"Stop & Save",v -> stopTrack()); root.addView(trackButtons);
        LinearLayout waypoint = new LinearLayout(this);
        addButton(waypoint,"Save Waypoint",v -> saveWaypoint()); root.addView(waypoint);
        TextView note = new TextView(this);
        note.setText("Note: Standard PDFs can be viewed offline, but GPS cannot be overlaid accurately unless the map is georeferenced or calibrated. This starter version displays live coordinates and records GPS tracks/waypoints separately.");
        note.setTextSize(12); note.setTextColor(Color.DKGRAY); note.setPadding(4,8,4,4); root.addView(note);
    }

    void addButton(LinearLayout row, String label, android.view.View.OnClickListener listener) {
        Button b = new Button(this); b.setText(label); b.setTextSize(12); b.setOnClickListener(listener);
        row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
    }

    void openPdf(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch(Exception ignored) {}
        try {
            File local = new File(getFilesDir(),"current_map.pdf");
            try(InputStream in=getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(local)) {
                byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0) out.write(buf,0,n);
            }
            closePdf();
            pdfFd = ParcelFileDescriptor.open(local,ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pdfFd); pageIndex=0;
            status.setText("Map loaded offline: "+renderer.getPageCount()+" page(s).");
            showPage(0);
        } catch(Exception e) { status.setText("Could not open PDF: "+e.getMessage()); }
    }

    void showPage(int index) {
        if(renderer==null) { status.setText("Import a PDF map first."); return; }
        if(index<0 || index>=renderer.getPageCount()) return;
        try {
            pageIndex=index; PdfRenderer.Page p=renderer.openPage(index);
            int width=Math.min(1800,p.getWidth()*2), height=(int)(width*(p.getHeight()/(float)p.getWidth()));
            Bitmap bitmap=Bitmap.createBitmap(width,Math.max(1,height),Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE); p.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pageView.setImageBitmap(bitmap); p.close();
            status.setText("Offline map • Page "+(pageIndex+1)+" / "+renderer.getPageCount());
        } catch(Exception e) { status.setText("Page render error: "+e.getMessage()); }
    }

    void closePdf() { try { if(renderer!=null) renderer.close(); } catch(Exception e){} try {if(pdfFd!=null)pdfFd.close();}catch(Exception e){} renderer=null; pdfFd=null; }

    void updateLocation() {
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            permissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}); return;
        }
        try {
            Location last=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(last==null) last=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(last!=null) onLocation(last);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,2,listener,Looper.getMainLooper());
        } catch(Exception e) { coords.setText("GPS unavailable: "+e.getMessage()); }
    }
    LocationListener listener = new LocationListener() { public void onLocationChanged(Location l){onLocation(l);} };
    void onLocation(Location l) {
        lastLocation=l;
        coords.setText(String.format(Locale.US,"GPS: %.6f, %.6f\nAccuracy: %.1f m • Altitude: %.1f m",
            l.getLatitude(),l.getLongitude(),l.getAccuracy(),l.hasAltitude()?l.getAltitude():0));
        if(recording && trackWriter!=null) try {
            trackWriter.write(String.format(Locale.US,"%s,%.7f,%.7f,%.1f%n",now(),l.getLatitude(),l.getLongitude(),l.getAccuracy()));
            trackWriter.flush();
        } catch(Exception ignored){}
    }
    String now(){return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date());}

    void startTrack() {
        if(lastLocation==null) { updateLocation(); Toast.makeText(this,"Wait for GPS fix, then start track.",Toast.LENGTH_LONG).show(); return; }
        try {
            File dir=new File(getExternalFilesDir(null),"ForestGPS"); dir.mkdirs();
            trackFile=new File(dir,"track_"+System.currentTimeMillis()+".csv");
            trackWriter=new BufferedWriter(new FileWriter(trackFile));
            trackWriter.write("timestamp,latitude,longitude,accuracy_m\n"); recording=true;
            trackStatus.setText("Track: recording • "+trackFile.getName());
        } catch(Exception e){trackStatus.setText("Track error: "+e.getMessage());}
    }
    void stopTrack() {
        recording=false; try {if(trackWriter!=null)trackWriter.close();}catch(Exception e){}
        trackWriter=null;
        trackStatus.setText(trackFile==null?"Track: stopped":"Track saved: "+trackFile.getAbsolutePath());
    }
    void saveWaypoint() {
        if(lastLocation==null){Toast.makeText(this,"GPS fix not available yet.",Toast.LENGTH_SHORT).show();return;}
        try {
            File dir=new File(getExternalFilesDir(null),"ForestGPS"); dir.mkdirs();
            File f=new File(dir,"waypoints.csv"); boolean exists=f.exists();
            try(BufferedWriter w=new BufferedWriter(new FileWriter(f,true))){
                if(!exists) w.write("timestamp,latitude,longitude,accuracy_m\n");
                w.write(String.format(Locale.US,"%s,%.7f,%.7f,%.1f%n",now(),lastLocation.getLatitude(),lastLocation.getLongitude(),lastLocation.getAccuracy()));
            }
            Toast.makeText(this,"Waypoint saved.",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"Save failed: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    @Override protected void onDestroy(){super.onDestroy();try{locationManager.removeUpdates(listener);}catch(Exception e){} stopTrack(); closePdf();}
}
