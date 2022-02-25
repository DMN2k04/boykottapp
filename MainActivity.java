package com.dn2k04.boykottapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements LocationListener {
    public static TextView status;
    public static String DATABASE;
    public static int Severity;
    static String sFirma;
    static String sDetails;
    FloatingActionButton fab;
    ImageView i;
    Button r;
    TextView l;
    LocationManager locationManager;
    double posx;
    double posy;
    FusedLocationProviderClient flpc;
    private LocationRequest locationRequest;

    public static boolean IsInBox(double pointx, double pointy, double radiusx, double radiusy, double TarX, double TarY) {
        /*
         * IsInBox(*4,4,2,2*,4,4)
         *
         * 5
         * 4       xy  rx
         * 3
         * 2       rx
         * 1
         * 0 1 2 3 4 5 6 7 8 9
         *
         */
        double rx, ry;
        rx = radiusx - pointx;
        ry = radiusy - pointy;
        System.out.println(pointx);
        System.out.println(pointy);
        System.out.println(rx);
        System.out.println(ry);
        if (pointy >= TarY - rx && pointy <= TarY) {
            return pointx >= TarX - ry && pointx <= TarX;
        }

        return false;
    }

    public static boolean checkDatabase(String db, double ux, double uy) {
        try {
            String[] c1 = db.split("::");
            for (int i = 0; i < db.length(); i++) {
                try {
                    System.out.println(c1[i]);
                    String[] c2 = c1[i].split(";");
                    String Firma = c2[0];
                    double PX = Double.parseDouble(c2[1]);
                    double PY = Double.parseDouble(c2[2]);
                    double PRX = Double.parseDouble(c2[3]);
                    double PRY = Double.parseDouble(c2[4]);
                    String Details = c2[6];
                    sFirma = Firma;
                    sDetails = Details;
                    System.out.println(sFirma);
                    if (IsInBox(PX, PY, PRX, PRY, ux, uy)) {
                        status.setText("IM BEREICH");
                        Severity = Integer.parseInt(c2[5]);
                        return true;
                    }
                    status.setText(DATABASE);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Boykottier-App");
        status = (TextView) findViewById(R.id.stat);
        r = (Button) findViewById(R.id.refresh);
        l = (TextView) findViewById(R.id.loc);
        fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        r.setEnabled(false);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
        }
        i = (ImageView) findViewById(R.id.imageView2);
        flpc = LocationServices.getFusedLocationProviderClient(this);
        status.setText("Initialisiere...");
        new RetrieveFeedTask().execute();
        getLocation();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getLocation();
                status.setText("Dienst bereit!\nTippen Sie auf \"PRÜFEN\".");
                r.setEnabled(true);
            }
        }, 1000);
        r.setOnClickListener(new View.OnClickListener() {
                                 @Override
                                 public void onClick(View v) {
                                     System.out.println("BUTTON");
                                     for (int i = 0; i < 50; i++) {
                                         status.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                         status.setTextSize(17);
                                         r.setEnabled(false);
                                     }
                                     status.setText("Daten werden abgefragt...");
                                     i.setImageResource(R.drawable.neutral);
                                     Handler handler = new Handler();
                                     handler.postDelayed(new Runnable() {
                                         @Override
                                         public void run() {


                                             new RetrieveFeedTask().execute();
                                             getLocation();
                                             if (posx == 0 && posy == 0) {

                                                 for (int ir = 0; ir < 50; ir++) {
                                                     status.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
                                                     status.setTextSize(16);
                                                     i.setImageResource(R.drawable.neutral);
                                                 }
                                                 status.setText("Die App konnte Sie nicht über GPS lokalisieren.\n\nMögliche Ursachen:\n     - GPS deaktiviert\n     - GPS-Signal gestört");
                                                 l.setText(posx + "\n" + posy);
                                             } else {
                                                 if (checkDatabase(DATABASE, posx, posy)) {

                                                     status.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                                     status.setTextSize(16);
                                                     status.setText("Dieses Unternehmen ist aufgrund diskriminierender Aktionen gelistet.\n\n" + sFirma + "\n" + sDetails);
                                                     l.setText(posx + "\n" + posy);
                                                     if (Severity == 0) {
                                                         i.setImageResource(R.drawable.warn);
                                                     } else {
                                                         i.setImageResource(R.drawable.warn2);
                                                     }
                                                 } else {
                                                     status.setTextSize(16);
                                                     status.setText("Es wurde kein Unternehmen in Ihrer nähe gefunden, dass in der Datenbank bekannt ist.\nSollten Sie ein Unternehmen melden wollen, tippen Sie einfach auf die Flagge.\nDies wird einen Mail-Entwurf öffnen, bitte schildern Sie dort den Sachverhalt mit Belegen und Adressen.");
                                                     l.setText(posx + "\n" + posy);
                                                     i.setImageResource(R.drawable.clear);
                                                 }
                                             }
                                             Severity = 0;


                                             r.setEnabled(true);
                                         }
                                     }, 2500);
                                 }


                             }
        );
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOpenUrlButtonClicked();
            }
        });

    }

    private void onOpenUrlButtonClicked() {
        String inputString = "mailto:ungeimpfterfi@outlook.com";
        Uri parsedUrl = null;
        try {
            parsedUrl = Uri.parse(inputString);
        } catch (Exception ex) {

            Toast.makeText(this, "Failed to parse URL", Toast.LENGTH_LONG).show();
        }

        if (parsedUrl != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(parsedUrl);
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                //Toast.makeText(this, "Can't open URL. Did you enter a valid URL (https://...)", Toast.LENGTH_LONG).show();
            }

        }
    }

    private void getLocation() {

        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, MainActivity.this);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        //Toast.makeText(this, ""+location.getLatitude()+","+location.getLongitude(), Toast.LENGTH_SHORT).show();
        try {
            /*Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            String address = addresses.get(0).getAddressLine(0);*/
            posx = location.getLongitude();
            posy = location.getLatitude();
            location.setLatitude(0.0);
            location.setLongitude(0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

    }

    class RetrieveFeedTask extends AsyncTask<String, Void, Document> {

        private Exception exception;


        @Override
        protected Document doInBackground(String... strings) {
            String url = "https://anmsvr.xp3.biz/ncov/lst.txt";
            Connection connection = Jsoup.connect(url);
            try {
                Document document = connection.get();
                DATABASE = document.text();
                return document;
            } catch (IOException e) {
                status.setText("ERR");
                return null;
            }
        }
    }

}