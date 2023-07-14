package ru.tsipino.tortureservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsipino.tortureservice.entity.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {}
