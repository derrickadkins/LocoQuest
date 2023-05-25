package com.locoquest.app

import CoinService
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.locoquest.app.AppModule.Companion.DEBUG
import com.locoquest.app.AppModule.Companion.SECONDS_TO_RECOLLECT
import com.locoquest.app.AppModule.Companion.cancelNotification
import com.locoquest.app.AppModule.Companion.scheduleNotification
import com.locoquest.app.AppModule.Companion.user
import com.locoquest.app.Converters.Companion.toMarkerOptions
import com.locoquest.app.MainActivity.Companion.secondaryFragment
import com.locoquest.app.dto.Coin
import com.locoquest.app.dto.User
import java.net.UnknownHostException

class Home(private val homeListener: HomeListener) : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    ISecondaryFragment, Profile.ProfileListener, NotifyService.Companion.Listener {

    var selectedCoin: Coin? = null
    private var switchingUser = false
    private var profile: Profile? = null
    lateinit var balance: TextView
    private var googleMap: GoogleMap? = null
    private var circle: Circle? = null
    private var selectedMarker: Marker? = null
    private var mapFragment: SupportMapFragment? = null
    private var tracking = true
    private var monitoringSelectedMarker = false
    private var loadingMarkers = false
    private var cameraIsBeingMoved = false
    private var notifyUserOfNetwork = true
    private var markerToCoin: HashMap<Marker, Coin> = HashMap()
    private var coinToMarker: HashMap<Coin, Marker> = HashMap()
    private lateinit var prefs: Prefs
    private lateinit var notifyFab: FloatingActionButton
    private lateinit var userImg: ImageView
    private lateinit var offlineImg: ImageView
    private lateinit var userLvl: TextView
    private lateinit var userName: TextView
    private lateinit var userExperience: ProgressBar
    private lateinit var layersLayout: LinearLayout
    private lateinit var layersFab: FloatingActionButton
    private lateinit var myLocation: FloatingActionButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var timeTravel: ImageView
    private lateinit var companion: ImageView
    private lateinit var drone: ImageView
    private lateinit var giant: ImageView
    private lateinit var timeTravelTimer: TextView
    private lateinit var companionTimer: TextView
    private lateinit var droneTimer: TextView
    private lateinit var giantTimer: TextView
    private lateinit var droneLayout: ConstraintLayout
    private lateinit var joystick: JoystickView
    private val inUseSkillTimers = Array<Skill?>(Skill.values().size) { null }
    private val reuseSkillTimers = Array<Skill?>(Skill.values().size) { null }

    interface HomeListener {
        fun onMushroomClicked()
        fun onCoinCollected(coin: Coin)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("tracker", "creating view")
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        mapFragment =
            childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        if (savedInstanceState != null)
            mapFragment?.onCreate(savedInstanceState)
        else mapFragment?.getMapAsync(this)

        mapFragment?.view?.setOnTouchListener { _, _ ->
            Log.d("tracker", "map fragment touched")
            updateTrackingStatus(false)
            false
        }

        myLocation = view.findViewById(R.id.my_location)
        myLocation.setImageResource(
            if (hasLocationPermissions() && isGpsOn()) R.drawable.my_location_not_tracking
            else R.drawable.location_disabled
        )
        myLocation.setOnClickListener {
            Log.d("tracker", "my location clicked")
            if(!hasLocationPermissions()){
                requestLocationPermission()
                return@setOnClickListener
            }
            if(!isGpsOn()){
                alertUserGps()
                return@setOnClickListener
            }
            updateTrackingStatus(true)
            updateCameraWithLastLocation()
            Log.d("tracker", "end of my location click fun")
        }

        layersLayout = view.findViewById(R.id.layers_layout)
        layersFab = view.findViewById(R.id.layersFab)
        layersFab.setOnClickListener { layersLayout.visibility = if(layersLayout.visibility == View.GONE) View.VISIBLE else View.GONE }
        //layersFab.visibility = if(selectedMarker == null) View.VISIBLE else View.GONE

        val layersClickListener = View.OnClickListener {
            Log.d("tracker", "layer selected")
            when(it.id){
                R.id.normalLayerFab -> {
                    prefs.mapType(GoogleMap.MAP_TYPE_NORMAL)
                }
                R.id.hybridLayerFab -> {
                    prefs.mapType(GoogleMap.MAP_TYPE_HYBRID)
                }
                R.id.satelliteLayerFab -> {
                    prefs.mapType(GoogleMap.MAP_TYPE_SATELLITE)
                }
                R.id.terrainLayerFab -> {
                    prefs.mapType(GoogleMap.MAP_TYPE_TERRAIN)
                }
            }
            googleMap?.mapType = prefs.mapType()
            layersFab.performClick()
        }

