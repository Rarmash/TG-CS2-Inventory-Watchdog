package com.rarmash.cs2_inventory_watchdog;

public class Options {
    private static final String STEAMID64 = System.getenv("STEAMID64");
    private static final String TELEGRAM_TOKEN = System.getenv("TELEGRAM_TOKEN");
    private static final String PRICE_FILE = "total_price_history.txt";

    public static String getSteamID64() {
        return STEAMID64;
    }

    public static String getTelegramToken() {
        return TELEGRAM_TOKEN;
    }

    public static String getPriceFile() {
        return PRICE_FILE;
    }
}