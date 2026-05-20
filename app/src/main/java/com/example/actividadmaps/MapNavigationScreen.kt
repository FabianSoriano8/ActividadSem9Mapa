package com.example.actividadmaps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MapNavigationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Coordenadas de la Plaza de Armas de Cajamarca
    val plazaArmasCajamarca = LatLng(-7.156595, -78.517056

    )
    
    // Estado de la cámara
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(plazaArmasCajamarca, 15f)
    }
    
    // Estado para habilitar la capa de "Mi ubicación"
    var isMyLocationEnabled by remember { mutableStateOf(value = false) }
    
    // Propiedades del mapa
    val mapProperties = remember(isMyLocationEnabled) {
        MapProperties(isMyLocationEnabled = isMyLocationEnabled)
    }

    val uiSettings = remember {
        MapUiSettings(myLocationButtonEnabled = false)
    }

    // Cliente de ubicación
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Lanzador para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            isMyLocationEnabled = true
            getCurrentLocation(context, fusedLocationClient) { location ->
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(location, 17f)
                    )
                }
            }
        }
    }

    var showPlazaMarker by remember { mutableStateOf(false) }


    Box(modifier = Modifier.fillMaxSize()) {
        // Componente de Mapa
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            // Si el estado es verdadero, Compose pintará el marcador automáticamente
            if (showPlazaMarker) {
                Marker(
                    state = rememberMarkerState(position = plazaArmasCajamarca),
                    title = "Plaza de Armas de Cajamarca",
                    snippet = "Centro histórico de la ciudad"
                )
            }
        }

        // Contenedor de botones en la parte inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón Plaza de Armas
            Button(
                onClick = {
                    // 1. Activamos el estado para que pinte el marcador
                    showPlazaMarker = true

                    // 2. Movemos la cámara
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(plazaArmasCajamarca, 17f)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text(text = "Plaza de Armas")
            }

            // Botón Mi Ubicación
            Button(
                onClick = {
                    if (hasLocationPermission(context)) {
                        isMyLocationEnabled = true
                        getCurrentLocation(context, fusedLocationClient) { location ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(location, 17f)
                                )
                            }
                        }
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text(text = "Mi Ubicación")
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineLocation || coarseLocation
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    context: Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationReceived: (LatLng) -> Unit
) {
    if (hasLocationPermission(context)) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                onLocationReceived(LatLng(it.latitude, it.longitude))
            }
        }
    }
}