        view.findViewById<ExtendedFloatingActionButton>(R.id.normalLayerFab).setOnClickListener(layersClickListener)
        view.findViewById<ExtendedFloatingActionButton>(R.id.hybridLayerFab).setOnClickListener(layersClickListener)
        view.findViewById<ExtendedFloatingActionButton>(R.id.satelliteLayerFab).setOnClickListener(layersClickListener)
        view.findViewById<ExtendedFloatingActionButton>(R.id.terrainLayerFab).setOnClickListener(layersClickListener)

        offlineImg = view.findViewById(R.id.offline_img)
        updateNetworkStatus()

        val openProfileClickListener = View.OnClickListener {
            profile = Profile(user, true, this, this)
            secondaryFragment = profile
            activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.secondary_container, profile!!)?.commit()
        }
        userImg = view.findViewById(R.id.user_image)
        userImg.setOnClickListener(openProfileClickListener)
        userName = view.findViewById(R.id.user_display_name)
        userName.setOnClickListener(openProfileClickListener)
        userLvl = view.findViewById(R.id.user_level)
        userLvl.setOnClickListener(openProfileClickListener)
        userExperience = view.findViewById(R.id.user_exp)
        userExperience.setOnClickListener(openProfileClickListener)
        balance = view.findViewById(R.id.balance)
        displayUserInfo()

