package com.temi.demotemi

import android.util.Log
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.constants.CliffSensorMode
import com.robotemi.sdk.constants.HardButton
import com.robotemi.sdk.constants.SensitivityLevel
import com.robotemi.sdk.listeners.OnBeWithMeStatusChangedListener
import com.robotemi.sdk.listeners.OnConversationStatusChangedListener
import com.robotemi.sdk.listeners.OnDetectionDataChangedListener
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnMovementStatusChangedListener
import com.robotemi.sdk.listeners.OnRobotDragStateChangedListener
import com.robotemi.sdk.listeners.OnRobotLiftedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.listeners.OnTtsVisualizerWaveFormDataChangedListener
import com.robotemi.sdk.map.MapDataModel
import com.robotemi.sdk.model.DetectionData
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SpeedLevel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Singleton

data class TtsStatus(val status: TtsRequest.Status)
enum class DetectionStateChangedStatus(val state: Int) { // Why is it like this?
    DETECTED(state = 2),
    LOST(state = 1),
    IDLE(state = 0);

    companion object {
        fun fromState(state: Int): DetectionStateChangedStatus? = entries.find { it.state == state }
    }
}
data class DetectionDataChangedStatus( val angle: Double, val distance: Double)
enum class MovementType {
    SKID_JOY,
    TURN_BY,
    NONE
}
enum class MovementStatus {
    START,
    GOING,
    OBSTACLE_DETECTED,
    NODE_INACTIVE,
    CALCULATING,
    COMPLETE,
    ABORT
}
data class MovementStatusChangedStatus(
    val type: MovementType,   // Use the MovementType enum
    val status: MovementStatus  // Use the MovementStatus enum
)
data class Dragged(
    val state: Boolean
)
data class Lifted(
    val state: Boolean
)
data class AskResult(val result: String, val id: Long = System.currentTimeMillis())
enum class Language(val value: Int) {
    SYSTEM(0),
    EN_US(1),
    ZH_CN(2),
    ZH_HK(3),
    ZH_TW(4),
    TH_TH(5),
    HE_IL(6),
    KO_KR(7),
    JA_JP(8),
    IN_ID(9),
    ID_ID(10),
    DE_DE(11),
    FR_FR(12),
    FR_CA(13),
    PT_BR(14),
    AR_EG(15),
    AR_AE(16),
    AR_XA(17),
    RU_RU(18),
    IT_IT(19),
    PL_PL(20),
    ES_ES(21),
    CA_ES(22),
    HI_IN(23),
    ET_EE(24),
    TR_TR(25),
    EN_IN(26),
    MS_MY(27),
    VI_VN(28),
    EL_GR(29);

    companion object {
        fun fromLanguage(value: Int): Language? = Language.entries.find { it.value == value }
    }
}
data class WakeUp(
    val result: String
)
data class WaveForm(
    val result: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaveForm

        return result.contentEquals(other.result)
    }

    override fun hashCode(): Int {
        return result.contentHashCode()
    }
}
data class ConversationStatus (
    val status: Int,
    val text: String
)
data class ConversationAttached (
    val isAttached: Boolean
)
enum class LocationState(val value:String) {
    START(value = "start"),
    CALCULATING(value = "calculating"),
    GOING(value = "going"),
    COMPLETE(value = "complete"),
    ABORT(value = "abort"),
    REPOSING(value = "reposing");

    companion object {
        fun fromLocationState(value: String): LocationState? = LocationState.entries.find { it.value == value }
    }
}
enum class BeWithMeState(val value:String) {
    ABORT(value = "abort"),
    CALCULATING(value = "calculating"),
    SEARCH(value = "search"),
    START(value = "start"),
    TRACK(value = "track"),
    OBSTACLE_DETECTED(value = "obstacle detected");

    companion object {
        fun fromBeWithMeState(value: String): BeWithMeState? = BeWithMeState.entries.find { it.value == value }
    }
}


@Module
@InstallIn(SingletonComponent::class)
object RobotModule {
    @Provides
    @Singleton
    fun provideRobotController() = RobotController()
}

