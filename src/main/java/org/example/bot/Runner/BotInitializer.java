package org.example.bot.Runner;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import org.example.bot.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BotInitializer {

    private final TelegramBot telegramBot;
    private final TelegramService telegramService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(50); // Tune this for real load

    @Autowired
    public BotInitializer(TelegramBot telegramBot, TelegramService telegramService) {
        this.telegramBot = telegramBot;
        this.telegramService = telegramService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                executorService.execute(() -> telegramService.handle(update));
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}