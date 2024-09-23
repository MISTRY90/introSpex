package com.aayush683.introspex

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Debug.MemoryInfo
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
import com.an.deviceinfo.device.model.Device
import com.an.deviceinfo.device.model.Memory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.oseamiya.deviceinformation.MemoryInformation


class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var phoneTempChart: LineChart
    private lateinit var usedMemChart: PieChart
    private val tempArr = ArrayList<Entry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home)

        // Initialize UI components here
        phoneTempChart = findViewById(R.id.phoneTempChart)
        usedMemChart = findViewById(R.id.usedMemChart)

        ShowData()
    }

    private fun ShowData() {
        setContentView(R.layout.home)

        // Find the views after setting the correct layout
        phoneTempChart = findViewById(R.id.phoneTempChart)
        usedMemChart = findViewById(R.id.usedMemChart)

        // Setup Charts
        phoneTempChart.setTouchEnabled(false)
        phoneTempChart.setPinchZoom(false)
        phoneTempChart.setBackgroundResource(R.drawable.vector)

        usedMemChart.setTouchEnabled(false)
        usedMemChart.setBackgroundResource(R.drawable.vector)

        // Update charts with data
        val battery = Battery(this)
        val temp = battery.batteryTemperature
        updateTempChart(temp, phoneTempChart)

        val memory = MemoryInformation(this)
        updateMemChart(memory, usedMemChart)
    }

    private fun updateTempChart(temperature: Float, phoneTempChart: LineChart) {
        tempArr.add(Entry(tempArr.size.toFloat(), temperature))
        if (tempArr.size > 10) tempArr.removeAt(0)
        val lineDataSet = LineDataSet(tempArr, "Device Temperature")
        lineDataSet.color = ContextCompat.getColor(this, R.color.cyan)
        lineDataSet.valueTextColor = ContextCompat.getColor(this, R.color.white)
        phoneTempChart.data = LineData(lineDataSet)
        phoneTempChart.description.text = "Device Temperature"
        phoneTempChart.description.textColor = ContextCompat.getColor(this, R.color.white)
        phoneTempChart.invalidate()
    }

    private fun updateMemChart(memory: MemoryInformation, usedMemChart: PieChart) {
        val totalMem = memory.totalRam
        val usedMem = memory.usedRam
        val usedMemPercentage = (usedMem.toFloat() / totalMem.toFloat()) * 100
        val freeMemPercentage = 100 - usedMemPercentage

        // Plot data in pie chart
        val pieEntries = ArrayList<PieEntry>()
        pieEntries.add(PieEntry(usedMemPercentage, "Used"))
        pieEntries.add(PieEntry(freeMemPercentage, "Free"))
        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.colors = listOf(ContextCompat.getColor(this, R.color.cyan), ContextCompat.getColor(this, R.color.white))
        usedMemChart.data = PieData(pieDataSet)
        usedMemChart.description.text = "Memory Usage"
        usedMemChart.description.textColor = ContextCompat.getColor(this, R.color.white)
        usedMemChart.invalidate()
    }
}

// killing processes and network? is a must