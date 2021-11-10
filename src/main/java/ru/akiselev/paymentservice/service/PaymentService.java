package ru.akiselev.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.akiselev.paymentservice.dto.PaymentDto;
import ru.akiselev.paymentservice.entity.Payment;
import ru.akiselev.paymentservice.persistence.dao.PaymentDao;
import ru.akiselev.paymentservice.utils.PaymentConverter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentDao paymentDao;
    private final CustomerService customerService;

    public List<PaymentDto> savePayments(final List<PaymentDto> paymentDtos) {
        if (isEmpty(paymentDtos)) {
            return emptyList();
        }
        final Set<Long> receiverIds = paymentDtos.stream()
                .map(PaymentDto::getReceiverId)
                .collect(Collectors.toSet());

        final List<Long> customerIds = customerService.getAllCustomerIds();
        customerIds.forEach(receiverIds::remove);
        if (!receiverIds.isEmpty()) {
            throw new IllegalArgumentException("Receiver IDs are not valid: " + receiverIds);
        }

        final List<Payment> payments = PaymentConverter.fromDto(paymentDtos);
        final List<Payment> savedPayments = paymentDao.saveAll(payments);
        return PaymentConverter.toDto(savedPayments);
    }

    public long getAmountBySenderId(long senderId) {
        return paymentDao.sumAmountBySenderId(senderId);
    }
}
