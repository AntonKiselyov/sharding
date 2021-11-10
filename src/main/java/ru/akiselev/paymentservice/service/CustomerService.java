package ru.akiselev.paymentservice.service;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.akiselev.paymentservice.dto.CustomerDto;
import ru.akiselev.paymentservice.entity.Customer;
import ru.akiselev.paymentservice.persistence.dao.CustomerDao;
import ru.akiselev.paymentservice.utils.CustomerConverter;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerDao customerDao;

    public CustomerDto saveCustomer(final CustomerDto customerDto) {
        Preconditions.checkNotNull(customerDto);
        final Customer customer = CustomerConverter.fromDto(customerDto);
        final Customer savedCustomer = customerDao.save(customer);
        return CustomerConverter.toDto(savedCustomer);
    }

    public List<Long> getAllCustomerIds() {
        return customerDao.findAllIds();
    }

    public List<CustomerDto> saveAllCustomers(final List<CustomerDto> customerDtos) {
        Preconditions.checkNotNull(customerDtos);
        final List<Customer> customers = CustomerConverter.fromDto(customerDtos);
        final List<Customer> savedCustomers = customerDao.saveAllInBatch(customers);
        return CustomerConverter.toDto(savedCustomers);
    }

    public List<CustomerDto> getAllCustomer() {
        final List<Customer> customers = customerDao.findAll();
        return CustomerConverter.toDto(customers);
    }

    public CustomerDto getCustomer(final long id) {
        final Customer customer = customerDao.findOne(id);
        return CustomerConverter.toDto(customer);
    }
}
