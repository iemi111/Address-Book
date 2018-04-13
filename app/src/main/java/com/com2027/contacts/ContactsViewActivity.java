package com.com2027.contacts;

import android.Manifest;
import android.app.MediaRouteButton;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContactsViewActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = ContactsViewActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 145;
    private static final int PERMISSIONS_READ_EXTERNAL_STORAGE = 150;
    private static final int PERMISSIONS_REQUESTS = 170;
    private GoogleMap mMap;
    // Google API client
    private GoogleApiClient mGoogleApiClient;
    private boolean mLocationPermissionGranted;
    private boolean mPhotoPermissionGranted;
    // the current location of the user
    private Location mLastKnownLocation;
    private LatLng mCurrentLatLng;
    //default destination location, when current location is off
    private LatLng london = new LatLng(51.5, -0.118);
    private LatLng latLngOfAddress;
    private Uri photoUri;
    private ImageView photoView;
    private String nameOfContact;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_view);


        photoView = (ImageView) findViewById(R.id.contact_view_image);

        // Build the Play services client for use by the Fused Location Provider
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();

        if (intentionViewContact()) {
            Contact contact = updateOrigContact();
            FormHelper fill = new FormHelper(this);
            //get the values from Contact and set them to the Text Views
            fill.fillViewForm(contact);
            //get the name of the Contact
            nameOfContact = contact.getName();
            //get the LatLng
            if(contact.getLatitude() != 0.0 && contact.getLongitude() != 0.0) {
                latLngOfAddress = new LatLng(contact.getLatitude(), contact.getLongitude());
            }

            //get image path if present
            if(contact.getImage()!=null)
                photoUri = Uri.parse(contact.getImage());
            displayPhoto();
        }

        // Set up the Back button
        Button buttonBack = (Button) findViewById(R.id.contacts_view_back);
        buttonBack.setOnClickListener( new View.OnClickListener() {
            // Method for action at button click. Goes back to ContactsListActivity.
            @Override
            public void onClick(View v) {
                Intent list = new Intent(ContactsViewActivity.this, ContactsListActivity.class);
                startActivity(list);
                finish();
            }
        });

    }


    private void displayPhoto(){
        try {
            //if the user has provided a photo for the contact
            if(photoUri!=null) {
                //set up a bitmap using it
                requestPermission();

                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        mPhotoPermissionGranted = true;
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(ContactsViewActivity.this.getContentResolver(), photoUri);
                        //fix the orientation
                        if (bitmap.getHeight() < bitmap.getWidth()) {
                            photoView.setRotation(90);
                        }
                        photoView.setImageBitmap(bitmap);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(ContactsViewActivity.this, "Image path could not be found", Toast.LENGTH_LONG).show();
        }
    }


    private Contact updateOrigContact() {
        Intent intent = getIntent();
        Contact contact = (Contact) intent.getSerializableExtra("contact");
        return contact;
    }

    private boolean intentionViewContact() {
        return getIntent().hasExtra("contact");
    }


    // Method creating options menu.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contact_view, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Method responding to item selected in options menu (Help Icon). Shows Toast notification.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ContactDB condb = new ContactDB(this);
        Toast.makeText(ContactsViewActivity.this, "Your contact's information is displayed here.", Toast.LENGTH_LONG).show();
        return super.onOptionsItemSelected(item);
    }



    //When map is ready, check permission, display route if current location is available
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //if the contact has an address get the current location
        if(latLngOfAddress !=null) {
            //request permission for location
            requestPermission();
            //if permission is not granted
            if(!mLocationPermissionGranted){
                //display only the address of the contact
                mMap.addMarker(new MarkerOptions().position(latLngOfAddress).title(nameOfContact + "'s address"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngOfAddress, 16));
            } else {
                //get the current location of the user
                getDeviceLocation();
                //else set the map to London

                if (mLastKnownLocation != null && mCurrentLatLng != null) {
                    //add marker for current location
                    mMap.addMarker(new MarkerOptions().position(mCurrentLatLng).title("Current location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    mMap.addMarker(new MarkerOptions().position(latLngOfAddress).title(nameOfContact + "'s address"));
                    //display the route to the address
                    getDirections();

                }
            }

        } else {
            //if no address is available, display London
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(london, 9));
        }
    }


    //Build map when everything is connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        map.getMapAsync(this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google Play services connection suspended");
    }

    //when there is a failure when connecting to the Google Play services client
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Google Play services connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }



    //Get the current location of user
    private void getDeviceLocation() {
        //request location permission
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        //get the best current location
        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
            if(mLastKnownLocation!=null)
                mCurrentLatLng = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
        }
    }


    //Getting the points for the route and adjusting the map's size
    private void getDirections() {

        getDeviceLocation();

        //the direction to the address for walking
        String parameters = "origin=" + mCurrentLatLng.latitude + "," + mCurrentLatLng.longitude + "&" +
                "destination=" + latLngOfAddress.latitude + "," + latLngOfAddress.longitude + "&" + "sensor=false&mode=walking";
        String url = "https://maps.googleapis.com/maps/api/directions/json?" + parameters;

        // Start downloading json data from Google Directions API
        URLTask getUrl = new URLTask();
        getUrl.execute(url);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(mCurrentLatLng);
        builder.include(latLngOfAddress);
        LatLngBounds bounds = builder.build();


        int distance = getDistance(mLastKnownLocation,latLngOfAddress);

        if(distance < 1.5){
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,300));
            } else if(distance < 3){
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,200));
        } else{
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,150));
        }

    }

    private int getDistance(Location x, LatLng y){
        try{
            Location b = new Location("B");
            b.setLatitude(y.latitude);
            b.setLongitude(y.longitude);
            //convert the result to km instead of meters
            return (Float.valueOf(x.distanceTo(b)).intValue())/1000 ;//convert the meters to km
        }catch (Exception e){
            //if an error returns 0 distance
            return 0;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }


    // Fetches data from url with the LatLng of both locations
    // https://www.codeproject.com/Articles/1113585/Google-Maps-Draw-Route-between-two-points-using-Go
    private class URLTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }


    // https://www.codeproject.com/Articles/1113585/Google-Maps-Draw-Route-between-two-points-using-Go
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... params) {
            // For storing data from web service
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;


            try {
                jObject = new JSONObject(params[0]);
                Log.d("ParserTask", params[0].toString());
                JSONDirections parser = new JSONDirections();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);
            }
            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }

    }


    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    getDirections();
                } else {
                    Toast.makeText(getApplicationContext(), "Unless current location is granted, " +
                            "route to this address would not be shown", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case PERMISSIONS_REQUESTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    getDirections();
                }
                if (grantResults.length > 0
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mPhotoPermissionGranted = true;
                    displayPhoto();
                }
            }
            case PERMISSIONS_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPhotoPermissionGranted = true;
                    displayPhoto();
                }
            }
        }
    }

    //check permission for location and photo display
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                mPhotoPermissionGranted = true;
            }
        }
    }


    private void requestPermission() {

        checkPermission();

        if(!mPhotoPermissionGranted && !mLocationPermissionGranted && photoUri!=null){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUESTS);
                }
            }
        } else if (!mLocationPermissionGranted){
            //the app doesn't stop when we ask the permission, result is handled by onRequestPermissionsResult
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else if (!mPhotoPermissionGranted && photoUri!=null) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                PERMISSIONS_READ_EXTERNAL_STORAGE);
                    }
                }
            }
        }

    }


}
