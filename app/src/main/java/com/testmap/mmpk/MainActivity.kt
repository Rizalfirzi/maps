package com.testmap.mmpk

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.data.TransportationNetworkDataset
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.testmap.mmpk.databinding.ActivityMainBinding
import com.testmap.mmpk.databinding.DialogManualPathBinding
import com.testmap.mmpk.databinding.DialogMmpkInfoBinding
import com.testmap.mmpk.model.DirectionStep
import com.testmap.mmpk.model.LayerInfoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Overlays for map visualization
    private val pinsOverlay = GraphicsOverlay()
    private val routeOverlay = GraphicsOverlay()

    // Current MMPK and Routing objects
    private var currentMmpk: MobileMapPackage? = null
    private var routeTask: RouteTask? = null
    private var transportationDataset: TransportationNetworkDataset? = null

    // Routing Stop Points
    private var originPoint: Point? = null
    private var destinationPoint: Point? = null
    private var originGraphic: Graphic? = null
    private var destGraphic: Graphic? = null
    private var routeGraphic: Graphic? = null

    // Pin Placement Mode: 0 = Origin (A), 1 = Destination (B)
    private var currentPinMode = 0

    // Adapters
    private val directionsAdapter = RouteDirectionAdapter()
    private val layerItems = mutableListOf<LayerInfoItem>()

    // File picker launcher
    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { copyAndLoadMmpkFromUri(it) }
        }

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Handled permissions
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lifecycle observer for MapView
        lifecycle.addObserver(binding.mapView)

        // Setup Map Overlays
        binding.mapView.graphicsOverlays.add(routeOverlay)
        binding.mapView.graphicsOverlays.add(pinsOverlay)

        // Setup RecyclerView for directions
        binding.rvDirections.layoutManager = LinearLayoutManager(this)
        binding.rvDirections.adapter = directionsAdapter

        checkAndRequestPermissions()
        setupListeners()
        observeMapTaps()

        // Check if there are sample mmpk files in typical phone download paths
        checkDefaultPhonePaths()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun setupListeners() {
        // Choose file button (SAF)
        binding.btnPickMmpk.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("*/*"))
        }

        // Manual path input dialog
        binding.btnManualPath.setOnClickListener {
            showManualPathDialog()
        }

        // Dataset info dialog
        binding.btnDatasetInfo.setOnClickListener {
            showDatasetInfoDialog()
        }

        // Pin Mode Selector
        binding.btnModeOrigin.setOnClickListener {
            currentPinMode = 0
            Toast.makeText(this, "Tap pada peta untuk menetapkan Titik Awal (A)", Toast.LENGTH_SHORT).show()
        }

        binding.btnModeDestination.setOnClickListener {
            currentPinMode = 1
            Toast.makeText(this, "Tap pada peta untuk menetapkan Titik Tujuan (B)", Toast.LENGTH_SHORT).show()
        }

        // Solve Route Button
        binding.btnSolveRoute.setOnClickListener {
            solveRoute()
        }

        // Reset Route Button
        binding.btnClearRoute.setOnClickListener {
            resetRouting()
        }

        // Toggle Directions List Visibility
        binding.btnToggleDirectionsList.setOnClickListener {
            if (binding.rvDirections.visibility == View.VISIBLE) {
                binding.rvDirections.visibility = View.GONE
                binding.btnToggleDirectionsList.text = "Petunjuk Arah"
            } else {
                binding.rvDirections.visibility = View.VISIBLE
                binding.btnToggleDirectionsList.text = "Tutup Daftar"
            }
        }
    }

    private fun observeMapTaps() {
        lifecycleScope.launch {
            binding.mapView.onSingleTapConfirmed.collect { event ->
                val tappedPoint = event.mapPoint ?: return@collect
                handleMapTap(tappedPoint)
            }
        }
    }

    private fun handleMapTap(point: Point) {
        val coordText = String.format(Locale.getDefault(), "%.4f, %.4f", point.y, point.x)

        if (currentPinMode == 0) {
            // Set Origin (A)
            originPoint = point
            binding.tvOriginCoord.text = "A: $coordText"

            originGraphic?.let { pinsOverlay.graphics.remove(it) }
            val greenMarker = SimpleMarkerSymbol(
                SimpleMarkerSymbolStyle.Circle,
                Color.fromRgba(16, 185, 129, 255),
                14f
            ).apply {
                outline = SimpleLineSymbol(
                    SimpleLineSymbolStyle.Solid,
                    Color.fromRgba(255, 255, 255, 255),
                    2f
                )
            }
            originGraphic = Graphic(point, greenMarker).also { pinsOverlay.graphics.add(it) }
            Toast.makeText(this, "Titik Awal (A) diset: $coordText", Toast.LENGTH_SHORT).show()

            // Auto-switch mode to destination for convenient workflow
            currentPinMode = 1
        } else {
            // Set Destination (B)
            destinationPoint = point
            binding.tvDestCoord.text = "B: $coordText"

            destGraphic?.let { pinsOverlay.graphics.remove(it) }
            val redMarker = SimpleMarkerSymbol(
                SimpleMarkerSymbolStyle.Circle,
                Color.fromRgba(239, 68, 68, 255),
                14f
            ).apply {
                outline = SimpleLineSymbol(
                    SimpleLineSymbolStyle.Solid,
                    Color.fromRgba(255, 255, 255, 255),
                    2f
                )
            }
            destGraphic = Graphic(point, redMarker).also { pinsOverlay.graphics.add(it) }
            Toast.makeText(this, "Titik Tujuan (B) diset: $coordText", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyAndLoadMmpkFromUri(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvMmpkStatus.text = "Menyalin file MMPK dari penyimpanan HP..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cacheMmpk = File(cacheDir, "current_loaded_map.mmpk")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheMmpk).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    loadMmpkFile(cacheMmpk.absolutePath, "File Terpilih")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvMmpkStatus.text = "Gagal menyalin file: ${e.localizedMessage}"
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkDefaultPhonePaths() {
        val commonLocations = listOf(
            "/sdcard/Download",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Documents",
            getExternalFilesDir(null)?.absolutePath ?: ""
        )

        for (dirPath in commonLocations) {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                val mmpkFiles = dir.listFiles { _, name -> name.endsWith(".mmpk", ignoreCase = true) }
                if (!mmpkFiles.isNullOrEmpty()) {
                    val first = mmpkFiles[0]
                    binding.tvMmpkStatus.text = "Ditemukan di HP: ${first.name}"
                    // Auto-load if found
                    loadMmpkFile(first.absolutePath, first.name)
                    return
                }
            }
        }
    }

    private fun showManualPathDialog() {
        val dialogBinding = DialogManualPathBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.etMmpkPath.setText("/sdcard/Download/")

        dialogBinding.btnCancelPath.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnLoadPath.setOnClickListener {
            val path = dialogBinding.etMmpkPath.text.toString().trim()
            if (path.isNotEmpty()) {
                val file = File(path)
                if (file.exists()) {
                    dialog.dismiss()
                    loadMmpkFile(path, file.name)
                } else {
                    dialogBinding.etMmpkPath.error = "File tidak ditemukan di path tersebut!"
                }
            }
        }

        dialog.show()
    }

    private fun loadMmpkFile(filePath: String, displayName: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvMmpkStatus.text = "Memuat $displayName..."
        layerItems.clear()

        lifecycleScope.launch {
            try {
                val mmpk = MobileMapPackage(filePath)
                currentMmpk = mmpk

                mmpk.load().onSuccess {
                    val map = mmpk.maps.firstOrNull()
                    if (map != null) {
                        map.load().onSuccess {
                            binding.mapView.map = map

                            // Inspect Transportation Networks (Road Network Dataset for routing)
                            val networks = map.transportationNetworks
                            if (networks.isNotEmpty()) {
                                val netDataset = networks.first()
                                transportationDataset = netDataset
                                routeTask = RouteTask(netDataset)

                                // Load routeTask
                                routeTask?.load()

                                layerItems.add(
                                    LayerInfoItem(
                                        name = netDataset.name.ifEmpty { "Transportation Network Dataset" },
                                        type = "Network Dataset (Offline Shortest Path Engine)",
                                        isRoutingDataset = true
                                    )
                                )

                                binding.tvMmpkStatus.text =
                                    "MMPK: $displayName | Rute Dataset: ${netDataset.name} (Siap)"
                            } else {
                                transportationDataset = null
                                routeTask = null
                                binding.tvMmpkStatus.text =
                                    "MMPK: $displayName (Tidak ada Network Dataset untuk Rute)"
                                Toast.makeText(
                                    this@MainActivity,
                                    "Peringatan: MMPK ini belum di-bundle dengan Transportation Network Dataset dari ArcGIS Pro.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            // Collect Operational Layers (Roads, Basemap layers, etc.)
                            map.operationalLayers.forEach { layer ->
                                layerItems.add(
                                    LayerInfoItem(
                                        name = layer.name,
                                        type = "Operational Feature/Map Layer",
                                        isRoutingDataset = false
                                    )
                                )
                            }

                            // Zoom to initial extent of map
                            map.initialViewpoint?.let { vp ->
                                binding.mapView.setViewpoint(vp)
                            }

                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this@MainActivity,
                                "MMPK berhasil dimuat! Ditemukan ${layerItems.size} dataset/layer.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { err ->
                            binding.progressBar.visibility = View.GONE
                            binding.tvMmpkStatus.text = "Gagal memuat Map dari MMPK: ${err.message}"
                        }
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.tvMmpkStatus.text = "File MMPK tidak memiliki peta valid."
                    }
                }.onFailure { err ->
                    binding.progressBar.visibility = View.GONE
                    binding.tvMmpkStatus.text = "Gagal membaca file MMPK: ${err.message}"
                    Toast.makeText(this@MainActivity, "Gagal memuat MMPK: ${err.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvMmpkStatus.text = "Error: ${e.localizedMessage}"
            }
        }
    }

    private fun solveRoute() {
        val start = originPoint
        val dest = destinationPoint

        if (start == null || dest == null) {
            Toast.makeText(this, "Harap tentukan Titik Awal (A) dan Titik Tujuan (B) terlebih dahulu!", Toast.LENGTH_LONG).show()
            return
        }

        val task = routeTask
        if (task == null) {
            // Fallback: If MMPK has no built-in network dataset, simulate shortest path or explain
            simulateOrWarnRouting(start, dest)
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvRouteSummaryTitle.text = "Menghitung rute terdekat offline..."

        lifecycleScope.launch {
            try {
                val params: RouteParameters = task.createDefaultParameters().getOrThrow().apply {
                    setStops(listOf(Stop(start), Stop(dest)))
                    returnDirections = true
                    returnRoutes = true
                    returnStops = true
                }

                val routeResult = task.solveRoute(params).getOrThrow()
                val route = routeResult.routes.firstOrNull()

                if (route != null) {
                    // 1. Draw route polyline
                    val routeGeometry = route.routeGeometry
                    routeGraphic?.let { routeOverlay.graphics.remove(it) }

                    val cyanRouteSymbol = SimpleLineSymbol(
                        SimpleLineSymbolStyle.Solid,
                        Color.fromRgba(0, 229, 255, 255),
                        5f
                    )

                    if (routeGeometry != null) {
                        routeGraphic = Graphic(routeGeometry, cyanRouteSymbol).also {
                            routeOverlay.graphics.add(it)
                        }
                        // Zoom map to fit the full route
                        binding.mapView.setViewpointGeometry(routeGeometry, 60.0)
                    }

                    // 2. Metrics (Distance & Travel Time)
                    val distanceKm = route.totalLength / 1000.0
                    val durationMin = route.totalTime

                    binding.tvRouteSummaryTitle.text = "Rute Terdekat Ditemukan"
                    binding.tvRouteMetrics.text = String.format(
                        Locale.getDefault(),
                        "Jarak: %.2f km  •  Waktu: %.1f menit",
                        distanceKm,
                        durationMin
                    )

                    // 3. Turn-by-turn maneuvers (Google Maps-like instructions)
                    val steps = route.directionManeuvers.mapIndexed { idx, maneuver ->
                        DirectionStep(
                            index = idx + 1,
                            text = maneuver.directionText.ifEmpty { "Lanjutkan rute jalan" },
                            distanceMeters = maneuver.length,
                            estimatedMinutes = maneuver.duration
                        )
                    }

                    directionsAdapter.submitList(steps)
                    binding.rvDirections.visibility = View.VISIBLE
                    binding.btnToggleDirectionsList.text = "Tutup Daftar"

                    Toast.makeText(
                        this@MainActivity,
                        "Rute terdekat berhasil dihitung! Jarak: ${String.format(Locale.getDefault(), "%.2f km", distanceKm)}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    binding.tvRouteSummaryTitle.text = "Tidak ditemukan rute antara titik A dan B"
                }
            } catch (e: Exception) {
                binding.tvRouteSummaryTitle.text = "Gagal menghitung rute: ${e.localizedMessage}"
                Toast.makeText(this@MainActivity, "Solve Route Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun simulateOrWarnRouting(start: Point, dest: Point) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Info Dataset Rute Jalan")
            .setMessage(
                "MMPK yang dimuat tidak menyertakan Transportation Network Dataset (Road Network) yang di-build menggunakan Network Analyst ArcGIS Pro.\n\n" +
                "Apakah Anda ingin melihat visualisasi simulasi jalur titik-ke-titik untuk pengujian?"
            )
            .setPositiveButton("Simulasi Jalur") { _, _ ->
                renderSimulatedPath(start, dest)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun renderSimulatedPath(start: Point, dest: Point) {
        val polylineBuilder = PolylineBuilder(start.spatialReference ?: SpatialReference.wgs84()).apply {
            addPoint(start)
            addPoint(dest)
        }
        val straightLine = polylineBuilder.toGeometry()

        routeGraphic?.let { routeOverlay.graphics.remove(it) }
        val lineSymbol = SimpleLineSymbol(
            SimpleLineSymbolStyle.Dash,
            Color.fromRgba(245, 158, 11, 255),
            4f
        )
        routeGraphic = Graphic(straightLine, lineSymbol).also { routeOverlay.graphics.add(it) }

        binding.tvRouteSummaryTitle.text = "Simulasi Garis Jalur (Mode Uji Coba)"
        binding.tvRouteMetrics.text = "Gunakan MMPK dengan Network Dataset untuk rute jalan otomatis akurat."
        directionsAdapter.submitList(
            listOf(
                DirectionStep(1, "Mulai dari Titik Awal (Origin)", 0.0, 0.0),
                DirectionStep(2, "Menuju Titik Tujuan (Destination)", 0.0, 0.0)
            )
        )
    }

    private fun resetRouting() {
        originPoint = null
        destinationPoint = null
        originGraphic?.let { pinsOverlay.graphics.remove(it) }
        destGraphic?.let { pinsOverlay.graphics.remove(it) }
        routeGraphic?.let { routeOverlay.graphics.remove(it) }
        originGraphic = null
        destGraphic = null
        routeGraphic = null

        binding.tvOriginCoord.text = "A: Belum dipilih"
        binding.tvDestCoord.text = "B: Belum dipilih"
        binding.tvRouteSummaryTitle.text = "Hasil Rute Otomatis Terdekat"
        binding.tvRouteMetrics.text = "Jarak: - | Waktu: -"
        directionsAdapter.submitList(emptyList())
        binding.rvDirections.visibility = View.GONE
        binding.btnToggleDirectionsList.text = "Petunjuk Arah"
        currentPinMode = 0
    }

    private fun showDatasetInfoDialog() {
        val dialogBinding = DialogMmpkInfoBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        val map = binding.mapView.map
        val sr = map?.spatialReference?.wkid?.toString() ?: "WGS84 / Local"
        val totalLayers = layerItems.size

        dialogBinding.tvMmpkMeta.text =
            "Peta Aktif: ${map?.item?.title ?: "Offline Map"}\n" +
            "Sistem Koordinat: WKID $sr\n" +
            "Jumlah Dataset Terdeteksi: $totalLayers"

        val inspectorAdapter = MmpkInspectorAdapter()
        dialogBinding.rvMmpkLayers.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvMmpkLayers.adapter = inspectorAdapter
        inspectorAdapter.submitList(layerItems)

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
