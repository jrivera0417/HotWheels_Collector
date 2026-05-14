package com.example.hotwheelscollector.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.models.Car
import com.example.hotwheelscollector.data.models.Collection
import com.example.hotwheelscollector.data.local.DatabaseHelper
import com.example.hotwheelscollector.data.local.SessionManager
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class StatisticsFragment :
    Fragment(R.layout.fragment_statistics) {

    private lateinit var tvTotalCars: TextView
    private lateinit var tvFavorites: TextView
    private lateinit var tvCollections: TextView
    private lateinit var tvTopBrand: TextView
    private lateinit var tvTopColor: TextView
    private lateinit var tvTotalInvested: TextView

    private lateinit var pieChartBrands: PieChart
    private lateinit var barChartCollections: BarChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalCars = view.findViewById(R.id.tvTotalCars)
        tvFavorites = view.findViewById(R.id.tvFavorites)
        tvCollections = view.findViewById(R.id.tvCollections)
        tvTopBrand = view.findViewById(R.id.tvTopBrand)
        tvTopColor = view.findViewById(R.id.tvTopColor)
        tvTotalInvested = view.findViewById(R.id.tvTotalInvested)

        pieChartBrands = view.findViewById(R.id.pieChartBrands)
        barChartCollections = view.findViewById(R.id.barChartCollections)

        loadStats()
    }

    private fun loadStats() {

        val db = DatabaseHelper(requireContext())
        val currentUserId = SessionManager.getCurrentUserId(requireContext())

        val cars = db.getCarsByUser(currentUserId)
        val collections = db.getCollectionsByUser(currentUserId)

        val favorites = cars.count { it.favorite }

        val topBrand = cars.groupingBy { it.brand }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "N/A"

        val topColor = cars.groupingBy { it.color }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "N/A"

        val totalInvested = cars.sumOf { it.price * it.quantity }

        // ================= UI =================
        tvTotalCars.text = cars.size.toString()
        tvFavorites.text = favorites.toString()
        tvCollections.text = collections.size.toString()
        tvTopBrand.text = topBrand
        tvTopColor.text = topColor
        tvTotalInvested.text = "$ ${"%,.0f".format(totalInvested)}"

        // ================= CHARTS =================
        setupBrandChart(pieChartBrands, cars)
        setupCollectionsChart(barChartCollections, cars, collections)
    }

    // ================= PIE CHART =================
    private fun setupBrandChart(
        chart: PieChart,
        cars: List<Car>
    ) {

        val brandMap = cars.groupingBy { it.brand }.eachCount()

        val entries = brandMap.map {
            PieEntry(it.value.toFloat(), it.key)
        }

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.primary),
            ContextCompat.getColor(requireContext(), R.color.primary_variant),
            ContextCompat.getColor(requireContext(), R.color.error),
            ContextCompat.getColor(requireContext(), R.color.icon_active),
            ContextCompat.getColor(requireContext(), R.color.icon_inactive)
        )

        val dataSet = PieDataSet(entries, "Marcas")
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)

        chart.data = pieData

        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.description.isEnabled = false

        chart.isDrawHoleEnabled = true
        chart.holeRadius = 60f
        chart.transparentCircleRadius = 65f
        chart.setHoleColor(ContextCompat.getColor(requireContext(), R.color.background_secondary))

        chart.centerText = "Marcas"
        chart.setCenterTextColor(Color.WHITE)
        chart.setCenterTextSize(14f)

        chart.setEntryLabelColor(Color.WHITE)

        chart.legend.textColor = Color.WHITE

        chart.animateY(900)
        chart.invalidate()
    }

    // ================= BAR CHART =================
    private fun setupCollectionsChart(
        chart: BarChart,
        cars: List<Car>,
        collections: List<Collection>
    ) {

        val collectionMap = collections.associate { collection ->
            collection.name to cars.count { it.collectionId == collection.id }
        }

        val sorted = collectionMap.toList()

        val entries = sorted.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.second.toFloat())
        }

        val labels = sorted.map { it.first }

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.primary),
            ContextCompat.getColor(requireContext(), R.color.primary_variant),
            ContextCompat.getColor(requireContext(), R.color.error)
        )

        val dataSet = BarDataSet(entries, "Colecciones")
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        chart.data = barData

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.granularity = 1f
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.textColor = Color.WHITE

        chart.axisLeft.textColor = Color.WHITE
        chart.axisRight.isEnabled = false

        chart.description.isEnabled = false

        chart.animateY(900)
        chart.invalidate()
    }
}