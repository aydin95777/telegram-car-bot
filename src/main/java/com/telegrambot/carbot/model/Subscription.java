package com.telegrambot.carbot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode
@Data
@Entity
@IdClass(SubscriptionId.class)
public class Subscription {

    @Id
    long chatId;
    @Id
    String make;
    @Id
    String model;

}
