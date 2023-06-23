package com.telegrambot.carbot.service;

import com.telegrambot.carbot.model.Car;
import com.telegrambot.carbot.model.Subscription;
import com.telegrambot.carbot.parser.HasznaltautoParser;
import com.telegrambot.carbot.repository.CarRepository;
import com.telegrambot.carbot.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CarService {
    private final CarRepository carRepository;
    private final HasznaltautoParser parser;
    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public CarService(CarRepository carRepository, HasznaltautoParser parser, SubscriptionRepository subscriptionRepository) {
        this.carRepository = carRepository;
        this.parser = parser;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Set<String> getNewCars(long chatId) {
        Set<Subscription> subscriptions = subscriptionRepository.findAllByChatId(chatId);
        String make;
        String model;
        Set<Car> previousCars;
        Set<Car> allCars;
        Set<String> difference = new HashSet<>();
        for (Subscription subscription : subscriptions) {
            make = subscription.getMake();
            model = subscription.getModel();
            previousCars = carRepository.findAllByMakeAndModel(make, model);
            allCars = parser.parse(make, model);
            difference.addAll(calculateDifference(previousCars, allCars));
            carRepository.deleteAll(previousCars);
            carRepository.saveAll(allCars);
        }
        return difference;
    }

    private Set<String> calculateDifference(Set<Car> previousCars, Set<Car> allCars) {
        Set<String> differentLinks = allCars.stream()
                .map(Car::getLink)
                .collect(Collectors.toSet());
        Set<String> previousCarLinks = previousCars.stream()
                .map(Car::getLink)
                .collect(Collectors.toSet());
        differentLinks.removeAll(previousCarLinks);

        return differentLinks;
    }
}
