package com.rarmash;

import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        String steamid64 = System.getenv("STEAMID64");
        if (steamid64 == null || steamid64.isEmpty()) {
            System.err.println("STEAMID64 env variable not set.");
            return;
        }

        while (true) {
            try {
                List<com.rarmash.Item> items = scanProfile(steamid64);
                com.rarmash.Watchdog.saveToExcel(Collections.singletonList(items));
                Thread.sleep(30000);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
