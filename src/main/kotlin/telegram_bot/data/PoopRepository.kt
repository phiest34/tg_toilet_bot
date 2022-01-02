package telegram_bot.data

import dev.inmo.tgbotapi.types.UserId

class PoopRepository {

    private val poopHistory = mutableListOf<PoopModel>()

    @Volatile
    var currentPoopingPersonId: UserId? = null

    fun setPoopInfo(userName: String) {
        poopHistory.add(PoopModel(userName, System.currentTimeMillis()))
    }

    fun setPoopEnded(userName: String) {
        poopHistory.last {
            it.userName == userName
        }.endedPoopingAt = System.currentTimeMillis()
    }

    fun getPoopInfoByUserName(userName: String): PoopModel = poopHistory.last {
        userName == userName
    }

    fun getPoopHistory() = if (poopHistory.isNotEmpty()) {
            "Пользователь\t Срал\n${poopHistory.map {
                "${it.userName}\t${it.formattedPoopingTime}"
            }.joinToString(separator = "\n")}"
    } else {
        "Пока что никто не срал."
    }
}