package Akari.Twitter.share.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import Akari.Twitter.share.R
import Akari.Twitter.share.data.PrefManager
import Akari.Twitter.share.databinding.ActivityMainBinding
import Akari.Twitter.share.hook.TweetUrlModifier
import Akari.Twitter.share.util.DigitConverter
import Akari.Twitter.share.util.LocaleHelper
import Akari.Twitter.share.util.ModuleStatus

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val SAMPLE_TWEET_URL = "https://x.com/zohanlin/status/2116188844500619417?a=ZRipCP_Wn-MjXIXrZpU&s=19"
        const val GITHUB_REPO_URL = "https://github.com/Akiyama-25/Twi-From-67"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdgeInsets()
        setupPredictiveBack()
        setupModuleStatus()
        setupParameterControls()
        setupSwitches()
        setupLiveTest()
        setupLanguageControls()
        setupAbout()
    }

    private fun setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())

            binding.appBarLayout.setPadding(0, statusBarInsets.top, 0, 0)
            binding.contentContainer.setPadding(
                binding.contentContainer.paddingStart,
                binding.contentContainer.paddingTop,
                binding.contentContainer.paddingEnd,
                navBarInsets.bottom + 24
            )
            insets
        }
    }

    private fun setupPredictiveBack() {
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun setupModuleStatus() {
        val isActive = ModuleStatus.isModuleActive()
        if (isActive) {
            binding.imgStatusIcon.setImageResource(R.drawable.ic_status_check)
            binding.tvStatusText.setText(R.string.status_active)
        } else {
            binding.imgStatusIcon.setImageResource(R.drawable.ic_status_alert)
            binding.tvStatusText.setText(R.string.status_inactive)
        }
    }

    private fun setupParameterControls() {
        // Full-width to half-width auto-conversion & digits-only input filter
        val digitFilter = InputFilter { source, start, end, _, _, _ ->
            val sb = StringBuilder()
            for (i in start until end) {
                val ch = source[i]
                when (ch) {
                    in '\uFF10'..'\uFF19' -> sb.append((ch.code - 0xFEE0).toChar())
                    in '0'..'9' -> sb.append(ch)
                    else -> {} // Discard any non-digit character
                }
            }
            sb.toString()
        }

        binding.editCustomS.filters = arrayOf(digitFilter, InputFilter.LengthFilter(2))

        val currentVal = PrefManager.getCustomS(this)
        binding.editCustomS.setText(currentVal)
        highlightMatchingChip(currentVal)

        binding.editCustomS.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLiveTestPreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup preset chips
        binding.chipIphoneApp.setOnClickListener { selectPreset("46") }
        binding.chipAndroid.setOnClickListener { selectPreset("19") }
        binding.chipWebCopy.setOnClickListener { selectPreset("20") }
        binding.chipWebNew.setOnClickListener { selectPreset("21") }
        binding.chipIphoneSys.setOnClickListener { selectPreset("12") }
        binding.chipWebEmail.setOnClickListener { selectPreset("17") }

        // Save Button Handler
        binding.btnSave.setOnClickListener {
            saveConfiguration()
        }
    }

    private fun selectPreset(value: String) {
        binding.editCustomS.setText(value)
        binding.editCustomS.setSelection(value.length)
        highlightMatchingChip(value)
        updateLiveTestPreview()
    }

    private fun highlightMatchingChip(value: String) {
        binding.chipIphoneApp.isChecked = (value == "46")
        binding.chipAndroid.isChecked = (value == "19")
        binding.chipWebCopy.isChecked = (value == "20")
        binding.chipWebNew.isChecked = (value == "21")
        binding.chipIphoneSys.isChecked = (value == "12")
        binding.chipWebEmail.isChecked = (value == "17")
    }

    private fun saveConfiguration() {
        val rawInput = binding.editCustomS.text?.toString() ?: ""
        val sanitized = DigitConverter.sanitizeToTwoDigits(rawInput)

        if (!DigitConverter.isValidTwoDigits(sanitized)) {
            binding.layoutCustomS.error = getString(R.string.err_invalid_number)
            Toast.makeText(this, getString(R.string.err_invalid_number), Toast.LENGTH_SHORT).show()
            return
        }
        binding.layoutCustomS.error = null

        PrefManager.setCustomS(this, sanitized)
        highlightMatchingChip(sanitized)

        // Secret Easter Egg Requirement:
        // When user enters 67 and saves, show Toast "67676767".
        if (sanitized == DigitConverter.EASTER_EGG_VALUE) {
            Toast.makeText(this, DigitConverter.EASTER_EGG_TOAST, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.msg_saved_success), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwitches() {
        binding.switchEnableHook.isChecked = PrefManager.isEnabled(this)
        binding.switchCleanTracking.isChecked = PrefManager.isCleanTracking(this)

        binding.switchEnableHook.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setEnabled(this, isChecked)
            updateLiveTestPreview()
        }

        binding.switchCleanTracking.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setCleanTracking(this, isChecked)
            updateLiveTestPreview()
        }
    }

    private fun setupLiveTest() {
        binding.editTestUrl.setText(SAMPLE_TWEET_URL)

        binding.btnSampleUrl.setOnClickListener {
            binding.editTestUrl.setText(SAMPLE_TWEET_URL)
            updateLiveTestPreview()
        }

        binding.editTestUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLiveTestPreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnCopyResult.setOnClickListener {
            val result = binding.tvTestResult.text.toString()
            if (result.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Rewritten URL", result)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_SHORT).show()
            }
        }

        updateLiveTestPreview()
    }

    private fun updateLiveTestPreview() {
        val inputUrl = binding.editTestUrl.text?.toString() ?: ""
        val targetS = binding.editCustomS.text?.toString()?.ifBlank { "67" } ?: "67"
        val cleanTracking = binding.switchCleanTracking.isChecked

        val rewritten = TweetUrlModifier.modify(inputUrl, targetS, cleanTracking)
        binding.tvTestResult.text = rewritten
    }

    private fun setupLanguageControls() {
        updateLanguageDisplay()

        binding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.btnChangeLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun updateLanguageDisplay() {
        val currentLang = LocaleHelper.getCurrentLanguage(this)
        val displayName = when (currentLang) {
            LocaleHelper.LANG_ZH_HANS -> getString(R.string.lang_zh_hans)
            LocaleHelper.LANG_ZH_HANT -> getString(R.string.lang_zh_hant)
            LocaleHelper.LANG_EN -> getString(R.string.lang_en)
            LocaleHelper.LANG_JA -> getString(R.string.lang_ja)
            else -> getString(R.string.lang_system)
        }
        binding.tvCurrentLanguage.text = displayName
    }

    private fun showLanguageDialog() {
        val languageCodes = arrayOf(
            LocaleHelper.LANG_SYSTEM,
            LocaleHelper.LANG_ZH_HANS,
            LocaleHelper.LANG_ZH_HANT,
            LocaleHelper.LANG_EN,
            LocaleHelper.LANG_JA
        )

        val languageLabels = arrayOf(
            getString(R.string.lang_system),
            getString(R.string.lang_zh_hans),
            getString(R.string.lang_zh_hant),
            getString(R.string.lang_en),
            getString(R.string.lang_ja)
        )

        val currentLang = LocaleHelper.getCurrentLanguage(this)
        val selectedIndex = languageCodes.indexOf(currentLang).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_select_language))
            .setSingleChoiceItems(languageLabels, selectedIndex) { dialog, which ->
                val chosenCode = languageCodes[which]
                dialog.dismiss()
                if (chosenCode != currentLang) {
                    // Hot update language immediately via AppCompatDelegate
                    LocaleHelper.applyLanguage(this, chosenCode)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupAbout() {
        binding.btnOpenGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL))
            startActivity(intent)
        }
    }
}