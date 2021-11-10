package ru.akiselev.paymentservice.utils;

import com.google.common.base.Preconditions;
import ru.akiselev.paymentservice.dto.PaymentDto;
import ru.akiselev.paymentservice.entity.Customer;
import ru.akiselev.paymentservice.entity.Payment;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class PaymentConverter {
    private PaymentConverter() {

    }

    public static List<PaymentDto> toDto(final List<Payment> payments) {
        Preconditions.checkNotNull(payments);
        return payments.stream()
                .map(PaymentConverter::toDto)
                .collect(Collectors.toList());
    }

    public static PaymentDto toDto(final Payment payment) {
        Preconditions.checkNotNull(payment);
        return new PaymentDto(
                payment.getId(),
                payment.getAmount(),
                ofNullable(payment.getSender()).map(Customer::getId).orElse(null),
                ofNullable(payment.getReceiver()).map(Customer::getId).orElse(null)
        );
    }

    public static List<Payment> fromDto(final List<PaymentDto> paymentDtos) {
        Preconditions.checkNotNull(paymentDtos);
        return paymentDtos.stream()
                .map(PaymentConverter::fromDto)
                .collect(Collectors.toList());
    }

    public static Payment fromDto(final PaymentDto paymentDto) {
        Preconditions.checkNotNull(paymentDto);
        return new Payment(
                paymentDto.getId(),
                paymentDto.getAmount(),
                new Customer(paymentDto.getSenderId(), null),
                new Customer(paymentDto.getReceiverId(), null)
        );
    }
}
