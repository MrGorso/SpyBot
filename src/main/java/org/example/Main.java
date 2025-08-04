package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        try {
            // Создаём Telegram API и регистрируем твоего бота
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new SpyBot());

            System.out.println("✅ Бот запущен и ждёт сообщения из группы...");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
