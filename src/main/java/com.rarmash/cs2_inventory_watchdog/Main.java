package com.rarmash.cs2_inventory_watchdog;

import java.util.List;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        String steamid64 = Options.getSteamID64();
        System.out.println(steamid64);

        if (steamid64 == null || steamid64.isEmpty()) {
            System.err.println("STEAMID64 env variable not set.");
            return;
        }

        while (true) {
            try {
                List<Item> items = Watchdog.scanProfile(steamid64);
                Watchdog.saveToExcel(items);
                Thread.sleep(30000);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
