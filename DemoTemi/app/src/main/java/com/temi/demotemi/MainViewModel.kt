package com.temi.demotemi

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.chat.chatMessage
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.navigation.model.SpeedLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.round
import kotlin.random.Random

// This is used to record all screens that are currently present in the app
enum class Screen() {
    Home,
    Play_music
}

// Track other action state
enum class State {
    CONSTRAINT_FOLLOW,
    NULL
}

// Track Y distance
enum class YDirection {
    FAR,
    MIDRANGE,
    CLOSE,
    MISSING
}

// Track X distance
enum class XDirection {
    LEFT,
    RIGHT,
    MIDDLE,
    GONE

}

// Y based movement
enum class YMovement {
    CLOSER,
    FURTHER,
    NOWHERE
}

// X based movement
enum class XMovement {
    LEFTER,
    RIGHTER,
    NOWHERE
}

// Used to create tourStates
enum class TourState {
    START_LOCATION,
    IDLE,
    NULL,
    TESTING,
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val robotController: RobotController,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    public val themeMusic = AudioPlayer(context, SoundManager.THEME_MUSIC.mediaResId)

    //-----------------------------------VIEW
    private val _currentScreen = MutableStateFlow(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen

    public fun setCurrentScreen(currentScreen: Screen) {
        _currentScreen.value = currentScreen
    }

    // StateFlow for holding the current image resource
    private val _shouldPlayGif = MutableStateFlow(true)
    val shouldPlayGif: StateFlow<Boolean> = _shouldPlayGif

    // Function to toggle the GIF state
    fun toggleGif() {
        _shouldPlayGif.value = !_shouldPlayGif.value
    }
    //-----------------------------------END
    //-------------------------------------------------State Machine

    // Create a job to allow a the tour to be added into it
    var main_tour: Job? = null

    // Keep track of the systems current state
    private var tourState = TourState.NULL

    // Used to see if state has finished
    private var isTourStateFinished = false


    private fun stateFinished() {
        isTourStateFinished = true
        stateMode = State.NULL
    }

    private fun stateMode(state: State) {
        stateMode = state
    }

      private suspend fun idleSystem(idle: Boolean) {
        while (idle) {
            buffer()
        } // Set this and run to turn off the tour for UI testing
    }

    private fun initiateTour() {
        // This is the initialisation for the tour, should only be run through once
    }

    private suspend fun tourState(newTourState: TourState) {
        tourState = newTourState
        Log.i("INFO!", "$tourState")
        conditionGate { !isTourStateFinished }
        isTourStateFinished = false
    }

    init {
        fun runTour(start: Boolean) {
            if (start) {
                main_tour = viewModelScope.launch {
                    idleSystem(false) // Used for idling the states if needed
                    initiateTour()

                    val trackTourState =
                        launch { // Use this to handle the stateflow changes for tour
                            while (true) { // This will loop the states
                                buffer()
                            }
                        }

                    val tour = launch {
                        while (true) {
                            when (tourState) {

                                TourState.START_LOCATION -> {

                                }

                                TourState.IDLE -> {

                                }

                                TourState.NULL -> {

                                }

                                TourState.TESTING -> {

                                }

                            }
                            buffer()
                        }
                    }

                    // This should never be broken out of if the tour is meant to be always running.
                    while (true) {
                        buffer()
                    }
                }
            } else {
                main_tour?.cancel()
            }
        }

        runTour(true)

        // Thread used for handling interrupt system
        viewModelScope.launch {
            var talkNow = false
            launch {
                while (true) {
//                     Log.i("DEBUG!", "In misuse state: ${isMisuseState()}")
                    // Log.i("DEBUG!", "Current Language: ${language.value}")
                    if ((yPosition == YDirection.MISSING && interruptFlags["userMissing"] == true) || (yPosition == YDirection.CLOSE && interruptFlags["userTooClose"] == true) || ((isMisuseState()) && interruptFlags["deviceMoved"] == true)) {
                        conditionTimer(
                            { !((yPosition == YDirection.MISSING && interruptFlags["userMissing"] == true) || (yPosition == YDirection.CLOSE && interruptFlags["userTooClose"] == true) || (isMisuseState()) && interruptFlags["deviceMoved"] == true) },
                            interruptTriggerDelay
                        )
                        if ((yPosition != YDirection.MISSING && interruptFlags["userMissing"] == true) || (yPosition != YDirection.CLOSE && interruptFlags["userTooClose"] == true) || ((isMisuseState()) && interruptFlags["deviceMoved"] != true)) continue
                        Log.i("DEBUG!", "Interrupt 1")
                        triggeredInterrupt = true
                        stopMovement()
                        buffer()
                        tiltAngle(20)

                        conditionTimer(
                            { !((yPosition == YDirection.MISSING && interruptFlags["userMissing"] == true) || (yPosition == YDirection.CLOSE && interruptFlags["userTooClose"] == true) || (isMisuseState()) && interruptFlags["deviceMoved"] == true) },
                            interruptTriggerDelay / 2
                        )
                        if ((yPosition != YDirection.MISSING && interruptFlags["userMissing"] == true) || (yPosition != YDirection.CLOSE && interruptFlags["userTooClose"] == true) || ((isMisuseState()) && interruptFlags["deviceMoved"] != true)) continue
                        Log.i("DEBUG!", "Interrupt 2")
                        triggeredInterrupt = true
                        repeatSpeechFlag = true
                        talkNow = true
                        repeatGoToFlag = true
                        stopMovement()
                        buffer()
                        tiltAngle(20)
                    } else {
                        triggeredInterrupt = false
                        talkNow = false
                    }
                    buffer()
                }
            }
            while (true) {
                var attempts = 0
                while (triggeredInterrupt) {
                    if (talkNow) {

                        if (attempts > 6) {
                            resetTourEarly = true
                            interruptFlags["deviceMoved"] = false
                            interruptFlags["userMissing"] = false
                            interruptFlags["userTooClose"] = false

                            createSpeakThread(false)
                            talkingInThreadFlag = false
                            runTour(false)
                            runTour(true)
                            attempts = 0
                        }

                        when {
                            interruptFlags["deviceMoved"] == true && isMisuseState() -> robotController.speak(
                                "Hey, do not touch me.",
                                buffer
                            )

                            interruptFlags["userMissing"] == true && yPosition == YDirection.MISSING -> {
                                robotController.speak(
                                    "Sorry, I am unable to see you. Please come closer and I will start the tour again.",
                                    buffer
                                )
                                attempts++
                            }

                            interruptFlags["userTooClose"] == true && yPosition == YDirection.CLOSE -> robotController.speak(
                                "Hey, you are too close.",
                                buffer
                            )

                            else -> {}
                        }
                        conditionTimer({ !triggeredInterrupt }, 10)
                    }
                    buffer()
                }
                while (!triggeredInterrupt && yPosition == YDirection.MISSING && !preventResetFromIdle) {
                    attempts++

                    if (attempts > 6) {
                        resetTourEarly = true
                        interruptFlags["deviceMoved"] = false
                        interruptFlags["userMissing"] = false
                        interruptFlags["userTooClose"] = false

                        createSpeakThread(false)
                        talkingInThreadFlag = false
                        runTour(false)
                        runTour(true)
                        Log.i("BluetoothServer", "Triggered early start")
                        attempts = 0
                    }

                    conditionTimer(
                        { triggeredInterrupt || yPosition != YDirection.MISSING || preventResetFromIdle },
                        10
                    )
                }
                buffer()
            }
        }
    }

    //-------------------------------------------------END
    //-------------------------------------------------Text Processing
    private suspend fun getUseConfirmation(
        initialQuestion: String? = null,
        rejected: String? = null,
        afterRejectedDelay: Long = 0L,
        confirmed: String? = null,
        notUnderstood: String? = null,
        ignored: String? = null,
        exitCase: (suspend () -> Unit)? = null
    ) {
        shouldExit = false

        while (true) {
            if (xPosition != XDirection.GONE) { // Check if there is a user present
                // Check if there is an initial question
                speak(initialQuestion)

                while (true) {
                    listen()

                    when { // Condition gate based on what the user says
                        containsPhraseInOrder(userResponse, reject, true) -> {
                            speak(rejected)
                            delay(afterRejectedDelay)
                            break
                        }

                        containsPhraseInOrder(userResponse, confirmation, true) -> {
                            speak(confirmed)
                            shouldExit = true
                            break
                        }

                        else -> {
                            if (yPosition != YDirection.MISSING) {

                                speak(notUnderstood)

                            } else {

                                forcedSpeak(ignored)

                                break
                            }
                        }
                    }
                    buffer()
                }

                if (shouldExit) {
                    exitCase?.invoke() // Calls exitCase if it’s not null
                    break
                }

            }
            buffer()
        }
    }

    private suspend fun exitCaseCheckIfUserClose(
        notClose: String? = null,
        close: String? = null
    ) {
        while (true) {
            if (yPosition != YDirection.CLOSE) {
                if (!notClose.isNullOrEmpty()) {
                    speak(notClose)
                }
                break
            } else {
                if (!close.isNullOrEmpty()) {
                    speak(close)
                }
                conditionTimer(
                    { yPosition != YDirection.CLOSE },
                    time = 50
                )
                if (yPosition != YDirection.CLOSE) {
                    robotController.speak(context.getString(R.string.thank_you), buffer)
                    conditionGate({ ttsStatus.value.status != TtsRequest.Status.COMPLETED })
                }
            }
        }
    }

    private fun extractName(userResponse: String): String? {
        // Define common patterns for introducing names
        val namePatterns = listOf(
            "my name is ([A-Za-z]+)",  // e.g., "My name is John"
            "i am ([A-Za-z]+)",        // e.g., "I am Alice"
            "it's ([A-Za-z]+)",        // e.g., "It's Bob"
            "this is ([A-Za-z]+)",     // e.g., "This is Sarah"
            "call me ([A-Za-z]+)",     // e.g., "Call me Mike"
            "name is ([A-Za-z]+)",
            "is ([A-Za-z]+)",
            "me ([A-Za-z]+)",
            "i ([A-Za-z]+)",
            "am ([A-Za-z]+)"
        )

//        val namePatterns = listOf(
//            "我叫([\\u4e00-\\u9fa5]+)",  // e.g., "我叫小明"
//            "我的名字是([\\u4e00-\\u9fa5]+)",  // e.g., "我的名字是李华"
//            "我是([\\u4e00-\\u9fa5]+)",  // e.g., "我是张伟"
//            "这是([\\u4e00-\\u9fa5]+)",  // e.g., "这是王芳"
//            "叫我([\\u4e00-\\u9fa5]+)",  // e.g., "叫我小李"
//            "名字是([\\u4e00-\\u9fa5]+)",  // e.g., "名字是陈琳"
//            "是([\\u4e00-\\u9fa5]+)",  // e.g., "是刘强"
//            "我([\\u4e00-\\u9fa5]+)",  // e.g., "我李杰"
//            "叫([\\u4e00-\\u9fa5]+)",  // e.g., "叫韩梅"
//            "名([\\u4e00-\\u9fa5]+)"  // e.g., "名赵云"
//        )

        // Iterate over each pattern to try to match the user's response
        for (pattern in namePatterns) {
            val regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            val matcher = regex.matcher(userResponse)

            // If a pattern matches, return the extracted name
            if (matcher.find()) {
                return matcher.group(1) // The name will be in the first capturing group
            }
        }

        // If no pattern matches, check if the userResponse is a single word and return it
//        val singleWordPattern = Pattern.compile("^[A-Za-z]+$", Pattern.CASE_INSENSITIVE)
        val singleWordPattern =
            Pattern.compile("^[\\u4e00-\\u9fa5]+$", Pattern.CASE_INSENSITIVE)
        val singleWordMatcher = singleWordPattern.matcher(userResponse.trim())

        return if (singleWordMatcher.matches()) userResponse.trim() else null
    }

    // key word lists
    private val confirmation = listOf(
        "Yes", "Okay", "Sure", "I'm willing", "Count me in", "Absolutely no problem",
        "Of course", "Right now", "Let's go", "I'll be there",
        "Sounds good", "I can join", "I'm ready", "It's settled",
        "Definitely", "On my way", "I'll come"
    )

    private val reject = listOf(
        "No", "Not now", "Can't", "Not attending", "Can't make it",
        "Impossible", "Sorry", "I have plans", "Not going",
        "Unfortunately can't", "I can't do it", "Regretfully no",
        "No way", "No thanks", "I'm busy", "I need to decline"
    )

    // Function to check for keywords or phrases
    private fun containsPhraseInOrder(
        userResponse: String?,
        phrases: List<String>,
        ignoreCase: Boolean = true
    ): Boolean {
        if (userResponse.isNullOrEmpty()) {
            return false // Return false if the response is null or empty
        }

        // Check for each phrase in the user response
        return phrases.any { phrase ->
            val words = phrase.split(" ")
            var userWords = userResponse.split(" ")

            var lastIndex = -1
            for (word in words) {
                // Find the word in user response
                lastIndex = userWords.indexOfFirst {
                    if (ignoreCase) {
                        it.equals(word, ignoreCase = true)
                    } else {
                        it == word
                    }
                }
                if (lastIndex == -1) return@any false // Word not found
                // Ensure the next word is after the last found word
                userWords = userWords.drop(lastIndex + 1)
            }
            true // All words were found in order
        }
    }
        //-------------------------------------------------END
        //-------------------------------------------------Actions
    private suspend fun goTo(
        location: String,
        speak: String? = null,
        haveFace: Boolean = true,
        backwards: Boolean = false,
        setInterruptSystem: Boolean = false,
        setInterruptConditionUserMissing: Boolean = false,
        setInterruptConditionUSerToClose: Boolean = false,
        setInterruptConditionDeviceMoved: Boolean = false
    ) {
        _isGoing.value = true
        updateInterruptFlag("userMissing", setInterruptConditionUserMissing)
        updateInterruptFlag("userTooClose", setInterruptConditionUSerToClose)
        updateInterruptFlag("deviceMoved", setInterruptConditionDeviceMoved)

        var hasGoneToLocation = false

        speak(
            speak,
            false,
            haveFace = haveFace,
            setInterruptSystem,
            setInterruptConditionUserMissing,
            setInterruptConditionUSerToClose,
            setInterruptConditionDeviceMoved
        )
        // *******************************************

//        updateInterruptFlag("userMissing", setInterruptConditionUserMissing)
//        updateInterruptFlag("userTooClose", setInterruptConditionUSerToClose)
//        updateInterruptFlag("deviceMoved", setInterruptConditionDeviceMoved)
        if (setInterruptSystem) {
            while (true) { // loop until to makes it to the start location
                Log.i("DEBUG!", "Has Gone To Location: $hasGoneToLocation")

                if (!triggeredInterrupt && !hasGoneToLocation) {
                    robotController.goTo(location, backwards); Log.i(
                        "DEBUG!",
                        "Hello: $repeatGoToFlag "
                    )
                }

                buffer()
                Log.i("DEBUG!", "Triggered?: ")
                conditionGate({ goToLocationState != LocationState.COMPLETE && goToLocationState != LocationState.ABORT || triggeredInterrupt && setInterruptSystem && (setInterruptConditionUserMissing || setInterruptConditionUSerToClose || setInterruptConditionDeviceMoved) })

                Log.i(
                    "DEBUG!",
                    "Should exit " + (hasGoneToLocation && !repeatGoToFlag).toString()
                )

                if (hasGoneToLocation && !repeatGoToFlag) break
                else if (goToLocationState == LocationState.COMPLETE) hasGoneToLocation = true

                if (repeatGoToFlag) repeatGoToFlag = false
                buffer()

            }
        } else {
            while (true) { // loop until to makes it to the start location
                Log.i("DEBUG!", "Has Gone To Location none: $hasGoneToLocation")

                if (!hasGoneToLocation) {
                    robotController.goTo(location, backwards); Log.i(
                        "DEBUG!",
                        "Hello none: $repeatGoToFlag "
                    )
                }

                buffer()
                Log.i("DEBUG!", "Triggered? none: ")
                conditionGate(
                    { goToLocationState != LocationState.COMPLETE && goToLocationState != LocationState.ABORT }
                )

                Log.i(
                    "DEBUG!",
                    "Should exit none" + (hasGoneToLocation && !repeatGoToFlag).toString()
                )

                if (hasGoneToLocation) break
                else if (goToLocationState == LocationState.COMPLETE) hasGoneToLocation = true

                buffer()

            }
        }

        // Log.i("DEBUG!", "THREE: $talkingInThreadFlag")
        if (speak != null) conditionGate(
            { talkingInThreadFlag }
        )// conditionGate({ ttsStatus.value.status != TtsRequest.Status.COMPLETED || triggeredInterrupt && setInterruptSystem && (setInterruptConditionUserMissing || setInterruptConditionUSerToClose || setInterruptConditionDeviceMoved)})

        // Log.i("DEBUG!", "$location: " + triggeredInterrupt)
        updateInterruptFlag("userMissing", false)
        updateInterruptFlag("userTooClose", false)
        updateInterruptFlag("deviceMoved", false)
        _isGoing.value = false
    }

    private fun stopMovement() {
        robotController.stopMovement()
    }

    private fun tiltAngle(degree: Int) {
        robotController.tileAngle(degree)
    }

    private fun goToSpeed(speedLevel: SpeedLevel) {
        robotController.setGoToSpeed(speedLevel)
    }

    suspend fun skidJoy(x: Float, y: Float, repeat: Int) {
        for (i in 1..repeat) {
            robotController.skidJoy(x, y)
            delay(500)
        }
    }

    // Function to update an interrupt flag value
    fun updateInterruptFlag(flag: String, value: Boolean) {
        if (interruptFlags.containsKey(flag)) {
            interruptFlags[flag] = value
        } else {
            println("Flag $flag does not exist in the interruptFlags map.")
        }
    }

    private suspend fun turnBy(degree: Int) {
        robotController.turnBy(degree, buffer = buffer)
        conditionGate({ movementState != MovementStatus.COMPLETE && movementState != MovementStatus.ABORT })
    }

    private fun setMainButtonMode(isEnabled: Boolean) {
        robotController.setMainButtonMode(isEnabled)
    }

    private suspend fun setCliffSensorOn(sensorOn: Boolean) {
        robotController.setCliffSensorOn(sensorOn)
    }
    //-------------------------------------------------END
    //-------------------------------------------------CHATGPT & Speech V1

    private var speakThread: Job? = null

    // Used along in the speak function
    private fun createSpeakThread(
        start: Boolean, sentences: List<String> = listOf("Apple", "Banana", "Cherry"),
        haveFace: Boolean = true,
        setInterruptSystem: Boolean = false,
        setInterruptConditionUserMissing: Boolean = false,
        setInterruptConditionUSerToClose: Boolean = false,
        setInterruptConditionDeviceMoved: Boolean = false
    ) {
        if (start) {
            // Log.i("BluetoothServer", "Hello")
            speakThread = viewModelScope.launch {
                // Log.i("DEBUG!", "In the thread!s")
                talkingInThreadFlag = true
                for (sentence in sentences) {
                    // Log.i("DEBUG!", "$sentence")
                    if (sentence.isNotBlank()) {
                        do {
                            // Log.i("DEBUG!", sentence)
                            // set the repeat flag to false once used
                            if (setInterruptSystem && repeatSpeechFlag) repeatSpeechFlag =
                                false
                            else if (!setInterruptSystem) {
                                repeatSpeechFlag = false
                            }

                            // Speak each sentence individually
                            // Log.i("DEBUG!", "repeatSpeechFlag: $repeatSpeechFlag")
                            robotController.speak(
                                sentence.trim(),
                                buffer,
                                haveFace
                            )

                            // Wait for each sentence to complete before moving to the next
                            conditionGate({
                                ttsStatus.value.status != TtsRequest.Status.COMPLETED || triggeredInterrupt && setInterruptSystem && (setInterruptConditionUserMissing || setInterruptConditionUSerToClose || setInterruptConditionDeviceMoved)
                            })
                        } while (repeatSpeechFlag && setInterruptSystem)
                    }
                }
                talkingInThreadFlag = false
            }
        } else {
            speakThread?.cancel()
        }
    }

    private suspend fun speak(
        speak: String?,
        setConditionGate: Boolean = true,
        haveFace: Boolean = true,
        setInterruptSystem: Boolean = false,
        setInterruptConditionUserMissing: Boolean = false,
        setInterruptConditionUSerToClose: Boolean = false,
        setInterruptConditionDeviceMoved: Boolean = false
    ) {
        _isSpeaking.value = true
        if (speak != null) {
            // Split the input text into sentences based on common sentence-ending punctuation
            val sentences = speak.split(Regex("(?<=[.!?])\\s+"))

//            // change the flags as needed
            updateInterruptFlag("userMissing", setInterruptConditionUserMissing)
            updateInterruptFlag("userTooClose", setInterruptConditionUSerToClose)
            updateInterruptFlag("deviceMoved", setInterruptConditionDeviceMoved)

            if (setConditionGate) {
                for (sentence in sentences) {
                    if (sentence.isNotBlank()) {
                        do {
                            Log.i("DEBUG!", sentence)
                            // set the repeat flag to false once used
                            if (setInterruptSystem && repeatSpeechFlag) {
                                repeatSpeechFlag = false
                                // Log.i ("DEGUG!", "Repeat speach")
                            } else if (!setInterruptSystem) {
                                repeatSpeechFlag = false
                            }

                            // Speak each sentence individually
                            robotController.speak(
                                sentence.trim(),
                                buffer,
                                haveFace
                            )

                            // Wait for each sentence to complete before moving to the next
                            conditionGate({
                                ttsStatus.value.status != TtsRequest.Status.COMPLETED || triggeredInterrupt && setInterruptSystem && (setInterruptConditionUserMissing || setInterruptConditionUSerToClose || setInterruptConditionDeviceMoved)
                            })
                        } while (repeatSpeechFlag && setInterruptSystem)
                    }
                }

                updateInterruptFlag("userMissing", false)
                updateInterruptFlag("userTooClose", false)
                updateInterruptFlag("deviceMoved", false)
            } else {
                //Log.i("BluetoothServer", "Hello Starts")
                if (!talkingInThreadFlag) {
                    createSpeakThread(
                        true,
                        sentences,
                        haveFace,
                        setInterruptSystem,
                        setInterruptConditionUserMissing,
                        setInterruptConditionUSerToClose,
                        setInterruptConditionDeviceMoved
                    )
                }
            }

        }
        _isSpeaking.value = false
    }

    private suspend fun basicSpeak(
        speak: String?,
        setConditionGate: Boolean = true,
        haveFace: Boolean = true
    ) {
        _isSpeaking.value = true
        if (speak != null) {
            robotController.speak(
                speak,
                buffer,
                haveFace
            )
            if (setConditionGate) conditionGate({ ttsStatus.value.status != TtsRequest.Status.COMPLETED })
        }
        _isSpeaking.value = false
    }

    private suspend fun forcedSpeak(speak: String?) {
        while (ttsStatus.value.status != TtsRequest.Status.STARTED) {
            if (speak != null) {
                robotController.speak(
                    speak,
                    buffer
                )
            }
            buffer()
        }
        conditionGate({ ttsStatus.value.status != TtsRequest.Status.COMPLETED })
    }
    //-------------------------------------------------CHATGPT & Speech V1
    private suspend fun listen() {
        _isListening.value = true
        robotController.wakeUp() // This will start the listen mode
        Log.i("GPT!", "Before Gate: ${conversationAttached.value.isAttached}")
        buffer()
        conditionGate({ conversationAttached.value.isAttached }) // Wait until listen mode completed
        Log.i("GPT!", "After Gate")

        // Make sure the speech value is updated before using it
        userResponse =
            speechUpdatedValue // Store the text from listen mode to be used
        speechUpdatedValue =
            null // clear the text to null so show that it has been used
        _isListening.value = false
    }

    private fun sendMessage(
        openAI: OpenAI,
        userResponse: String,
        info: String = "Replace with information explaining how ChatGPT should act to prompts") {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Define the model you want to use (GPT-3.5 or GPT-4)
                val modelId = ModelId("gpt-4o-mini")

                // Prepare the initial user message
                val chatMessages = mutableListOf(
                    chatMessage {
                        role = ChatRole.System
                        content = info
                    },
                    chatMessage {
                        role = ChatRole.User
                        content = userResponse
                    }
                )

                // Create the chat completion request
                val request = chatCompletionRequest {
                    model = modelId
                    messages = chatMessages
                }

                // Send the request and receive the response
                val response = openAI.chatCompletion(request)

                // Extract and log the model's response
                val modelResponse = response.choices.first().message.content.orEmpty()
                Log.d("DEBUG!", modelResponse)
                responseGPT = modelResponse
            } catch (e: Exception) {
                Log.e("DEBUG!", "Error sending message: ${e.message}")
                errorFlagGPT = true
            }
        }
    }

    private suspend fun askQuestion(
        askGPT: Boolean = true,
        info: String,
        script: String = "none"
    ) {
        shouldExit = false
        var noQuestion = false
        var response: String? = null
        while (true) {
            speak(context.getString(R.string.does_anyone_have_a_question))

            Log.i("GPT!", "Before listen")
            listen()
            Log.i("GPT!", "Before validation: $userResponse")

            if (userResponse != null && userResponse != " " && userResponse != "") {
                response = userResponse
                buffer()
                if (containsPhraseInOrder(response, reject, true)) {
                    speak(context.getString(R.string.all_good_i_will_continue_on))
                    noQuestion = true
                    if (!askGPT) userResponse = null
                    break
                }
                speak(
                    context.getString(
                        R.string.did_you_say_please_just_say_yes_or_no,
                        userResponse
                    )
                )
                while (true) {
                    listen()
                    if (userResponse != null && userResponse != " " && userResponse != "") {
                        when { // Condition gate based on what the user says
                            containsPhraseInOrder(userResponse, reject, true) -> {
                                speak(context.getString(R.string.sorry_lets_try_this_again))
                                break
                            }

                            containsPhraseInOrder(userResponse, confirmation, true) -> {
                                speak(context.getString(R.string.great_let_me_think_for_a_moment))
                                shouldExit = true
                                break
                            }

                            else -> {
                                speak(context.getString(R.string.sorry_i_did_not_understand_you))
                            }
                        }
                    }
                    buffer()
                }
                if (shouldExit) break
            } else {
                Log.i("GPT!", "In else state")
                if (userResponse != " ") {
                    speak(context.getString(R.string.all_good_i_will_continue_on))
                    if (!askGPT) userResponse = null
                    noQuestion = true
                    break
                } else {
                    speak(context.getString(R.string.sorry_i_had_an_issue_with_hearing_you))
                }
            }
            buffer()
        }

        Log.i("GPT!", "Passed Question asking")

        if (!noQuestion && askGPT) {
            Log.i("GPT!", response.toString())
            response?.let { sendMessage(openAI, it, info + script) }

            conditionGate({ responseGPT == null })
            Log.i("GPT!", responseGPT.toString())

            val randomDelayMillis = Random.nextLong(7001, 15001)
            delay(randomDelayMillis)

            speak(responseGPT.toString())
            responseGPT = null
        }

        if (!askGPT && !noQuestion) {
            responseGPT = response
            speak("Sorry, I do not actually know that question.")
        }
    }
    //-------------------------------------------------END
    //-------------------------------------------------RobotController Variables
    // These collect data from services in robotController
    private val ttsStatus = robotController.ttsStatus // Current speech state
    private val detectionStatus = robotController.detectionStateChangedStatus
    private val detectionData = robotController.detectionDataChangedStatus
    private val movementStatus = robotController.movementStatusChangedStatus
    private val lifted = robotController.lifted
    private val dragged = robotController.dragged
    private val askResult = robotController.askResult
    private val language = robotController.language
    private val wakeUp = robotController.wakeUp
    private val waveForm = robotController.waveform
    private val conversationStatus = robotController.conversationStatus
    private val conversationAttached = robotController.conversationAttached
    private val locationState = robotController.locationState
    private val beWithMeStatus = robotController.beWithMeState

    //-------------------------------------------------END
    //-------------------------------------------------Data Collection Variables ViewModel
    // Keep track of the systems current state
    private var isAttached = false
    private var goToLocationState = LocationState.ABORT
    private var movementState = MovementStatus.ABORT

    private var shouldExit = false // Flag to determine if both loops should exit

    private var speechUpdatedValue: String? = null
    private var userResponse: String? = null

    // Allows adding a delay in functions inside RobotController if needed
    private var buffer = 100L

    // Variables for other state
    private var stateMode = State.NULL // Keep track of system state
    private var defaultAngle =
        270.0 // 180 + round(Math.toDegrees(robotController.getPositionYaw().toDouble())) // Default angle Temi will go to.
    private var boundary = 90.0 // Distance Temi can turn +/- the default angle
    private var userRelativeDirection =
        XDirection.GONE // Used for checking direction user was lost

    // Bellow is the data used to keep track of movement
    // Variables for other x direction and motion
    private var previousUserAngle = 0.0
    private var currentUserAngle = 0.0
    private var xPosition = XDirection.GONE
    private var xMotion = XMovement.NOWHERE

    // Variables for other y direction and motion
    private var previousUserDistance = 0.0
    private var currentUserDistance = 0.0
    private var yPosition = YDirection.MISSING
    private var yMotion = YMovement.NOWHERE

    // Use y-position data to determine detection state
    private val _detectionState = MutableStateFlow(yPosition)
    val detectionState: StateFlow<YDirection> = _detectionState

    init {
        // Other state actions
        viewModelScope.launch {
            while (true) {
                when (stateMode) {
                    State.CONSTRAINT_FOLLOW -> {
                        //' check to see if the state is not in misuse
                        if (!dragged.value.state && !lifted.value.state) {

                            val currentAngle =
                                180 + round(
                                    Math.toDegrees(
                                        robotController.getPositionYaw().toDouble()
                                    )
                                )
                            val userRelativeAngle =
                                round(Math.toDegrees(detectionData.value.angle)) / 1.70
                            val turnAngle = (userRelativeAngle).toInt()

                            // Use this to determine which direction the user was lost in
                            when {
                                userRelativeAngle > 0 -> {
                                    userRelativeDirection = XDirection.LEFT
                                }

                                userRelativeAngle < 0 -> {
                                    userRelativeDirection = XDirection.RIGHT
                                }

                                else -> {
                                    // Do nothing
                                }
                            }

                            // This method will allow play multiple per detection
                            var isDetected = false
                            var isLost = false

                            // Launch a coroutine to monitor detectionStatus
                            val job = launch {
                                detectionStatus.collect { status ->
                                    when (status) {
                                        DetectionStateChangedStatus.DETECTED -> {
                                            isDetected = true
                                            isLost = false
                                            buffer()
                                        }

                                        DetectionStateChangedStatus.LOST -> {
                                            isDetected = false
                                            isLost = true
                                            buffer()
                                        }

                                        else -> {
                                            isDetected = false
                                            isLost = false
                                            buffer()
                                        }
                                    }
                                }
                            }

                            fun normalizeAngle(angle: Double): Double {
                                var normalizedAngle =
                                    angle % 360  // Ensure the angle is within 0-360 range

                                if (normalizedAngle < 0) {
                                    normalizedAngle += 360  // Adjust for negative angles
                                }

                                return normalizedAngle
                            }

                            val lowerBound = normalizeAngle(defaultAngle - boundary)
                            val upperBound = normalizeAngle(defaultAngle + boundary)

                            // Helper function to calculate the adjusted turn angle that keeps within the bounds
                            fun clampTurnAngle(
                                currentAngle: Double,
                                targetTurnAngle: Double
                            ): Double {
                                val newAngle = normalizeAngle(currentAngle + targetTurnAngle)

                                return when {
                                    // If the new angle is within the bounds, return the target turn angle
                                    lowerBound < upperBound && newAngle in lowerBound..upperBound -> targetTurnAngle
                                    lowerBound > upperBound && (newAngle >= lowerBound || newAngle <= upperBound) -> targetTurnAngle

                                    // Otherwise, return the angle that brings it closest to the boundary
                                    lowerBound < upperBound -> {
                                        if (newAngle < lowerBound) lowerBound + 1 - currentAngle
                                        else upperBound - 1 - currentAngle
                                    }

                                    else -> {
                                        if (abs(upperBound - currentAngle) < abs(lowerBound - currentAngle)) {
                                            upperBound - 1 - currentAngle
                                        } else {
                                            lowerBound + 1 - currentAngle
                                        }
                                    }
                                }
                            }

                            // Now clamp the turn angle before turning the robot
                            val adjustedTurnAngle =
                                clampTurnAngle(currentAngle, turnAngle.toDouble())


                            if (abs(adjustedTurnAngle) > 0.1 && yPosition != YDirection.CLOSE) {  // Only turn if there's a meaningful adjustment to make
                                robotController.turnBy(adjustedTurnAngle.toInt(), 1f, buffer)
                            } else if (isLost && (currentAngle < defaultAngle + boundary && currentAngle > defaultAngle - boundary)) {
                                // Handles condition when the user is lost
                                when (userRelativeDirection) {
                                    XDirection.LEFT -> {
                                        robotController.turnBy(45, 0.1f, buffer)
                                        userRelativeDirection = XDirection.GONE
                                    }

                                    XDirection.RIGHT -> {
                                        robotController.turnBy(-45, 0.1f, buffer)
                                        userRelativeDirection = XDirection.GONE
                                    }

                                    else -> {
                                        // Do nothing
                                    }
                                }
                            } else if (!isDetected && !isLost) {
                                // Handles conditions were the robot has detected someone
                                val angleThreshold = 2.0 // Example threshold, adjust as needed

                                if (abs(defaultAngle - currentAngle) > angleThreshold) {
                                    robotController.turnBy(
                                        getDirectedAngle(
                                            defaultAngle,
                                            currentAngle
                                        ).toInt(), 1f, buffer
                                    )
                                    conditionGate({
                                        movementStatus.value.status !in listOf(
                                            MovementStatus.COMPLETE,
                                            MovementStatus.ABORT
                                        )
                                    })
                                }
                            }
                            // Ensure to cancel the monitoring job if the loop finishes
                            job.cancel()
                        }
                    }

                    State.NULL -> {}
                }
                buffer()
            }
        }

        // x-detection
        viewModelScope.launch { // Used to get state for x-direction and motion
            while (true) {
                // This method will allow play multiple per detection
                var isDetected = false

                // Launch a coroutine to monitor detectionStatus
                val job = launch {
                    detectionStatus.collect { status ->
                        if (status == DetectionStateChangedStatus.DETECTED) {
                            isDetected = true
                            buffer()
                        } else {
                            isDetected = false
                        }
                    }
                }

                previousUserAngle = currentUserAngle
                delay(500L)
                currentUserAngle = detectionData.value.angle


                if (isDetected && previousUserDistance != 0.0) { //&& previousUserDistance != 0.0 && previousUserDistance == currentUserDistance) {
                    // logic for close or far position
//                    Log.i("STATE", (yPosition).toString())
                    xPosition = when {
                        currentUserAngle > 0.1 -> {
                            XDirection.LEFT
                        }

                        currentUserAngle < -0.1 -> {
                            XDirection.RIGHT
                        }

                        else -> {
                            XDirection.MIDDLE
                        }
                    }
                } else {
                    xPosition = XDirection.GONE
                }

                if (isDetected && previousUserAngle != 0.0 && previousUserAngle != currentUserAngle) {

                    when (yPosition) {
                        YDirection.FAR -> {
                            xMotion = when {
                                currentUserAngle - previousUserAngle > 0.07 -> XMovement.LEFTER
                                currentUserAngle - previousUserAngle < -0.07 -> XMovement.RIGHTER
                                else -> XMovement.NOWHERE
                            }
                        }

                        YDirection.MIDRANGE -> {
                            xMotion = when {
                                currentUserAngle - previousUserAngle > 0.12 -> XMovement.LEFTER
                                currentUserAngle - previousUserAngle < -0.12 -> XMovement.RIGHTER
                                else -> XMovement.NOWHERE
                            }
                        }

                        YDirection.CLOSE -> {
                            xMotion = when {
                                currentUserAngle - previousUserAngle > 0.17 -> XMovement.LEFTER
                                currentUserAngle - previousUserAngle < -0.17 -> XMovement.RIGHTER
                                else -> XMovement.NOWHERE
                            }
                        }

                        YDirection.MISSING -> {
                            XMovement.NOWHERE
                        }
                    }
                }

                job.cancel()
            }
        }

        // y-detection
        viewModelScope.launch { // Used to get state for y-direction and motion

            while (true) {
                // This method will allow play multiple per detection
                var isDetected = false

                // Launch a coroutine to monitor detectionStatus
                val job = launch {
                    detectionStatus.collect { status ->
                        if (status == DetectionStateChangedStatus.DETECTED) {
                            isDetected = true
                            buffer()
                        } else {
                            isDetected = false
                        }
                    }
                }

                previousUserDistance = currentUserDistance
                delay(500L)
                currentUserDistance = detectionData.value.distance

                if (isDetected && previousUserDistance != 0.0) { //&& previousUserDistance != 0.0 && previousUserDistance == currentUserDistance) {
                    // logic for close or far position
                    yPosition = when {
                        currentUserDistance < 1.0 -> {
                            YDirection.CLOSE
                        }

                        currentUserDistance < 1.5 -> {
                            YDirection.MIDRANGE
                        }

                        else -> {
                            YDirection.FAR
                        }
                    }
                } else {
                    yPosition = YDirection.MISSING
                }

                if (isDetected && previousUserDistance != 0.0 && previousUserDistance != currentUserDistance) { //&& previousUserDistance != 0.0 && previousUserDistance == currentUserDistance) {
                    yMotion = when {
                        currentUserDistance - previousUserDistance > 0.01 -> {
                            YMovement.FURTHER
                        }

                        currentUserDistance - previousUserDistance < -0.01 -> {
                            YMovement.CLOSER
                        }

                        else -> {
                            YMovement.NOWHERE
                        }
                    }
                }
                job.cancel()
            }
        }

        // End Conversation after it gets and updates its value
        viewModelScope.launch {
            var speech: String? = null

            // Collect results in a separate coroutine
            val job = launch {
                robotController.askResult.collect { status ->
                    Log.i("GPT!", "$status")
                    robotController.finishConversation()
                    if (status.result == "hzdghasdfhjasdfb") {
                        speechUpdatedValue = null
                    } else speechUpdatedValue = status.result

                    speech = status.result
                }
            }
        }
    }

    // Define a mutable map to hold interrupt flags
    private val interruptFlags = mutableMapOf(
        "userMissing" to false,
        "userTooClose" to false,
        "deviceMoved" to false
    )
    private var repeatSpeechFlag: Boolean = false
    private var talkingInThreadFlag = false
    private var repeatGoToFlag: Boolean = false
    private var interruptTriggerDelay = 10

    private var resetTourEarly = false
    private var preventResetFromIdle = false

    private var triggeredInterrupt: Boolean = false

    // Used to see if Temi is listening to user
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isGoing = MutableStateFlow(false)
    val isGoing: StateFlow<Boolean> = _isGoing

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    // Store response from GPT here, when used make null
    private var responseGPT: String? = null

    // Use this to tell system if waiting, null is default or Error
    // Check logs to confirm error
    private var errorFlagGPT: Boolean = false

    private val apiKey =
        "REPLACE WITH API KEY TO USE CHATGPT"

    private val openAI = OpenAI(apiKey)
    //-------------------------------------------------END
    //-------------------------------------------------Functions for View
    fun isMisuseState(): Boolean {
        return (dragged.value.state || lifted.value.state)
    }

    // Control the volume of the Temi
    fun volumeControl(volume: Int) {
        robotController.volumeControl(volume)
    }

    //-------------------------------------------------END
    //-------------------------------------------------System Function
    private suspend fun buffer() {
        delay(100L)
    }

    private suspend fun conditionTimer(trigger: () -> Boolean, time: Int) {
        if (!trigger()) {
            for (i in 1..time) {
                delay(1000)
                if (trigger()) {
                    break
                }
            }
        }
    }

    private suspend fun conditionGate(trigger: () -> Boolean) {
        while (trigger()) {
            buffer() // Pause between checks to prevent busy-waiting
        }
    }

    private fun getDirectedAngle(a1: Double, a2: Double): Double {
        var difference = a1 - a2
        // Normalize the angle to keep it between -180 and 180 degrees
        if (difference > 180) difference -= 360
        if (difference < -180) difference += 360
        return difference
    }
    //-------------------------------------------------END
}