package org.example.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramBot telegramBot;
    private final TgUserRepository tgUserRepository;
    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;
    private String password="Ibrohim";

    public void handle(Update update) {
        try {
            if (update.message() != null) {
                String text = update.message().text();
                Long id = update.message().chat().id();
                TgUser tgUser = tgUserRepository.findById(id).orElseGet(() -> {
                    TgUser newUser = TgUser.builder().id(id).state(State.CHECK_NAME).build();
                    tgUserRepository.save(newUser);
                    return newUser;
                });

                if (text != null && text.equals("/start")) {
                    SendMessage sendMessage = new SendMessage(id, "Salom bu test bot ishlasa ishladi, ismingizni kiriting");
                    sendMessage.replyMarkup(new ReplyKeyboardRemove());
                    telegramBot.execute(sendMessage);
                    if (tgUser.getState() != State.CHECK_NAME) {
                        tgUser.setState(State.CHECK_NAME);
                        tgUserRepository.save(tgUser);
                    }
                    return;
                }

                if (tgUser.getState() == State.CHECK_NAME) {
                    if (Objects.equals(text, password)) {
                        SendMessage sendMessage = new SendMessage(
                                id,
                                "Asosiy menyu"
                        );
                        tgUser.setUsername(password);
                        sendMessage.replyMarkup(createCategoryWithAdminPanelButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.CATEGORY);
                        tgUserRepository.save(tgUser);
                        return;
                    } else {
                        SendMessage sendMessage = new SendMessage(
                                id,
                                "Asosiy menyu"
                        );
                        tgUser.setUsername(text);
                        sendMessage.replyMarkup(createCategoryButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.CATEGORY);
                        tgUserRepository.save(tgUser);
                        return;
                    }
                }

                if (text != null && text.equals("Ortga") || text != null && text.equals("Asosiy menyu")) {
                    if (tgUser.getUsername().equals(password)) {
                        SendMessage sendMessage = new SendMessage(id, "Choose category:");
                        sendMessage.replyMarkup(createCategoryWithAdminPanelButton());
                        telegramBot.execute(sendMessage);

                        if (tgUser.getState() != State.CATEGORY) {
                            tgUser.setState(State.CATEGORY);
                            tgUserRepository.save(tgUser);
                        }
                        return;
                    } else {
                        SendMessage sendMessage = new SendMessage(id, "Choose category:");
                        sendMessage.replyMarkup(createCategoryButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.CATEGORY);
                        tgUserRepository.save(tgUser);
                        return;
                    }
                }

                if (tgUser.getState() == State.CATEGORY) {
                    if (text != null && text.equals("Admin panel")) {
                        SendMessage sendMessage = new SendMessage(
                                id,
                                "Admin panelga xush kelibsiz tanlang"
                        );
                        sendMessage.replyMarkup(createAdminPanelButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.ADMIN_PANEL);
                        tgUserRepository.save(tgUser);
                        return;
                    } else {
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
                }

                if (tgUser.getState() == State.TOPIC) {
                    Video video = videoRepository.findByTitle(text);
                    if (video == null) {
                        telegramBot.execute(new SendMessage(id, "Video topilmadi"));
                        return;
                    }

                    ForwardMessage forward = new ForwardMessage(id, video.getChannelId(), video.getMessageId());
                    forward.protectContent(true);
                    telegramBot.execute(forward);
                }

                if (tgUser.getState() == State.ADMIN_PANEL) {
                    if (text != null && text.equals("Kitob qo'shish")) {
                        SendMessage sendMessage = new SendMessage(id, "Kitob nomini yozing");
                        sendMessage.replyMarkup(new ReplyKeyboardRemove());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.ADD_CATEGORY);
                        tgUserRepository.save(tgUser);
                        return;
                    } else if (text != null && text.equals("Video qo'shish")) {
                        SendMessage sendMessage = new SendMessage(
                                id,
                                "Kategoriya tanlang"
                        );
                        sendMessage.replyMarkup(createCategoryButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.CHOOSE_CATEGORY);
                        tgUserRepository.save(tgUser);
                        return;
                    }
                }

                if (tgUser.getState() == State.ADD_CATEGORY) {
                    Category category = new Category();
                    category.setTitle(text);
                    categoryRepository.save(category);
                    SendMessage sendMessage = new SendMessage(
                            id,
                            "Kitob saqlandi, iltimos asosiy menyu tugmasini bosing"
                    );
                    sendMessage.replyMarkup(getAsosiyMenyu());
                    telegramBot.execute(sendMessage);
                    return;
                }

                if (tgUser.getState() == State.CHOOSE_CATEGORY) {
                    tgUser.setTempCategoryTitle(text);
                    tgUserRepository.save(tgUser);
                    SendMessage sendMessage = new SendMessage(
                            id,
                            "Video yuboring"
                    );
                    sendMessage.replyMarkup(new ReplyKeyboardRemove());
                    telegramBot.execute(sendMessage);
                    tgUser.setState(State.ADD_VIDEO);
                    tgUserRepository.save(tgUser);
                    return;
                }

                if (tgUser.getState() == State.ADD_VIDEO) {
                    if (update.message() != null && update.message().forwardFromChat() != null) {
                        Long channelId = Long.valueOf("-1002805667393");
                        if (!channelId.equals(update.message().forwardFromChat().id())) {
                            return;
                        }
                        String caption = update.message().caption();
                        int dotIndex = caption.indexOf('.');
                        String videoId = caption.substring(0, dotIndex);
                        String title = caption.substring(dotIndex + 1);
                        Integer messageId = update.message().forwardFromMessageId();
                        Video video = new Video();
                        video.setTitle(title);
                        video.setId(Integer.parseInt(videoId));
                        video.setChannelId(channelId);
                        video.setMessageId(messageId);
                        Category category = categoryRepository.findByTitle(tgUser.getTempCategoryTitle());
                        video.setCategory(category);
                        videoRepository.save(video);
                        SendMessage sendMessage = new SendMessage(
                                id,
                                "Video saqlandi, iltimos asosiy menyu tugmasini bosing"
                        );
                        sendMessage.replyMarkup(getAsosiyMenyu());
                        telegramBot.execute(sendMessage);

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Keyboard createCategoryWithAdminPanelButton() {
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

        KeyboardButton adminButton = new KeyboardButton("Admin panel");
        rows.add(new KeyboardButton[]{adminButton});

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(rows.toArray(new KeyboardButton[0][]));
        replyKeyboardMarkup.resizeKeyboard(true).oneTimeKeyboard(false);
        return replyKeyboardMarkup;

    }


    private ReplyKeyboardMarkup getAsosiyMenyu() {
        return new ReplyKeyboardMarkup(
                new KeyboardButton("Asosiy menyu")
        );
    }

    private Keyboard createAdminPanelButton() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup("");
        replyKeyboardMarkup.addRow(
                new KeyboardButton("Kitob qo'shish"),
                new KeyboardButton("Video qo'shish")
        );
        return replyKeyboardMarkup;
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