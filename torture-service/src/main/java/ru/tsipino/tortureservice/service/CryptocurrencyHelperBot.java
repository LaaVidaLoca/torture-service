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
import ru.tsipino.tortureservice.entity.CurrencyParameters;
import ru.tsipino.tortureservice.entity.Subscription;
import ru.tsipino.tortureservice.repository.ParametersRepository;
import ru.tsipino.tortureservice.repository.SubscriptionRepository;

@Component
public class CryptocurrencyHelperBot extends TelegramLongPollingBot {

  private BotConfig config;
  private ParametersRepository parametersRepository;
  private SubscriptionRepository subscriptionRepository;

  private String lastMessage;

  public CryptocurrencyHelperBot(
      BotConfig config,
      ParametersRepository parametersRepository,
      SubscriptionRepository subscriptionRepository) {
    this.config = config;
    this.parametersRepository = parametersRepository;
    this.subscriptionRepository = subscriptionRepository;
    List<BotCommand> commandList = new ArrayList<>();
    commandList.add(new BotCommand("/start", "начало взаимодействия"));
    commandList.add(new BotCommand("/subscribe", "подписатья на курс криптовалюты"));
    commandList.add(new BotCommand("/unsubscribe", "отписаться от курса"));
    commandList.add(new BotCommand("/show", "показать..."));
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
    if (update.hasMessage() && update.getMessage().hasText()) {
      String messageText = update.getMessage().getText();
      long chatId = update.getMessage().getChatId();
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
        case "/show":
          sendMessage(chatId, "Отправить запрос");
          break;
        default:
          sendMessage(chatId, "Нет такой команды");
      }
    } else if (update.hasCallbackQuery()) {
      long chatId = update.getCallbackQuery().getMessage().getChatId();
      long msgId = update.getCallbackQuery().getMessage().getMessageId();
      String data = update.getCallbackQuery().getData();
      // Передаём в kafka chatId, msgId, data
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
