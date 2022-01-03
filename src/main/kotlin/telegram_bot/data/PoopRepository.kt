package telegram_bot.data

import dev.inmo.tgbotapi.types.UserId
import kotlinx.coroutines.*
import java.io.File
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


    fun getTopFivePoopTime(): String {
        val stringBuilder = StringBuilder()
        poopHistory.sortedByDescending { it.poopingTimeInMillis }.subList(0, 5).forEachIndexed { index, poopModel ->
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

    private fun <T, R> pairOf(first: T, second: R): Pair<T, R> = Pair(first, second)

    private fun <T, R> isPairNullByFields(pair: Pair<T?, R?>) =
        pair.first == null && pair.second == null

    companion object {
        private const val FILENAME = "src/main/kotlin/telegram_bot/log/Log.txt"
    }
}