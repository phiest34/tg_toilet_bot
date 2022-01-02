package telegram_bot

import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.utils.PreviewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import telegram_bot.data.PoopRepository

class ToiletBot(botKey: String) {
    private val bot = telegramBot(botKey)

    private val repository = PoopRepository()

    @OptIn(PreviewFeature::class)
    suspend fun onStartPolling() {
        val scope = CoroutineScope(Dispatchers.Default)

        bot.buildBehaviourWithLongPolling(scope) {
            val me = getMe()
            onCommand("start", requireOnlyCommandInMessage = true) {
                reply(
                    it,
                    "Привет, работяги, меня зовут ${me.firstName}, \nпожалуйста пишите каждый раз 'Я СРУ', перед тем как идти в туалет, и 'Я ПОСРАЛ' после окончания процесса. Так же пользуйтесь командами 'КТО СРАЛ', 'КТО СРЕТ', 'ТОП СРУНОВ'"
                )
            }
            onContentMessage { contentMessage ->
                contentMessage.content.asTextContent()?.let { textContent ->
                    when (textContent.text) {
                        "Я СРУ" -> {
                            if (repository.currentPoopingPersonId == null) {
                                contentMessage.asFromUser()?.user?.let { user ->
                                    sendMessage(
                                        contentMessage.chat,
                                        "В данный момент срет ${user.username?.username ?: user.firstName}"
                                    )
                                    repository.currentPoopingPersonId = user.id
                                    repository.setPoopInfo(user.username?.username ?: user.firstName)
                                }
                            } else {
                                if (contentMessage.asFromUser()?.user?.id == repository.currentPoopingPersonId) {
                                    sendMessage(contentMessage.chat, "Вы уже и так срете!!")
                                } else {
                                    sendMessage(
                                        contentMessage.chat,
                                        "Вы не можете срать так как туалет занят ${
                                            getChatMember(
                                                contentMessage.chat.id,
                                                repository.currentPoopingPersonId!!
                                            ).user.username
                                        }}"
                                    )
                                }
                            }
                        }
                        "Я ПОСРАЛ", "Я ПОСРАЛА" -> {
                            repository.currentPoopingPersonId = null
                            contentMessage.asFromUser()?.user?.let {
                                if (repository.getPoopInfoByUserName(
                                        it.username?.username ?: it.firstName
                                    ).isPooping
                                ) {
                                    repository.setPoopEnded(it.username?.username ?: it.firstName)
                                    sendMessage(
                                        contentMessage.chat,
                                        "Пользователь " +
                                                "${it.username?.username ?: it.firstName} срал " +
                                                repository.getPoopInfoByUserName(
                                                    it.username?.username ?: it.firstName
                                                ).formattedPoopingTime
                                    )
                                    repository.logData()
                                } else {
                                    sendMessage(contentMessage.chat, "В данный момент вы не срали !!")
                                }
                            }
                        }
                        "КТО СРЕТ", "КТО СРЁТ" -> {
                            repository.currentPoopingPersonId?.let {
                                val poopingUser = getChatMember(contentMessage.chat.id, it).user
                                sendMessage(
                                    contentMessage.chat,
                                    "В данный момент срет ${poopingUser.username?.username ?: poopingUser.firstName}"
                                )
                            } ?: run {
                                sendMessage(contentMessage.chat, "В данный момент никто не срет")
                            }
                        }
                        "КТО СРАЛ" -> {
                            sendMessage(contentMessage.chat, repository.getPoopHistory())
                        }
                        "ТОП СРУНОВ" -> {
                            sendMessage(contentMessage.chat, "ТОП 5 СРУНОВ\n${repository.getTopFivePoopTime()}")
                        }
                    }
                }
            }
            println(me)
        }.join()
    }

}