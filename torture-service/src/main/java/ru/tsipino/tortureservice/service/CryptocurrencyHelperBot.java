package ru.tsipino.tortureservice.service;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.tsipino.tortureservice.config.BotConfig;
import ru.tsipino.tortureservice.controller.MsgController;
import ru.tsipino.tortureservice.dto.MessageDTO;
import ru.tsipino.tortureservice.entity.CurrencyParameters;
import ru.tsipino.tortureservice.entity.Subscription;
import ru.tsipino.tortureservice.repository.ParametersRepository;
import ru.tsipino.tortureservice.repository.SubscriptionRepository;

@Component
@Slf4j
public class CryptocurrencyHelperBot extends TelegramLongPollingBot {

  private final BotConfig config;
  private final ParametersRepository parametersRepository;
  private final SubscriptionRepository subscriptionRepository;

  private final MsgController controller;

  private final CurrencyRequestService currencyRequestService;

  private String lastMessage;

  public CryptocurrencyHelperBot(
      BotConfig config,
      ParametersRepository parametersRepository,
      SubscriptionRepository subscriptionRepository,
      MsgController controller,
      CurrencyRequestService currencyRequestService) {
    this.config = config;
    this.parametersRepository = parametersRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.controller = controller;
    this.currencyRequestService = currencyRequestService;
    List<BotCommand> commandList = new ArrayList<>();
    commandList.add(new BotCommand("/start", "начало взаимодействия"));
    commandList.add(new BotCommand("/subscribe", "подписатья на курс криптовалюты"));
    commandList.add(new BotCommand("/unsubscribe", "отписаться от курса"));
    commandList.add(new BotCommand("/last", "показать последние изменения курсов"));
    commandList.add(new BotCommand("/max", "показать максимальный курс"));
    commandList.add(new BotCommand("/min", "показать минимальный курс"));

    try {
      execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
    } catch (TelegramApiException e) {
      log.error("Меню некоректно");
    }
  }

  @Override
  public String getBotUsername() {
    return config.getBotName();
  }

  @Override
  public String getBotToken() {
    return config.getToken();
  }

  @Override
  public void onUpdateReceived(Update update) {
    long chatId = 0L;
    long msgId = 0L;
    if (update.hasMessage() && update.getMessage().hasText()) {
      String messageText = update.getMessage().getText();
      chatId = update.getMessage().getChatId();
      msgId = update.getMessage().getMessageId();
      lastMessage = messageText;
      switch (messageText) {
        case "/start" -> startCommandExecute(chatId, update.getMessage().getChat().getFirstName());
        case "/subscribe" -> subscribeCommandExecute(chatId);
        case "/unsubscribe" -> unSubscribeCommandExecute(chatId);
        case "/last" -> sendMessage(chatId, currencyRequestService.getLastCurrencyList(chatId));
        case "/max" -> sendMessage(chatId, currencyRequestService.getMaxCurrency(chatId));
        case "/min" -> sendMessage(chatId, currencyRequestService.getMinCurrency(chatId));
        default -> sendMessage(chatId, "Нет такой команды");
      }
    } else if (update.hasCallbackQuery()) {
      chatId = update.getCallbackQuery().getMessage().getChatId();
      msgId = update.getCallbackQuery().getMessage().getMessageId();
      String data = update.getCallbackQuery().getData();
      CurrencyParameters parameters = parametersRepository.findFirstByType(data).get();
      String requestMsgText ="";
      switch (lastMessage) {
        case "/subscribe":
          requestMsgText = saveSubscribe(chatId, parameters) ?  "Подписка оформлена" : "Вы уже подписаны";
          break;
        case "/unsubscribe":
          requestMsgText = removeSubscribe(chatId, parameters) ? "Подписка отключена" : "Вы уже отключили подписку";
          break;
      }
      EditMessageText message = new EditMessageText();
      message.setChatId(chatId);
      message.setMessageId((int)msgId);
      message.setText(requestMsgText);
      try {
        execute(message);
      } catch (TelegramApiException e) {

      }
    }
    if ((update.hasMessage() && update.getMessage().hasText()) || update.hasCallbackQuery()) {
      MessageDTO msg = new MessageDTO(chatId, msgId, lastMessage);
      controller.sendOrder(1L, msg);
    }
  }

  private void startCommandExecute(long chatId, String userName) {
    String answer = "Привет, " + userName;
    sendMessage(chatId, answer);
  }

  private boolean isNotSubscribe(Long chatId, CurrencyParameters parameters) {
    return subscriptionRepository.findFirstByChatIdAndParameters(chatId, parameters).isEmpty();
  }

  private boolean saveSubscribe(Long chatId, CurrencyParameters parameters) {
    if (isNotSubscribe(chatId, parameters)) {
      subscriptionRepository.save(
          Subscription.builder().chatId(chatId).parameters(parameters).build());
          return true;
    }
    return false;
  }

  private boolean removeSubscribe(Long chatId, CurrencyParameters parameters) {
    if (!isNotSubscribe(chatId, parameters)) {
      subscriptionRepository.deleteByChatIdAndParameters(chatId, parameters);
      return true;
    }
    return false;
  }

  private InlineKeyboardMarkup createMarkup(List<String> buttonText) {
    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
    buttonText.forEach(
        elem -> {
          List<InlineKeyboardButton> buttonRow = new ArrayList<>();
          InlineKeyboardButton button = new InlineKeyboardButton();
          button.setText(elem);
          button.setCallbackData(elem);
          buttonRow.add(button);
          buttons.add(buttonRow);
        });
    markup.setKeyboard(buttons);
    return markup;
  }

  private List<String> getUserSubscriptionsTypes(long chatId) {
    List<Subscription> subscriptions = subscriptionRepository.findAllByChatId(chatId);
    if (subscriptions.isEmpty()) {
      return null;
    } else {
      return subscriptions.stream().map(elem -> elem.getParameters().getType()).toList();
    }
  }

  private void subscribeCommandExecute(long chatId) {
    SendMessage message =
        new SendMessage(String.valueOf(chatId), "На какую валюту вы хотите подписаться?");
    List<CurrencyParameters> parametersList = parametersRepository.findAll();
    List<String> types = new ArrayList<>(parametersList.stream().map(elem -> elem.getType()).toList());
    if (getUserSubscriptionsTypes(chatId) != null) {
      types.removeAll(getUserSubscriptionsTypes(chatId));
    }
    if (types.isEmpty()) {
      message.setText("Вы уже подписаны на все курсы!");
    } else {
      System.out.println("3");
      message.setReplyMarkup(createMarkup(types));
    }
    try {
      execute(message);
    } catch (TelegramApiException e) {

    }
  }

  private void unSubscribeCommandExecute(long chatId) {
    SendMessage message =
        new SendMessage(String.valueOf(chatId), "От какого курса вы хотите отказаться?");
      List<String> types = getUserSubscriptionsTypes(chatId);
      if (types == null) {
        message.setText("У вас нет ни одной подписки!");
      } else {
        message.setReplyMarkup(createMarkup(types));
      }

    try {
      execute(message);
    } catch (TelegramApiException e) {

    }
  }

  private void sendMessage(long chatId, String sendMessage) {
    SendMessage message = new SendMessage();
    message.setChatId(String.valueOf(chatId));
    message.setText(sendMessage);
    try {
      execute(message);
    } catch (TelegramApiException e) {

    }
  }
}
