package com.ramsesandco.langbot

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    companion object {
        const val ASSISTANT = "assistant"
        const val USER = "user"
        const val MESSAGE = 0
        const val MISTAKE = 1
        const val TRANSLATION = 2
        const val IS_HIDDEN = 0
        const val IS_SHOWN = 1

    }

    private var languageToLearn = ""
    private var nativeLanguage = ""
    private var nativeLanguageAbbreviation = ""
    private var learnLanguageAbbreviation = ""
    private var systemPrompt = ""
    private var gridLine = 0
    private var messages = mutableListOf<String>()
    private var diffsList = mutableListOf<String>()
    private var dictMessageId = mutableMapOf<Int, Int>()

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout = findViewById<DrawerLayout>(R.id.main_constraint_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.parameters -> {
                    val chooseLanguagesIntent = Intent(this, ChooseLanguagesActivity::class.java)
                    startActivity(chooseLanguagesIntent)
                }

                R.id.erase_current_conversation -> {
                    AlertDialog.Builder(this)
                        .setTitle("Erase conversation")
                        .setMessage(R.string.erase_current_conversation)
                        .setPositiveButton(
                            getString(R.string.yes)
                        ) { _, _ -> eraseCurrentConversation("$languageToLearn|$nativeLanguage") }
                        .setNegativeButton(getString(R.string.no), null)  // A null listener allows the button to dismiss the dialog and take no further action.
                        .show()
                }

                R.id.help -> {
                    val tutorialIntent = Intent(this, TutorialActivity::class.java)
                    startActivity(tutorialIntent)
                }

                R.id.about -> {
                    val aboutIntent = Intent(this, AboutActivity::class.java)
                    startActivity(aboutIntent)
                }

                else -> super.onOptionsItemSelected(menuItem)
            }
            menuItem.isChecked = true
            drawerLayout.close()
            true
        }

        val actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

//        val languageToLearnList =
            loadJson("languageToLearn", "French|fr")!!.split("|").also {
            languageToLearn = it[0]
            learnLanguageAbbreviation = it[1]

        }
//        val nativeLanguageList =
            loadJson("nativeLanguage", "English|en")!!.split("|").also {
            nativeLanguage = it[0]
            nativeLanguageAbbreviation = it[1]
        }

//        val exampleQuestions = getLocalizedString(applicationContext, Locale(languageToLearnList[1]), R.string.example_question)
//            .split("\", ")
//            .toMutableList()
//        exampleQuestions.forEachIndexed { index, s ->
//            exampleQuestions[index] = s.replace("\"", "")
//        }
//
//        val exampleResponsesLearn = getLocalizedString(applicationContext, Locale(languageToLearnList[1]), R.string.example_response)
//            .split("\", ")
//            .toMutableList()
//        exampleResponsesLearn.forEachIndexed { index, s ->
//            exampleResponsesLearn[index] = s.replace("\"", "").split("|")[0]
//        }
//
//        val exampleResponsesNative = getLocalizedString(applicationContext, Locale(nativeLanguageList[1]), R.string.example_response)
//            .split("\", ")
//            .toMutableList()
//            .also {
//                Log.d("exampleResponsesNative", it.toString())
//                it.forEachIndexed { index, s ->
//                    Log.d("s$index", s)
//                    it[index] = s.replace("\"", "").split("|", limit = 2)[1]
//                }
//            }

        systemPrompt = "{\"role\": \"system\", \"content\": \"You are an expert $languageToLearn AI teacher who teaches to a $nativeLanguage user. You are having a conversation with the user so must keep your messages shorter than 265 characters per parts. You MUST ALWAYS write you messages using the following structure with '|' to separate the differents parts of your answer : '''<Write your answer to the user's last message in $languageToLearn here. Try to keep the conversation going.>|<In $nativeLanguage, write a short explanation to any potential spelling, grammatical or conjugation mistake that the user made in his last message here. If and only if there was no mistake in the user's last message, only write '/'.>|<Finally, rewrite the FIRST PART of your answer the same way, but now translated into $nativeLanguage here.>'''.\"}"

        Log.d("nativeLanguagePreference", nativeLanguage)
        Log.d("languageToLearnPreference", languageToLearn)
        Log.d("systemPrompt", systemPrompt)

        var messagesJson = JSONArray()
        var diffsArray = JSONArray()
        val messagesLoad =  loadJson("json$languageToLearn|$nativeLanguage")
        val diffsLoad = loadJson("diffs$languageToLearn|$nativeLanguage")
