package ru.tsipino.tortureservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "currencies")
public class Currency {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @CreationTimestamp private LocalDateTime createdAt;
  private Double value;
  @ManyToOne private CurrencyParameters parameters;

  @Override
  public String toString() {
    return "Курс "
        + parameters.getType()
        + " на "
        + createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        + " - "
        + value
        + "\n";
  }
}
