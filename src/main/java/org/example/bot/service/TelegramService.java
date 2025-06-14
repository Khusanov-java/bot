package org.example.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import lombok.RequiredArgsConstructor;
import org.example.bot.entity.Category;
import org.example.bot.entity.State;
import org.example.bot.entity.TgUser;
import org.example.bot.entity.Video;
import org.example.bot.repo.CategoryRepository;
import org.example.bot.repo.TgUserRepository;
import org.example.bot.repo.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramBot telegramBot;
    private final TgUserRepository tgUserRepository;
    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;

    public void handle(Update update) {
        try {
            if (update.message() != null) {
                String text = update.message().text();
                Long id = update.message().chat().id();
                TgUser tgUser = tgUserRepository.findById(id).orElse(TgUser.builder().id(id).build());
                tgUserRepository.save(tgUser);

                if (text != null && text.equals("/start")) {
                    SendMessage sendMessage = new SendMessage(
                            id,
                            "Salom bu test bot ishlasa ishladi"
                    );
                    sendMessage.replyMarkup(createCategoryButton());
                    telegramBot.execute(sendMessage);
                    tgUser.setState(State.CATEGORY);
                    tgUserRepository.save(tgUser);

                } else {
                    if (tgUser.getState() == State.CATEGORY) {
                        Category category = categoryRepository.findByTitle(text);
                        List<Video> videos = videoRepository.findByCategory_Id(category.getId());

                        SendMessage sendMessage = new SendMessage(
                                id,
                                category.getTitle()
                        );
                        sendMessage.replyMarkup(createVideosButton(videos));
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.TOPIC);
                        tgUserRepository.save(tgUser);
                    } else if (tgUser.getState() == State.TOPIC) {
                        telegramBot.execute(new SendMessage(id, "TOPIC holatidasiz. Kanaldagi rasmni kuting..."));
                    }
                }
            }
            else if (update.channelPost() != null && update.channelPost().photo() != null) {
                List<TgUser> topicUsers = tgUserRepository.findAll()
                        .stream()
                        .filter(u -> State.TOPIC.equals(u.getState()))
                        .toList();

                PhotoSize[] photos = update.channelPost().photo();
                PhotoSize largest = photos[photos.length - 1];

                for (TgUser user : topicUsers) {
                    SendPhoto sendPhoto = new SendPhoto(user.getId(), largest.fileId());
                    telegramBot.execute(sendPhoto);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Keyboard createVideosButton(List<Video> videos) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup("");
        for (Video video : videos) {
            replyKeyboardMarkup.addRow(
                    new KeyboardButton(video.getTitle())
            );
        }
        return replyKeyboardMarkup;
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
