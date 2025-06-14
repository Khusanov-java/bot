package org.example.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    private final TelegramBot telegramBot;

    public TelegramService(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public  void handle(Update update) {
        if (update.message() != null && update.message().text() != null) {
            Long id = update.message().chat().id();
            String text = update.message().text();
            if (text.equals("/start")) {
                telegramBot.execute(new SendMessage(id, "Welcome!"));
            }
        }
    }
}