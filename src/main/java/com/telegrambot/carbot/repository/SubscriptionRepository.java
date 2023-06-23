package com.telegrambot.carbot.repository;

import com.telegrambot.carbot.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    void deleteByChatIdAndMakeAndModel(long chatId, String make, String model);

    Set<Subscription> findAllByChatId(long chatId);
}
