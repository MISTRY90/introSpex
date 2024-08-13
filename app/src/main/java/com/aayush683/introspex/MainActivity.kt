package com.aayush683.introspex

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.an.deviceinfo.device.model.Battery
import com.an.deviceinfo.location.LocationInfo
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet


class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            ShowData()
        }
    }

    private val tempArr = ArrayList<Entry>();

    private fun ShowData() {
        val locationInfo = LocationInfo(this)
        val battery = Battery(this)
        val location = locationInfo.location
        val addr = location.addressLine1
        val city = location.city
        val state = location.state
        val country = location.countryCode

        val textBox1 = findViewById<TextView>(R.id.textBox1)
        val batteryTempChart = findViewById<LineChart>(R.id.batteryTempChart)

        val text = "Your Location: ${addr}, ${city}, ${state} - ${country}"
        textBox1.text = text

        // Add a 2 second interval
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val t = battery.batteryTemperature
                updateBatteryTempChart(t, batteryTempChart)
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(runnable)
    }

    private fun updateBatteryTempChart(temperature: Float, batteryTempChart: LineChart) {
        tempArr.add(Entry(tempArr.size.toFloat(), temperature))
        if (tempArr.size > 10) tempArr.removeAt(0)
        val lineDataSet = LineDataSet(tempArr, "Battery Temperature")
        lineDataSet.color = ContextCompat.getColor(this@MainActivity, R.color.cyan)
        lineDataSet.valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.white)
        batteryTempChart.data = LineData(lineDataSet)
        batteryTempChart.invalidate()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                ShowData()
            } else {
                val textBox1 = findViewById<TextView>(R.id.textBox1)
                textBox1.text = "Location permission denied."
            }
        }
    }
}
