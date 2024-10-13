package com.aayush683.introspex

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.an.deviceinfo.device.model.Battery
import com.oseamiya.deviceinformation.MemoryInformation
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

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
        phoneTempChart.description = null;
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
        usedMemChart.description = null;
        usedMemChart.invalidate()
    }

    private fun updateCpuChart(cpuUsageChart: LineChart) {
        val data = readSystemFileAsInt("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
        if (data == -1) return
        cpuArr.add(Entry(cpuArr.size.toFloat(), data.toFloat()))
        if (cpuArr.size > 10) cpuArr.removeAt(0)

        val lineDataSet = LineDataSet(cpuArr, "CPU Clock Speed (MHz)")
        lineDataSet.color = ContextCompat.getColor(this, R.color.cyan)
        lineDataSet.valueTextColor = ContextCompat.getColor(this, R.color.white)

        cpuUsageChart.data = LineData(lineDataSet)
        cpuUsageChart.description = null;
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
        storageChart.description = null;
        storageChart.invalidate()
    }

    private fun updateBatteryChart(batteryLevel: Int, batteryChart: LineChart) {
        batteryArr.add(Entry(batteryArr.size.toFloat(), batteryLevel.toFloat()))
        if (batteryArr.size > 10) batteryArr.removeAt(0)

        val lineDataSet = LineDataSet(batteryArr, "Battery Voltage (V)")
        lineDataSet.color = ContextCompat.getColor(this, R.color.cyan)
        lineDataSet.valueTextColor = ContextCompat.getColor(this, R.color.white)

        batteryChart.data = LineData(lineDataSet)
        batteryChart.description = null;
        batteryChart.invalidate()
    }

    private fun readSystemFileAsInt(filePath: String): Int {
        return try {
            // Read the file and convert the content to Int
            val content = File(filePath).readText().trim()
            content.toInt()
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            -1
        }
    }
}
