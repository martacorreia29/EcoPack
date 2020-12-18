package fcul.cm.g20.ecopack.fragments.map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import fcul.cm.g20.ecopack.R;
import fcul.cm.g20.ecopack.fragments.map.company.CompanyFragment;

// TODO: TRATAR DA ROTAÇÃO E CRIAR LISTENER PARA RETER AS COORDENADAS (NO CREATE STORE FRAGMENT) E A STRING PROCURADA NA ROTAÇÃO
// TODO: VER OS TIPOS DE EXCEÇÃO E ERROS DE CONECTIVIDADE NO ONCREATE() E ONCREATEVIEW() NA OBTENÇÃO DAS LOJAS

public class MapFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private int DEFAULT_MAP_ZOOM = 16;
    private FirebaseFirestore database;
    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private LatLng currentCoordinates;
    private Marker userLastLocationMarker;
    private boolean isMenuOpen = false;
    private HashMap<String, DocumentSnapshot> markersMap;
    private List<DocumentSnapshot> stores;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View mapFragment = inflater.inflate(R.layout.fragment_map, container, false);
        setupFragment(mapFragment);
        return mapFragment;
    }

    private void setupFragment(final View mapFragment) {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        Objects.requireNonNull(supportMapFragment).getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;

                markersMap = new HashMap<>();
                try {
                    database.collection("stores")
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        stores = task.getResult().getDocuments();
                                        for (DocumentSnapshot store : stores) {
                                            LatLng storeCoordinates = new LatLng((double) store.get("lat"), (double) store.get("lng"));

                                            // TODO: ADICIONAR IFS PARA OS COUNTERS

                                            Marker currMarker = map.addMarker(new MarkerOptions().position(storeCoordinates).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                            markersMap.put(currMarker.getId(), store);
                                        }
                                    } else showToast("A: " + Objects.requireNonNull(task.getException()).toString());
                                }
                            });
                } catch (Exception e) {
                    showToast("B: " + e.getMessage());
                }

                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) buildGoogleApiClient();
                else showToast("Não foi possível obter a sua localização. Por favor, dê as permissões necessárias.");

                // LOCALIZAÇÃO TEMPORÁRIA, NO CASO DO UTILIZADOR TER O SERVIÇO DE LOCALIZAÇÃO DESLIGADO, A LOCALIZAÇÃO NÃO TER SIDO AINDA CALCULADA OU NÃO HAVER PERMISSÕES
                LatLng defaultCoordinates = new LatLng(38.71667, -9.13333);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCoordinates, DEFAULT_MAP_ZOOM));

                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        createFragment(new CompanyFragment());
                        return false;
                    }
                });

                googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(final LatLng latLng) {
                        new AlertDialog.Builder(getActivity(), R.style.AlertDialogMap)
                                .setTitle("Registar estabelecimento")
                                .setMessage("Tem a certeza que quer registar um estabelecimento neste local?")
                                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                                            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                                            String address = addresses.get(0).getAddressLine(0);
                                            createFragment(new CreateStoreFragment(latLng.latitude, latLng.longitude, address));
                                        } catch (IOException e) {
                                            showToast("Não foi possível registar o estabelecimento. Por favor, verifique a sua conexão à Internet.");
                                        }
                                    }
                                })
                                .setNegativeButton("Não", null)
                                .show();
                    }
                });
            }
        });

        final SearchView searchView = mapFragment.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String addressInput = searchView.getQuery().toString();

                if (!addressInput.equals("")) {
                    try {
                        Geocoder geocoder = new Geocoder(getContext());
                        List<Address> addressList = geocoder.getFromLocationName(addressInput, 1);
                        if (addressList == null || addressList.size() == 0) showToast("Não foi possível obter a localização. Localização não existente.");
                        else {
                            Address address = addressList.get(0);
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_MAP_ZOOM));
                        }
                    } catch (IOException e) {
                        showToast("Não foi possível obter a localização. Por favor, verifique a sua conexão à Internet.");
                        return false;
                    }
                } else showToast("Por favor, insira uma localização.");

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        mapFragment.findViewById(R.id.marker_information_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFragment(new MarkersInformationFragment());
            }
        });

        mapFragment.findViewById(R.id.create_information_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFragment(new CreateInformationFragment());
            }
        });

        mapFragment.findViewById(R.id.center_map_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCoordinates != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, DEFAULT_MAP_ZOOM));
            }
        });

        mapFragment.findViewById(R.id.menu_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMenuOpen) {
                    isMenuOpen = false;
                    mapFragment.findViewById(R.id.marker_information_button).setVisibility(View.INVISIBLE);
                    mapFragment.findViewById(R.id.create_information_button).setVisibility(View.INVISIBLE);
                    mapFragment.findViewById(R.id.center_map_button).setVisibility(View.INVISIBLE);
                } else {
                    isMenuOpen = true;
                    mapFragment.findViewById(R.id.marker_information_button).setVisibility(View.VISIBLE);
                    mapFragment.findViewById(R.id.create_information_button).setVisibility(View.VISIBLE);
                    mapFragment.findViewById(R.id.center_map_button).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void createFragment(Fragment fragment) {
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_map, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
        if (userLastLocationMarker == null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, DEFAULT_MAP_ZOOM));
        else userLastLocationMarker.remove();
        userLastLocationMarker = map.addMarker(new MarkerOptions().position(currentCoordinates).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1100);
        locationRequest.setFastestInterval(1100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(2000);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}