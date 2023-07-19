package ru.tsipino.tortureservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tsipino.tortureservice.dto.MessageDTO;

@Component
@RestController
@RequestMapping("request")
public class MsgController {
  public MsgController(KafkaTemplate<Long, MessageDTO> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Autowired private KafkaTemplate<Long, MessageDTO> kafkaTemplate;

  @PostMapping
  public void sendOrder(Long msgId, MessageDTO msg) {
    kafkaTemplate.send("request", msgId, msg);
  }
}
