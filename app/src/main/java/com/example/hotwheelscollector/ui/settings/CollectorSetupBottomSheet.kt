package com.example.hotwheelscollector.ui.settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioGroup
import androidx.navigation.findNavController
import com.example.hotwheelscollector.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class CollectorSetupBottomSheet : BottomSheetDialogFragment() {

    private lateinit var prefs: android.content.SharedPreferences

    // VARIABLES TEMPORALES
    private var selectedTheme = "red"
    private var selectedGrid = 2
    private var selectedSort = "name"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        prefs = requireContext().getSharedPreferences(
            "collector_prefs",
            Context.MODE_PRIVATE
        )

        // CARGAR CONFIG ACTUAL
        selectedTheme =
            prefs.getString("theme", "red") ?: "red"

        selectedGrid =
            prefs.getInt("grid_columns", 2)

        selectedSort =
            prefs.getString("sort_order", "name") ?: "name"

        val view = LayoutInflater.from(context)
            .inflate(R.layout.bottomsheet_collector_setup, null)

        setupTheme(view)
        setupGrid(view)
        setupSort(view)

        // BOTÓN APLICAR
        val btnApply =
            view.findViewById<MaterialButton>(R.id.btnApply)

        btnApply.setOnClickListener {

            val currentDestination =
                requireActivity()
                    .findNavController(R.id.nav_host_fragment)
                    .currentDestination
                    ?.id ?: R.id.nav_home

            prefs.edit()
                .putString("theme", selectedTheme)
                .putInt("grid_columns", selectedGrid)
                .putString("sort_order", selectedSort)
                .putInt("last_destination", currentDestination)
                .apply()

            dismiss()

            requireActivity().recreate()
        }

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(view)

        return dialog
    }

    private fun setupTheme(view: View) {

        val rgTheme =
            view.findViewById<RadioGroup>(R.id.rgTheme)

        when (selectedTheme) {

            "orange" -> rgTheme.check(R.id.rbOrange)

            else -> rgTheme.check(R.id.rbRed)
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->

            selectedTheme = when (checkedId) {

                R.id.rbOrange -> "orange"

                else -> "red"
            }
        }
    }

    private fun setupGrid(view: View) {

        val rgGrid =
            view.findViewById<RadioGroup>(R.id.rgGrid)

        when (selectedGrid) {

            3 -> rgGrid.check(R.id.rbGrid3)

            else -> rgGrid.check(R.id.rbGrid2)
        }

        rgGrid.setOnCheckedChangeListener { _, checkedId ->

            selectedGrid = when (checkedId) {

                R.id.rbGrid3 -> 3

                else -> 2
            }
        }
    }

    private fun setupSort(view: View) {

        val rgSort =
            view.findViewById<RadioGroup>(R.id.rgSort)

        when (selectedSort) {

            "recent" -> rgSort.check(R.id.rbSortRecent)

            else -> rgSort.check(R.id.rbSortName)
        }

        rgSort.setOnCheckedChangeListener { _, checkedId ->

            selectedSort = when (checkedId) {

                R.id.rbSortRecent -> "recent"

                else -> "name"
            }
        }
    }
}