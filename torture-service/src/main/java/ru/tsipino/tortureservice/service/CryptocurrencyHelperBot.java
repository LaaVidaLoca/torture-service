package ru.tsipino.tortureservice.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
public class CryptocurrencyHelperBot extends TelegramLongPollingBot {

  private BotConfig config;
  private ParametersRepository parametersRepository;
  private SubscriptionRepository subscriptionRepository;

  private MsgController controller;

  private CurrencyRequestService currencyRequestService;

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
    commandList.add(new BotCommand("/showLast", "показать последние изменения курсов"));
    commandList.add(new BotCommand("/showMax", "показать максимальный курс"));
    commandList.add(new BotCommand("/showMin", "показать минимальный курс"));
    try {
      execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
    } catch (TelegramApiException e) {

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
        case "/start":
          startCommandExecute(chatId, update.getMessage().getChat().getFirstName());
          // Передаём в kafka chatId, update.getMessage().getChat().getFirstName()
          break;
        case "/subscribe":
          subscribeCommandExecute(chatId);
          break;
        case "/unsubscribe":
          unSubscribeCommandExecute(chatId);
          break;
        case "/showLast":
          sendMessage(chatId, currencyRequestService.getLastCurrencyList(chatId));
          break;
        case "/showMax":
          sendMessage(chatId,currencyRequestService.getMaxCurrency(chatId));
          break;
        case "/showMin":
          break;
        default:
          sendMessage(chatId, "Нет такой команды");
      }
    } else if (update.hasCallbackQuery()) {
      chatId = update.getCallbackQuery().getMessage().getChatId();
      msgId = update.getCallbackQuery().getMessage().getMessageId();
      String data = update.getCallbackQuery().getData();
      // Передаём в kafka chatId, msgId
      CurrencyParameters parameters = parametersRepository.findFirstByType(data).get();
      switch (lastMessage) {
        case "/subscribe":
          saveSubscribe(chatId, parameters);
          break;
        case "/unsubscribe":
          removeSubscribe(chatId, parameters);
          break;
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

  private void saveSubscribe(Long chatId, CurrencyParameters parameters) {
    if (isNotSubscribe(chatId, parameters)) {
      subscriptionRepository.save(
          Subscription.builder().chatId(chatId).parameters(parameters).build());
    } else {
      sendMessage(chatId, "Вы уже подписаны на этот курс");
    }
  }

  private void removeSubscribe(Long chatId, CurrencyParameters parameters) {
    if (!isNotSubscribe(chatId, parameters)) {
      subscriptionRepository.deleteByChatIdAndParameters(chatId, parameters);
    }
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

  private void subscribeCommandExecute(long chatId) {
    SendMessage message =
        new SendMessage(String.valueOf(chatId), "На какую валюту вы хотите подписаться?");
    List<CurrencyParameters> parametersList = parametersRepository.findAll();
    List<String> types = parametersList.stream().map(elem -> elem.getType()).toList();
    message.setReplyMarkup(createMarkup(types));
    try {
      execute(message);
    } catch (TelegramApiException e) {

    }
  }

  private void unSubscribeCommandExecute(long chatId) {
    SendMessage message =
        new SendMessage(String.valueOf(chatId), "От какого курса вы хотите отказаться?");
    List<Subscription> subscriptions = subscriptionRepository.findAllByChatId(chatId);
    if (subscriptions.isEmpty()) {
      message.setText("У вас нет ни одной подписки!");
    } else {
      List<String> types =
          subscriptions.stream().map(elem -> elem.getParameters().getType()).toList();
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
