package com.campusconnect.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.campusconnect.MainActivity
import com.campusconnect.data.model.Crossing
import com.campusconnect.data.repository.AuthRepository
import com.campusconnect.data.repository.DiscoverRepository
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground service that tracks user location and detects proximity crossings.
 *
 * Architecture Decision: Uses a foreground service (required by Android for continuous
 * location access). Updates location every 30 seconds, computes geohash, writes to
 * Firestore, and checks for nearby users to record crossings.
 */
@AndroidEntryPoint
class LocationService : Service() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var discoverRepository: DiscoverRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "campus_connect_location"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val TAG = "LocationService"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            createNotificationChannel()
            setupLocationCallback()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize location services: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLocationTracking()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationTracking() {
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot start foreground service — missing permission: ${e.message}")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            30_000L // 30 seconds
        ).setMinUpdateDistanceMeters(50f) // Only update if moved 50m
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            stopSelf()
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        processLocation(location.latitude, location.longitude)
                    }
                }
            }
        }
    }

    private suspend fun processLocation(lat: Double, lng: Double) {
        val uid = authRepository.currentUser?.uid ?: return
        val geohash = GeoHashUtil.encode(lat, lng)

        // Update own location in Firestore
        discoverRepository.updateLocation(uid, lat, lng, geohash)

        // Find nearby users and record crossings
        val nearbyResult = discoverRepository.findNearbyUsers(uid, geohash)
        nearbyResult.getOrNull()?.forEach { nearbyUser ->
            val crossing = Crossing(
                userId = uid,
                otherUserId = nearbyUser.uid,
                otherUserName = nearbyUser.name,
                otherUserPhotoUrl = nearbyUser.photoUrl,
                otherUserCollege = nearbyUser.college,
                otherUserDepartment = nearbyUser.department
            )
            discoverRepository.recordCrossing(crossing)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Campus Connect is discovering people nearby"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Campus Connect")
            .setContentText("Discovering people nearby...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }
}
