/**

MainActivity is the main entry point of the application. It is responsible for initializing
the application, requesting location permission, showing Google Maps, retrieving data from a
remote server, and authenticating with Firebase.
@constructor Creates a new instance of the MainActivity class.
 */
package com.locoquest.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.locoquest.app.AppModule.Companion.db
import com.locoquest.app.AppModule.Companion.guest
import com.locoquest.app.AppModule.Companion.scheduleNotification
import com.locoquest.app.AppModule.Companion.user
import com.locoquest.app.dao.DB
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User
import kotlin.math.max


class MainActivity : AppCompatActivity(), ISecondaryFragment, Home.HomeListener,
    CoinCollectedDialog.IWatchAdButtonClickListener {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var home: Home

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        Log.d("event", "MainActivity.onCreate")
        setContentView(R.layout.activity_main)

        home = Home(this)
        supportFragmentManager.beginTransaction().replace(R.id.primary_container, home).commit()

        val secFrag = savedInstanceState?.getString("secondaryFragment")
        if(secFrag != null){
            Log.d(TAG, "onCreate; secondaryFragment=$secFrag")
            secondaryFragment = when (secFrag) {
                "Profile" -> Profile(user, true, this, home)
                "CoinCollectedDialog" -> CoinCollectedDialog(this, this)
                else -> null
            }
            secondaryFragment?.let {
                supportFragmentManager.beginTransaction().replace(R.id.secondary_container, it).commit()
            }
        }

        Thread{
            db = Room.databaseBuilder(this, DB::class.java, "db")
                .fallbackToDestructiveMigration().build()
            if (auth.currentUser != null)
                user = User(auth.currentUser!!.uid)
            Log.d("user", "Database loaded, switching to user:${user.uid}")
            home.switchUser()
        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if(secondaryFragment == null) return
        val secFrag = when (secondaryFragment) {
            is Profile -> "Profile"
            is CoinCollectedDialog -> "CoinCollectedDialog"
            else -> ""
        }
        Log.d(TAG, "onSaveInstanceState; secondaryFragment=$secFrag")
        outState.putString("secondaryFragment", secFrag)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("event", "MainActivity.onRequestPermissionsResult")
        if(grantResults.isEmpty()) return
        when(requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults[0] == 0) {
                    home.startLocationUpdates()
                    home.updateCameraWithLastLocation(false)
                } else if (grantResults[0] == -1 && grantResults[1] == -1) {
                    AlertDialog.Builder(this)
                        .setTitle("Location Permissions")
                        .setMessage("LocoQuest needs your location to provide accurate directions. Please grant location permissions in settings.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", packageName, null)
                                )
                            )
                        }.show()
                }
            }
            NOTIFICATIONS_REQUEST_CODE -> if(grantResults[0] == 0) home.onNotificationsEnabled()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("event", "MainActivity.onActivityResult")
        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = Identity.getSignInClient(this).getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    user = User(auth.currentUser!!.uid,
                                        auth.currentUser!!.displayName!!,
                                        auth.currentUser!!.photoUrl.toString())
                                    home.switchUser()
                                    Log.d(TAG, "signInWithCredential:success")
                                } else {
                                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                                }
                            }
                    } else {
                        Log.d(TAG, "No ID token!")
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
                        }
                        else -> {
                            Log.d(
                                TAG, "Couldn't get credential from result." +
                                        " (${e.localizedMessage})"
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCoinCollected(benchmark: Benchmark) {
        Log.d("event", "MainActivity.onCoinCollected")
        //Toast.makeText(this, "Coin collected", Toast.LENGTH_SHORT).show()
        secondaryFragment = CoinCollectedDialog(this, this)
        supportFragmentManager.beginTransaction().replace(R.id.secondary_container, secondaryFragment!!).commit()
        if(benchmark.notify) scheduleNotification(this, benchmark)
    }

    override fun onWatchAdButtonClicked() {
        Log.d("event", "MainActivity.onWatchAdButtonClicked")
        startActivity(Intent(this, ExtraCoinAdMobActivity::class.java))
    }

    override fun onClose(fragment: Fragment) {
        Log.d("event", "MainActivity.onClose")
        supportFragmentManager.beginTransaction().remove(fragment).commit()
        secondaryFragment = null
        if(user.isBoosted()) home.monitorBoostedTimer()
        home.balance.text = user.balance.toString()
    }

    override fun onMushroomClicked() {
        Log.d("event", "MainActivity.onMushroomClicked")
        supportFragmentManager.beginTransaction().replace(R.id.secondary_container, Store(this)).commit()
    }

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        const val REQ_ONE_TAP = 2
        const val NOTIFICATIONS_REQUEST_CODE = 3
        var secondaryFragment: Fragment? = null
        private val TAG: String = MainActivity::class.java.name
    }
}