package com.telegrambot.carbot.repository;

import com.telegrambot.carbot.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Repository
@Transactional
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    void deleteByChatIdAndMakeAndModel(long chatId, String make, String model);

    Set<Subscription> findAllByChatId(long chatId);
}
