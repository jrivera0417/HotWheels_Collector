package com.example.hotwheelscollector.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hotwheelscollector.R
import com.example.hotwheelscollector.data.Car
import com.example.hotwheelscollector.data.Collection
import com.example.hotwheelscollector.data.DatabaseHelper
import com.example.hotwheelscollector.data.SessionManager
import com.example.hotwheelscollector.utils.FavoriteEvents
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ColeccionFragment : Fragment(R.layout.fragment_coleccion) {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var etBuscar: EditText
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: ColeccionAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var favoriteListener: () -> Unit

    private var userId: Int = 0
    private var selectedCategoryId: Int? = null
    private var expandedCategory: Int? = null
    private var cachedCars: List<Car> = emptyList()
    private var cachedCollections: List<Collection> = emptyList()
    private var filteredCars: List<Car> = emptyList()
    private var currentQuery = ""
    private var currentColumns = 2

    private val PREFS_NAME = "collection_prefs"
    private val KEY_LAST_CATEGORY = "last_category"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DatabaseHelper(requireContext())
        userId = SessionManager.getCurrentUserId(requireContext())
        recycler = view.findViewById(R.id.recyclerCars)
        recyclerCategories = view.findViewById(R.id.recyclerCategories)
        emptyState = view.findViewById(R.id.emptyState)
        etBuscar = view.findViewById(R.id.etBuscar)

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)

        setupRecycler()
        setupCategories()
        setupSearch()

        loadInitialData()
        render()

        favoriteListener = {
            view.post {
                reloadData()
                render()
            }
        }

        fab.setOnClickListener {

            AddCarBottomSheet {

                reloadData()
                render()

            }.show(parentFragmentManager, "AddCar")
        }
    }

    private fun setupRecycler() {

        adapter = ColeccionAdapter(
            db = db,
            onHeaderClick = { toggleCategory(it) },
            onCarClick = { car -> openCarDetail(car.id) },
            onChange = {
                reloadData()
                render()
            }
        )

        val prefs = requireContext().getSharedPreferences(
            "collector_prefs",
            Context.MODE_PRIVATE
        )

        currentColumns = prefs.getInt("grid_columns", 2)

        applyGridLayoutManager()

        recycler.adapter = adapter
        recycler.setHasFixedSize(true)

        recycler.itemAnimator =
            DefaultItemAnimator().apply {
                supportsChangeAnimations = false
            }
    }

    private fun applyGridLayoutManager() {

        val layoutManager =
            GridLayoutManager(requireContext(), currentColumns)

        layoutManager.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {

                override fun getSpanSize(position: Int): Int {

                    return if (
                        adapter.getItemViewType(position)
                        == ColeccionAdapter.TYPE_HEADER
                    ) {
                        currentColumns
                    } else {
                        1
                    }
                }
            }

        recycler.layoutManager = layoutManager
    }

    private fun setupCategories() {

        categoryAdapter = CategoryAdapter(emptyList()) { category ->

            val clickedId = category?.id
            val isSameCategory =
                selectedCategoryId == clickedId

            if (isSameCategory) {

                selectedCategoryId = null

                expandedCategory =
                    getLastCategory()
                        ?: cachedCollections.firstOrNull()?.id

            } else {

                selectedCategoryId = clickedId
                expandedCategory = clickedId
            }

            categoryAdapter.setSelectedCategory(
                selectedCategoryId
            )

            saveLastCategory(expandedCategory)

            render()
        }

        recyclerCategories.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        recyclerCategories.adapter = categoryAdapter
    }

    private fun setupSearch() {

        etBuscar.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                currentQuery = s.toString().trim()

                filteredCars =
                    if (currentQuery.isEmpty()) {

                        cachedCars

                    } else {

                        cachedCars.filter { car ->

                            val collectionName =
                                cachedCollections
                                    .find {
                                        it.id == car.collectionId
                                    }
                                    ?.name
                                    .orEmpty()

                            car.name.contains(currentQuery, true) ||
                                    car.modelCode.contains(currentQuery, true) ||
                                    car.seriesNumber.contains(currentQuery, true) ||
                                    collectionName.contains(currentQuery, true)
                        }
                    }

                render()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        etBuscar.setOnEditorActionListener { v, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                val imm = requireContext()
                    .getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager

                imm.hideSoftInputFromWindow(
                    v.windowToken,
                    0
                )

                v.clearFocus()

                if (
                    filteredCars.isEmpty()
                    && currentQuery.isNotEmpty()
                ) {

                    Toast.makeText(
                        requireContext(),
                        "No se encontraron resultados",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                true

            } else {
                false
            }
        }
    }

    // =========================
    // DATA
    // =========================

    private fun getSortedCollections(): List<Collection> {

        val prefs = requireContext().getSharedPreferences(
            "collector_prefs",
            Context.MODE_PRIVATE
        )

        val sortOrder =
            prefs.getString("sort_order", "name")

        val collections =
            db.getCollectionsByUser(userId)

        return when (sortOrder) {

            "recent" -> {
                collections.sortedByDescending { it.id }
            }

            else -> {
                collections.sortedBy {
                    it.name.lowercase()
                }
            }
        }
    }

    private fun loadInitialData() {

        cachedCars = db.getCarsByUser(userId)

        filteredCars = cachedCars

        cachedCollections = getSortedCollections()

        expandedCategory =
            cachedCollections.firstOrNull()?.id

        selectedCategoryId = null

        categoryAdapter.updateData(cachedCollections)
    }

    private fun reloadData() {

        cachedCars = db.getCarsByUser(userId)

        filteredCars =
            if (currentQuery.isEmpty()) {

                cachedCars

            } else {

                cachedCars.filter { car ->

                    val collectionName =
                        cachedCollections
                            .find {
                                it.id == car.collectionId
                            }
                            ?.name
                            .orEmpty()

                    car.name.contains(currentQuery, true) ||
                            car.modelCode.contains(currentQuery, true) ||
                            car.seriesNumber.contains(currentQuery, true) ||
                            collectionName.contains(currentQuery, true)
                }
            }

        cachedCollections = getSortedCollections()

        categoryAdapter.updateData(cachedCollections)
    }

    // =========================
    // RENDER
    // =========================

    private fun render() {

        if (cachedCars.isEmpty()) {

            recycler.visibility = View.GONE
            recyclerCategories.visibility = View.GONE
            emptyState.visibility = View.VISIBLE

            return
        }

        recycler.visibility = View.VISIBLE
        recyclerCategories.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        val items = buildItems()

        adapter.currentQuery = currentQuery

        adapter.submitList(items.toList())

        recycler.post {
            adapter.notifyDataSetChanged()
        }

        autoScroll(items)
    }

    private fun buildItems(): List<ColeccionItem> {

        val items = mutableListOf<ColeccionItem>()

        cachedCollections.forEach { collection ->

            val fullList =
                filteredCars.filter {
                    it.collectionId == collection.id
                }

            if (fullList.isEmpty()) return@forEach

            val isExpanded =
                expandedCategory == collection.id

            val title =
                "${collection.name} (${fullList.size}/${collection.totalCars})"

            items.add(
                ColeccionItem.Header(
                    title = title,
                    collectionId = collection.id,
                    expanded = isExpanded
                )
            )

            if (isExpanded) {

                items.addAll(
                    fullList.map {
                        ColeccionItem.CarItem(it)
                    }
                )
            }
        }

        return items
    }

    private fun autoScroll(items: List<ColeccionItem>) {

        expandedCategory?.let { id ->

            val index = items.indexOfFirst {

                it is ColeccionItem.Header
                        && it.collectionId == id
            }

            if (index != -1) {

                recycler.post {

                    (recycler.layoutManager
                            as? GridLayoutManager)
                        ?.scrollToPositionWithOffset(
                            index,
                            0
                        )
                }
            }
        }
    }

    // =========================
    // ACCIONES
    // =========================

    private fun toggleCategory(collectionId: Int) {

        expandedCategory =
            if (expandedCategory == collectionId) {
                null
            } else {
                collectionId
            }

        saveLastCategory(expandedCategory)

        render()
    }

    private fun openCarDetail(carId: Int) {

        val bundle = Bundle().apply {

            putInt("CAR_ID", carId)

            putBoolean(
                "FROM_COLLECTION",
                true
            )
        }

        findNavController().navigate(
            R.id.carDetailFragment,
            bundle
        )
    }

    // =========================
    // PREFS
    // =========================

    private fun saveLastCategory(categoryId: Int?) {

        val prefs =
            requireContext()
                .getSharedPreferences(
                    PREFS_NAME,
                    0
                )

        prefs.edit()
            .putInt(
                KEY_LAST_CATEGORY,
                categoryId ?: -1
            )
            .apply()
    }

    private fun getLastCategory(): Int? {

        val prefs =
            requireContext()
                .getSharedPreferences(
                    PREFS_NAME,
                    0
                )

        val id =
            prefs.getInt(
                KEY_LAST_CATEGORY,
                -1
            )

        return if (id == -1) null else id
    }

    override fun onResume() {
        super.onResume()

        FavoriteEvents.addListener(favoriteListener)

        val prefs = requireContext().getSharedPreferences(
            "collector_prefs",
            Context.MODE_PRIVATE
        )

        val newColumns = prefs.getInt("grid_columns", 2)

        if (newColumns != currentColumns) {
            currentColumns = newColumns
            applyGridLayoutManager()
        }
    }

    override fun onPause() {

        FavoriteEvents.removeListener(favoriteListener)

        super.onPause()
    }
}