//        Log.d("messagesLoad Log", messagesLoad!!)
//        Log.d("diffsLoad Log", diffsLoad!!)
        if (messagesLoad!!.isNotEmpty()) messagesJson = JSONArray(messagesLoad)
        if (diffsLoad!!.isNotEmpty()) {
            diffsArray = JSONArray(diffsLoad)
            for (i in 0 until diffsArray.length()) {
                diffsList.add(diffsArray[i].toString())
            }
        }
        messages.add(systemPrompt)

        var diffId = 0
        for (i in 1 until messagesJson.length())
        {
            val sender = JSONObject(messagesJson.getString(i)).getString("role")
            if (sender == ASSISTANT) {
                val mistakeStrBuilder = diffArrayToSpannable(diffsArray.getJSONArray(diffId))
                diffId++
                createNewText(JSONObject(messagesJson.getString(i)).getString("content"), mistakeStrBuilder, sender)
            }
            else createNewText(JSONObject(messagesJson.getString(i)).getString("content"), sender = sender)
        }

        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val message = it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!![0]
                findViewById<EditText>(R.id.message_edit_text).setText(message)

            }
        }

        val microphoneButton = findViewById<ImageButton>(R.id.microphone_button)
        microphoneButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                learnLanguageAbbreviation
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak ")
            try {
                launcher.launch(intent)
            } catch (error: Exception) {
                Toast.makeText(this@MainActivity, "Error : " + error.message, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val submitButton = findViewById<ImageButton>(R.id.submit_message_button)
        submitButton.setOnClickListener {
            val messageEditText = findViewById<EditText>(R.id.message_edit_text)
            if (messageEditText.text.isNotEmpty()) {
                createNewText(messageEditText.text.toString(), sender = USER)
                messageEditText.setText("")
                getMessageFromAI()
            }
        }

        val messageSizeView = findViewById<TextView>(R.id.message_size)
        val messageEditText = findViewById<EditText>(R.id.message_edit_text)
        messageEditText.doOnTextChanged { _, _, _, _ ->
            val messageCharactersCount = messageEditText.text.count()
            messageSizeView.text = getString(R.string.message_size, messageCharactersCount)
            if (messageCharactersCount > 0) {
                microphoneButton.visibility = View.GONE
                submitButton.visibility = View.VISIBLE
            } else {
                submitButton.visibility = View.GONE
                microphoneButton.visibility = View.VISIBLE
            }
        }

        if (loadJson("firstTime", "1") == "1") {
            val tutorialIntent = Intent(this, TutorialActivity::class.java)
            saveJson("firstTime", "0")
            startActivity(tutorialIntent)
        }
    }

    private fun addMessageToRequest(message: String, sender: String){
        messages.add("{\"role\": \"$sender\", \"content\": \"${message.replace("\"", "'").replace("\n","\\n")}\"}")
        Log.d("Messages : ", messages.toString())
        val messagesJson = JSONArray(messages)
        saveJson("json$languageToLearn|$nativeLanguage", messagesJson.toString())

    }

    private fun createNewText(message : String, mistake: SpannableStringBuilder = SpannableStringBuilder(),sender : String) {
        val jsonMessages = if (sender == ASSISTANT) message.split('|') else listOf()
        val cardParams = GridLayout.LayoutParams(GridLayout.spec(gridLine), GridLayout.spec(0))
        cardParams.setGravity(if (sender == USER) Gravity.END else Gravity.START)

        val textParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val newCardView = CardView(applicationContext)
        newCardView.layoutParams = cardParams
        val cardColor = if (sender == USER) R.color.usermessagecolor else R.color.assistantmessagecolor
        newCardView.setCardBackgroundColor(getColor(cardColor))
        newCardView.radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20F, resources.displayMetrics)
        newCardView.cardElevation = 0F
        newCardView.useCompatPadding = true

        val gridLayout = findViewById<GridLayout>(R.id.grid_layout)

        val newTextView = TextView(applicationContext)
        newTextView.id = View.generateViewId()
        dictMessageId[gridLine] = newTextView.id
        newTextView.layoutParams = textParams
        newTextView.setPadding(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12F, resources.displayMetrics).toInt(),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8F, resources.displayMetrics).toInt(),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12F, resources.displayMetrics).toInt(),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8F, resources.displayMetrics).toInt()
        )
        newTextView.text = if (sender == ASSISTANT) jsonMessages[MESSAGE] else message
        newTextView.setTextColor(getColor(R.color.black))
        newTextView.maxEms = 12
        newTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)

        newCardView.addView(newTextView)
        if (sender == ASSISTANT) {
            if (jsonMessages.size >= 3) {
                newCardView.isClickable = true
                newTextView.tag = IS_HIDDEN
                newCardView.setOnClickListener {
                    if (newTextView.tag == IS_HIDDEN) {
                        newTextView.append("\n-----------------------------\n${jsonMessages[TRANSLATION]}")
                        newTextView.tag = IS_SHOWN
                    } else {
                        newTextView.text = jsonMessages[MESSAGE]
                        newTextView.tag = IS_HIDDEN
                    }
                }
            }
            if (jsonMessages.size >= 2) {
                val mistakeMessage = jsonMessages[MISTAKE]
                if (mistakeMessage.replace("/", "").replace(" ", "") != "") {
                    val userTextView = findViewById<TextView>(dictMessageId[gridLine - 1]!!)
                    val userCardView = userTextView.parent as CardView
                    val currentText = userTextView.text
                    userCardView.isClickable = true
                    userTextView.tag = IS_HIDDEN
                    userCardView.setOnClickListener {
                        if (userTextView.tag == IS_HIDDEN) {
                            if (listOf("en", "fr", "it", "es").contains(learnLanguageAbbreviation)) {
                                userTextView.text = SpannableStringBuilder(userTextView.text).append("\n----------------------------------\n${
                                    getLocalizedString(
                                        applicationContext,
                                        Locale(nativeLanguageAbbreviation),
                                        R.string.mistake
                                    )}").append(mistake)
                            }
                            else userTextView.text = SpannableStringBuilder(userTextView.text).append("\n----------------------------------\n$mistake")
                            userTextView.tag = IS_SHOWN
                        } else {
                            userTextView.text = currentText
                            userTextView.tag = IS_HIDDEN
                        }
                    }
                }
            }
            newCardView.setOnLongClickListener {
//                TODO Add code to copy message to clipboard when long click from user
                true
            }
        }
        gridLayout.addView(newCardView)
        addMessageToRequest(message, sender)
        gridLine += 1

        val scrollview = findViewById<ScrollView>(R.id.scroll_view)
        scrollview.post { scrollview.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun getMessageFromAI() {
        val httpThread = Thread {
            val client = OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
            val mediaType = "application/json".toMediaType()
            val body = "{\"messages\": $messages, \"srcLang\": \"$nativeLanguageAbbreviation\", \"outLang\": \"$learnLanguageAbbreviation\", \"outLanguage\": \"$languageToLearn\", \"lastUserMessage\": \"${JSONObject(messages.last()).getString("content")}\"}".toRequestBody(mediaType)
            Log.d("RequestBody" ,"{\"messages\": $messages, \"srcLang\": \"$nativeLanguageAbbreviation\", \"outLang\": \"$learnLanguageAbbreviation\"}, \"outLanguage\": \"$languageToLearn\", \"lastUserMessage\": \"${JSONObject(messages.last()).getString("content")}\"")
            val request = Request.Builder()
                .url("https://8506vdeujh.execute-api.eu-west-3.amazonaws.com/test/getapiresponse")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val sendButton = findViewById<ImageButton>(R.id.submit_message_button)
            runOnUiThread { sendButton.isEnabled = false }
            sendButton.setOnClickListener {
                Toast.makeText(applicationContext, "Please wait 10 seconds between each message.", Toast.LENGTH_LONG).show()
            }
            sendButton.postDelayed(
                {
                    sendButton.isEnabled = true
                    sendButton.setOnClickListener {
                        val messageEditText = findViewById<EditText>(R.id.message_edit_text)
                        if (messageEditText.text.isNotEmpty()) {
                            createNewText(messageEditText.text.toString(), sender = USER)
                            messageEditText.setText("")
                            getMessageFromAI()
                        }
                    }
                },
                10000)
            var response = Response.Builder().request(request).protocol(Protocol.HTTP_2).message("Error").code(408).build()
            response = try {
                client.newCall(request).execute()
            } catch (e: SocketTimeoutException) {
                response.newBuilder().request(request).protocol(Protocol.HTTP_2).body("Error 408 : TimeOut exception".toResponseBody(mediaType)).message("Error").code(408).build()
            }
            if (response.code == 200) {
                val stringResponse = JSONObject(response.body?.string()!!)
                var responseMessage = stringResponse.getString("messages")
                val responseMessageList = responseMessage.split("|").toMutableList()
                val diffArray = JSONArray(stringResponse.getString("diff"))
                val mistakeStrBuild = diffArrayToSpannable(diffArray)
                responseMessageList[1] = mistakeStrBuild.toString()
                responseMessage = responseMessageList.joinToString("|")

                runOnUiThread { createNewText(responseMessage, mistakeStrBuild, ASSISTANT) }
                diffsList.add(diffArray.toString())
                saveJson("diffs$languageToLearn|$nativeLanguage", diffsList.toString())
                Log.d("Request response message : ", responseMessage)
            } else {
                runOnUiThread { Toast.makeText(applicationContext, "An error occured while sending/receiving response.", Toast.LENGTH_LONG).show()
                Log.d("Response Not Received", response.body!!.string())}
            }
        }
        httpThread.start()
    }

    private fun saveJson(preferenceKey: String, json: String) {
        val sharedPreferences = applicationContext.getSharedPreferences("preference", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(preferenceKey, json).apply()
    }

    private fun loadJson(preferenceKey: String, defaultValue: String = ""): String? {
        val sharedPreferences = applicationContext.getSharedPreferences("preference", MODE_PRIVATE)
        return sharedPreferences.getString(preferenceKey, defaultValue)
    }

    private fun deleteJson(preferenceKey: String) {
        val sharedPreferences = applicationContext.getSharedPreferences("preference", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(preferenceKey).apply()
    }

    private fun eraseCurrentConversation(preferenceKey: String) {
        deleteJson("json$preferenceKey")
        deleteJson("diffs$preferenceKey")
        messages.clear()
        diffsList.clear()
        gridLine = 0
        findViewById<GridLayout>(R.id.grid_layout).removeAllViews()
        messages.add(systemPrompt)
    }

    private fun getLocalizedString(context: Context, desiredLocale: Locale?, id: Int): String {
        var conf = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources.getString(id)
    }

    private fun diffArrayToSpannable(diffArray: JSONArray): SpannableStringBuilder {
        val mistakeStrBuild = SpannableStringBuilder("")
        var indexStart = 0
        for (i in 0 until diffArray.length()) {
            val id = diffArray.getJSONArray(i).getString(0)
            val text = diffArray.getJSONArray(i).getString(1)
            Log.d("Mistake Log", "$id, $text")
            when(id) {
                "1" -> {
                    mistakeStrBuild.append(text)
                    mistakeStrBuild.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), indexStart, indexStart + text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "0" -> mistakeStrBuild.append(text)
                "-1" -> indexStart -= text.length
            }
            indexStart += text.length
        }
        return mistakeStrBuild
    }

}