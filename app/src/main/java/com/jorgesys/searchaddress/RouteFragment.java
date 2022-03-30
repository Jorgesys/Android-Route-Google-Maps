package com.jorgesys.searchaddress;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Created by Jorgesys on 12/02/15.
 */
public class RouteFragment extends Fragment implements RoutingListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "RouteFragment";
    //Use https://www.daftlogic.com/projects-google-maps-area-calculator-tool.htm
    private static final LatLngBounds BOUNDS_CDMX = new LatLngBounds(new LatLng(-99.37448143959045,19.626470044363725), new LatLng(-98.78848314285278,19.20493613389559));
    private static final LatLng CDMX_CENTER = new LatLng(19.4324512, -99.1329994);

    protected GoogleMap map;
    protected LatLng start;
    protected LatLng end;
    protected GoogleApiClient mGoogleApiClient;
    AutoCompleteTextView starting;
    AutoCompleteTextView destination;
    ImageView send;
    private PlacesAutoCompleteAdapter mAdapter;
    private ProgressDialog progressDialog;
    private Polyline polyline;
    private Button estimate;
    private CardView cardView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_directions, container, false);
        starting = (AutoCompleteTextView) rootView.findViewById(R.id.start);
        destination = (AutoCompleteTextView) rootView.findViewById(R.id.destination);
        send = (ImageView) rootView.findViewById(R.id.send);
        cardView = (CardView) rootView.findViewById(R.id.cardview);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        MapsInitializer.initialize(getActivity());
        mGoogleApiClient.connect();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        map = mapFragment.getMap();
        mAdapter = new PlacesAutoCompleteAdapter(getActivity(), android.R.layout.simple_list_item_1,
                mGoogleApiClient, BOUNDS_CDMX, null);

       /* Updates the bounds being used by the auto complete adapter based on the position of the map. */
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
                mAdapter.setBounds(bounds);
            }
        });

        CameraUpdate center = CameraUpdateFactory.newLatLng(CDMX_CENTER);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
        map.moveCamera(center);
        map.animateCamera(zoom);
        /*
        * Adds auto complete adapter to both auto complete
        * text views.
        * */
        starting.setAdapter(mAdapter);
        destination.setAdapter(mAdapter);
        /* Sets the start and destination points based on the values selected from the autocomplete text views. */

        starting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PlacesAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.i(TAG, "Autocomplete item selected: " + item.description);

            /* Issue a request to the Places Geo Data API to retrieve a Place object with additional details about the place. */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);
                        Log.i(TAG, "*Place (Pick-up) latitude: " + place.getLatLng().latitude + " longitude: " + place.getLatLng().latitude);
                        start = place.getLatLng();
                    }
                });

            }
        });
        destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PlacesAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.i(TAG, "Autocomplete item selected: " + item.description);
            /* Issue a request to the Places Geo Data API to retrieve a Place object with additional details about the place. */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            places.release();
                            // Request did not complete successfully
                            Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);
                        Log.i(TAG, "*Place (Drop-off) latitude: " + place.getLatLng().latitude + " longitude: " + place.getLatLng().latitude);
                        end = place.getLatLng();
                    }
                });
            }
        });

        /*
        These text watchers set the start and end points to null because once there's
        * a change after a value has been selected from the dropdown
        * then the value has to reselected from dropdown to get
        * the correct location.
        * */
        starting.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
               //No action
            }

            @Override
            public void onTextChanged(CharSequence s, int startNum, int before, int count) {
                if (start != null) {
                    start = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                 //No action
            }
        });

        destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //No action
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (end != null) {
                    end = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //No action
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                route();
            }
        });
        return rootView;
    }

    public void route() {
        if (start == null || end == null) {
            if (start == null) {
                if (starting.getText().length() > 0) {
                    starting.setError("Choose location!.");
                } else {
                    Toast.makeText(getActivity(), "Please choose a pick up point.", Toast.LENGTH_SHORT).show();
                }
            }
            if (end == null) {
                if (destination.getText().length() > 0) {
                    destination.setError("Choose location!.");
                } else {
                    Toast.makeText(getActivity(), "Please choose a destination.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            progressDialog = ProgressDialog.show(getActivity(), "Please wait...",
                    "Fetching route information...", true);
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .waypoints(start, end)
                    .build();
            routing.execute();
        }
    }


    @Override
    public void onRoutingFailure() {
        // The Routing request failed
        progressDialog.dismiss();
        Toast.makeText(getActivity(), "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingStart() {
        // The Routing Request starts
    }

    @Override
    public void onRoutingSuccess(PolylineOptions mPolyOptions, final Route route) {
        progressDialog.dismiss();
        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
        map.moveCamera(center);

        if (polyline != null)
            polyline.remove();

        polyline = null;
        //adds route to the map.
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(getResources().getColor(R.color.colorPrimaryDark));
        polyOptions.width(12);
        polyOptions.addAll(mPolyOptions.getPoints());
        polyline = map.addPolyline(polyOptions);

        //Start marker
        MarkerOptions options = new MarkerOptions()
                .position(start)
                .title("Pick up: " + route.getName())
                .snippet("Distance : " + route.getDistanceText());
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start));
        map.addMarker(options);
        //End marker
        options = new MarkerOptions()
                .position(end)
                .title("Drop off: " + route.getEndAddressText())
                .snippet("Distance : " + route.getDistanceText() + ", Duration : "  + route.getDurationText());
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end));
        map.addMarker(options);
        cardView.setVisibility(View.GONE);

    }

    @Override
    public void onRoutingCancelled() {
        Log.i(TAG, "Routing was cancelled.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, connectionResult.toString());
    }

    @Override
    public void onConnected(Bundle bundle) {
        //No action
    }

    @Override
    public void onConnectionSuspended(int i) {
        //No action
    }


}
