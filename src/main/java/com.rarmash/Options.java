package com.rarmash;

public class Options {
    public static final String STEAMID64 = System.getenv("STEAMID64");
    public static final String PRICE_FILE = "total_price_history.txt";

    public String getSteamID64() {
        return STEAMID64;
    }

    public String getPriceFile() {
        return PRICE_FILE;
    }
}
