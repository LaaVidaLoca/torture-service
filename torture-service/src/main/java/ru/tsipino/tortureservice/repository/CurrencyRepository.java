package ru.tsipino.tortureservice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsipino.tortureservice.entity.Currency;
import ru.tsipino.tortureservice.entity.CurrencyParameters;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
  List<Currency> findAllByParameters(CurrencyParameters parameters);
}
