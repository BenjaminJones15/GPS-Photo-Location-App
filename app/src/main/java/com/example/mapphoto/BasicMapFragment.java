package com.example.mapphoto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 *  Shows a simple map.
 *
 *  This one, with the same code as compassFragment won't draw in bottomnavview the second time it
 *  is clicked.  but the others work just fine?!  I'm seeing this in other apps too, so something
 *  about how the map and bottomnavview interact with each other.  this was org using a viewpager,
 *  which still works just fine.
 *
 */
public class BasicMapFragment extends Fragment implements OnMapReadyCallback {

    ImageView iv;
    String imagefile;
    Uri mediaURI;

    FloatingActionButton CameraButton;
    ActivityResultLauncher<Intent> ActivityResultNoPic, ActivityResultlocal, ActivityResultSD;

    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    // for checking permissions.
    ActivityResultLauncher<String[]> rpl_onConnected, rpl_startLocationUpdates;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private FusedLocationProviderClient mFusedLocationClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private SettingsClient mSettingsClient;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;

    private GoogleMap map;
    View myView;
    String TAG = "BasicMapFragment";
    private ArrayList<LatLng> locationArrayList;
    private ArrayList<String> names;

    public BasicMapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Because of the maps, we need to have the view inflated only once (viewpager, may call this multiple times
        // so if this is the first time, ie myView is null, then do the setup, otherwise, "reset" the view, by removing it
        // and return the already setup view.
        if (myView == null) {
            myView = inflater.inflate(R.layout.basicmap_fragment, container, false);
            Log.d(TAG, "new view");
        } else {
            ((ViewGroup) container.getParent()).removeView(myView);
            Log.d(TAG, "old view, returning now.");  //likely this one will stop showing.
            return myView;
        }
        //in a fragment
        ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        //in an activity
        //((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        ActivityResultlocal = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

            }
        });

        rpl_onConnected = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> isGranted) {
                        if (allPermissionsGranted()) {

                            getLastLocation();
                        } else {
                            Toast.makeText(getContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        mSettingsClient = LocationServices.getSettingsClient(getActivity());

        createLocationRequest();
        createLocationCallback();
        buildLocationSettingsRequest();
        getLastLocation();

        //for the local picture returns.
        ActivityResultlocal = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

                if (result.getResultCode() == Activity.RESULT_OK) {

                } else {
                    Toast.makeText(getContext(), "Request was canceled.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        locationArrayList = new ArrayList<>();
        names = new ArrayList<>();

        CameraButton = myView.findViewById(R.id.floatingActionButton);       //listener for FAB
        CameraButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {

                getLastLocation();

                Double LatDoub = mLastLocation.getLatitude();
                Double LongDoub = mLastLocation.getLongitude();
                LatLng pos = new LatLng(LatDoub, LongDoub);
                String Lat = Double.toString(LatDoub);
                String Long = Double.toString(LongDoub);

                File storageDir = myView.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File mediaFile = new File(storageDir.getPath() + File.separator + "IMG_" + Lat + "_" + Long + ".jpg");
                Uri photoURI = FileProvider.getUriForFile(myView.getContext().getApplicationContext(),"com.example.mapphoto.fileprovider",mediaFile);
                String UriString = photoURI.toString();
                imagefile = mediaFile.getAbsolutePath();  //we need to store value to use on the return.
                Log.wtf("File", imagefile);

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                ActivityResultlocal.launch(intent);

                String mytitle = storageDir.getPath() + File.separator + "IMG_" + Lat + "_" + Long + ".jpg";

                locationArrayList.add(pos);
                names.add(mytitle);

                for (int i = 0; i < locationArrayList.size(); i++) {
                    map.addMarker(new MarkerOptions()
                            .position(locationArrayList.get(i))
                            .title(names.get(i))
                            .snippet("Photo")
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.ic_launcher))
                    );
                }

            }
        });

        return myView;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        //now that we have the map, add some things.

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MainActivity.LARAMIE, 15));

        // Zoom in, animating the camera.
        map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);

        // Sets the map type to be "hybrid"
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL); //normal map
        //map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //allow user to use zoom controls (ie the + - buttons on the map.
        map.getUiSettings().setZoomControlsEnabled(true);

        //add a marker click event.
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(@NonNull Marker myMarker) {

                String title = myMarker.getTitle();
                Bundle bundle = new Bundle();
                bundle.putString("path", title);

               // ShowPicture pic = new ShowPicture();
               // pic.onCreate(bundle);
                ShowPicture pic = ShowPicture.newInstance(title);
                pic.show(requireActivity().getSupportFragmentManager(), "dialog");
                return false; //so the default action is shown as well.
            }

        });


        //add map click listener.
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(@NonNull LatLng point) {

                Toast.makeText(requireContext(), "Lat: " + point.latitude + " Long:" + point.longitude, Toast.LENGTH_SHORT).show();
            }

        });

    }



    /**
     * Uses a LocationSettingsRequest.Builder to build a LocationSettingsRequest that is used
     * for checking if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }


    /**
     * helper function to create a locationRequest for the variable. otherwise, this could be
     * done in onCreate.
     */
    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest.Builder(100000)  //create a request with 10000 interval and default rest.
                //now set the rest of the pieces we want to change.
                //.setIntervalMillis(10000)  //not needed, since it is part of the builder.
                .setMinUpdateIntervalMillis(50000)  //get an update no faster then 5 seconds.
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setWaitForAccurateLocation(true)  //waits a couple of second initially for a accurate measurement.
                .setMaxUpdateDelayMillis(20000)  //wait only 20 seconds max between
                .build();
    }

    /**
     * Creates a callback for receiving location events.  again a helper function, this could
     * also be done in onCreate.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mLastLocation = locationResult.getLastLocation();

            }
        };
    }

    /**
     *  This shows how to get a "one off" location.  instead of using the location updates shown in
     *  above the methods.
     */
    @SuppressLint("MissingPermission") //I'm really checking, but studio can't tell.
    public void getLastLocation() {
        //first check to see if I have permissions (marshmallow) if I don't then ask, otherwise start up the demo.
        if (!allPermissionsGranted()) {

            rpl_onConnected.launch(REQUIRED_PERMISSIONS);
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            return;
                        }
                        mLastLocation = location;
                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getLastLocation:onFailure", e);

                    }
                });

    }



    //helper function to check if all the permissions are granted.
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}
