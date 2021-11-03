package com.example.geoo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if(geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("Geoo", errorMessage)
            return
        }

        //This indicates whether the was an entry or exit transition
        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        when(geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> for(geofence: Geofence in triggeringGeofences) {
                Log.d("Geoo", "User entered with details - $geofence")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> for(geofence: Geofence in triggeringGeofences) {
                Log.d("Geoo", "User exited with details - $geofence")
            }
            else -> Log.e("Geoo", "Invalid transition Type - $geofenceTransition")
        }
    }
}