package com.rarmash.cs2_inventory_watchdog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Watchdog {
    private static final String inventoryLink = "https://steamcommunity.com/inventory/%s/730/2?l=english&count=1000";
    private static final String marketLink = "https://steamcommunity.com/market/priceoverview/?country=us&appid=730&market_hash_name=%s&format=json";

    public static List<Item> scanProfile(String steamid64) throws Exception {
        StringBuilder inventory = scanProfileToResponse(steamid64);
        JSONArray inventoryArray = parseResponseToJSON(inventory);
        List<Item> items = fillItemsInfo(inventoryArray);

        return items;
    }

    private static StringBuilder scanProfileToResponse(String steamid64) throws Exception {
        while (true) {
            String inventoryURL = String.format(inventoryLink, steamid64);

            URI uri = new URI(inventoryURL);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == 429) {
                    System.err.println("Steam is blocking me :< (" + responseCode + ")");
                    Thread.sleep(10000);
                } else {
                    throw new IOException("Failed to fetch item price data.. HTTP error code: " + responseCode);
                }
            } else {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                System.out.println("Got the inventory");
                return response;
            }
        }
    }

    private static JSONArray parseResponseToJSON(StringBuilder response) {
        JSONObject inventoryJSON = new JSONObject(response.toString());
        JSONArray inventoryArray = inventoryJSON.getJSONArray("descriptions");

        return inventoryArray;
    }

    private static List<Item> fillItemsInfo(JSONArray inventoryArray) throws Exception {
        List<Item> items = new ArrayList<>();

        double totalInventoryPrice = 0.0;

        for (int item = 0; item < inventoryArray.length(); item++) {
            JSONObject itemJSON = inventoryArray.getJSONObject(item);
            String name = getItemName(itemJSON);
            System.out.println(name);
            String exterior = getItemExterior(itemJSON);
            double price;
            if (isMarketable(itemJSON)) {
                price = getItemPrice(itemJSON);
            } else {
                price = 0; // пустышка
            }

            Item currentItem = new Item(name, exterior, price);
            items.add(currentItem);
            Thread.sleep(3000);
        }
        return items;
    }

    private static String getItemName(JSONObject item) {
        return item.getString("name");
    }

    private static String getItemExterior(JSONObject item) {
        JSONArray tags = item.getJSONArray("tags");
        for (int i = 0; i < tags.length(); i++) {
            JSONObject itemTag = tags.getJSONObject(i);
            if ("Exterior".equals(itemTag.getString("category"))) {
                return itemTag.getString("localized_tag_name");
            }
        }
        return "No exterior";
    }

    private static boolean isMarketable(JSONObject item) {
        int marketable = item.getInt("marketable");
        return marketable == 1;
    }

    private static double getItemPrice(JSONObject item) throws Exception {
        String marketHashName = item.getString("market_hash_name").replace(":", "%3A").replace(" ", "%20").replace("|", "%7C").replace("(", "%28").replace(")", "%29");
        String priceURL = String.format(marketLink, marketHashName);

        URI uri = new URI(priceURL);
        URL url = uri.toURL();

        while (true) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == 429) {
                    System.err.println("Steam is blocking me :< (" + responseCode + ")");
                    Thread.sleep(5000);
                } else if (responseCode == 500) {
                    return 0;
                } else {
                    throw new IOException("Failed to fetch item price data.. HTTP error code: " + responseCode);
                }
            } else {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                JSONObject priceJSON = new JSONObject(response.toString());
                return parsePrice(priceJSON);
            }
        }
    }

    private static double parsePrice(JSONObject priceJSON) {
        if (priceJSON.has("lowest_price")) {
            String lowestPrice = priceJSON.getString("lowest_price").substring(1);
            return Double.parseDouble(lowestPrice);
        } else {
            return 0;
        }
    }
}
