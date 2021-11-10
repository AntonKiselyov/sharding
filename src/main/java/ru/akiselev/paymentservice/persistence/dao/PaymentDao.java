package ru.akiselev.paymentservice.persistence.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.akiselev.paymentservice.entity.Payment;
import ru.akiselev.paymentservice.persistence.shard.ShardManager;

import java.util.Iterator;
import java.util.List;

import static ru.akiselev.paymentservice.persistence.dao.EntityValidator.validate;

@Component
@RequiredArgsConstructor
public class PaymentDao {

    private final ShardManager shardManager;

    public List<Payment> saveAll(final List<Payment> payments) {
        validate(payments);
        final List<Long> ids = shardManager.saveAllInBatch(payments, Payment.class);
        final Iterator<Long> it = ids.iterator();
        for (final Payment payment : payments) {
            if (it.hasNext()) {
                payment.setId(it.next());
            }
        }
        return payments;
    }

    public long sumAmountBySenderId(final long senderId) {
        return shardManager.sum(Payment.class, "amount")
                .where()
                .eq("sender_id", senderId)
                .exec(Long.class);
    }
}
