package com.projeto.gpsopcua

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder
import org.eclipse.milo.opcua.stack.client.DiscoveryClient
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnConectar: Button
    private lateinit var btnParar: Button

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var latitude = 0.0
    private var longitude = 0.0

    // OPC UA
    private val serverUrl = "opc.tcp://192.168.0.155:4840/gps"
    private var opcClient: OpcUaClient? = null
    private var jobEnvio: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // NodeIds dos nós no servidor
    private val nodeLatitude  = NodeId(2, "GPS/Latitude")
    private val nodeLongitude = NodeId(2, "GPS/Longitude")
    private val nodeTimestamp = NodeId(2, "GPS/Timestamp")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Compatibilidade Netty/Milo com Android
        System.setProperty("io.netty.noUnsafe", "true")
        System.setProperty("io.netty.noKeySetOptimization", "true")
        System.setProperty("io.netty.allocator.type", "unpooled")

        setContentView(R.layout.activity_main)

        tvLatitude  = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvStatus    = findViewById(R.id.tvStatus)
        btnConectar = findViewById(R.id.btnConectar)
        btnParar    = findViewById(R.id.btnParar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnConectar.setOnClickListener { iniciar() }
        btnParar.setOnClickListener    { parar()   }

        pedirPermissaoGPS()
    }

    private fun pedirPermissaoGPS() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    }

    private fun iniciarGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    latitude  = it.latitude
                    longitude = it.longitude
                    runOnUiThread {
                        tvLatitude.text  = "%.6f".format(latitude)
                        tvLongitude.text = "%.6f".format(longitude)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun iniciar() {
        btnConectar.isEnabled = false
        btnParar.isEnabled    = true
        atualizarStatus("Conectando...", "#FF9800")
        iniciarGPS()

        jobEnvio = scope.launch {
            try {
                val endpoints = DiscoveryClient.getEndpoints(serverUrl).get()
                val endpoint = endpoints.first()

                val config = OpcUaClientConfigBuilder()
                    .setEndpoint(endpoint)
                    .build()

                opcClient = OpcUaClient.create(config)
                opcClient!!.connect().get()

                atualizarStatus("Conectado ✓", "#4CAF50")

                while (isActive) {
                    escreverGPS()
                    delay(2000)
                }
            } catch (e: Exception) {
                atualizarStatus("Erro: ${e.message}", "#F44336")
                runOnUiThread {
                    btnConectar.isEnabled = true
                    btnParar.isEnabled    = false
                }
            }
        }
    }

    private suspend fun escreverGPS() {
        val ts = LocalDateTime.now().toString()
        opcClient?.let { client ->
            client.writeValue(nodeLatitude,  DataValue(Variant(latitude),  null, null)).get()
            client.writeValue(nodeLongitude, DataValue(Variant(longitude), null, null)).get()
            client.writeValue(nodeTimestamp, DataValue(Variant(ts),        null, null)).get()
        }
    }

    private fun parar() {
        jobEnvio?.cancel()
        opcClient?.disconnect()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        atualizarStatus("Desconectado", "#F44336")
        btnConectar.isEnabled = true
        btnParar.isEnabled    = false
    }

    private fun atualizarStatus(msg: String, cor: String) {
        runOnUiThread {
            tvStatus.text = msg
            tvStatus.setTextColor(android.graphics.Color.parseColor(cor))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        parar()
        scope.cancel()
    }
}