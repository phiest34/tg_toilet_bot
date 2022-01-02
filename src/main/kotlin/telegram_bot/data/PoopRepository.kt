package telegram_bot.data

import dev.inmo.tgbotapi.types.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.properties.Delegates


class PoopRepository {

    private val poopHistory = mutableListOf<PoopModel>()

    @Volatile
    var currentPoopingPersonId: UserId? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            deserializeFromFile()
            println(poopHistory.toString())
        }
    }

    fun getTopFivePoopTime(): String {
        val stringBuilder = StringBuilder()
        poopHistory.sortedByDescending { it.poopingTimeInMillis }.subList(0, 4).forEachIndexed { index, poopModel ->
            stringBuilder.append(
                "${index + 1}. ${poopModel.userName} срал ${poopModel.formattedPoopingTime}\n"
            )
        }
        return stringBuilder.toString()
    }

    fun setPoopInfo(userName: String) {
        poopHistory.add(PoopModel(userName, System.currentTimeMillis()))
    }

    fun setPoopEnded(userName: String) {
        poopHistory.last {
            it.userName == userName
        }.endedPoopingAt = System.currentTimeMillis()
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

    companion object {
        private const val FILENAME = "src/main/kotlin/telegram_bot/log/Log.txt"
    }
}