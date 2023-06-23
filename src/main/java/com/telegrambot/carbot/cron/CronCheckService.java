package com.telegrambot.carbot.cron;

import com.telegrambot.carbot.exception.ApiException;
import com.telegrambot.carbot.model.Subscription;
import com.telegrambot.carbot.parser.HasznaltautoParser;
import com.telegrambot.carbot.repository.SubscriptionRepository;
import com.telegrambot.carbot.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class CronCheckService {

    final HasznaltautoParser parser;
    final TelegramBotService telegramBotService;
    final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void runDailyTask() {
        try {
            log.info("Daily check is starting...");
            List<Subscription> allSubscriptions = subscriptionRepository.findAll();
            Set<Subscription> uniqueSubscriptions = new HashSet<>(allSubscriptions);
            telegramBotService.processDifference(uniqueSubscriptions);
        } catch (Exception e) {
            log.error("Error during daily update: {}", e.getMessage());
            throw new ApiException("Error during daily update: {}", e);
        }
    }
}
