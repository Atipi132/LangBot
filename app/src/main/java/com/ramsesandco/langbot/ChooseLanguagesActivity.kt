package com.ramsesandco.langbot

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar


class ChooseLanguagesActivity : AppCompatActivity() {

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_languages)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val languageToLearn = loadJson(applicationContext, "languageToLearn", "French|fr")!!.split("|")[1]
        val nativeLanguage = loadJson(applicationContext, "nativeLanguage", "English|en")!!.split("|")[1]

        val languageToLearnDropdown = findViewById<Spinner>(R.id.spinner_languageToLearn)
        val items = mutableMapOf("Afrikaans" to "af", "Albanian" to "sq", "Armenian" to "hy", "Azerbaijani" to "az", "Catalan" to "ca", "Chinese" to "zh", "Danish" to "da", "Dutch" to "nl", "English" to "en", "Finnish" to "fi", "French" to "fr", "Georgian" to "ka", "German" to "de", "Hungarian" to "hu", "Indonesian" to "in", "Italian" to "it", "Japanese" to "ja", "Korean" to "ko", "Latvian" to "lv", "Modern Greek" to "el", "Mongolian" to "mn", "Polish" to "pl", "Portugese" to "pt", "Romanian" to "ro", "Russian" to "ru", "Slovenian" to "sl", "Spanish" to "es", "Swahili" to "sw", "Swedish" to "sv", "Tamil" to "ta", "Telugu" to "te", "Thai" to "th", "Turkish" to "tr", "Vietnamese" to "vi", "Welsh" to "cy")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items.keys.toList())
        languageToLearnDropdown.adapter = adapter
        languageToLearnDropdown.setSelection(items.values.indexOf(languageToLearn))

        languageToLearnDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @Override
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                saveJson(applicationContext, "languageToLearn", "${languageToLearnDropdown.selectedItem}|${items[languageToLearnDropdown.selectedItem]}")
                // Toast.makeText(applicationContext, languageToLearnDropdown.selectedItem.toString(), Toast.LENGTH_LONG).show()
            }

            @Override
            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // your code here
            }
        }

        val nativeLanguageDropdown = findViewById<Spinner>(R.id.spinner_nativeLanguage)
        nativeLanguageDropdown.adapter = adapter
        nativeLanguageDropdown.setSelection(items.values.indexOf(nativeLanguage))

        nativeLanguageDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @Override
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                saveJson(applicationContext, "nativeLanguage", "${nativeLanguageDropdown.selectedItem}|${items[nativeLanguageDropdown.selectedItem]}")
                // Toast.makeText(applicationContext, nativeLanguageDropdown.selectedItem.toString(), Toast.LENGTH_LONG).show()
            }

            @Override
            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // your code here
            }
        }
    }
    
    @Override
    override fun onBackPressed() {
    }

    private fun saveJson(context: Context, preferenceKey: String, json: String) {
        val sharedPreferences = context.getSharedPreferences("preference", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(preferenceKey, json).apply()
    }

    private fun loadJson(context: Context, preferenceKey: String, defaultValue: String = ""): String? {
        val sharedPreferences = context.getSharedPreferences("preference", MODE_PRIVATE)
        return sharedPreferences.getString(preferenceKey, defaultValue)
    }
}