package ru.akiselev.paymentservice.utils;

import com.google.common.base.Preconditions;
import ru.akiselev.paymentservice.dto.CustomerDto;
import ru.akiselev.paymentservice.entity.Customer;

import java.util.List;
import java.util.stream.Collectors;

public class CustomerConverter {
    private CustomerConverter() {
    }

    public static Customer fromDto(final CustomerDto customerDto) {
        Preconditions.checkNotNull(customerDto);
        return new Customer(customerDto.getId(), customerDto.getName());
    }

    public static List<CustomerDto> toDto(final List<Customer> customers) {
        Preconditions.checkNotNull(customers);
        return customers.stream()
                .map(CustomerConverter::toDto)
                .collect(Collectors.toList());
    }

    public static CustomerDto toDto(final Customer customer) {
        Preconditions.checkNotNull(customer);
        return new CustomerDto(
                customer.getId(),
                customer.getName()
        );
    }

    public static List<Customer> fromDto(final List<CustomerDto> customerDtos) {
        Preconditions.checkNotNull(customerDtos);
        return customerDtos.stream()
                .map(CustomerConverter::fromDto)
                .collect(Collectors.toList());
    }
}
