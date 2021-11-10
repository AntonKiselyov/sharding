package ru.akiselev.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.akiselev.paymentservice.persistence.ShardId;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "payment")
@AllArgsConstructor
@Getter
public class Payment {

    @Id
    @Setter
    private Long id;

    private final Integer amount;

    @ShardId
    private final Customer sender;

    private final Customer receiver;

}
