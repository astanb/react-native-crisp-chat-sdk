package com.reactnativecrispchatsdk
import android.content.Intent
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import im.crisp.client.external.ChatActivity
import im.crisp.client.external.Crisp
import im.crisp.client.external.EventsCallback
import im.crisp.client.external.data.SessionEvent
import im.crisp.client.external.data.SessionEvent.Color
import im.crisp.client.external.data.message.Message
import im.crisp.client.external.data.message.content.TextContent


class CrispChatSdkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "CrispChatSdk"
    }

    @ReactMethod
    fun configure(websiteId: String) {
        val context = reactApplicationContext
        Crisp.configure(context, websiteId)
    }

    @ReactMethod
    fun setTokenId(id: String){
        try {
            Crisp.setTokenID(reactApplicationContext, id)
        } catch(error : Exception) { }
    }

    @ReactMethod
    fun setUserEmail(email: String) {
        Crisp.setUserEmail(email)
    }
    @ReactMethod
    fun setUserNickname(name: String) {
        Crisp.setUserNickname(name)
    }

    @ReactMethod
    fun setUserPhone(phone: String){
        Crisp.setUserPhone(phone)
    }

    @ReactMethod
    fun setUserAvatar(url: String){
        Crisp.setUserAvatar(url)
    }

    @ReactMethod
    fun setSessionSegment(segment: String){
        Crisp.setSessionSegment(segment)
    }

    @ReactMethod
    fun setSessionString(key: String, value: String){
        Crisp.setSessionString(key, value)
    }

    @ReactMethod
    fun setSessionBool(key: String, value: Boolean){
        Crisp.setSessionBool(key, value)
    }

    @ReactMethod
    fun setSessionInt(key: String, value: Int){
        Crisp.setSessionInt(key, value)
    }

    @ReactMethod
    fun pushSessionEvent(name: String, color: Int){
        var sessionEventColor: Color = Color.BLACK

        when(color){
            0->sessionEventColor= Color.RED
            1->sessionEventColor= Color.ORANGE
            2->sessionEventColor= Color.YELLOW
            3->sessionEventColor= Color.GREEN
            4->sessionEventColor= Color.BLUE
            5->sessionEventColor= Color.PURPLE
            6->sessionEventColor= Color.PINK
            7->sessionEventColor= Color.BROWN
            8->sessionEventColor= Color.GREY
            9->sessionEventColor= Color.BLACK
        }

        Crisp.pushSessionEvent(SessionEvent(
            name,
            sessionEventColor
        ))
    }

    var currentSessionId : String? = null

    @ReactMethod
    fun resetSession() {
        val context = reactApplicationContext
        Crisp.resetChatSession(context)
        currentSessionId = null
    }

    fun getJSMessage (message: Message) : WritableMap {
        val params =  Arguments.createMap().apply {
            putMap("content", Arguments.createMap().apply {putString("text",(message.content as TextContent).text)})
            putString("fingerprint", message.fingerprint.toString())
            putString("from", message.from.toString().lowercase())
            putMap("user", Arguments.createMap().apply {
                putString("nickname", message.user.nickname)
                putString("user_id", message.user.userId)
            })
        }
        return Arguments.createMap().apply {putMap("message",params)}
    }


    private val crispEventsCallback: EventsCallback = object : EventsCallback {
        override fun onSessionLoaded(sessionId: String) {
            currentSessionId = sessionId

            val params = Arguments.createMap().apply {
                putString("sessionId", sessionId)
            }

            sendEvent(reactContext, "onSessionLoaded", params)
        }

        override fun onChatOpened() {
            val params = Arguments.createMap().apply {
                putString("sessionId", currentSessionId ?: "")
            }
            sendEvent(reactContext, "onChatOpened", params)
        }

        override fun onChatClosed() {
            // avoids this event for some reason being sent the first time the chat is opened.
           if (currentSessionId != null) {
               val params = Arguments.createMap().apply {
                   putString("sessionId", currentSessionId?: "")
               }
               sendEvent(reactContext, "onChatClosed", params)
           }
        }

        override fun onMessageSent(message: Message) {
            sendEvent(reactContext, "onMessageSent", getJSMessage(message))
        }

        override fun onMessageReceived(message: Message) {
            sendEvent(reactContext, "onMessageReceived", getJSMessage(message))
        }
    }

    private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private var listenerCount = 0

    @ReactMethod
    fun addListener(eventName: String) {
        if (listenerCount == 0) {
            Crisp.addCallback(crispEventsCallback);
        }
        listenerCount += 1
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        listenerCount -= count
        if (listenerCount == 0) {
            Crisp.removeCallback(crispEventsCallback);
        }
    }

    @ReactMethod
    fun show() {
        val context = reactApplicationContext
        val crispIntent = Intent(context, ChatActivity::class.java)
        crispIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(crispIntent)
    }

    @ReactMethod
    fun searchHelpdesk() {
        val context = reactApplicationContext
        Crisp.searchHelpdesk(context)
    }

    @ReactMethod
    fun openHelpdeskArticle(id: String, locale: String, title: String?, category:String?) {
        val context = reactApplicationContext
        Crisp.openHelpdeskArticle(context, id, locale, title, category)
    }
}
