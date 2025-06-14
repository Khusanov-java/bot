package org.example.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.example.bot.entity.Category;
import org.example.bot.entity.State;
import org.example.bot.entity.TgUser;
import org.example.bot.repo.CategoryRepository;
import org.example.bot.repo.TgUserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramBot telegramBot;
    private final TgUserRepository tgUserRepository;
    private final CategoryRepository categoryRepository;




    public void handle(Update update) {
        try {
            if (update.message() != null && update.message().text() != null) {
                Long id = update.message().chat().id();
                String text = update.message().text();
                TgUser tgUser = tgUserRepository.findById(id).orElse(TgUser.builder().id(id).build());
                if (text.equals("/start")) {
                    SendMessage sendMessage = new SendMessage(
                            id,
                            "Salom bu test bot ishlasa ishladi"
                    );
                    sendMessage.replyMarkup(createCategoryButton());
                    telegramBot.execute(sendMessage);
                    tgUser.setState(State.CATEGORY);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Keyboard createCategoryButton() {
        List<Category> categories = categoryRepository.findAll();

        List<KeyboardButton[]> rows = new ArrayList<>();
        List<KeyboardButton> currentRow = new ArrayList<>();

        for (int i = 0; i < categories.size(); i++) {
            currentRow.add(new KeyboardButton(categories.get(i).getTitle()));
            if ((i + 1) % 2 == 0 || i == categories.size() - 1) {
                rows.add(currentRow.toArray(new KeyboardButton[0]));
                currentRow.clear();
            }
        }

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(rows.toArray(new KeyboardButton[0][]));
        replyKeyboardMarkup.resizeKeyboard(true).oneTimeKeyboard(false);
        return replyKeyboardMarkup;
    }



}