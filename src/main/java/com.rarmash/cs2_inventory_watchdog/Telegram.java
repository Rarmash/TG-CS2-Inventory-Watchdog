package com.rarmash.cs2_inventory_watchdog;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
                    List<Item> items = Watchdog.scanProfile(Options.getSteamID64());
                    double totalInventoryValue = 0;
                    for (Item item : items) {
                        System.out.println(item.getName() + " (" + item.getExterior() + ") - " + item.getPrice());
                        totalInventoryValue += item.getPrice();
                    }
                    sendText(chatId, "Total inventory value: $" + totalInventoryValue);
                } catch (Exception e) {
                    sendText(chatId, "Bruh.");
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
                .chatId(chatID.toString()) //Who are we sending a message to
                .text(textline).build();    //Message content
    try {
        execute(sm);                        //Actually sending the message
    } catch (TelegramApiException e) {
        throw new RuntimeException(e);      //Any error will be printed here
    }
}
}
