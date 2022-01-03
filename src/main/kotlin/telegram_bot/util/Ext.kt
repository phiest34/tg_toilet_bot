package telegram_bot.util

import java.util.concurrent.TimeUnit

fun Long.asMillyGetFormatTime(): String {
    return if (TimeUnit.MILLISECONDS.toSeconds(this) > 59) {
        val minutes = this / 1000 / 60
        val seconds = this / 1000 % 60
        "$minutes мин. и $seconds сек."
    } else {
        "${TimeUnit.MILLISECONDS.toSeconds(this)} сек."
    }
}