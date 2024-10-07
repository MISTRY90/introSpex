package com.aayush683.introspex

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.an.deviceinfo.device.model.Battery
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.oseamiya.deviceinformation.MemoryInformation
import java.io.BufferedReader
import java.io.FileReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE: Int = 100

    private lateinit var phoneTempChart: LineChart
    private val tempArr = ArrayList<Entry>()
    private lateinit var usedMemChart: PieChart
    private lateinit var cpuChart: LineChart
    private val cpuArr = ArrayList<Entry>()
    private lateinit var storageChart: PieChart
    private lateinit var batteryChart: LineChart
    private val batteryArr = ArrayList<Entry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request for permissions
        checkAndRequestPermissions()

        // Load home.xml by default
        showHome()

        // Map Buttons
        mapButtons()
    }

    private fun hanldeButton(button: String) {
        if (button == "homeButton") {
            showHome()
            mapButtons()
        } else if (button == "processButton") {
            showProcess()
            mapButtons()
        } else if (button == "aboutButton") {
            showAbout()
//            mapButtons()
        }
    }

    private fun mapButtons() {
        // Load buttons
        val homeButton = findViewById<LinearLayout>(R.id.homeButton)
        val processButton = findViewById<LinearLayout>(R.id.processButton)
        val aboutButton = findViewById<LinearLayout>(R.id.aboutButton)

        // Set on click listeners
        homeButton.setOnClickListener {
            hanldeButton("homeButton")
        }

        processButton.setOnClickListener {
            hanldeButton("processButton")
        }

        aboutButton.setOnClickListener {
            hanldeButton("aboutButton")
        }
    }

    private fun showHome() {
        setContentView(R.layout.home)

        // Load Charts
        phoneTempChart = findViewById(R.id.phoneTempChart)
        usedMemChart = findViewById(R.id.usedMemChart)
        cpuChart = findViewById(R.id.cpuChart)
        storageChart = findViewById(R.id.storageChart)
        batteryChart = findViewById(R.id.batteryChart)

        // Show data
        showData()
    }

    private fun showProcess() {
        setContentView(R.layout.processes)
        displayAppPermissions() // Display app permissions if permissions are granted
    }

    private fun showAbout() {
        setContentView(R.layout.about)
    }

    private fun showData() {
        // Load data from APIs
        val battery = Battery(this)
        val memory = MemoryInformation(this)

        // Temperature Chart (Battery Temp)
        updateTempChart(battery.batteryTemperature, phoneTempChart)
        // Memory Chart (Used vs Free RAM)
        updateMemChart(memory, usedMemChart)
        // CPU Chart (Frequency/Clock Speed)
        updateCpuChart(cpuChart)
        // Storage Chart (Used vs Free Storage)
        updateStorageChart(memory, storageChart)
        // Battery Level Chart
        updateBatteryChart(battery.batteryVoltage, batteryChart)

        // A timer to update all charts every 10 seconds
        val timer = object : CountDownTimer(10000, 10000) {
            override fun onTick(millisUntilFinished: Long) {
                // Temperature Chart (Battery Temp)
                updateTempChart(battery.batteryTemperature, phoneTempChart)
                // Memory Chart (Used vs Free RAM)
                updateMemChart(memory, usedMemChart)
                // CPU Chart (Frequency/Clock Speed)
                updateCpuChart(cpuChart)
                // Storage Chart (Used vs Free Storage)
                updateStorageChart(memory, storageChart)
                // Battery Level Chart
                updateBatteryChart(battery.batteryVoltage, batteryChart)
            }

            override fun onFinish() {
                start()
            }
        }
        timer.start()
        
    }


    private fun updateTempChart(temperature: Float, phoneTempChart: LineChart) {
        tempArr.add(Entry(tempArr.size.toFloat(), temperature))
        if (tempArr.size > 10) tempArr.removeAt(0)

        val lineDataSet = LineDataSet(tempArr, "Device Temperature (°C)")
        lineDataSet.color = ContextCompat.getColor(this, R.color.cyan)
        lineDataSet.valueTextColor = ContextCompat.getColor(this, R.color.white)

        phoneTempChart.data = LineData(lineDataSet)
        phoneTempChart.description = null
        phoneTempChart.invalidate()
    }

    private fun updateMemChart(memory: MemoryInformation, usedMemChart: PieChart) {
        val totalMem = memory.totalRam.toFloat() / (1024 * 1024) // Convert to MB
        val usedMem = memory.usedRam.toFloat() / (1024 * 1024) // Convert to MB
        val usedMemPercentage = (usedMem / totalMem) * 100
        val freeMemPercentage = 100 - usedMemPercentage

        val pieEntries = ArrayList<PieEntry>()
        pieEntries.add(PieEntry(usedMemPercentage, "Used"))
        pieEntries.add(PieEntry(freeMemPercentage, "Free"))

        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.colors = listOf(ContextCompat.getColor(this, R.color.cyan), ContextCompat.getColor(this, R.color.white))
        pieDataSet.label = "Total Memory: ${totalMem}MB"
        pieDataSet.valueTextColor = ContextCompat.getColor(this, R.color.white)

        usedMemChart.data = PieData(pieDataSet)
        usedMemChart.description = null
        usedMemChart.invalidate()
    }

    private fun updateCpuChart(cpuUsageChart: LineChart) {
        val data = readCpuData()
        if (data == -1) return
        cpuArr.add(Entry(cpuArr.size.toFloat(), data.toFloat()))
        if (cpuArr.size > 10) cpuArr.removeAt(0)

        val lineDataSet = LineDataSet(cpuArr, "CPU Clock Speed (MHz)")
        lineDataSet.color = ContextCompat.getColor(this, R.color.cyan)
        lineDataSet.valueTextColor = ContextCompat.getColor(this, R.color.white)

        cpuUsageChart.data = LineData(lineDataSet)
        cpuUsageChart.description = null
        cpuUsageChart.invalidate()
    }

    private fun updateStorageChart(storage: MemoryInformation, storageChart: PieChart) {
        val freeExt = storage.availableExternalStorageSize.toFloat() / (1024 * 1024) // Convert to MB
        val freeInt = storage.availableInternalMemorySize.toFloat() / (1024 * 1024) // Convert to MB
        val freeTotal = freeExt + freeInt

        val usedExt = storage.usedExternalStorageSize.toFloat() / (1024 * 1024) // Convert to MB
        val usedInt = storage.usedInternalMemorySize.toFloat() / (1024 * 1024) // Convert to MB
        val usedTotal = usedExt + usedInt

        val totalInt = storage.totalInternalMemorySize.toFloat() / (1024 * 1024) // Convert to MB
        val totalExt = storage.totalExternalStorageSize.toFloat() / (1024 * 1024) // Convert to MB
        val totalMem = totalExt + totalInt

        val pieEntries = ArrayList<PieEntry>()
        pieEntries.add(PieEntry(usedTotal, "Used"))
        pieEntries.add(PieEntry(freeTotal, "Free"))

        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.colors = listOf(ContextCompat.getColor(this, R.color.cyan), ContextCompat.getColor(this, R.color.white))
        pieDataSet.label = "Total Memory: ${totalMem}MB"
        pieDataSet.valueTextColor = ContextCompat.getColor(this, R.color.white)

        storageChart.data = PieData(pieDataSet)
        storageChart.description = null
        storageChart.invalidate()
    }

    private fun updateBatteryChart(batteryLevel: Int, batteryChart: LineChart) {
        batteryArr.add(Entry(batteryArr.size.toFloat(), batteryLevel.toFloat()))
        if (batteryArr.size > 10) batteryArr.removeAt(0)

        val lineDataSet = LineDataSet(batteryArr, "Battery Voltage (V)")
        lineDataSet.color = ContextCompat.getColor(this, R.color.cyan)
        lineDataSet.valueTextColor = ContextCompat.getColor(this, R.color.white)

        batteryChart.data = LineData(lineDataSet)
        batteryChart.description = null
        batteryChart.invalidate()
    }

    private fun readCpuData(): Int {
        return try {
            // Read the file and convert the content to Int
            val content = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim()
            content.toInt()
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            -1
        }
    }

    private fun displayAppPermissions() {
        val packageManager = packageManager
        val installedApps = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val appListLayout = findViewById<LinearLayout>(R.id.networkHolder)

        var rows = 0;

        val headerRow = LinearLayout(this)
        headerRow.orientation = LinearLayout.HORIZONTAL
        headerRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        headerRow.setPadding(16, 16, 16, 16)

        val appNameTextView = TextView(this)
        appNameTextView.text = "App Name"
        appNameTextView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val packageNameTextView = TextView(this)
        packageNameTextView.text = "Package Name"
        packageNameTextView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        headerRow.addView(appNameTextView)
        headerRow.addView(packageNameTextView)
        appListLayout.addView(headerRow)

        for (app in installedApps) {
            // Create a row for each app
            val appRow = LinearLayout(this)
            appRow.orientation = LinearLayout.HORIZONTAL
            appRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            appRow.setPadding(16, 16, 16, 16)

            // Get App Name and Package Name
            val appName = packageManager.getApplicationLabel(app.applicationInfo).toString()
            val packageName = app.packageName

            // Display App Name
            val appNameTextView = TextView(this)
            appNameTextView.text = appName
            appNameTextView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // Display Package Name
            val packageNameTextView = TextView(this)
            packageNameTextView.text = packageName
            packageNameTextView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // Set OnClickListener to show permissions in a pop-up dialog
            appRow.setOnClickListener {
                // Call function to show permissions for the selected app
                showAppPermissionsPopup(packageName, app.requestedPermissions)
            }

            // Add views to the app row
            appRow.addView(appNameTextView)
            appRow.addView(packageNameTextView)

            // Add app row to the appListLayout
            appListLayout.addView(appRow)
            rows++;
        }

        Log.d("ROWS", rows.toString())
        val appListTitle = findViewById<TextView>(R.id.AppListTitle)
        appListTitle.text = "${rows} Packages Found"
    }

    // Function to show a pop-up dialog with app permissions
    private fun showAppPermissionsPopup(packageName: String, permissions: Array<String>?) {
        if (permissions != null && permissions.isNotEmpty()) {
            // Build a dialog to display permissions
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Permissions for $packageName")

            // Create a string from the list of permissions
            val permissionsString = permissions.joinToString(separator = "\n")

            builder.setMessage(permissionsString)

            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        } else {
            // Show a message if the app has no requested permissions
            Toast.makeText(this, "$packageName has no declared permissions", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.QUERY_ALL_PACKAGES), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if permissions are granted
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                // Permissions granted, proceed with your functionality
                showHome()
            } else {
                // Log which permissions were denied
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] != PackageManager.PERMISSION_GRANTED
                }
                Log.d("Permissions", "Denied: $deniedPermissions")
                Toast.makeText(this, "Some permissions were denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
