package com.rarmash.cs2_inventory_watchdog;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class Watchdog {
    private static final String inventoryLink = "https://steamcommunity.com/inventory/%s/730/2?l=english&count=1000";

    static Options options = new Options();

    public static List<Item> scanProfile(String steamid64) throws Exception {
        while (true) {
            String inventoryURL = String.format(inventoryLink, steamid64);

            HttpURLConnection connection = (HttpURLConnection) new URL(inventoryURL).openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == 429) {
                    System.err.println("Steam is blocking me :< (" + responseCode + ")");
                    Thread.sleep(5000);
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

                JSONObject inventoryJSON = new JSONObject(response.toString());
                JSONArray descriptions = inventoryJSON.getJSONArray("descriptions");

                List<Item> items = new ArrayList<>();
                double totalPrice = 0.0;

                for (int i = 0; i < descriptions.length(); i++) {
                    JSONObject itemJSON = descriptions.getJSONObject(i);
                    String exterior = getExterior(itemJSON);
                    System.out.println(itemJSON.getString("name") + " (" + exterior + ")");
                    double itemPrice = getItemPrice(itemJSON);

                    items.add(new Item(itemJSON.getString("name"), exterior, itemPrice));
                    totalPrice += itemPrice;
                }

                double previousTotalPrice = 0.0;
                String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                File priceFile = new File(options.getPriceFile());
                if (priceFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(priceFile))) {
                        String[] previous = reader.readLine().split(",");
                        previousTotalPrice = Double.parseDouble(previous[0]);
                    }
                }

                double priceDifference = totalPrice - previousTotalPrice;

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(priceFile))) {
                    writer.write(totalPrice + "," + currentDate);
                }

                System.out.println("Previous Total Price: $" + String.format("%.2f", previousTotalPrice));
                System.out.println("Current Total Price: $" + String.format("%.2f", totalPrice));
                System.out.println("Price Difference: $" + String.format("%.2f", priceDifference));

                return items;
            }
        }
    }

    private static String getExterior(JSONObject itemJSON) {
        JSONArray tags = itemJSON.getJSONArray("tags");
        for (int i = 0; i < tags.length(); i++) {
            JSONObject tag = tags.getJSONObject(i);
            if ("Exterior".equals(tag.getString("category"))) {
                return tag.getString("localized_tag_name");
            }
        }
        return "No exterior";
    }

    private static double getItemPrice(JSONObject itemJSON) throws IOException, InterruptedException {
        while (true) {
            String marketHashName = itemJSON.getString("market_hash_name").replace(":", "%3A").replace(" ", "%20").replace("|", "%7C").replace("(", "%28").replace(")", "%29");
            String priceURL = "https://steamcommunity.com/market/priceoverview/?country=us&appid=730&market_hash_name=" + marketHashName + "&format=json";

            HttpURLConnection connection = (HttpURLConnection) new URL(priceURL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == 429) {
                    System.err.println("Steam is blocking me :< ("+ responseCode + ")");
                    Thread.sleep(5000);
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
                if (priceJSON.has("lowest_price")) {
                    String lowestPrice = priceJSON.getString("lowest_price").substring(1);
                    return Double.parseDouble(lowestPrice);
                } else {
                    return 0.0;
                }
            }
        }
    }
    public static void saveToExcel(List<Item> items) throws IOException {
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
