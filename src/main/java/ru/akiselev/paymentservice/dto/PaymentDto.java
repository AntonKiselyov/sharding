package ru.akiselev.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PaymentDto {

    private final Long id;

    private final Integer amount;

    private final Long senderId;

    private final Long receiverId;

}
