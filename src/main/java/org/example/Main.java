package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        Logger logger = new Logger();
        try {
            TelegramBotsApi bots = new TelegramBotsApi(DefaultBotSession.class);
            bots.registerBot(new Bot());
            logger.logi("Successfully started bot session");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}