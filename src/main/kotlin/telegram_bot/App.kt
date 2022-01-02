package telegram_bot

suspend fun main(args: Array<String>) {
    ToiletBot(args.first()).also {
        it.onStartPolling()
    }
}




