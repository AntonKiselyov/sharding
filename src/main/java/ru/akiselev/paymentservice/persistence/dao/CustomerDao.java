package ru.akiselev.paymentservice.persistence.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.akiselev.paymentservice.entity.Customer;
import ru.akiselev.paymentservice.persistence.generator.HashKeyGenerator;
import ru.akiselev.paymentservice.persistence.shard.ShardManager;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.setValueIfNull;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.shardIdFor;
import static ru.akiselev.paymentservice.persistence.dao.EntityValidator.validate;

@RequiredArgsConstructor
@Component
public class CustomerDao {

    private final ShardManager shardManager;
    private final HashKeyGenerator hashKeyGenerator;

    public Customer save(final Customer customer) {
        final Field shardField = shardIdFor(Customer.class);
        setValueIfNull(shardField, customer, hashKeyGenerator.next());
        validate(customer);
        final long id = shardManager.save(customer, Customer.class);
        customer.setId(id);
        return customer;
    }

    public List<Customer> saveAllInBatch(final List<Customer> customers) {
        for (final Customer customer : customers) {
            final Field shardField = shardIdFor(Customer.class);
            setValueIfNull(shardField, customer, hashKeyGenerator.next());
            validate(customer);
        }
        final List<Long> ids = shardManager.saveAllInBatch(customers, Customer.class);
        final Iterator<Long> it = ids.iterator();
        for (final Customer customer : customers) {
            if (it.hasNext()) {
                final Long id = it.next();
                customer.setId(id);
            }
        }
        return customers;
    }

    public List<Long> findAllIds() {
        return shardManager.executeQuery("select c.id from customer as c", Customer.class);
    }

    public List<Customer> findAll() {
        return shardManager.find(Customer.class)
                .all()
                .stream()
                .sorted(comparing(Customer::getId))
                .collect(toList());
    }

    public Customer findOne(final long id) {
        return shardManager.find(Customer.class)
                .where()
                .eq("id", id)
                .one();
    }
}
