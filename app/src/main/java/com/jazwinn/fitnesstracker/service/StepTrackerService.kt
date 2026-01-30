package com.jazwinn.fitnesstracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jazwinn.fitnesstracker.MainActivity
import com.jazwinn.fitnesstracker.R
import com.jazwinn.fitnesstracker.data.repository.StepsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat

@AndroidEntryPoint
class StepTrackerService : Service(), SensorEventListener {

    @Inject
    lateinit var stepsRepository: StepsRepository

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // We need to track offset if phone reboots or sensor resets, 
    // but for simple implementation we'll focus on just getting the sensor value.
    // Ideally we store the 'startOfDay' sensor value.
    private var initialStepCount = -1L

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        startForegroundService()
        
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun startForegroundService() {
        val channelId = "step_tracker_channel"
        val channelName = "Step Tracking"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Fitness Tracker")
            .setContentText("Tracking your steps...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val sensorSteps = it.values[0].toLong()
                
                // Very basic logic: we just assume sensorSteps is the total for now.
                // In production, we'd resetting logic at midnight.
                // For MVP: We persist this number to DB. Repository logic handles overwrite.
                // We should probably handle "steps since service start" or similar logic.
                
                serviceScope.launch {
                    stepsRepository.updateSteps(sensorSteps)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, StepTrackerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
         fun stop(context: Context) {
            val intent = Intent(context, StepTrackerService::class.java)
            context.stopService(intent)
        }
    }
}
