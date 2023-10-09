package com.rarmash.cs2_inventory_watchdog;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Watchdog {
    private static final String inventoryLink = "https://steamcommunity.com/inventory/%s/730/2?l=english&count=1000";
    private static final String marketLink = "https://steamcommunity.com/market/priceoverview/?country=us&appid=730&market_hash_name=%s&format=json";

    public static List<Item> scanProfile(String steamid64) throws Exception {
        System.out.println(readPreviousTotal());
        StringBuilder inventory = scanProfileToResponse(steamid64);
        JSONArray inventoryArray = parseResponseToJSON(inventory);
        List<Item> items = fillItemsInfo(inventoryArray);
        saveToExcel(items);
        writeTotalToFile(getTotalInventoryPrice(items));

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

        return inventoryJSON.getJSONArray("descriptions");
    }

    private static List<Item> fillItemsInfo(JSONArray inventoryArray) throws Exception {
        List<Item> items = new ArrayList<>();

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
            // Thread.sleep(3000);
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

    private static double getTotalInventoryPrice(List<Item> items) {
        double totalInventoryPrice = 0;
        for (Item item: items) {
            totalInventoryPrice += item.getPrice();
        }
        return totalInventoryPrice;
    }

    public static void writeTotalToFile(double totalInventoryPrice) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Options.getPriceFile()));
            writer.write(Double.toString(totalInventoryPrice));
            writer.close();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String currentDate = sdf.format(new Date());
            writer = new BufferedWriter(new FileWriter(Options.getPriceFile()));
            writer.write("Total Inventory Price: $" + totalInventoryPrice);
            writer.newLine();
            writer.write("Date: " + currentDate);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static double readPreviousTotal() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Options.getPriceFile()));
            String line;
            double previousTotal = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Total Inventory Price: $")) {
                        String totalString = line.replace("Total Inventory Price: $", "");
                        previousTotal = Double.parseDouble(totalString);
                }
            }
            reader.close();
            return previousTotal;
        } catch (IOException e) {
            return 0;
        }
    }

    private static void saveToExcel(List<Item> items) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventory");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Exterior");
        headerRow.createCell(2).setCellValue("Price ($)");

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            Row row = sheet.createRow(i+1);
            row.createCell(0).setCellValue(item.getName());
            row.createCell(1).setCellValue(item.getExterior());
            row.createCell(2).setCellValue(item.getPrice());
        }

        try (FileOutputStream outputStream = new FileOutputStream(Options.getSteamID64() + ".xlsx")) {
            workbook.write(outputStream);
        }

        workbook.close();
    }
}
