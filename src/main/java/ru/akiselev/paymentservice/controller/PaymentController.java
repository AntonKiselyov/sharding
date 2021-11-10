package ru.akiselev.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.akiselev.paymentservice.dto.PaymentDto;
import ru.akiselev.paymentservice.service.PaymentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @RequestMapping(path = "/payments", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PaymentDto> savePayments(final @RequestBody List<PaymentDto> paymentDtos) {
        return paymentService.savePayments(paymentDtos);
    }

    @RequestMapping(path = "/payments/amount", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public long getAmountBySenderId(final @RequestParam("senderId") Long senderId) {
        return paymentService.getAmountBySenderId(senderId);
    }
}
