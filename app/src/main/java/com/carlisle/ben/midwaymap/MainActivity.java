package com.carlisle.ben.midwaymap;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.carlisle.ben.midwaymap.R;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.travijuu.numberpicker.library.NumberPicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String originAddress = "";
    private String destinationAddress = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        ((PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.origin)).setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                originAddress = place.getAddress().toString().replace(" ", "+");
                new Thread(() -> {
                    try {
                        setLocation();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        errorOccured();
                    }
                }).start();
            }

            @Override
            public void onError(Status status) {
                return;
            }
        });
        ((PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.destination)).setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destinationAddress = place.getAddress().toString().replace(" ", "+");
                new Thread(() -> {
                    try {
                        setLocation();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        errorOccured();
                    }
                }).start();
            }

            @Override
            public void onError(Status status) {
                return;
            }
        });
        ((PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.midwayStop)).setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                new Thread(() -> {                    try {
                        setMap(place.getAddress().toString().replace(" ", "+"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        errorOccured();
                    }
                }).start();
            }

            @Override
            public void onError(Status status) {
                return;
            }
        });
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
    }

    private void setLocation() throws JSONException {
        int multiplier = ((NumberPicker) findViewById(R.id.multiplier)).getValue();
//		String originAddress = "25 West Passage Drive, Jamestown, RI";
//		String destinationAddress = "612 Academy Avenue, Providence, RI";
        if (originAddress.length() == 0) {
            errorOccured();
            return;
        } else if (destinationAddress.length() == 0) {
            errorOccured();
            return;
        }

        originAddress = originAddress.replaceAll(" ", "+");
        destinationAddress = destinationAddress.replaceAll(" ", "+");
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + originAddress + "&destination=" + destinationAddress + "&key=AIzaSyAXNPhz085mn2KbU7Ti40NgRW1IARfPjec";
        Scanner scanner;
        String content;
        try {
            URLConnection connection = new URL(url).openConnection();
            scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
            scanner.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            errorOccured();
            return;
        }
        JSONObject jsonObject = new JSONObject(content);
        int yards = ((JSONObject) ((JSONObject) jsonObject.getJSONArray("routes").get(0)).getJSONArray("legs").get(0)).getJSONObject("distance").getInt("value");
        JSONArray steps = ((JSONObject) ((JSONObject) jsonObject.getJSONArray("routes").get(0)).getJSONArray("legs").get(0)).getJSONArray("steps");
        int yardsAway = (int) (yards * multiplier);
        int currentYards = 0;
        double[] pos = null;
        JSONObject currentObject;
        for (int i = 0; i < steps.length(); i++) {
            currentObject = (JSONObject) steps.get(i);
            currentYards += currentObject.getJSONObject("distance").getInt("value");
            if (currentYards >= yardsAway) {
                pos = new double[2];
                pos[0] = currentObject.getJSONObject("start_location").getDouble("lat");
                pos[1] = currentObject.getJSONObject("start_location").getDouble("lng");
                break;
            }
        }
        if (pos == null) {
            System.out.println("Could not be recorded");
            errorOccured();
            return;
        }
        ((PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.midwayStop)).setBoundsBias(new LatLngBounds(new LatLng(pos[0], pos[1]), new LatLng(pos[0], pos[1])));
    }

    public void setMap(String result) throws JSONException {
        String newURL = "https://maps.googleapis.com/maps/api/directions/json?origin=" + originAddress + "&destination=" + destinationAddress + "&waypoints=" + result + "&key=AIzaSyAXNPhz085mn2KbU7Ti40NgRW1IARfPjec";
        final List<Address> positions = new ArrayList<>();
        Geocoder geocoder = new Geocoder(getApplicationContext());
        try {
            positions.add(geocoder.getFromLocationName(originAddress, 1).get(0));
            positions.add(geocoder.getFromLocationName(destinationAddress, 1).get(0));
            positions.add(geocoder.getFromLocationName(result, 1).get(0));
        } catch (IOException | IndexOutOfBoundsException e) {
            errorOccured();
            return;
        }
        String content;
        try {
            URLConnection connection = new URL(newURL).openConnection();
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
            scanner.close();
        } catch (Exception ex) {
            errorOccured();
            return;
        }
        final JSONObject finalDirObject = new JSONObject(content);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
//					TextView name = findViewById(R.id.name);
//					name.setText(result.getString("name"));
//					name.setVisibility(View.VISIBLE);
//					TextView addressView = findViewById(R.id.address);
//					addressView.setText(result.getString("vicinity"));
//					addressView.setVisibility(View.VISIBLE);
                    mMap.clear();
                    Log.e("JSON", String.valueOf(mMap == null));
                    List<LatLng> decodedPath = PolyUtil.decode(((JSONObject) finalDirObject.getJSONArray("routes").get(0)).getJSONObject("overview_polyline").getString("points"));
                    mMap.addPolyline(new PolylineOptions().addAll(decodedPath));
                    MarkerOptions marker;
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (Address address : positions) {
                        marker = new MarkerOptions().position(new LatLng(address.getLatitude(), address.getLongitude()));
                        mMap.addMarker(marker);
                        builder.include(marker.getPosition());
                    }
                    LatLngBounds bounds = builder.build();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, (int) (getResources().getDisplayMetrics().widthPixels * 0.15));
                    mMap.moveCamera(cameraUpdate);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void errorOccured() {
        ((PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.midwayStop)).setBoundsBias(null);
    }
}
