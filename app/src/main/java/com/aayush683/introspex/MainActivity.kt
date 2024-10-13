package com.aayush683.introspex

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.an.deviceinfo.device.DeviceInfo
import com.an.deviceinfo.device.model.Battery
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.oseamiya.deviceinformation.BatteryInformation
import com.oseamiya.deviceinformation.CpuInformation
import com.oseamiya.deviceinformation.DeviceInformation
import com.oseamiya.deviceinformation.LocationInformation
import com.oseamiya.deviceinformation.MemoryInformation
import com.oseamiya.deviceinformation.NetworkInformation
import com.oseamiya.deviceinformation.SystemInformation
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections


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
    private var initialProcess = false;

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
            if (!initialProcess) {
                Toast.makeText(this,"Loading Packages, Please wait", Toast.LENGTH_LONG).show();
                initialProcess = true;
            }
            showProcess()
            mapButtons()
        } else if (button == "aboutButton") {
            showAbout()
            mapButtons()
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

        // Load swipe refresh container
        val swipeContainerHome = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeContainerHome)

        swipeContainerHome.setOnRefreshListener {
            swipeContainerHome.isRefreshing = false
            showData()
        }

        // Show data
        showData()
    }

    private fun showProcess() {
        setContentView(R.layout.processes)
        displayAppPermissions() // Display app permissions if permissions are granted

        val swipeContainerHome = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeContainerPackages)

        swipeContainerHome.setOnRefreshListener {
            swipeContainerHome.isRefreshing = false
            hanldeButton("processButton")
        }
    }

    private fun showAbout() {
        setContentView(R.layout.about)

        // Load text views
        val DeviceInfoBox = findViewById<TextView>(R.id.abt1)
        val batteryHealthBox = findViewById<TextView>(R.id.text_abt2)
        val LocationInfoBox = findViewById<TextView>(R.id.text_abt3)
        val networkBox = findViewById<TextView>(R.id.abt4)

        // Device information
        val deviceInfo = DeviceInformation(this)
        val modelName = deviceInfo.modelName
        val host = deviceInfo.host
        val buildUser = deviceInfo.buildUser
        val serialID = deviceInfo.serial
        var kernel = SystemInformation(this).kernalVersion
        val deviceId = deviceInfo.deviceId
        val rooted = (if (deviceInfo.isRooted) "Yes" else "No")

        // Split kernel version
        val kernelSplit = kernel.split(" ")
        kernel = kernelSplit[0] + " " + kernelSplit[1] + " " + kernelSplit[2]

        // Add text to a string
        val deviceInfoText = """
            Device Id: $deviceId
            Model Name: $modelName
            Host: $host
            Build User: $buildUser
            Serial ID: $serialID
            Kernel Info: $kernel
            Is Rooted?: $rooted
        """.trimIndent()

        // Add string to box
        DeviceInfoBox.text = deviceInfoText

        // Battery Health
        val batteryInfo = BatteryInformation(this)
        val batteryHealth = batteryInfo.batteryCapacity

        // Add text to string
        val batteryHealthText = "$batteryHealth mAh"

        // Add string to box
        batteryHealthBox.text = batteryHealthText

        // Location Info
        val locationInfo = LocationInformation(this)
        var longitude = locationInfo.currentLongitude
        var latitude = locationInfo.currentLatitude

        // trim longitude and latitude to 2 decimal points
        longitude  = longitude.toString().substring(0, 2).toDouble()
        latitude = latitude.toString().substring(0, 2).toDouble()

        // Add string to text
        val locationInfoText = "$longitude, $latitude"

        // Add text to box
        LocationInfoBox.text = locationInfoText

        // Network Info
        val networkInfo = NetworkInformation(this)
        var ipAddr = "0.0.0.0"
        val servers = networkInfo.servers
        val adbDebug = (if (networkInfo.isADBDebuggingEnabled) "Enabled" else "Disabled")
        var vpnConn = "None"

        // Try for IP Address
        try {
            val ip = networkInfo.getIpAddress(true)
            if (ip.isNotEmpty()) {
                ipAddr = ip
            } else {
                ipAddr = "Not Connected"
            }
        } catch (e: Exception) {
            ipAddr = "Not Connected"
            Log.e("MainActivity", "Error retrieving IP address: ${e.message}")
        }

        // Try for vpn connectivity
        try {
            if (networkInfo.isVpnConnection) {
                vpnConn = "Connected"
            } else {
                vpnConn = "Not Connected"
            }
        } catch (e: Exception) {
            vpnConn = "Not Connected"
            Log.e("MainActivity", "Error checking VPN connection: ${e.message}")
        }



        // Add text to string
        val networkText = """
            IP Address: $ipAddr
            Servers: ${servers.joinToString(".")}
            ADB Debugging: $adbDebug
            VPN Connection: $vpnConn
        """.trimIndent()

        // Add string to box
        networkBox.text = networkText
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
        // Battery Voltage Chart
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

        // Configure LineDataSet
        val lineDataSet = LineDataSet(tempArr, "Device Temperature (°C)").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.cyan)  // Line color
            valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.white)  // Value text color
            valueTextSize = 16f  // Increase text size for readability
            setCircleColor(ContextCompat.getColor(this@MainActivity, R.color.cyan))  // Circle color for data points
            circleRadius = 5f  // Increase circle radius for visibility
            lineWidth = 2f  // Set line width for better visibility
        }

        // Set LineData and configure chart
        phoneTempChart.apply {
            data = LineData(lineDataSet)
            description = null
            legend.apply {
                textColor = ContextCompat.getColor(context, R.color.cyan)  // Cyan text for legend
                textSize = 14f
                form = Legend.LegendForm.LINE
            }
            xAxis.textColor = ContextCompat.getColor(context, R.color.white)  // X-axis text color
            axisLeft.textColor = ContextCompat.getColor(context, R.color.white)  // Y-axis text color
            axisRight.isEnabled = false  // Disable right Y-axis
            invalidate()  // Refresh the chart
        }
    }


    private fun updateMemChart(memory: MemoryInformation, usedMemChart: PieChart) {
        val totalMem = memory.totalRam.toFloat() / (1024 * 1024) // Convert to MB
        val usedMem = memory.usedRam.toFloat() / (1024 * 1024) // Convert to MB
        val usedMemPercentage = (usedMem / totalMem) * 100
        val freeMemPercentage = 100 - usedMemPercentage

        // Create pie entries
        val pieEntries = ArrayList<PieEntry>().apply {
            add(PieEntry(usedMemPercentage, "Used"))
            add(PieEntry(freeMemPercentage, "Free"))
        }

        // Configure PieDataSet
        val pieDataSet = PieDataSet(pieEntries, "").apply {
            colors = listOf(
                ContextCompat.getColor(this@MainActivity, R.color.cyan),  // Used Memory color
                ContextCompat.getColor(this@MainActivity, R.color.light_gray)  // Free Memory color
            )
            valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.white)
            valueTextSize = 16f  // Bigger value text size for readability
            valueFormatter = PercentFormatter()  // Assign percentage formatter
            sliceSpace = 2f  // Add spacing between slices
        }

        // Set PieData and configure chart
        val pieData = PieData(pieDataSet)

        usedMemChart.apply {
            data = pieData
            setUsePercentValues(true)  // Display values as percentages
            isDrawHoleEnabled = true  // Add a hole in the center for aesthetics
            holeRadius = 50f
            setHoleColor(ContextCompat.getColor(context, R.color.dark_gray))  // Hole matches background
            transparentCircleRadius = 44f  // Transparent circle for smooth effect
            setEntryLabelColor(ContextCompat.getColor(context, R.color.light_gray))  // Labels in light gray
            setEntryLabelTextSize(12f)

            centerText = "RAM Usage"
            setCenterTextSize(16f)
            setCenterTextColor(ContextCompat.getColor(context, R.color.white))

            // Configure legend
            legend.apply {
                textColor = ContextCompat.getColor(context, R.color.cyan)  // Cyan text for legend
                textSize = 14f
                form = Legend.LegendForm.CIRCLE
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                orientation = Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true  // Wrap legend if needed
            }

            description = null
            invalidate()  // Refresh the chart
        }
    }

    private fun updateCpuChart(cpuUsageChart: LineChart) {
        val data = readCpuData()
        if (data == -1) return

        cpuArr.add(Entry(cpuArr.size.toFloat(), data.toFloat()))
        if (cpuArr.size > 10) cpuArr.removeAt(0)

        // Configure LineDataSet
        val lineDataSet = LineDataSet(cpuArr, "CPU Clock Speed (MHz)").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.cyan)  // Line color
            valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.white)  // Value text color
            valueTextSize = 16f  // Increase text size for better readability
            setCircleColor(ContextCompat.getColor(this@MainActivity, R.color.cyan))  // Circle color for data points
            circleRadius = 5f  // Increase circle radius for visibility
            lineWidth = 2f  // Set line width for better visibility
        }

        // Set LineData and configure chart
        cpuUsageChart.data = LineData(lineDataSet)
        cpuUsageChart.apply {
            description = null  // Remove description for a cleaner look
            legend.apply {
                textColor = ContextCompat.getColor(context, R.color.cyan)  // Cyan text for legend
                textSize = 14f
                form = Legend.LegendForm.LINE
            }
            xAxis.textColor = ContextCompat.getColor(context, R.color.white)  // X-axis text color
            axisLeft.textColor = ContextCompat.getColor(context, R.color.white)  // Y-axis text color
            axisRight.isEnabled = false  // Disable right Y-axis
            invalidate()  // Refresh the chart
        }
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

        // Create pie entries
        val pieEntries = ArrayList<PieEntry>().apply {
            add(PieEntry(usedTotal, "Used"))
            add(PieEntry(freeTotal, "Free"))
        }

        // Configure PieDataSet
        val pieDataSet = PieDataSet(pieEntries, "").apply {
            colors = listOf(
                ContextCompat.getColor(this@MainActivity, R.color.cyan),  // Used Storage color
                ContextCompat.getColor(this@MainActivity, R.color.light_gray)  // Free Storage color
            )
            valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.white)  // Value text color
            valueTextSize = 16f  // Bigger value text size for readability
            valueFormatter = PercentFormatter()
            sliceSpace = 2f  // Add spacing between slices
        }

        // Set PieData and configure chart
        storageChart.apply {
            data = PieData(pieDataSet)
            setUsePercentValues(true)  // Display values as percentages
            isDrawHoleEnabled = true  // Add a hole in the center for aesthetics
            holeRadius = 50f
            setHoleColor(ContextCompat.getColor(context, R.color.dark_gray))  // Hole matches background
            transparentCircleRadius = 44f  // Transparent circle for smooth effect
            setEntryLabelColor(ContextCompat.getColor(context, R.color.light_gray))  // Labels in light gray
            setEntryLabelTextSize(12f)

            centerText = "Storage Usage"
            setCenterTextSize(16f)
            setCenterTextColor(ContextCompat.getColor(context, R.color.white))

            // Configure legend
            legend.apply {
                textColor = ContextCompat.getColor(context, R.color.cyan)  // Cyan text for legend
                textSize = 14f
                form = Legend.LegendForm.CIRCLE
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                orientation = Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true  // Wrap legend if needed
            }

            description = null
            invalidate()  // Refresh the chart
        }
    }


    private fun updateBatteryChart(batteryLevel: Int, batteryChart: LineChart) {
        batteryArr.add(Entry(batteryArr.size.toFloat(), batteryLevel.toFloat()))
        if (batteryArr.size > 10) batteryArr.removeAt(0)

        // Configure LineDataSet
        val lineDataSet = LineDataSet(batteryArr, "Battery Voltage (V)").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.cyan)  // Line color
            valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.white)  // Value text color
            valueTextSize = 16f  // Increase text size for readability
            setCircleColor(ContextCompat.getColor(this@MainActivity, R.color.cyan))  // Circle color for data points
            circleRadius = 5f  // Increase circle radius for visibility
            lineWidth = 2f  // Set line width for better visibility
        }

        // Set LineData and configure chart
        batteryChart.apply {
            data = LineData(lineDataSet)
            description = null
            legend.apply {
                textColor = ContextCompat.getColor(context, R.color.cyan)  // Cyan text for legend
                textSize = 14f
                form = Legend.LegendForm.LINE
            }
            xAxis.textColor = ContextCompat.getColor(context, R.color.white)  // X-axis text color
            axisLeft.textColor = ContextCompat.getColor(context, R.color.white)  // Y-axis text color
            axisRight.isEnabled = false  // Disable right Y-axis
            invalidate()  // Refresh the chart
        }
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
        appListTitle.text = getString(R.string.packages_found, rows.toString())
    }

    // Function to show a pop-up dialog with app permissions
    private fun showAppPermissionsPopup(packageName: String, permissions: Array<String>?) {
        if (!permissions.isNullOrEmpty()) {
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.QUERY_ALL_PACKAGES, android.Manifest.permission.ACCESS_NETWORK_STATE, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
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
                // Restart app
                recreate()
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
