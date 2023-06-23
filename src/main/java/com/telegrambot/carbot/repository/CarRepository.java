package com.telegrambot.carbot.repository;

import com.telegrambot.carbot.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Repository
@Transactional
public interface CarRepository extends JpaRepository<Car, Long> {

    Set<Car> findAllByMakeAndModel(String make, String model);
}
