package ru.tsipino.tortureservice.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tsipino.tortureservice.entity.CurrencyParameters;

@Repository
public interface ParametersRepository extends JpaRepository<CurrencyParameters, Long> {
  Optional<CurrencyParameters> findFirstByType(String type);
}
