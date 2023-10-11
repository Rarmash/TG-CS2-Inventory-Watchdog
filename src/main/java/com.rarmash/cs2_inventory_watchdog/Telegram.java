package com.rarmash.cs2_inventory_watchdog;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Telegram extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        long chatId = update.getMessage().getChatId();

        if (msg.isCommand()) {
            if (msg.getText().equals("/get_price")) {
                sendText(chatId, "Parsing the inventory, please wait...");
                try {
                    double previousTotal = Watchdog.readPreviousTotal();
                    List<Item> items = Watchdog.scanProfile(Options.getSteamID64());

                    double totalInventoryValue = 0;
                    for (Item item : items) {
                        totalInventoryValue += item.getPrice();
                    }

                    double priceDifference = totalInventoryValue - previousTotal;
                    double priceChangePercentage = (priceDifference / previousTotal) * 100;

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    String currentDate = sdf.format(new Date());

                    sendFile(chatId, new InputFile(new File(Options.getSteamID64() + ".xlsx"), Options.getSteamID64() + "-" + currentDate + ".xlsx"), "Counter-Strike 2 inventory price:\n\nPrevious inventory price: $" + String.format("%.2f", previousTotal) + "\nCurrent inventory price: $" + String.format("%.2f",totalInventoryValue) + "\nPrice difference: $" + String.format("%.2f", priceDifference) + " (" + String.format("%.2f", priceChangePercentage) + "%)");
                } catch (Exception e) {
                    sendText(chatId, "Bruh. " + e);
                }
            }
        }

        System.out.println(user.getFirstName() + " wrote " + msg.getText());
    }

    @Override
    public String getBotUsername() {
        return "CS2Watchdog_bot";
    }

    @Override
    public String getBotToken() {
        return Options.getTelegramToken();
    }

    public void sendText(Long chatID, String textline){
        SendMessage sm = SendMessage.builder()
                .chatId(chatID.toString())
                .text(textline).build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendFile(Long chatID, InputFile file, String caption) {
        SendDocument sendDocumentRequest = new SendDocument();
        sendDocumentRequest.setChatId(chatID);
        sendDocumentRequest.setDocument(file);
        sendDocumentRequest.setCaption(caption);
        try {
            execute(sendDocumentRequest);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
