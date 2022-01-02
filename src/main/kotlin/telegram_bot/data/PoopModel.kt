package telegram_bot.data

import java.util.concurrent.TimeUnit

data class PoopModel(
    val userName: String,
    val startedPoopingAt: Long,
    var endedPoopingAt: Long = 0
) {
    val isPooping
        get() = endedPoopingAt != 0L

    val formattedPoopingTime: String
        get() {
            return if (poopingTimeInSeconds > 59) {
                val minutes = poopingTimeInMillis / 1000 / 60
                val seconds = poopingTimeInMillis / 1000 % 60
                "$minutes мин. и $seconds сек."
            } else {
                "$poopingTimeInSeconds сек."
            }
        }

    private val poopingTimeInMillis
        get() = endedPoopingAt - startedPoopingAt

    private val poopingTimeInSeconds
        get() = TimeUnit.MILLISECONDS.toSeconds(poopingTimeInMillis)


}