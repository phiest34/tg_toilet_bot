package telegram_bot.data

import dev.inmo.tgbotapi.types.UserId
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import telegram_bot.util.asMillyGetFormatTime
import java.io.File
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates


class PoopRepository : CoroutineScope {

    private val poopHistory = mutableListOf<PoopModel>()

    @Volatile
    var currentPoopingPersonId: UserId? = null
        private set

    var jobPoopMap = mutableMapOf<PoopModel, Job?>()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    init {
        launch {
            withContext(Dispatchers.IO) {
                deserializeFromFile()
            }
        }
    }

    fun getPoopingSum(): String {
        var sum = 0L
        poopHistory.forEach {
            if (it.poopingTimeInMillis >= 0L) {
                sum += it.poopingTimeInMillis
            }
        }
        return sum.asMillyGetFormatTime()
    }

    fun getTopFivePoopTime(): String {
        val stringBuilder = StringBuilder()
        poopHistory.sortedByDescending { it.poopingTimeInMillis }.subList(0, 5).forEachIndexed { index, poopModel ->
            stringBuilder.append(
                "${index + 1}. ${poopModel.userName} срал ${poopModel.formattedPoopingTime}\n"
            )
        }
        return stringBuilder.toString()
    }

    fun getTopFiveUser(): String {
        val stringBuilder = StringBuilder()
        val uniqueList = mutableListOf<PoopModel>()
        poopHistory
            .groupBy { it.userName }
            .values
            .forEach {
                uniqueList.add(it.maxByOrNull { it.poopingTimeInMillis }!!)
            }
        uniqueList
            .sortedByDescending { it.poopingTimeInMillis }
            .subList(0, 5)
            .forEachIndexed { index, poopModel ->
                stringBuilder.append(
                    "${index + 1}. ${poopModel.userName} срал ${poopModel.formattedPoopingTime}\n"
                )
            }
        return stringBuilder.toString()
    }

    fun setPoopInfo(userId: UserId?, userName: String, job: Job?) {
        currentPoopingPersonId = userId
        PoopModel(userName, System.currentTimeMillis()).also {
            jobPoopMap[it] = job
            poopHistory.add(it)
        }
    }

    fun setPoopEnded(userName: String) {
        poopHistory.last {
            it.userName == userName
        }.also {
            jobPoopMap[it]?.cancel()
            it.endedPoopingAt = System.currentTimeMillis()
        }
        currentPoopingPersonId = null
    }

    private fun deserializeFromFile() {
        lateinit var username: String
        var startedPoopingAt by Delegates.notNull<Long>()
        var endedPoopingAt by Delegates.notNull<Long>()
        File(FILENAME).apply {
            readText()
                .split(' ')
                .forEachIndexed { index, s ->
                    when (index % 3) {
                        0 -> username = s
                        1 -> startedPoopingAt = s.toLongOrNull() ?: 0L
                        2 -> {
                            endedPoopingAt = s.toLongOrNull() ?: 0L
                            poopHistory.add(PoopModel(username, startedPoopingAt, endedPoopingAt))
                        }
                    }
                }
        }
    }

    suspend fun logData() = withContext(Dispatchers.IO) {
        File(FILENAME).apply {
            writeText(poopHistory.joinToString(separator = "") { "$it" })
        }
    }

    suspend fun getRandomJoke(): String = withContext(Dispatchers.IO) {
        HttpClient(CIO).let {
            val response: HttpResponse = it.get("https://www.anekdot.ru/rss/randomu.html") {
                headers {
                    append(HttpHeaders.Accept, "text/html")
                }

            }
            return@withContext response.readText()
                .substringAfter("[\\\"")
                .substringBeforeLast("\\\"")
                .replace("\\", "")
                .replace("<br>", "")
        }

    }

    fun getPoopInfoByUserName(userName: String): PoopModel = poopHistory.last {
        userName == it.userName
    }

    fun getPoopHistory() = if (poopHistory.isNotEmpty()) {
        "Пользователь\t Срал\n${
            poopHistory.joinToString(separator = "\n") {
                "${it.userName}\t${if (it.isPooping) "Срет сейчас" else it.formattedPoopingTime + " " + it.poopingDate}"
            }
        }"
    } else {
        "Пока что никто не срал."
    }

    companion object {
        private const val FILENAME = "src/main/kotlin/telegram_bot/log/Log.txt"
    }
}