package ru.tsipino.tortureservice.service;

import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.tsipino.tortureservice.entity.Currency;
import ru.tsipino.tortureservice.entity.CurrencyParameters;
import ru.tsipino.tortureservice.entity.Subscription;
import ru.tsipino.tortureservice.repository.CurrencyRepository;
import ru.tsipino.tortureservice.repository.ParametersRepository;
import ru.tsipino.tortureservice.repository.SubscriptionRepository;

@Component
@AllArgsConstructor
public class CurrencyRequestService {
  private CurrencyRepository currencyRepository;
  private ParametersRepository parametersRepository;
  private SubscriptionRepository subscriptionRepository;

  private static final int NUMBER_LAST_REQUEST_ROWS = 10;

  private List<Currency> cropCurrencyList(List<Currency> currencyList) {

    return currencyList.subList(
        0,
        currencyList.size() < NUMBER_LAST_REQUEST_ROWS
            ? currencyList.size()
            : NUMBER_LAST_REQUEST_ROWS);
  }

  private List<CurrencyParameters> getParametersList(Long chatId) {
    List<Subscription> subscriptionList = subscriptionRepository.findAllByChatId(chatId);
    List<CurrencyParameters> parametersList =
        subscriptionList.stream().map(elem -> elem.getParameters()).toList();
    return parametersList;
  }

  public String getLastCurrencyList(Long chatId) {

    StringBuilder sb = new StringBuilder();
    getParametersList(chatId)
        .forEach(
            parameters -> {
              sb.append("Изменения курса ").append(parameters.getType()).append(":").append("\n");
              cropCurrencyList(currencyRepository.findAllByParameters(parameters))
                  .forEach(currency -> sb.append(currency));
            });
    return sb.toString();
  }

  public String getMaxCurrency(Long chatId) {
    StringBuilder sb = new StringBuilder();
    getParametersList(chatId)
        .forEach(
            parameters -> {
              sb.append("Максимальное значание ")
                  .append(parameters.getType())
                  .append(":")
                  .append("\n");
              Currency currency =
                  (currencyRepository.findAllByParameters(parameters))
                      .stream().max(Comparator.comparingDouble(elem -> elem.getValue())).get();
              sb.append(currency);
            });
    sb.toString();
    return sb.toString();
  }

  public String getMinCurrency(Long chatId) {
    StringBuilder sb = new StringBuilder();
    getParametersList(chatId)
        .forEach(
            parameters -> {
              sb.append("Минимальное значение ")
                  .append(parameters.getType())
                  .append(":")
                  .append("\n");
              Currency currency =
                  (currencyRepository.findAllByParameters(parameters))
                      .stream().min(Comparator.comparingDouble(elem -> elem.getValue())).get();
              sb.append(currency);
            });
    sb.toString();
    return sb.toString();
  }
}
