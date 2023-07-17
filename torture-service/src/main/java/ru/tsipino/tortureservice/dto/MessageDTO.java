package ru.tsipino.tortureservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class MessageDTO implements Serializable {
  private Long chatId;
  private Long messageId;
  private String command;

  @JsonCreator
  public MessageDTO(
      @JsonProperty("chatId") Long chatId,
      @JsonProperty("messageId") Long messageId,
      @JsonProperty("command") String command) {
    this.chatId = chatId;
    this.messageId = messageId;
    this.command = command;
  }
}
