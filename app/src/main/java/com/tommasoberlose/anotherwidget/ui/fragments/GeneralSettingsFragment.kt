package com.tommasoberlose.anotherwidget.ui.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetColorPicker
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentGeneralSettingsBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_general_settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GeneralSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = GeneralSettingsFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var colors: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<FragmentGeneralSettingsBinding>(inflater, R.layout.fragment_general_settings, container, false)

        subscribeUi(viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListener()
        lifecycleScope.launch(Dispatchers.IO) {
            val lazyColors = requireContext().resources.getIntArray(R.array.material_colors)
            withContext(Dispatchers.Main) {
                colors = lazyColors
            }
        }
    }


    private fun subscribeUi(
        viewModel: MainViewModel
    ) {

        viewModel.textMainSize.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                main_text_size_label.text = String.format("%.0fsp", it)
            }
        })

        viewModel.textSecondSize.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                second_text_size_label.text = String.format("%.0fsp", it)
            }
        })

        viewModel.textGlobalColor.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                try {
                    Color.parseColor(it)
                } catch (e: Exception) {
                    Preferences.textGlobalColor = "#FFFFFF"
                }
                font_color_label.text = it.toUpperCase()
            }
        })

        viewModel.textShadow.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                text_shadow_label.text = getString(SettingsStringHelper.getTextShadowString(it))
            }
        })

        viewModel.customFont.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                custom_font_label.text = getString(SettingsStringHelper.getCustomFontLabel(it))
            }
        })
    }

    private fun maintainScrollPosition(callback: () -> Unit) {
        val scrollPosition = scrollView.scrollY
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            scrollView.smoothScrollTo(0, scrollPosition)
        }
    }

    private fun setupListener() {
        action_main_text_size.setOnClickListener {
            val dialog = BottomSheetMenu<Float>(requireContext(), header = getString(R.string.title_main_text_size)).setSelectedValue(Preferences.textMainSize)
            (32 downTo 10).filter { it % 2 == 0 }.forEach {
                dialog.addItem("${it}sp", it.toFloat())
            }
            dialog.addOnSelectItemListener { value ->
                    Preferences.textMainSize = value
            }.show()
        }

        action_second_text_size.setOnClickListener {
            val dialog = BottomSheetMenu<Float>(requireContext(), header = getString(R.string.title_second_text_size)).setSelectedValue(Preferences.textSecondSize)
            (28 downTo 10).filter { it % 2 == 0 }.forEach {
                dialog.addItem("${it}sp", it.toFloat())
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.textSecondSize = value
            }.show()
        }

        action_font_color.setOnClickListener {
            val textColor = try {
                Color.parseColor(Preferences.textGlobalColor)
            } catch (e: Exception) {
                Preferences.textGlobalColor = "#FFFFFF"
                Color.parseColor(Preferences.textGlobalColor)
            }
            BottomSheetColorPicker(requireContext(),
                colors = colors,
                header = getString(R.string.settings_font_color_title),
                selected = textColor,
                onColorSelected = { color: Int ->
                    val colorString = Integer.toHexString(color)
                    Preferences.textGlobalColor = "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                }
            ).show()
        }

        action_text_shadow.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.title_text_shadow)).setSelectedValue(Preferences.textShadow)
            (2 downTo 0).forEach {
                dialog.addItem(getString(SettingsStringHelper.getTextShadowString(it)), it)
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.textShadow = value
            }.show()
        }

        action_custom_font.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_custom_font_title)).setSelectedValue(Preferences.customFont)
            (0..1).forEach {
                dialog.addItem(getString(SettingsStringHelper.getCustomFontLabel(it)), it)
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.customFont = value
            }.show()

/*
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "* / *" TO FIX WITHOUT SPACE
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            try {
                startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), Constants.CUSTOM_FONT_CHOOSER_REQUEST_CODE)
            } catch (ex: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
            }
*/
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                RequestCode.CUSTOM_FONT_CHOOSER_REQUEST_CODE.code -> {
                    /*val uri = data.data
                    Log.d("AW", "File Uri: " + uri.toString())
                    val path = Util.getPath(this, uri)
                    Log.d("AW", "File Path: " + path)
                    SP.edit()
                            .putString(Constants.PREF_CUSTOM_FONT_FILE, path)
                            .commit()
                    sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
                    updateSettings()*/
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
