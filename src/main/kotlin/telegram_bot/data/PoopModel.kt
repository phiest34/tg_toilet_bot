package telegram_bot.data

import telegram_bot.util.asMillyGetFormatTime
import java.util.*

data class PoopModel(
    val userName: String,
    val startedPoopingAt: Long,
    var endedPoopingAt: Long = 0
) {
    override fun toString(): String {
        return "$userName $startedPoopingAt $endedPoopingAt "
    }

    val poopingDate: String
        get() = Date(endedPoopingAt).toString()

    val isPooping
        get() = endedPoopingAt == 0L

    val formattedPoopingTime: String
        get() = poopingTimeInMillis.asMillyGetFormatTime()

    val poopingTimeInMillis
        get() = endedPoopingAt - startedPoopingAt

}
