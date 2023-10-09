package com.rarmash.cs2_inventory_watchdog;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String steamid64 = Options.getSteamID64();
        System.out.println(steamid64);

        if (steamid64 == null || steamid64.isEmpty()) {
            System.err.println("STEAMID64 environment variable not set.");
        }

        try {
            List<Item> items = Watchdog.scanProfile(steamid64);
            for (Item item: items) {
                System.out.println(item.getName() + " (" + item.getExterior() + ") - " + item.getPrice());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
