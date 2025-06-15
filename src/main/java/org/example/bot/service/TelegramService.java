package org.example.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.ForwardMessage;
import com.pengrad.telegrambot.request.SendMessage;
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
            if (update.message() != null && update.message().forwardFromChat() != null) {
                Long channelId = update.message().forwardFromChat().id();
                Integer messageId = update.message().forwardFromMessageId();
                telegramBot.execute(new SendMessage(update.message().chat().id(),
                        "Received forwarded video.\nChannel ID: " + channelId + "\nMessage ID: " + messageId));

                Video video = new Video();
                video.setTitle("Some Title");
                video.setChannelId(channelId);
                video.setMessageId(messageId);
                Category category = categoryRepository.findByTitle("category1");
                video.setCategory(category);
                videoRepository.save(video);
                return;
            }

            if (update.message() != null) {
                String text = update.message().text();
                Long id = update.message().chat().id();
                TgUser tgUser = tgUserRepository.findById(id).orElseGet(() -> {
                    TgUser newUser = TgUser.builder().id(id).state(State.CATEGORY).build();
                    tgUserRepository.save(newUser);
                    return newUser;
                });

                if (text != null && text.equals("/start")) {
                    SendMessage sendMessage = new SendMessage(id, "Salom bu test bot ishlasa ishladi");
                    sendMessage.replyMarkup(createCategoryButton());
                    telegramBot.execute(sendMessage);

                    if (tgUser.getState() != State.CATEGORY) {
                        tgUser.setState(State.CATEGORY);
                        tgUserRepository.save(tgUser);
                    }
                    return;
                }

                if (text != null && text.equals("Ortga")) {
                    SendMessage sendMessage = new SendMessage(id, "Choose category:");
                    sendMessage.replyMarkup(createCategoryButton());
                    telegramBot.execute(sendMessage);

                    if (tgUser.getState() != State.CATEGORY) {
                        tgUser.setState(State.CATEGORY);
                        tgUserRepository.save(tgUser);
                    }
                    return;
                }

                if (tgUser.getState() == State.CATEGORY) {
                    Category category = categoryRepository.findByTitle(text);
                    if (category == null) {
                        telegramBot.execute(new SendMessage(id, "Kategoriya topilmadi"));
                        return;
                    }

                    List<Video> videos = videoRepository.findByCategory_Id(category.getId());
                    SendMessage sendMessage = new SendMessage(id, "Videoni tanlang:");
                    sendMessage.replyMarkup(createVideosButton(videos));
                    telegramBot.execute(sendMessage);

                    tgUser.setState(State.TOPIC);
                    tgUserRepository.save(tgUser);
                    return;
                }

                if (tgUser.getState() == State.TOPIC) {
                    Video video = videoRepository.findByTitle(text);
                    if (video == null) {
                        telegramBot.execute(new SendMessage(id, "Video topilmadi"));
                        return;
                    }

                    ForwardMessage forward = new ForwardMessage(id, video.getChannelId(), video.getMessageId());
                    telegramBot.execute(forward);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Keyboard createVideosButton(List<Video> videos) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup("");
        for (Video video : videos) {
            replyKeyboardMarkup.addRow(new KeyboardButton(video.getTitle()));
        }
        replyKeyboardMarkup.addRow(new KeyboardButton("Ortga"));
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