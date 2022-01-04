package telegram_bot

import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.utils.PreviewFeature
import kotlinx.coroutines.*
import telegram_bot.data.PoopRepository
import kotlin.random.Random

class ToiletBot(botKey: String) {
    private val bot = telegramBot(botKey)

    private val repository = PoopRepository()

    private val scope = CoroutineScope(Dispatchers.Default)

    @OptIn(PreviewFeature::class, InternalCoroutinesApi::class)
    suspend fun onStartPolling() {
        bot.buildBehaviourWithLongPolling(scope) {
            val me = getMe()

            onCommand("start", requireOnlyCommandInMessage = true) {
                reply(
                    it,
                    "Привет, работяги, меня зовут ${me.firstName}, \nпожалуйста пишите каждый раз 'Я СРУ', перед тем как идти в туалет, и 'Я ПОСРАЛ' после окончания процесса. Так же пользуйтесь командами 'КТО СРАЛ', 'КТО СРЕТ', 'ТОП СРУНОВ', 'CКОЛЬКО ПОСРАНО'"
                )
            }
            onCommand("rules", requireOnlyCommandInMessage = true) {
                reply(
                    it,
                    "1. Занимать туалет не больше 20 минут\n2. Перед тем как идти срать, обязательно узнать 'КТО СРЕТ'\n3. В толчок туалетку не бросать!!"
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
                                    val username = user.username?.username ?: user.firstName
                                    repository.setPoopInfo(
                                        user.id,
                                        username,
                                        wakeUpJob(contentMessage.chat.id, username).apply {

                                        }
                                    )
                                }
                            } else {
                                if (contentMessage.asFromUser()?.user?.id == repository.currentPoopingPersonId) {
                                    sendMessage(contentMessage.chat, "Вы уже и так срете!!")
                                } else {
                                    val username =
                                        getChatMember(contentMessage.chat.id, repository.currentPoopingPersonId!!)
                                            .user.username?.username ?: getChatMember(
                                            contentMessage.chat.id,
                                            repository.currentPoopingPersonId!!
                                        ).user.firstName
                                    sendMessage(
                                        contentMessage.chat,
                                        "Вы не можете срать так как туалет занят ${username}}"
                                    )
                                }
                            }
                        }
                        "Я ПОСРАЛ", "Я ПОСРАЛА" -> {
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
                        "СКОЛЬКО ПОСРАНО" -> {
                            sendMessage(contentMessage.chat, "ВСЕГО СРАЛИ ${repository.getPoopingSum()}")
                        }
                    }
                }
            }
            println(me)
        }.apply {
            invokeOnCompletion(onCancelling = true) {
                println("main coroutine canceled")
            }
        }.join()
    }

    private fun wakeUpJob(chatId: ChatId, username: String) = scope.launch {
        runCatching {
            val twoMinutesMilly = 2 * 60 * 1000L
            val threeMinutesMilly = 3 * 60 * 1000L
            val sevenMinutesMilly = 7 * 60 * 1000L
            when (Random.nextInt(7)) {
                1 -> {
                    runDelayed(500) {
                        bot.sendPhoto(chatId, FileId(POOP_RULE_IMAGE_URL), "$username, не забывай об осанке")
                    }
                }
                2 -> {
                    runDelayed(500) {
                        bot.sendMessage(chatId, "$username желаю приятно провести время")
                    }
                }
                3 -> {
                    runDelayed(500) {
                        bot.sendMessage(
                            chatId,
                            "$username не забывай о правилах туалета! Узнать их можно с помощью команды /rules"
                        )
                    }
                }
                4, 5, 6 -> {
                    runDelayed(500) {
                        bot.sendMessage(chatId = chatId, text = repository.getRandomJoke())
                    }
                }
            }

            runDelayed(threeMinutesMilly) {
                if (Random.nextBoolean()) {
                    bot.sendMessage(chatId, "Внимание, $username вы срете уже 3 минуты!! Не забывайте о времени")
                }
            }
            runDelayed(twoMinutesMilly) {
                bot.sendPhoto(chatId, FileId(LEAVE_TOILET_IMAGE_URL), "$username, 5 минут")
            }
            runDelayed(sevenMinutesMilly) {
                bot.sendPhoto(
                    chatId,
                    FileId(POOP_FORBIDDEN_IMAGE_URL),
                    "$username 10 минут прошло, все съеби уже не смешно"
                )
            }
            runDelayed(twoMinutesMilly * 2) {
                bot.sendPhoto(
                    chatId,
                    FileId(STOP_POOP_IMAGE_URL),
                    "$username через 6 минут ты нарушишь правило туалета"
                )
            }
            runDelayed(twoMinutesMilly * 3) {
                bot.sendPhoto(
                    chatId,
                    FileId(EXTRA_WARNING_IMAGE_URL),
                    "$username $username $username, ВЫ СРЕТЕ УЖЕ БОЛЕЕ 20 МИНУТ НЕМЕДЛЕННО ПОКИНЬТЕ ПАРАШУ!!!!"
                )
            }
        }.onSuccess { println("coroutine worked successfully") }.onFailure { ex ->
            println(ex.message)
        }
    }

    private suspend fun runDelayed(delay: Long, block: suspend () -> Unit) {
        delay(delay)
        block.invoke()
    }

    companion object {
        private const val EXTRA_WARNING_IMAGE_URL =
            "http://storage.inovaco.ru/media/project_mo_99/db/48/d3/93/2e/c5/ekstrennoe-preduprezhdenie.jpg"
        private const val STOP_POOP_IMAGE_URL =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ_cu1o0sJTRMr8EIkjNXeboEEHyPWv5h14qA&usqp=CAU"
        private const val POOP_FORBIDDEN_IMAGE_URL =
            "https://memepedia.ru/wp-content/uploads/2019/09/ja-vam-zapreshchaju-srat-1.jpg"
        private const val POOP_RULE_IMAGE_URL =
            "https://cs10.pikabu.ru/video/2018/07/20/10/og_og_153210878938386180.jpg"
        private const val LEAVE_TOILET_IMAGE_URL =
            "https://i.ytimg.com/vi/4dnbD2IFI50/hqdefault.jpg"
    }

}