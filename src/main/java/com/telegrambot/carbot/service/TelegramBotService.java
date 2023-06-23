package com.telegrambot.carbot.service;

import com.telegrambot.carbot.config.BotConfiguration;
import com.telegrambot.carbot.exception.ApiException;
import com.telegrambot.carbot.model.Subscription;
import com.telegrambot.carbot.parser.HasznaltautoParser;
import com.telegrambot.carbot.repository.CarRepository;
import com.telegrambot.carbot.repository.SubscriptionRepository;
import com.telegrambot.carbot.util.Utils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@AllArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    final BotConfiguration botConfiguration;
    final HasznaltautoParser parser;
    final CarService carService;
    final CarRepository carRepository;
    final SubscriptionRepository subscriptionRepository;

    @Override
    public String getBotUsername() {
        return botConfiguration.getUserName();
    }

    @Override
    public String getBotToken() {
        return botConfiguration.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                switch (text) {
                    case "/start" -> {
                        sendMessage(chatId, "Hi, " + update.getMessage().getFrom().getFirstName());
                        sendMessage(chatId, "For subscrubing use /subsribe make model (e.g. /subscribe hyundai ix_35)");
                        sendMessage(chatId, "For unsubscrubing use /unsubsribe make model (e.g. /unsubscribe hyundai ix_35)");
                    }
                    case "/info" -> sendMessage(chatId, "I check hasznaltauto.hu");
                    case "/difference" -> processDifference(chatId);
                    case "/status" -> processStatus(chatId);
                    case "/clear" -> carRepository.deleteAll();
                }

                if (text.contains("/subscribe")) {
                    processSubscription(chatId, text);
                }

                if (text.contains("/unsubscribe")) {
                    processUnsubscription(chatId, text);
                }
            }
        } catch (Exception e) {
            log.error("Error during processing query: {}", e.getMessage());
            throw new ApiException("Error during processing query: {}", e);
        }
    }

    private void processStatus(long chatId) {
        Set<Subscription> subscriptions = subscriptionRepository.findAllByChatId(chatId);
        if (!CollectionUtils.isEmpty(subscriptions)) {
            subscriptions.forEach(subscription -> sendMessage(chatId,
                    "Your subscription: " + subscription.getMake() + " " + subscription.getModel()));
        } else {
            sendMessage(chatId, "You do not have a subscription");
        }
    }

    public void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error during sending message: {}", e.getMessage());
            throw new ApiException("Error during sending message: ", e);
        }
    }

    public void sendDocument(long chatId, InputStream inputStream, String fileName) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(inputStream, fileName));
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            log.error("Error during sending document: {}", e.getMessage());
            throw new ApiException("Error during sending document: ", e);
        }
    }

    private void processDifference(long chatId) {
        sendMessage(chatId, "Process is started");
        Set<String> difference = carService.getNewCars(chatId);
        if (!CollectionUtils.isEmpty(difference)) {
            if (difference.size() > 10) {
                sendDocument(chatId, Utils.convertSetToInputStream(difference), "difference.txt");
            } else {
                difference.forEach(link -> sendMessage(chatId, link));
            }
        } else {
            sendMessage(chatId, "There are no new cars");
        }
    }

    private void processSubscription(long chatId, String text) {
        String[] input = text.split(" ");
        String make = Objects.requireNonNull(input)[1];
        String model = input[2];
        Subscription subscription = new Subscription();
        subscription.setChatId(chatId);
        subscription.setMake(make);
        subscription.setModel(model);
        Set<Subscription> subscriptions = subscriptionRepository.findAllByChatId(chatId);
        if (subscriptions.contains(subscription)) {
            sendMessage(chatId, "You have already subscribed for this car");
            return;
        }
        subscriptionRepository.save(subscription);
        sendMessage(chatId, "You have been successfully subscribed");
    }

    private void processUnsubscription(long chatId, String text) {
        String[] input = text.split(" ");
        String make = Objects.requireNonNull(input)[1];
        String model = input[2];
        subscriptionRepository.deleteByChatIdAndMakeAndModel(chatId, make, model);
        sendMessage(chatId, "You have been successfully unsubscribed");
    }
}
