package com.telegrambot.carbot.service;

import com.telegrambot.carbot.config.BotConfiguration;
import com.telegrambot.carbot.exception.ApiException;
import com.telegrambot.carbot.model.Car;
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
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    final BotConfiguration botConfiguration;
    final HasznaltautoParser parser;
    final SubscriptionRepository subscriptionRepository;
    final CarRepository carRepository;

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

                if (text.contains("/subscribe")) {
                    subscribe(chatId, text);
                    return;
                }

                if (text.contains("/unsubscribe")) {
                    unsubscribe(chatId, text);
                    return;
                }

                switch (text) {
                    case "/start" -> {
                        sendMessage(chatId, "Hi, " + update.getMessage().getFrom().getFirstName());
                        sendMessage(chatId, "Use /subsribe make model (e.g. /subscribe hyundai ix_35) to start receiving new advertisements");
                        sendMessage(chatId, "Use /unsubsribe make model (e.g. /unsubscribe hyundai ix_35) to stop receiving new advertisements");
                        sendMessage(chatId, "Use /status to get your subscription status");
                        sendMessage(chatId, "Use /difference to get new cars now");
                        return;
                    }
                    case "/info" -> {
                        sendMessage(chatId, "I check hasznaltauto.hu");
                        return;
                    }
                    case "/difference" -> {
                        sendMessage(chatId, "Process is started");
                        Set<Subscription> subscriptions = subscriptionRepository.findAllByChatId(chatId);
                        processDifference(subscriptions);
                        return;
                    }
                    case "/status" -> {
                        getStatus(chatId);
                        return;
                    }
                    default -> {
                        sendMessage(chatId, "Unknown command");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during executing command: {}", e.getMessage());
            throw new ApiException("Error during executing command: {}", e);
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

    public void processDifference(Set<Subscription> subscriptions) {
        HashMap<Subscription, Set<Car>> previousCarsForEachChat = new HashMap<>();
        for (Subscription subscription : subscriptions) {
            Set<Car> previousCars = carRepository.findAllByMakeAndModel(subscription.getMake(), subscription.getModel());
            previousCarsForEachChat.put(subscription, previousCars);
        }
        subscriptions.forEach(subscription ->
                calculateDifference(subscription.getChatId(), subscription.getMake(), subscription.getModel(),
                        previousCarsForEachChat.get(subscription)));
    }

    private void calculateDifference(long chatId, String make, String model, Set<Car> previousCars) {
        var actualCars = parser.parse(make, model);
        actualCars.removeAll(previousCars);
        var difference = actualCars.stream()
                .map(Car::getLink)
                .collect(Collectors.toSet());

        if (!CollectionUtils.isEmpty(difference)) {
            carRepository.saveAll(actualCars);
            if (difference.size() > 10) {
                sendDocument(chatId, Utils.convertSetToInputStream(difference), make + " " + model + ".txt");
            } else {
                sendMessage(chatId, "For " + make + " " + model + ": ");
                difference.forEach(link -> sendMessage(chatId, link));
            }
        } else {
            sendMessage(chatId, "No new cars for " + make + " " + model);
        }
    }

    private void subscribe(long chatId, String text) {
        String[] carInfo = text.split(" ");
        String make = Objects.requireNonNull(carInfo)[1];
        String model = carInfo[2];
        Subscription subscription = new Subscription(chatId, make, model);
        Set<Subscription> subscriptions = subscriptionRepository.findAllByChatId(chatId);
        if (subscriptions.contains(subscription)) {
            sendMessage(chatId, "You subscribed to current car before");
            return;
        }
        subscriptionRepository.save(subscription);
        sendMessage(chatId, "You have successfully subscribed");
        Set<Car> previousCars = carRepository.findAllByMakeAndModel(make, model);
        if (CollectionUtils.isEmpty(previousCars)) {
            calculateDifference(chatId, make, model, previousCars);
        } else {
            var difference = previousCars.stream()
                    .map(Car::getLink)
                    .collect(Collectors.toSet());
            sendMessage(chatId, "Sending available cars");
            sendDocument(chatId, Utils.convertSetToInputStream(difference), make + " " + model + ".txt");
        }
    }

    private void unsubscribe(long chatId, String text) {
        String[] carInfo = text.split(" ");
        subscriptionRepository.deleteByChatIdAndMakeAndModel(chatId, Objects.requireNonNull(carInfo)[1], carInfo[2]);
        sendMessage(chatId, "You have successfully unsubscribed");
    }

    private void getStatus(long chatId) {
        Set<Subscription> subscriptions = subscriptionRepository.findAllByChatId(chatId);
        if (!CollectionUtils.isEmpty(subscriptions)) {
            subscriptions.forEach(subscription -> sendMessage(chatId,
                    "Your subscription: " + subscription.getMake() + " " + subscription.getModel()));
        } else {
            sendMessage(chatId, "You do not have a subscription");
        }
    }
}
