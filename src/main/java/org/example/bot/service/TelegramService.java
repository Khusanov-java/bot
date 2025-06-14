package org.example.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.example.bot.constants.BotConstants;
import org.example.bot.entity.TgUser;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    private final TelegramBot telegramBot;

    public TelegramService(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void handle(Update update) {
        try {
            if (update.message() != null) {
                Long chatId = update.message().chat().id();
                TgUser user = UserService.getOrCreateUser(chatId);
                String text = update.message().text();
                if (text != null && text.equals("/start")) {
                    SendMessage sendMessage = new SendMessage(chatId, BotConstants.GREETING);
                    telegramBot.execute(sendMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}