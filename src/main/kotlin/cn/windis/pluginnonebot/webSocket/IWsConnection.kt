package cn.windis.pluginnonebot.webSocket

import cn.windis.pluginnonebot.PluginNonebot
import cn.windis.pluginnonebot.api.CallApi.Companion.callAPI
import cn.windis.pluginnonebot.utils.MessageBuilder
import cn.windis.pluginnonebot.utils.Serializer
import cn.windis.pluginnonebot.webSocket.client.Client
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit

interface IWsConnection {
    var session: WebSocketSession?
    var isSessionValid: Boolean
    var selfId: String
    var messageChannel: Channel<String>

    fun disconnect(closeReason: CloseReason) {
        runBlocking { Client.session?.close(closeReason) }
        Client.session = null
        Client.isSessionValid = false
    }

    fun broadcast(message: String) {
        runBlocking {
            launch { messageChannel.send(message) }
        }
    }

    fun broadcast(message: Map<String, Any>) {
        runBlocking { launch { messageChannel.send(Serializer.serialize(message)) } }
    }

    fun call(api: String, params: MutableMap<String, Any>, seq: String) {
        val result: MutableMap<String, Any> = mutableMapOf()
        try {
            Bukkit.getScheduler().runTask(PluginNonebot.instance, Runnable { result["data"] = callAPI(api, params) })
        } catch (e: Exception) {
            result["code"] = -1
            result["message"] = "[Internal Error] ${e.message ?: "unknown error"}"
        }
        result["seq"] = seq
        broadcast(result)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun broadcast(message: MessageBuilder) {
        GlobalScope.launch { messageChannel.send(Serializer.serialize(message.build())) }
    }
}