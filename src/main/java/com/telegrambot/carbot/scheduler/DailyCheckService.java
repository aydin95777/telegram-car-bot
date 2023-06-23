package com.telegrambot.carbot.scheduler;

import com.telegrambot.carbot.exception.ApiException;
import com.telegrambot.carbot.model.Subscription;
import com.telegrambot.carbot.repository.SubscriptionRepository;
import com.telegrambot.carbot.service.TelegramBotService;
import com.telegrambot.carbot.service.CarService;
import com.telegrambot.carbot.parser.HasznaltautoParser;
import com.telegrambot.carbot.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyCheckService {

    final HasznaltautoParser parser;
    final CarService carService;
    final TelegramBotService telegramBotService;
    final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void runDailyTask() {
        try {
            log.info("Daily check is starting...");
            long chatId;
            List<Subscription> allSubscriptions = subscriptionRepository.findAll();
            Set<Subscription> uniqueSubscriptions = new HashSet<>(allSubscriptions);
            for (Subscription uniqueSubscription : uniqueSubscriptions) {
                chatId = uniqueSubscription.getChatId();
                Set<String> difference = carService.getNewCars(chatId);
                if (!CollectionUtils.isEmpty(difference)) {
                    if (difference.size() > 10) {
                        telegramBotService.sendDocument(chatId, Utils.convertSetToInputStream(difference), "report.txt");
                    } else {
                        for (String link : difference) {
                            telegramBotService.sendMessage(chatId, link);
                        }
                    }
                } else {
                    telegramBotService.sendMessage(chatId, "There are no new cars");
                }
            }
        } catch (Exception e) {
            log.error("Error during daily update: {}", e.getMessage());
            throw new ApiException("Error during daily update: {}", e);
        }
    }
}
