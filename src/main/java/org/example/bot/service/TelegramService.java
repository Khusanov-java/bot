package org.example.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.example.bot.entity.TgUser;
import org.example.bot.repo.TgUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramBot telegramBot;
    private final TgUserRepository tgUserRepository;




    public void handle(Update update) {
        try {
            if (update.message() != null && update.message().text() != null) {
                Long id = update.message().chat().id();
                String text = update.message().text();
                TgUser tgUser = tgUserRepository.findById(id).orElse(TgUser.builder().id(id).build());
                if (text.equals("/start")) {
                    SendMessage sendMessage = new SendMessage(
                            id,
                            tgUser.getId().toString()
                    );
                    telegramBot.execute(sendMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}