        notifyFab = view.findViewById(R.id.notify_fab)
        notifyFab.setOnClickListener {
            val coin = markerToCoin[selectedMarker]!!
            val notify = coin.notify
            coin.notify = !notify
            user.visited[coin.pid] = coin
            user.update()

            if(coin.notify){
                val notificationManger = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !notificationManger.areNotificationsEnabled()) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        MainActivity.NOTIFICATIONS_REQUEST_CODE
                    )
                    return@setOnClickListener
                }

                if(collected(coin)) scheduleNotification(requireContext(), coin)
                notifyFab.setImageResource(R.drawable.notifications_active)
            }else{
                cancelNotification(requireContext(), coin)
                notifyFab.setImageResource(R.drawable.notifications_off)
            }
        }

        timeTravel = view.findViewById(R.id.time_travel)
        companion = view.findViewById(R.id.companion)
        drone = view.findViewById(R.id.drone_skill)
        giant = view.findViewById(R.id.giant)

        timeTravelTimer = view.findViewById(R.id.time_timer)
        companionTimer = view.findViewById(R.id.companion_timer)
        droneTimer = view.findViewById(R.id.drone_timer)
        giantTimer = view.findViewById(R.id.giant_timer)

        timeTravel.visibility = if(user.skills.contains(Skill.TIME)) View.VISIBLE else View.GONE
        companion.visibility = if(user.skills.contains(Skill.COMPANION)) View.VISIBLE else View.GONE
        drone.visibility = if(user.skills.contains(Skill.DRONE)) View.VISIBLE else View.GONE
        giant.visibility = if(user.skills.contains(Skill.GIANT)) View.VISIBLE else View.GONE

        var pair = user.isSkillAvailable(Skill.TIME)
        timeTravelTimer.visibility = if(!pair.first && user.skills.contains(Skill.TIME)) {
            monitorInUseSkillTimer(Skill.TIME)
            View.VISIBLE
        } else View.GONE

        pair = user.isSkillAvailable(Skill.COMPANION)
        companionTimer.visibility = if(!pair.first && user.skills.contains(Skill.COMPANION)) {
            monitorInUseSkillTimer(Skill.COMPANION)
            View.VISIBLE
        } else View.GONE

        pair = user.isSkillAvailable(Skill.DRONE)
        droneTimer.visibility = if(!pair.first && user.skills.contains(Skill.DRONE)) {
            monitorInUseSkillTimer(Skill.DRONE)
            View.VISIBLE
        } else View.GONE

        pair = user.isSkillAvailable(Skill.GIANT)
        giantTimer.visibility = if(!pair.first && user.skills.contains(Skill.GIANT)) {
            monitorInUseSkillTimer(Skill.GIANT)
            View.VISIBLE
        } else View.GONE

        val skillClickListener = View.OnClickListener {
            when(it.id){
                R.id.giant -> {
                    if(!user.isSkillAvailable(Skill.GIANT).first)return@OnClickListener
                    user.lastUsedGiant = Timestamp.now()
                    monitorInUseSkillTimer(Skill.GIANT)
                    circle?.remove()
                    circle = googleMap?.addCircle(getMyLocationCircle())
                }
                R.id.companion -> {
                    if(!user.isSkillAvailable(Skill.COMPANION).first)return@OnClickListener
                    user.lastUsedCompanion = Timestamp.now()
                    monitorInUseSkillTimer(Skill.COMPANION)
                }
                R.id.time_travel -> {
                    if(!user.isSkillAvailable(Skill.TIME).first)return@OnClickListener
                    user.lastUsedTimeTravel = Timestamp.now()
                    monitorInUseSkillTimer(Skill.TIME)
                    loadMarkers(true)
                }
                R.id.drone_skill -> {
                    if(!user.isSkillAvailable(Skill.DRONE).first)return@OnClickListener
                    user.lastUsedDrone = Timestamp.now()
                    monitorInUseSkillTimer(Skill.DRONE)
                    droneLayout.visibility = View.VISIBLE
                    val map = googleMap ?: return@OnClickListener
                    map.uiSettings.isScrollGesturesEnabled = false
                    map.uiSettings.isZoomGesturesEnabled = false
                    map.uiSettings.isRotateGesturesEnabled = false
                    map.uiSettings.isTiltGesturesEnabled = false
                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(Converters.toLatLng(prefs.lastLocation()))
                                .zoom(DEFAULT_ZOOM)
                                .build()
                        )
                    )
                }
            }
            user.update()
        }

        giant.setOnClickListener(skillClickListener)
        companion.setOnClickListener(skillClickListener)
        drone.setOnClickListener(skillClickListener)
        timeTravel.setOnClickListener(skillClickListener)

        droneLayout = view.findViewById(R.id.drone_layout)
        joystick = view.findViewById(R.id.joystick)
        joystick.setOnJoystickMoveListener(object : JoystickView.OnJoystickMoveListener {
            override fun onJoystickMove(xPercent: Float, yPercent: Float) {
                Log.d(TAG, "joystick moved; x:$xPercent, y:$yPercent")
                if (user.isSkillInUse(Skill.DRONE).first) monitorJoystick()
            }
            override fun onJoystickRelease() {monitorJoystick = false}
        })

        return view
    }

    var monitorJoystick = false
    private fun monitorJoystick(){
        if(monitorJoystick) return
        monitorJoystick = true
        Thread{
            while(monitorJoystick){
                val map = googleMap ?: return@Thread
                var maxCameraSpeed = Skill.DRONE.effect
                if(user.upgrades.contains(Upgrade.DRONE_MOTOR)) maxCameraSpeed += Upgrade.DRONE_MOTOR.effect

                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "x:${joystick.xPercent};y:${joystick.yPercent}")
                    val cameraSpeedX = joystick.xPercent * maxCameraSpeed
                    val cameraSpeedY = joystick.yPercent * maxCameraSpeed

                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(
                                    LatLng(
                                        map.cameraPosition.target.latitude + cameraSpeedY,
                                        map.cameraPosition.target.longitude + cameraSpeedX
                                    )
                                )
                                .zoom(map.cameraPosition.zoom)
                                .build()
                        )
                    )

                    val coins = getMarkersWithinRadius(map.cameraPosition.target, 100, markerToCoin.values)
                    coins.forEach { c -> if (canCollect(c)) collectCoin(c) }
                }
                Thread.sleep(100)
            }
            monitorJoystick = false
        }.start()
    }

    private fun collectCoin(c: Coin){
        if(!coinToMarker.contains(c)) return
        val marker = coinToMarker[c]!!
        c.lastVisited = Timestamp.now().seconds
        markerToCoin[marker] = c
        user.visited[c.pid] = c
        user.balance++
        user.experience++
        balance.text = user.balance.toString()
        updateProgress()
        user.update()
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.hour_glass_6))
        marker.snippet = "Collected ${Converters.formatSeconds(c.lastVisited)}"
        scheduleSetMarkerIcon(marker, c)
    }

    private fun getMarkersWithinRadius(center: LatLng, radius: Int, coinCollection: Collection<Coin>): List<Coin> {
        return coinCollection.filter { c ->  inProximity(radius, center, LatLng(c.lat, c.lon))}
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("tracker", "saving map state")
        mapFragment?.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        Log.d("tracker", "resuming")
        mapFragment?.onResume()
        if(hasLocationPermissions() && isGpsOn())
            startLocationUpdates()
        updateNetworkStatus()
        cameraIsBeingMoved = false
        notifyUserOfNetwork = true

        balance.text = user.balance.toString()
        NotifyService.listener = this
    }

    override fun onPause() {
        super.onPause()
        Log.d("tracker", "pausing")
        stopLocationUpdates()
        mapFragment?.onPause()
        NotifyService.listener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("tracker", "destroying")
        mapFragment?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d("tracker", "low memory")
        mapFragment?.onLowMemory()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        Log.d("tracker", "map is ready")
        googleMap = map
        /*map.uiSettings.isScrollGesturesEnabled = false
        map.uiSettings.isZoomGesturesEnabled = false
        map.uiSettings.isRotateGesturesEnabled = false
        map.uiSettings.isTiltGesturesEnabled = false*/
        map.setMinZoomPreference(MIN_ZOOM)
        map.mapType = prefs.mapType()
        map.setOnMarkerClickListener(this)
        map.setOnCameraIdleListener {
            Log.d("tracker", "camera stopped moving, loading markers")
            cameraIsBeingMoved = false
            loadMarkers()
        }
        map.setOnMapClickListener {
            Log.d("tracker", "map was clicked on")
            selectedMarker = null
            notifyFab.visibility = View.GONE
            layersLayout.visibility = View.GONE
            monitoringSelectedMarker = false
            /*Thread{
                Thread.sleep(500) // wait for direction btn to hide
                Handler(Looper.getMainLooper()).post{layersFab.visibility = View.VISIBLE}
            }.start()*/
        }
        map.setOnCameraMoveStartedListener {
            Log.d("tracker", "camera started moving: tracking:$tracking")
            updateTrackingStatus(cameraIsBeingMoved)
            if(!tracking) layersLayout.visibility = View.GONE
            Log.d("tracker", "end of moving fun: tracking:$tracking")
        }

        if(tracking) {
            Log.d("tracker", "initializing map camera")
            updateCameraWithLastLocation(false)
        }
        if(hasLocationPermissions() && isGpsOn()) startLocationUpdates()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Log.d("tracker", "marker clicked on")
        marker.showInfoWindow()
        selectedMarker = marker
        //layersFab.visibility = View.GONE
        //layersLayout.visibility = View.GONE
        //updateTrackingStatus(false)

        if(!markerToCoin.contains(marker)) return true
        val coin = markerToCoin[marker]!!

        notifyFab.visibility = View.VISIBLE
        notifyFab.setImageResource(if(coin.notify) R.drawable.notifications_active else R.drawable.notifications_off)

        monitorSelectedMarker()

        if(!hasLocationPermissions() || !isGpsOn()) return false

        val lastLocation = prefs.lastLocation()
        val inProximity = if(DEBUG) true
            else inProximity(user.getReach(), marker.position, LatLng(lastLocation.latitude, lastLocation.longitude))

        //if coin is collectable
        if(canCollect(coin)) {
            if(inProximity) {
                collectCoin(coin)
                homeListener.onCoinCollected(coin)
            }else {
                marker.snippet = getSnippet(coin)
                Toast.makeText(context, "Not close enough to complete", Toast.LENGTH_SHORT).show()
            }
        }else{
            marker.snippet = getSnippet(coin)
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return true
    }

    override fun onUpdateBalance() {
        balance.text = user.balance.toString()
    }

    override fun onClose(fragment: Fragment) {
        activity?.supportFragmentManager?.beginTransaction()?.remove(fragment)?.commit()
        profile = null
        secondaryFragment = null
        balance.text = user.balance.toString()
        displayUserInfo()
    }

    override fun onCoinClicked(coin: Coin) {
        Log.d("event", "onCoinClicked")
        hideProfile()
        selectedCoin = coin
        loadMarkers(false)
    }

    override fun onLogin() {
        try {
            Log.d("event", "onLogin")

            val signUpRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.web_client_id))
                        // Show all accounts on the device.
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .build()

            Identity.getSignInClient(requireActivity()).beginSignIn(signUpRequest)
                .addOnSuccessListener(requireActivity()) { result ->
                    try {
                        activity?.startIntentSenderForResult(
                            result.pendingIntent.intentSender, MainActivity.REQ_ONE_TAP,
                            null, 0, 0, 0, null
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(requireActivity()) { e ->
                    // No Google Accounts found. Just continue presenting the signed-out UI.
                    e.localizedMessage?.let { it1 -> Log.d(TAG, it1) }
                }
        } catch (ex: java.lang.Exception) {
            ex.localizedMessage?.let { Log.d(TAG, it) }
        }
    }

    override fun onSignOut() {
        Log.d("event", "onSignOut")
        Firebase.auth.signOut()
        userImg.setImageResource(R.drawable.account)
        user = AppModule.guest
        switchUser()
    }

    private fun displayUserInfo() {
        if (user.displayName == "") {
            user.displayName = Firebase.auth.currentUser?.displayName.toString()
        }

        balance.text = user.balance.toString()
        userLvl.text = user.level.toString()
        userName.text = user.displayName
        updateProgress()

        if(this.isDetached) return

        Glide.with(this)
            .load(user.photoUrl)
            .transform(CircleCrop())
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    drawable: Drawable,
                    transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                ) {
                    userImg.setImageDrawable(drawable)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun scheduleSetMarkerIcon(marker: Marker, coin: Coin){
        Handler(Looper.getMainLooper()).postDelayed({
            try{
                marker.setIcon(getMarkerRes(coin))
                if(collected(coin)) scheduleSetMarkerIcon(marker, coin)
            }catch (e: Exception){
                Log.e("marker set icon", e.toString())
            }
        }, (SECONDS_TO_RECOLLECT * 1000 / 7).toLong())
    }

    private fun updateProgress(){
        userExperience.progress = Level.getProgress(user.level, user.experience)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            userExperience.tooltipText = "${user.experience}/${Level.getLimits(user.level).second}"
        }
    }

    fun switchUser() {
        if (switchingUser) return
        Thread {
            switchingUser = true
            val userDao = AppModule.db!!.localUserDAO()
            val tmpUser = userDao.getUser(user.uid)

            if (tmpUser == null) {
                userDao.insert(user)
            } else user = tmpUser

            Log.d("user", "user loaded from db; ${user.dump()}")

            Handler(Looper.getMainLooper()).post {
                userName.text = user.displayName
                hideProfile()

                if (user.uid == AppModule.guest.uid) {
                    loadMarkers(true)
                    displayUserInfo()
                    Log.d("user", "user switched; ${user.dump()}")
                    switchingUser = false
                    return@post
                }

                Firebase.firestore.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener {
                        if (it.data == null) {
                            user.push()
                            return@addOnSuccessListener
                        }
                        Log.d(TAG, "${it.id} => ${it.data}")

                        user = User.pullUser(it)
                        user.update()
                        loadMarkers(true)
                    }
                    .addOnFailureListener {
                        Log.d(TAG, it.toString())
                        user.push()
                        loadMarkers(true)
                    }.addOnCompleteListener {
                        //home.balance.text = user.balance.toString()
                        prefs.uid(user.uid)
                        displayUserInfo()
                        Log.d("user", "user switched; ${user.dump()}")
                        switchingUser = false
                    }
            }
        }.start()
    }

    private fun hideProfile(){
        if(profile != null){
            activity?.supportFragmentManager?.beginTransaction()?.remove(profile!!)?.commit()
            profile = null
            secondaryFragment = null
        }
    }

    private fun getSnippet(coin: Coin) : String{
        var now = Timestamp.now().seconds
        if(user.lastUsedSkill(Skill.TIME) > coin.lastVisited) now += Skill.TIME.effect.toInt()
        return if (coin.lastVisited + SECONDS_TO_RECOLLECT > now) {
            val secondsLeft = coin.lastVisited + SECONDS_TO_RECOLLECT - now
            "Collect in ${Converters.toCountdownFormat(secondsLeft)}"
        }else if(coin.lastVisited > 0) "Collected ${Converters.formatSeconds(coin.lastVisited)}"
        else "Never collected"
    }

    private fun monitorSelectedMarker(){
        if(monitoringSelectedMarker) return
        monitoringSelectedMarker = true
        Thread{
            Thread.sleep(1000)
            while (monitoringSelectedMarker){
                Handler(Looper.getMainLooper()).post{
                    if(selectedMarker == null) {
                        monitoringSelectedMarker = false
                        return@post
                    }
                    try {
                        selectedMarker!!.snippet = getSnippet(markerToCoin[selectedMarker]!!)
                        selectedMarker!!.showInfoWindow()
                    }catch (e: Exception){
                        monitoringSelectedMarker = false
                        Log.e("selected marker", e.toString())
                    }
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun updateTrackingStatus(tracking: Boolean){
        this.tracking = tracking
        myLocation.setImageResource(
            if (!hasLocationPermissions() || !isGpsOn()) R.drawable.location_disabled
            else if (tracking) R.drawable.my_location
            else R.drawable.my_location_not_tracking
        )
    }

    // Chat-GPT wrote this function
    private fun inProximity(meters: Int, latlng1: LatLng, latlng2: LatLng): Boolean {
        val R = 6371e3 // Earth's radius in meters
        val φ1 = Math.toRadians(latlng1.latitude)
        val φ2 = Math.toRadians(latlng2.latitude)
        val Δφ = Math.toRadians(latlng2.latitude - latlng1.latitude)
        val Δλ = Math.toRadians(latlng2.longitude - latlng1.longitude)

        val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        val d = R * c // distance between the two points in meters
        //val ft = d * 3.28084 // convert distance to feet

        return d <= meters
    }

    private fun loadMarkers(){loadMarkers(false)}
    private fun loadMarkers(isUserSwitched: Boolean) {
        try {
            Log.d("tracker", "loading markers")
            goToSelectedCoin()
            if (!isOnline()) {
                if (notifyUserOfNetwork) {
                    Toast.makeText(context, "No network: Can't load markers", Toast.LENGTH_SHORT)
                        .show()
                    notifyUserOfNetwork = false
                }
                return
            }
            if ((loadingMarkers && !isUserSwitched) || googleMap == null) return

            loadingMarkers = true
            val map = googleMap!!
            val bounds = map.projection.visibleRegion.latLngBounds

            Thread {
                try {
                    val coinList = CoinService().getCoins(bounds)
                    if (coinList == null) {
                        println("Error: unable to retrieve coin data")
                        return@Thread
                    }
                    if (coinList.isEmpty() || (isSameCoins(coinList) && !isUserSwitched)) {
                        loadingMarkers = false
                        return@Thread
                    }

                    val newCoins = mutableListOf<Coin>()
                    val existingCoins = mutableListOf<Coin>()

                    // Find new and existing coins
                    for (coin in coinList) {
                        val mark = if (user.visited.containsKey(coin.pid))
                            user.visited[coin.pid]!! else coin
                        if (coinToMarker.keys.contains(mark))
                            existingCoins.add(mark)
                        else newCoins.add(mark)
                    }

                    // Update marker colors after user switch
                    if (isUserSwitched) {
                        Handler(Looper.getMainLooper()).post {
                            existingCoins.forEach {
                                coinToMarker[it]?.setIcon(getMarkerRes(it))
                            }
                        }
                    }

                    // Remove markers for deleted coins
                    val iterator = coinToMarker.iterator()
                    val pids = coinList.map { it.pid }
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (!pids.contains(entry.key.pid)) {
                            val marker = entry.value
                            iterator.remove()
                            markerToCoin.remove(marker)
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    if(selectedMarker == marker){
                                        selectedMarker = null
                                        notifyFab.visibility = View.GONE
                                    }
                                    marker.remove()
                                }catch (e: Exception){
                                    Log.e("marker", e.toString())
                                }
                            }
                        }
                    }

                    // Add markers for new coins
                    Handler(Looper.getMainLooper()).post {
                        for (coin in newCoins) {
                            val marker = addCoinToMap(coin)
                            if(marker != null && collected(coin))
                                scheduleSetMarkerIcon(marker, coin)
                        }
                        loadingMarkers = false
                        Log.d("tracker", "markers loaded")
                    }
                } catch (e: ConcurrentModificationException) {
                    loadingMarkers = false
                    Log.e("LoadMarkers", e.toString())
                } catch (e: UnknownHostException) {
                    loadingMarkers = false
                    Log.e("LoadMarkers", e.toString())
                }
            }.start()
        } catch (e: Exception) {
            loadingMarkers = false
            println("Error: ${e.message}")
        }
    }

    private fun getMarkerRes(coin: Coin) : BitmapDescriptor {
        return BitmapDescriptorFactory.fromResource(
            if (user.visited.contains(coin.pid) && collected(coin)) {
                val secondsLeft = coin.lastVisited + SECONDS_TO_RECOLLECT - System.currentTimeMillis() / 1000
                when(mapToRange(secondsLeft, IntRange(0, SECONDS_TO_RECOLLECT), IntRange(0,6))){
                    0 -> R.drawable.hour_glass_0
                    1 -> R.drawable.hour_glass_1
                    2 -> R.drawable.hour_glass_2
                    3 -> R.drawable.hour_glass_3
                    4 -> R.drawable.hour_glass_4
                    5 -> R.drawable.hour_glass_5
                    6 -> R.drawable.hour_glass_6
                    else -> R.drawable.hour_glass
                }
            }
            else R.drawable.coin)
    }

    private fun mapToRange(number: Long, original: IntRange, target: IntRange): Int {
        val ratio = number.toFloat() / (original.last - original.first)
        return (ratio * (target.last - target.first)).toInt()
    }

    private fun isSameCoins(coinList: List<Coin>) : Boolean{
        return coinList.map { it.pid }.sorted() == markerToCoin.values.map { it.pid }.sorted()
    }

    private fun goToSelectedCoin() {
            if (selectedCoin == null) return
            Log.d("tracker", "going to selected coin")

            val coin = selectedCoin!!
            tracking = false
            googleMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        coin.lat,
                        coin.lon
                    ), DEFAULT_ZOOM
                )
            )

            val selectedMarker =
                if (coinToMarker.contains(coin))
                    coinToMarker[coin]
                else addCoinToMap(coin)

            selectedMarker?.showInfoWindow()

            selectedCoin = null
    }

    private fun addCoinToMap(coin: Coin) : Marker? {
        val marker = googleMap!!.addMarker(toMarkerOptions(coin).icon(getMarkerRes(coin)))
        if(marker != null) {
            markerToCoin[marker] = coin
            coinToMarker[coin] = marker
        }
        return marker
    }

    private fun collected(coin: Coin) : Boolean{
        var now = Timestamp.now().seconds
        if (user.lastUsedSkill(Skill.TIME) > coin.lastVisited) now += Skill.TIME.effect.toInt()
        return now - coin.lastVisited < SECONDS_TO_RECOLLECT
    }

    private fun canCollect(c: Coin) : Boolean{
        if(!user.visited.contains(c.pid)) return true
        var now = Timestamp.now().seconds
        if (user.lastUsedSkill(Skill.TIME) > c.lastVisited) now += Skill.TIME.effect.toInt()
        return user.visited.contains(c.pid) && now - c.lastVisited > SECONDS_TO_RECOLLECT
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        Log.d("tracker", "starting location updates: tracking:$tracking")

        fusedLocationClient.requestLocationUpdates(
            createLocationRequest(TRACKING_INTERVAL, TRACKING_FASTEST_INTERVAL),
            locationCallback,
            Looper.getMainLooper())

        googleMap?.let {
            it.isMyLocationEnabled = true
            it.uiSettings.isMyLocationButtonEnabled = false
        }
    }
    private fun stopLocationUpdates() {
        Log.d("tracker", "stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun alertUserGps() {
        AlertDialog.Builder(requireContext())
            .setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }.show()
    }

    private fun isGpsOn() : Boolean{
        val manager: LocationManager? = getSystemService(requireContext(), LocationManager::class.java)
        return manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun hasLocationPermissions() : Boolean {
        return (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun isOnline() : Boolean{
        // Check network connectivity and start location updates accordingly
        val connectivityManager = getSystemService(requireContext(), ConnectivityManager::class.java) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)))
    }

    private fun requestLocationPermission() {
        Log.d("tracker", "requesting location permissions")
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            MainActivity.MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun createLocationRequest(interval: Long, fastestInterval: Long): LocationRequest {
        return LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(interval)
            .setFastestInterval(fastestInterval)
    }

    private fun updateCameraWithLastLocation(){updateCameraWithLastLocation(true)}
    fun updateCameraWithLastLocation(animate: Boolean) {
        Log.d("tracker", "moving camera to last location")
        val lastLocation = prefs.lastLocation()
        if (lastLocation.provider == "" || googleMap == null || cameraIsBeingMoved) return
        cameraIsBeingMoved = true
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(lastLocation.latitude, lastLocation.longitude), DEFAULT_ZOOM)
        if(animate) {
            googleMap?.animateCamera(cameraUpdate, CAMERA_ANIMATION_DURATION,
                object : CancelableCallback {
                    override fun onFinish() {
                        cameraIsBeingMoved = false
                        loadMarkers()
                    }

                    override fun onCancel() {
                        cameraIsBeingMoved = false
                        updateTrackingStatus(false)
                        layersLayout.visibility = View.GONE
                    }
                })
        }else{
            googleMap?.moveCamera(cameraUpdate)
            Log.d("tracker", "camera moved, loading markers")
            loadMarkers()
            cameraIsBeingMoved = false
        }
    }

    private fun updateNetworkStatus(){
        offlineImg.visibility = if(isOnline()) View.GONE else View.VISIBLE
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d("tracker", "got location update")
            locationResult.lastLocation.let { location ->
                prefs.lastLocation(location)
                circle?.remove()
                circle = googleMap?.addCircle(getMyLocationCircle())
                if (!tracking) return
                Log.d("tracker", "updating camera from location update")
                updateCameraWithLastLocation()
            }
        }
    }

    private fun getMyLocationCircle() : CircleOptions{
        val loc = prefs.lastLocation()
        val isBoosted = user.isSkillInUse(Skill.GIANT).first
        val color = if(isBoosted) Color.argb(50, 255, 255, 0)
        else Color.argb(50, 0, 0, 255)
        return CircleOptions()
            .center(LatLng(loc.latitude, loc.longitude))
            .fillColor(color)
            .strokeColor(Color.argb(0,0,0,0))
            .strokeWidth(0f)
            .radius(user.getReach().toDouble())
    }

    private fun monitorInUseSkillTimer(skill: Skill){
        if(inUseSkillTimers.contains(skill)) return
        Log.d(TAG, "starting ${skill.name} in use timer thread")
        inUseSkillTimers[skill.ordinal] = skill
        val timerTxt = when(skill){
            Skill.GIANT -> giantTimer
            Skill.DRONE -> droneTimer
            Skill.TIME -> timeTravelTimer
            Skill.COMPANION -> companionTimer
        }

        timerTxt.visibility = View.VISIBLE

        Thread{
            scheduleNotification(requireContext(), skill)
            var pair = user.isSkillInUse(skill)
            while (pair.first){
                Handler(Looper.getMainLooper()).post { timerTxt.text = Converters.toCountdownFormat(pair.second) }
                Thread.sleep(1000)
                pair = user.isSkillInUse(skill)
            }

            Handler(Looper.getMainLooper()).post {
                timerTxt.visibility = View.GONE
                when(skill){
                    Skill.GIANT -> {
                        circle?.remove()
                        circle = googleMap?.addCircle(getMyLocationCircle())
                    }
                    Skill.DRONE -> {
                        droneLayout.visibility = View.GONE
                        monitorJoystick = false
                        val map = googleMap ?: return@post
                        map.uiSettings.isScrollGesturesEnabled = true
                        map.uiSettings.isZoomGesturesEnabled = true
                        map.uiSettings.isRotateGesturesEnabled = true
                        map.uiSettings.isTiltGesturesEnabled = true
                    }
                    else -> {}
                }

                inUseSkillTimers[skill.ordinal] = null
                Log.d(TAG, "exiting ${skill.name} in use timer thread")
                monitorReuseInSkillTimer(skill)
            }
        }.start()
    }

    private fun monitorReuseInSkillTimer(skill: Skill){
        if(reuseSkillTimers.contains(skill)) return
        Log.d(TAG, "starting ${skill.name} reuse in timer thread")
        reuseSkillTimers[skill.ordinal] = skill
        val timerTxt = when(skill){
            Skill.GIANT -> giantTimer
            Skill.DRONE -> droneTimer
            Skill.TIME -> timeTravelTimer
            Skill.COMPANION -> companionTimer
        }

        timerTxt.visibility = View.VISIBLE

        Thread{
            var pair = user.isSkillAvailable(skill)
            while (!pair.first){
                Handler(Looper.getMainLooper()).post { timerTxt.text = Converters.toCountdownFormat(pair.second) }
                Thread.sleep(1000)
                pair = user.isSkillAvailable(skill)
            }

            Handler(Looper.getMainLooper()).post { timerTxt.visibility = View.GONE }
            reuseSkillTimers[skill.ordinal] = null
            Log.d(TAG, "exiting ${skill.name} reuse in timer thread")
        }.start()
    }

    fun onNotificationsEnabled(){
        val coin = markerToCoin[selectedMarker]!!
        if(collected(coin)) scheduleNotification(requireContext(), coin)
        notifyFab.setImageResource(R.drawable.notifications_active)
    }

    companion object{
        private const val TAG = "Home"
        private const val DEFAULT_ZOOM = 15f
        private const val MIN_ZOOM = 14f
        private const val TRACKING_INTERVAL = 5000L
        private const val TRACKING_FASTEST_INTERVAL = 1000L
        private const val CAMERA_ANIMATION_DURATION = 2000
    }
}
