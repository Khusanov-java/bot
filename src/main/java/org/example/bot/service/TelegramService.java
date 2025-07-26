package org.example.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Contact;
import com.pengrad.telegrambot.model.Document;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramBot telegramBot;
    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;
    private final TgUserRepository tgUserRepository;
    private static Category category;

    public void handle(Update update) {
        try {
            if (update.message() != null) {
                String text = update.message().text();
                Long id = update.message().chat().id();
                TgUser tgUser = tgUserRepository.findById(id).orElseGet(() -> {
                    TgUser newUser = TgUser.builder().id(id).state(State.SEND_CONTACT).createdAt(LocalDateTime.now()).build();
                    tgUserRepository.save(newUser);
                    return newUser;
                });

                if (text != null && text.equals("/start")) {
                    SendMessage sendMessage = new SendMessage(id, "Assalomu aleykum. Botga xush kelibsiz\nIltimos ismingizni kiriting\uD83D\uDE0A");
                    sendMessage.replyMarkup(new ReplyKeyboardRemove());
                    telegramBot.execute(sendMessage);
                    if (tgUser.getState() != State.SEND_CONTACT) {
                        tgUser.setState(State.SEND_CONTACT);
                        tgUserRepository.save(tgUser);
                    }
                    return;
                }

                if (tgUser.getState().equals(State.SEND_CONTACT)) {
                    tgUser.setUsername(text);
                    SendMessage sendMessage = new SendMessage(
                            id,
                            "Iltimos kontakt raqamingizni kiriting\uD83D\uDE0A"
                    );
                    sendMessage.replyMarkup(new ReplyKeyboardMarkup(
                            new KeyboardButton("Kontakt yuborish").requestContact(true)
                    ));
                    telegramBot.execute(sendMessage);
                    tgUser.setState(State.CHECK_NAME);
                    tgUserRepository.save(tgUser);
                    return;
                }

                if (tgUser.getState() == State.CHECK_NAME) {
                    Contact contact = update.message().contact();
                    tgUser.setPhoneNumber(contact.phoneNumber());
                    if (contact.phoneNumber().equals("998974034224")||contact.phoneNumber().equals("909188019")) {
                        SendMessage sendMessage = new SendMessage(
                                id,
                                "Asosiy menyu"
                        );
                        sendMessage.replyMarkup(new ReplyKeyboardRemove());
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
                        sendMessage.replyMarkup(new ReplyKeyboardRemove());
                        sendMessage.replyMarkup(createCategoryButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.CATEGORY);
                        tgUserRepository.save(tgUser);
                        return;
                    }
                }

                if (text != null && text.equals("Orqaga") || text != null && text.equals("Asosiy menyu")) {
                    if (tgUser.getPhoneNumber().equals("998974034224")) {
                        SendMessage sendMessage = new SendMessage(id, "Iltimos tanlang\uD83D\uDC47");
                        sendMessage.replyMarkup(createCategoryWithAdminPanelButton());
                        telegramBot.execute(sendMessage);

                        if (tgUser.getState() != State.CATEGORY) {
                            tgUser.setState(State.CATEGORY);
                            tgUserRepository.save(tgUser);
                        }
                        return;
                    } else {
                        SendMessage sendMessage = new SendMessage(id, "Iltimos tanlang\uD83D\uDC47");
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
                        List<Video> videos = videoRepository.findByCategory_IdOrderByMessageIdAsc(category.getId());
                        SendMessage sendMessage = new SendMessage(id, "Videoni tanlang\uD83D\uDC47");
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
                        sendMessage.replyMarkup(new ReplyKeyboardRemove());
                        sendMessage.replyMarkup(createCategoryButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.CHOOSE_CATEGORY);
                        tgUserRepository.save(tgUser);
                        return;
                    } else if (text != null && text.equals("Userlarni ko'rish")) {
                        List<TgUser> all = tgUserRepository.findAll();
                        String string = allUsersString(all);
                        SendMessage sendMessage = new SendMessage(id, string);
                        sendMessage.replyMarkup(new ReplyKeyboardRemove());
                        sendMessage.replyMarkup(getAsosiyMenyu());
                        telegramBot.execute(sendMessage);
                        return;
                    } else if (text != null && text.equals("Category nomi o'zgartirish")) {
                        SendMessage sendMessage = new SendMessage(id, "Tanlang");
                        sendMessage.replyMarkup(createCategoryButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.CHOOSE_EDITING_CATEGORY);
                        tgUserRepository.save(tgUser);
                        return;
                    } else if (text != null && text.equals("Video nomini o'zgartirish")) {
                        SendMessage sendMessage = new SendMessage(id, "Kategoriya tanlang");
                        sendMessage.replyMarkup(createCategoryButton());
                        telegramBot.execute(sendMessage);
                        tgUser.setState(State.VIDEO_RENAMING);
                        tgUserRepository.save(tgUser);
                        return;
                    }

                }

                if (tgUser.getState() == State.VIDEO_RENAMING) {
                    Category category = categoryRepository.findByTitle(text);
                    if (category == null) {
                        telegramBot.execute(new SendMessage(id, "Kategoriya topilmadi"));
                        return;
                    }
                    tgUser.setTempCategoryTitle(category.getTitle());
                    tgUser.setState(State.CHOOSE_VIDEO_FOR_RENAME);
                    tgUserRepository.save(tgUser);

                    List<Video> videos = videoRepository.findByCategory_IdOrderByMessageIdAsc(category.getId());
                    SendMessage sendMessage = new SendMessage(id, "Nomini o‘zgartirmoqchi bo‘lgan videoni tanlang:");
                    sendMessage.replyMarkup(createVideosButton(videos));
                    telegramBot.execute(sendMessage);
                    return;
                }


                if (tgUser.getState() == State.CHOOSE_VIDEO_FOR_RENAME) {
                    Video video = videoRepository.findByTitle(text);
                    if (video == null) {
                        telegramBot.execute(new SendMessage(id, "Video topilmadi"));
                        return;
                    }
                    tgUser.setTempVideoTitle(video.getTitle());
                    tgUser.setState(State.INPUT_NEW_VIDEO_TITLE);
                    tgUserRepository.save(tgUser);

                    SendMessage sendMessage = new SendMessage(id, "Yangi nomni kiriting:");
                    telegramBot.execute(sendMessage);
                    return;
                }

                if (tgUser.getState() == State.INPUT_NEW_VIDEO_TITLE) {
                    Video video = videoRepository.findByTitle(tgUser.getTempVideoTitle());
                    if (video == null) {
                        telegramBot.execute(new SendMessage(id, "Video topilmadi"));
                        return;
                    }
                    video.setTitle(text);
                    videoRepository.save(video);
                    telegramBot.execute(
                            new com.pengrad.telegrambot.request.EditMessageCaption(
                                    video.getChannelId(),
                                    video.getMessageId()
                            ).caption(text)
                    );
                    SendMessage sendMessage = new SendMessage(id, "Video nomi va caption yangilandi ✅");
                    sendMessage.replyMarkup(getAsosiyMenyu());
                    telegramBot.execute(sendMessage);
                    tgUser.setState(State.CATEGORY);
                    tgUserRepository.save(tgUser);
                    return;
                }

                if (tgUser.getState() == State.CHOOSE_EDITING_CATEGORY) {
                    category = categoryRepository.findByTitle(text);
                    SendMessage sendMessage = new SendMessage(
                            id,
                            "Kategoriyaga yangi nom bering"
                    );
                    telegramBot.execute(sendMessage);
                    tgUser.setState(State.SAVE_CATEGORY);
                    tgUserRepository.save(tgUser);
                    return;
                }

                if (tgUser.getState() == State.SAVE_CATEGORY) {
                    Category category1 = category;
                    category1.setTitle(text);
                    categoryRepository.save(category1);
                    SendMessage sendMessage = new SendMessage(
                            id,
                            "O'zgartirildi, asosiy menyu tugmasini bosing"
                    );
                    sendMessage.replyMarkup(getAsosiyMenyu());
                    telegramBot.execute(sendMessage);
                    return;
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
                        Document document = update.message().document();
                        Integer messageId = update.message().forwardFromMessageId();
                        if (document != null) {
                            Video video = new Video();
                            video.setFileName(document.fileName());
                            video.setTitle(document.fileName());
                            video.setChannelId(channelId);
                            video.setMessageId(messageId);
                            Category category = categoryRepository.findByTitle(tgUser.getTempCategoryTitle());
                            video.setCategory(category);
                            videoRepository.save(video);
                            SendMessage sendMessage = new SendMessage(
                                    id,
                                    "Pdf saqlandi tanlang"
                            );
                            sendMessage.replyMarkup(getAsosiyMenyu());
                            telegramBot.execute(sendMessage);
                        } else {
                            Video video = new Video();
                            video.setFileName("");
                            video.setTitle(caption);
                            video.setChannelId(channelId);
                            video.setMessageId(messageId);
                            Category category = categoryRepository.findByTitle(tgUser.getTempCategoryTitle());
                            video.setCategory(category);
                            videoRepository.save(video);
                            SendMessage sendMessage = new SendMessage(
                                    id,
                                    "Video saqlandi tanlang"
                            );
                            sendMessage.replyMarkup(getAsosiyMenyu());
                            telegramBot.execute(sendMessage);
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String allUsersString(List<TgUser> all) {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 1;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMMM, yyyy - HH:mm");

        for (TgUser user : all) {
            stringBuilder.append(i++).append(". ").append(user.getUsername()).append("\n");
            stringBuilder.append("+").append(user.getPhoneNumber()).append("\n");

            if (user.getCreatedAt() != null) {
                stringBuilder.append("Ro'yxatdan o'tgan: ").append(user.getCreatedAt().format(formatter)).append("\n");
            } else {
                stringBuilder.append("Ro'yxatdan o'tgan: noma'lum\n");
            }

            stringBuilder.append("=================================\n");
        }

        return stringBuilder.toString();
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
        replyKeyboardMarkup.addRow(
                new KeyboardButton("Userlarni ko'rish"),
                new KeyboardButton("Category nomi o'zgartirish")
        );
        replyKeyboardMarkup.addRow(
                new KeyboardButton("Video nomini o'zgartirish")
        );
        return replyKeyboardMarkup;
    }

    private Keyboard createVideosButton(List<Video> videos) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup("");
        for (Video video : videos) {
            if (video.getFileName().endsWith(".pdf")) {
                replyKeyboardMarkup.addRow(new KeyboardButton(video.getTitle()));
            }
        }
        for (Video video : videos) {
            if (!video.getFileName().endsWith(".pdf")) {
                replyKeyboardMarkup.addRow(new KeyboardButton(video.getTitle()));
            }
        }
        replyKeyboardMarkup.addRow(new KeyboardButton("Orqaga"));
        replyKeyboardMarkup.resizeKeyboard(true);
        return replyKeyboardMarkup;
    }


    private Keyboard createCategoryButton() {
        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
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