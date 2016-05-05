package mis2.milad.mis_2;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraChangeListener {

    private GoogleMap mMap;
    private EditText editText;
    private int MY_LOCATION_REQUEST_CODE = 1;
    private HashMap<String,Marker> markers  = new HashMap<>();
    private HashMap<String, Circle> circles = new HashMap<>();
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
         this.mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        this.mapFragment.getMapAsync(this);


        this.editText = (EditText) findViewById(R.id.marker_title);
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        super.onStop();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);
        //load locations from shared preferences
        this.loadMarkersToMap();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                mMap.setMyLocationEnabled(true);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_LOCATION_REQUEST_CODE);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }

            } else {
                Toast.makeText(this,"current location is disabled", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadMarkersToMap(){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        Map<String, ?> markers = sharedPref.getAll();
        for (Map.Entry<String, ?> entry : markers.entrySet()){
            String marker_name = entry.getKey();
            String latLngStr   = (String) entry.getValue();
            String[] components = latLngStr.split(",");
            LatLng ln = new LatLng(Double.parseDouble(components[0]), Double.parseDouble(components[1]));
            this.addingMarker(marker_name, ln);
        }
    }

    private void addingMarker(String name, LatLng ln) {
        MarkerOptions options = new MarkerOptions();
        options.title(name);
        options.position(ln);
        Marker m = mMap.addMarker(options);
        this.markers.put(m.getTitle(), m);

    }


    @Override
    public void onMapLongClick(LatLng latLng) {

        String marker_name = this.editText.getText().toString();
        if(marker_name.isEmpty()) {
            Toast.makeText(this,"Please add a name for your marker on the top edit text!!", Toast.LENGTH_LONG).show();
            return;
        }

        this.addingMarker(marker_name, latLng);

        String latLngStr =  latLng.latitude + "," + latLng.longitude;
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(marker_name, latLngStr);
        editor.commit();

    }

    public void drawCircleMarkers(LatLng target, float zoom) {
        Location targetLoc = new Location("");
        targetLoc.setLatitude(target.latitude);
        targetLoc.setLongitude(target.longitude);

        Iterator<String> it = this.markers.keySet().iterator();
        while(it.hasNext()){
            String mName = it.next();
            Marker m = this.markers.get(mName);

            if(!this.mMap.getProjection().getVisibleRegion().latLngBounds.contains(m.getPosition())) {

                Location markerLoc = new Location("");
                markerLoc.setLatitude(m.getPosition().latitude);
                markerLoc.setLongitude(m.getPosition().longitude);

                float radius = findPointDistance(markerLoc);

                CircleOptions circleOptions = new CircleOptions()
                        .center(m.getPosition()).strokeColor(Color.RED).radius(radius); // In meters

                //check if we already drawing the circle
                if(this.circles.containsKey(mName)){
                    Circle circle = this.circles.get(mName);
                    circle.setVisible(true);
                    circle.setCenter(circleOptions.getCenter());
                    circle.setStrokeColor(circleOptions.getStrokeColor());
                    circle.setRadius(circleOptions.getRadius());
                } else {
                    // Get back the mutable Circle
                    Circle circle = mMap.addCircle(circleOptions);
                    this.circles.put(mName, circle);

                }
            } else {
                if(this.circles.containsKey(mName)) {
                    Circle circle = this.circles.get(mName);
                    circle.setVisible(false);
                }
            }


        }
    }

    private float findPointDistance(Location markerLoc) {

        LatLng ln = new LatLng(markerLoc.getLatitude(), markerLoc.getLongitude());
        Point position = this.mMap.getProjection().toScreenLocation(ln);

        int width = this.mapFragment.getView().getWidth();
        int height = this.mapFragment.getView().getHeight();
        float zoom = this.mMap.getMaxZoomLevel();

        LatLng ne = this.mMap.getProjection().getVisibleRegion().latLngBounds.northeast;
        LatLng sw = this.mMap.getProjection().getVisibleRegion().latLngBounds.southwest;
        LatLng se = new LatLng(sw.latitude, ne.longitude);
        LatLng nw = new LatLng(ne.latitude, sw.longitude);

        Location loc = new Location("");
        boolean locationFound = false;
        if(position.x < 0 && position.y > 0) {//to the left
            loc.setLongitude(nw.longitude);
            loc.setLatitude(markerLoc.getLatitude());
            Log.i("app", "west--------");
            locationFound = true;
        }

        if(position.x > 0 && position.y < 0) {// to the up
            loc.setLongitude(markerLoc.getLongitude());
            loc.setLatitude(nw.latitude);
            Log.i("app", "north--------");
            locationFound = true;
        }

        if(position.x > width && position.y < height) {// to the right
            loc.setLongitude(ne.longitude);
            loc.setLatitude(markerLoc.getLatitude());
            Log.i("app", "east--------");
            locationFound = true;
        }

        if(position.x < width && position.y > height) {// to the down
            loc.setLongitude(markerLoc.getLongitude());
            loc.setLatitude(se.latitude);
            Log.i("app", "north--------");
            locationFound = true;
        }

        if(locationFound) {
            float distance = loc.distanceTo(markerLoc) + (700000 * (1/zoom));
            return distance;
        } else {
            return 0.0f;
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        drawCircleMarkers(cameraPosition.target, cameraPosition.zoom);
    }
}
