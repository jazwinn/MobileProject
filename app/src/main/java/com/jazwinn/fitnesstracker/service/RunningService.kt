package com.jazwinn.fitnesstracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.jazwinn.fitnesstracker.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.app.ServiceCompat

@AndroidEntryPoint
class RunningService : Service() {

    @Inject
    lateinit var runningSensorManager: RunningSensorManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Global state accessible by UI (Simplified for this architecture)
    // Ideally this would be pushed to a repository or bounded context
    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _locationUpdates = MutableStateFlow<List<Location>>(emptyList())
        val locationUpdates = _locationUpdates.asStateFlow()

        private val _currentPace = MutableStateFlow(0f) // min/km
        val currentPace = _currentPace.asStateFlow()

        private val _elapsedTime = MutableStateFlow(0L)
        val elapsedTime = _elapsedTime.asStateFlow()
        
        private val _paceHistory = MutableStateFlow<List<Float>>(emptyList())
        val paceHistory = _paceHistory.asStateFlow()
        
        fun reset() {
            _locationUpdates.value = emptyList()
            _paceHistory.value = emptyList()
            _currentPace.value = 0f
            _elapsedTime.value = 0L
        }

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    _locationUpdates.update { it + location }
                    
                    // Simple pace calc (speed is m/s -> min/km)
                    if (location.hasSpeed() && location.speed > 0) {
                        val speedMps = location.speed
                        val paceMinPerKm = (1000 / speedMps) / 60
                        _currentPace.value = paceMinPerKm
                        _paceHistory.update { it + paceMinPerKm }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> startRunning()
            ACTION_STOP -> stopRunning()
        }
        return START_STICKY
    }

    private fun startRunning() {
        reset()
        _isRunning.value = true
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        // Location Request
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(10000)
            .build()
            
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission should be checked before starting service
        }
        
        // Timer
        serviceScope.launch {
            val startTime = System.currentTimeMillis()
            while (_isRunning.value) {
                _elapsedTime.value = System.currentTimeMillis() - startTime
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun stopRunning() {
        _isRunning.value = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val channelId = "running_channel"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Running Service", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Run in Progress")
            .setContentText("Tracking your run...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this exists
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    

}
