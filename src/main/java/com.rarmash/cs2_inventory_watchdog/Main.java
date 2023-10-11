package com.rarmash.cs2_inventory_watchdog;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        String steamid64 = Options.getSteamID64();
        System.out.println("SteamID64: " + steamid64);

        if (steamid64 == null || steamid64.isEmpty()) {
            System.err.println("STEAMID64 environment variable not set.");
        }

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        try {
            Telegram bot = new Telegram();
            botsApi.registerBot(bot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