class RobotController():
    OnRobotReadyListener,
    OnDetectionStateChangedListener,
    Robot.TtsListener,
    OnDetectionDataChangedListener,
    OnMovementStatusChangedListener,
    OnRobotLiftedListener,
    OnRobotDragStateChangedListener,
    Robot.AsrListener,
    Robot.WakeupWordListener,
    OnTtsVisualizerWaveFormDataChangedListener,
    OnConversationStatusChangedListener,
    Robot.ConversationViewAttachesListener,
    OnGoToLocationStatusChangedListener,
    OnBeWithMeStatusChangedListener
{
    private val robot = Robot.getInstance() //This is needed to reference the data coming from Temi

    //-------------------------------------------------Stateflows for listeners
    private val _isReady = MutableStateFlow( false )
    val isReady = _isReady.asStateFlow()

    private val _ttsStatus = MutableStateFlow( TtsStatus(status = TtsRequest.Status.PENDING) )
    val ttsStatus = _ttsStatus.asStateFlow()

    private val _detectionStateChangedStatus = MutableStateFlow(DetectionStateChangedStatus.IDLE)
    val detectionStateChangedStatus = _detectionStateChangedStatus.asStateFlow()

    private val _detectionDataChangedStatus = MutableStateFlow(DetectionDataChangedStatus(angle = 0.0, distance = 0.0))
    val detectionDataChangedStatus = _detectionDataChangedStatus.asStateFlow() // This can include talking state as well

    private val _movementStatusChangedStatus = MutableStateFlow(
        MovementStatusChangedStatus(
            MovementType.NONE, MovementStatus.NODE_INACTIVE
        )
    )
    val movementStatusChangedStatus = _movementStatusChangedStatus.asStateFlow() // This can include talking state as well

    private val _dragged = MutableStateFlow(Dragged(false))
    val dragged = _dragged.asStateFlow() // This can include talking state as well

    private val _lifted = MutableStateFlow(Lifted(false))
    val lifted = _lifted.asStateFlow() // This can include talking state as well

    private val _askResult = MutableStateFlow(AskResult("hzdghasdfhjasdfb"))
    val askResult = _askResult.asStateFlow()

    private val _language = MutableStateFlow(Language.SYSTEM)
    val language = _language.asStateFlow()

    private val _wakeUp = MutableStateFlow(WakeUp("56"))
    val wakeUp = _wakeUp.asStateFlow()

    private val _waveform = MutableStateFlow(WaveForm(byteArrayOf(0)))
    val waveform = _waveform.asStateFlow()

    private val _conversationStatus = MutableStateFlow(ConversationStatus(status = 0, text = "56"))
    val conversationStatus = _conversationStatus.asStateFlow()

    private val _conversationAttached = MutableStateFlow(ConversationAttached(false))
    val conversationAttached = _conversationAttached.asStateFlow()

    private val _locationState = MutableStateFlow(LocationState.ABORT)
    val locationState = _locationState.asStateFlow()

    private val _beWithMeStatus = MutableStateFlow(BeWithMeState.ABORT)
    val beWithMeState = _beWithMeStatus.asStateFlow()
    //-------------------------------------------------END
        init {
        robot.addOnRobotReadyListener(this)
        robot.addTtsListener(this)
        robot.addOnDetectionStateChangedListener((this))
        robot.addOnDetectionDataChangedListener(this)
        robot.addOnMovementStatusChangedListener(this)
        robot.addOnRobotLiftedListener(this)
        robot.addOnRobotDragStateChangedListener(this)
        robot.addAsrListener(this)
        robot.addWakeupWordListener(this)
        robot.addOnTtsVisualizerWaveFormDataChangedListener(this)
        robot.addOnConversationStatusChangedListener(this)
        robot.addConversationViewAttachesListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnBeWithMeStatusChangedListener(this)
        robot.addOnBeWithMeStatusChangedListener(this)
    }
    //-------------------------------------------------General Functions
    fun speak(speech: String,buffer: Long, haveFace: Boolean = true) {
        val request = TtsRequest.create(
            speech = speech,
            isShowOnConversationLayer = false,
            showAnimationOnly = haveFace,
            language = TtsRequest.Language.EN_US
        )

        // Need to create TtsRequest
        robot.speak(request)
    }

    fun turnBy(degree: Int, speed: Float = 1f, buffer: Long) {
        robot.turnBy(degree, speed)
    }

    fun tiltAngle(degree: Int, speed: Float = 1f, buffer: Long) {
        robot.tiltAngle(degree, speed)
    }

    fun listOfLocations() {
        Log.i("Location Info", robot.locations.toString())
    }

    fun goTo(location: String, backwards: Boolean = false) {
        robot.goTo(location, noBypass = false, backwards = backwards)
    }

    fun setGoToSpeed(speedLevel: SpeedLevel) {
        robot.goToSpeed = speedLevel
    }

    fun goToPosition(position: Position) {
        robot.goToPosition(position)
    }

    suspend fun skidJoy(x: Float, y: Float) {
        robot.skidJoy(x, y)
        delay(500)
    }

    fun askQuestion(question: String) {
        robot.askQuestion(question)
    }

    fun wakeUp() {
        robot.wakeup(listOf(SttLanguage.SYSTEM))
    }

    fun finishConversation() {
        robot.finishConversation()
    }

    fun getPosition(): Position {
        Log.i("Robot Position Data", "${robot.getPosition()}")
        return robot.getPosition()
    }

    fun getMapData(): MapDataModel? {
        return robot.getMapData()
    }

    fun detectionMode(detectionOn: Boolean) {
        robot.setDetectionModeOn(detectionOn, 2f)
        Log.i("Detection Mode On", "${robot.detectionModeOn}")
    }

    fun togglePowerButton(toggleOn: Boolean) {
        if (toggleOn) {
            robot.setHardButtonMode(HardButton.POWER, HardButton.Mode.DISABLED)
        } else {
            robot.setHardButtonMode(HardButton.POWER, HardButton.Mode.ENABLED)
        }
    }
    //-------------------------------------------------END
    //-------------------------------------------------General Data
    fun getPositionYaw(): Float
    {
        return robot.getPosition().yaw
    }

    fun volumeControl (volume: Int) {
        robot.volume = volume
    }

    fun setMainButtonMode(isEnabled: Boolean) {
        if (isEnabled){
            robot.setHardButtonMode(HardButton.MAIN, HardButton.Mode.ENABLED)
        } else {
            robot.setHardButtonMode(HardButton.MAIN, HardButton.Mode.DISABLED)
        }
    }

    fun setCliffSensorOn(sensorOn: Boolean) {
        robot.groundDepthCliffDetectionEnabled = sensorOn
        robot.frontTOFEnabled = sensorOn
        robot.backTOFEnabled = sensorOn
        if (sensorOn) {
            robot.cliffSensorMode = CliffSensorMode.HIGH_SENSITIVITY
            robot.headDepthSensitivity = SensitivityLevel.HIGH
        } else {
            robot.cliffSensorMode = CliffSensorMode.OFF
            robot.headDepthSensitivity = SensitivityLevel.LOW
        }
    }

    fun stopMovement() {
        robot.stopMovement()
    }

    fun tileAngle(degree: Int) {
        robot.tiltAngle(degree)
    }

    fun constrainBeWith() {
        robot.constraintBeWith()
    }

    fun getBatteryLevel(): Int {
        // if you get -1 that means there has been an issue
        return robot.batteryData?.level ?: -1
    }
    //-------------------------------------------------End
    //-------------------------------------------------Overrides
    /**
     * Called when connection with robot was established.
     *
     * @param isReady `true` when connection is open. `false` otherwise.
     */
    override fun onRobotReady(isReady: Boolean) {

        if (!isReady) return

    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
//        Log.i("onTtsStatusChanged", "status: ${ttsRequest.status}")
        _ttsStatus.update {
            TtsStatus(status = ttsRequest.status)
        }
    }

    override fun onDetectionStateChanged(state: Int) {
        _detectionStateChangedStatus.update {
//            Log.d("DetectionState", "Detection state changed: ${DetectionStateChangedStatus.fromState(state)}")
            DetectionStateChangedStatus.fromState(state = state) ?: return@update it
        }
    }

    override fun onDetectionDataChanged(detectionData: DetectionData) {
        _detectionDataChangedStatus.update {
            DetectionDataChangedStatus(angle = detectionData.angle, distance = detectionData.distance)
        }
    }

    override fun onMovementStatusChanged(type: String, status: String) {
        _movementStatusChangedStatus.update { currentStatus ->
            // Convert the type and status to their respective enums
            val movementType = when (type) {
                "skidJoy" -> MovementType.SKID_JOY
                "turnBy" -> MovementType.TURN_BY
                else -> return@update currentStatus // If the type is unknown, return the current state
            }
            val movementStatus = when (status) {
                "start" -> MovementStatus.START
                "going" -> MovementStatus.GOING
                "obstacle detected" -> MovementStatus.OBSTACLE_DETECTED
                "node inactive" -> MovementStatus.NODE_INACTIVE
                "calculating" -> MovementStatus.CALCULATING
                "complete" -> MovementStatus.COMPLETE
                "abort" -> MovementStatus.ABORT
                else -> return@update currentStatus // If the status is unknown, return the current state
            }
            // Create a new MovementStatusChangedStatus from the enums
            MovementStatusChangedStatus(movementType, movementStatus)
        }
    }

    override fun onRobotLifted(isLifted: Boolean, reason: String) {
        _lifted.update {
            Lifted(isLifted)
        }
    }

    override fun onRobotDragStateChanged(isDragged: Boolean) {
        _dragged.update {
            Dragged(isDragged)
        }
    }

    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        _askResult.update {
            AskResult(asrResult)
        }

        _language.update {
            Language.fromLanguage(value = sttLanguage.value) ?: return@update it
        }

    }

    override fun onWakeupWord(wakeupWord: String, direction: Int) {
        _wakeUp.update {
            WakeUp(wakeupWord)
        }
    }

    override fun onTtsVisualizerWaveFormDataChanged(waveForm: ByteArray) {
        _waveform.update {
            WaveForm(waveForm)
        }
    }

    override fun onConversationStatusChanged(status: Int, text: String) {
        _conversationStatus.update {
            ConversationStatus(status, text)
        }
    }

    override fun onConversationAttaches(isAttached: Boolean) {
        _conversationAttached.update {
            ConversationAttached(isAttached)
        }
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        _locationState.update {
            LocationState.fromLocationState(value = status) ?: return@update it
        }
    }

    override fun onBeWithMeStatusChanged(status: String) {
        _beWithMeStatus.update {
            BeWithMeState.fromBeWithMeState(value = status) ?: return@update it
        }
    }
    //-------------------------------------------------END
}