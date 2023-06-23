package com.telegrambot.carbot.config;

import com.telegrambot.carbot.exception.ApiException;
import com.telegrambot.carbot.service.TelegramBotService;
import com.telegrambot.carbot.parser.HasznaltautoParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
public class BotInit {

    final TelegramBotService telegramBotService;
    final HasznaltautoParser parser;

    public BotInit(TelegramBotService telegramBotService, HasznaltautoParser parser) {
        this.telegramBotService = telegramBotService;
        this.parser = parser;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        log.info("Registering telegram bot..");
        TelegramBotsApi telegramBotsApi;
        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBotService);
        } catch (TelegramApiException e) {
            log.error("Error during initializing bot: {}", e.getMessage());
            throw new ApiException("Error during initializing bot: ", e);
        }
    }
}
