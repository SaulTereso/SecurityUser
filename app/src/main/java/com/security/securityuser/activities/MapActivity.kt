package com.security.securityuser.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.security.securityuser.R
import com.security.securityuser.databinding.ActivityMapBinding
import com.security.securityuser.fragments.ModalBottomSheetMenu
import com.security.securityuser.providers.AuthProvider
import com.security.securityuser.providers.GeoProvider

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {
    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null
    private var myLocationLating: LatLng? = null
    private var MarkerUser: Marker? = null
    private var geoProvider = GeoProvider()
    private var authProvider = AuthProvider()

    private var modalMeu = ModalBottomSheetMenu()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply{
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)
        LocationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))

        binding.btnConnect.setOnClickListener { connectUser() }
        binding.btnDisconnect.setOnClickListener { disconnectUser() }
        binding.imageViewMenu.setOnClickListener{showModalMenu()}
    }

    var LocationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permission ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            when{
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ->{
                  Log.d("Localizacion", "Permiso concedido")
                    //easyWayLocation?.startLocation();
                    checkIfUserisConnect()
                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) ->{
                    Log.d("Localizacion", "Permiso concedido con limitacion")
                    //easyWayLocation?.startLocation();
                    checkIfUserisConnect()
                }
                else -> {
                    Log.d("Localizacion", "Permiso denegado")
                }
            }
        }
    }

    private fun checkIfUserisConnect(){
        geoProvider.getLocation(authProvider.getId()).addOnSuccessListener { document ->
            if (document.exists()){
                if (document.contains("l")){
                    connectUser()
                }
                else{
                    showButtonDisconnect()
                }
            }
            else{
                showButtonConnect()
            }
        }
    }

    private fun showModalMenu(){
        modalMeu.show(supportFragmentManager, ModalBottomSheetMenu.TAG)
    }

    private fun saveLocation(){
        if (myLocationLating != null){
            geoProvider.saveLocation(authProvider.getId(), myLocationLating!!)
        }
    }

    private fun disconnectUser(){
        easyWayLocation?.endUpdates()
        if (myLocationLating != null){
            geoProvider.removeLocation(authProvider.getId())
            showButtonConnect()
        }
    }

    private fun connectUser(){
        easyWayLocation?.endUpdates() // OTROS HILOS DE EJECUCION
        easyWayLocation?.startLocation()
        showButtonDisconnect()
    }

    private fun showButtonConnect(){
        binding.btnDisconnect.visibility = View.GONE // OCULTANDO EL BOTON DE DESCONECTARSE
        binding.btnConnect.visibility = View.VISIBLE // MOSTRANDO EL BOTON DE CONECTARSE
    }

    private fun showButtonDisconnect(){
        binding.btnDisconnect.visibility = View.VISIBLE // MOSTRANDO EL BOTON DE DESCONECTARSE
        binding.btnConnect.visibility = View.GONE // OCULATNDO EL BOTON DE CONECTARSE
    }

    private fun addMarker(){
        val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.new_ubicacion_usuario)
        val markerIcon = getMarkerFromDrawable(drawable!!)
        if (MarkerUser != null){

            //No redibujar el icono
            MarkerUser?.remove()
        }

        if (myLocationLating != null){
            MarkerUser = googleMap?.addMarker(
                MarkerOptions()
                    .position(myLocationLating!!)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .icon(markerIcon)
            )
        }
    }

    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor{
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0,0,100,100)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    //Se ejecuta cada que se abre la pantalla actual
    override fun onResume() {
        super.onResume()
        //easyWayLocation?.startLocation()
    }

    //Se ejecuta cuando se cierra la app o pasa a otra activity
    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        //easyWayLocation?.startLocation();

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        googleMap?.isMyLocationEnabled = false

        try {
            val sucess = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )
            if (!sucess!!){
                Log.d("Mapas", "No se encontro el estilo")
            }
        }catch (e: Resources.NotFoundException){
            Log.d("Mapas", "Error: ${e.toString()}")
        }
    }

    override fun locationOn() {

    }

    //Actuluzacion de la posicion en tiempo real
    override fun currentLocation(location: Location) {
        //Latitud y Longitud de la posicion actual
        myLocationLating = LatLng(location.latitude, location.longitude)

        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(myLocationLating!!).zoom(17f).build()
        ))
        addMarker()
        saveLocation()
    }

    override fun locationCancelled() {

    }


}