package com.example.piyush.smartparking;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class SearchResultMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String latitude, longitude, range;
    private float defaultZoomLevel = 14.0f;

    private GetParkingLotsTask getParkingLotsTask = null;
    private JSONObject jsonObject;
    private Polyline polylin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            latitude = extras.getString(getString(R.string.bundle_search_latitude));
            longitude = extras.getString(getString(R.string.bundle_search_longitude));
            range = extras.getString(getString(R.string.bundle_search_range));
        }
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

        // Add a marker in Sydney and move the camera
        LatLng userlocation = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
        mMap.addMarker(new MarkerOptions().position(userlocation).title(getString(R.string.title_marker_user_location)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userlocation, defaultZoomLevel ));

        getParkingLotsTask = new GetParkingLotsTask();
        getParkingLotsTask.execute((Void) null);
    }

    protected void Route(LatLng sourcePosition, LatLng destPosition, String mode) {
        final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    Document doc = (Document) msg.obj;
                    GMapV2Direction md = new GMapV2Direction();
                    ArrayList<LatLng> directionPoint = md.getDirection(doc);
                    PolylineOptions rectLine = new PolylineOptions().width(15).color(R.color.colorRoute);

                    for (int i = 0; i < directionPoint.size(); i++) {
                        rectLine.add(directionPoint.get(i));
                    }

                    polylin = mMap.addPolyline(rectLine);
                    md.getDurationText(doc);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        };

        new GMapV2DirectionAsyncTask(handler, sourcePosition, destPosition, GMapV2Direction.MODE_DRIVING).execute();
    }

    public class GetParkingLotsTask extends AsyncTask<Void, Void, Boolean> {
        private String parkingLots;

        @Override
        protected Boolean doInBackground(Void... params) {
            String url = "http://54.68.124.173:8080/SmartParking/api/user/getAllSensorsByRange";
            HttpConnectionHelper connectionHelper;
            int returnCode;

            try {
                connectionHelper = new HttpConnectionHelper(url, "POST", HttpConnectionHelper.DEFAULT_CONNECT_TIME_OUT);
                connectionHelper.setRequestProperty("Content-type", "application/json");

                JSONObjectHelper jsonObjectHelper = new JSONObjectHelper();
                jsonObjectHelper.add(getString(R.string.user_data_location), latitude + "," + longitude);
                jsonObjectHelper.add(getString(R.string.user_data_range), range);

                returnCode = connectionHelper.request_InOutput(HttpConnectionHelper.DEFAULT_READ_TIME_OUT, jsonObjectHelper.getResult());

                if (HttpURLConnection.HTTP_OK == returnCode) {
                    parkingLots = connectionHelper.getResponseString();
                }

            } catch (IOException e) {
                return false;
            }

            return HttpURLConnection.HTTP_OK == returnCode;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            getParkingLotsTask = null;

            if (success) {
                try {
                    jsonObject = new JSONObject(parkingLots);
                    JSONArray jsonArray = jsonObject.getJSONArray(getString(R.string.parking_lots_data_field_root));

                    for (int index = 0;index < jsonArray.length();++index) {
                        JSONObject parkingLotInto = jsonArray.getJSONObject(index);

                        String status = parkingLotInto.getString(getString(R.string.parking_lots_data_field_info_status));

                        if (status.equals("1")) { //status 1 means it's free parking lot
                            String location = parkingLotInto.getString(getString(R.string.parking_lots_data_field_info_location));
                            String[] latlon = location.split(", ");
                            String id = parkingLotInto.getString("idSensor");

                            LatLng parkingLotLocation = new LatLng(Double.parseDouble(latlon[0]), Double.parseDouble(latlon[1]));
                            mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.mipmap.park_icon)).position(parkingLotLocation).title(Integer.toString(index)));
                            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                                @Override
                                public boolean onMarkerClick(Marker marker) {
                                    String title = marker.getTitle();
                                    if (polylin != null) {
                                        polylin.remove();
                                    }
                                    try {
                                        JSONArray jsonArray = jsonObject.getJSONArray(getString(R.string.parking_lots_data_field_root));

                                        String location = jsonArray.getJSONObject(Integer.parseInt(title)).getString(getString(R.string.parking_lots_data_field_info_location));
                                        String[] latlon = location.split(", ");

                                        LatLng sourcePosition = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                                        LatLng destPosition = new LatLng(Double.parseDouble(latlon[0]), Double.parseDouble(latlon[1]));
                                        Route(sourcePosition, destPosition, GMapV2Direction.MODE_DRIVING);
                                    } catch (JSONException e) {}
                                    return false;
                                }
                            });
                        }

                    }

                } catch (JSONException e) {

                }
            } else {
                //TODO: Show error?
            }
        }

        @Override
        protected void onCancelled() {
            getParkingLotsTask = null;
        }
    }

    public class GMapV2DirectionAsyncTask extends AsyncTask<String, Void, Document> {

        private final String TAG = GMapV2DirectionAsyncTask.class.getSimpleName();
        private Handler handler;
        private LatLng start, end;
        private String mode;

        public GMapV2DirectionAsyncTask(Handler handler, LatLng start, LatLng end, String mode) {
            this.start = start;
            this.end = end;
            this.mode = mode;
            this.handler = handler;
        }

        @Override
        protected Document doInBackground(String... params) {

            String url = "http://maps.googleapis.com/maps/api/directions/xml?"
                    + "origin=" + start.latitude + "," + start.longitude
                    + "&destination=" + end.latitude + "," + end.longitude
                    + "&sensor=false&units=metric&mode=" + mode;

            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();
                Document doc = builder.parse(url);
                return doc;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Document result) {
            if (result != null) {
                Message message = new Message();
                message.obj = result;
                handler.dispatchMessage(message);
            } else {
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
