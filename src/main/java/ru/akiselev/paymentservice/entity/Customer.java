package ru.akiselev.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.akiselev.paymentservice.persistence.ShardId;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "customer")
@AllArgsConstructor
@Getter
public class Customer {

    @Id
    @Setter
    @ShardId
    private Long id;

    @NotNull(message = "Customer name can't be null")
    @Size(max = 100, message = "Customer name can't be more than 100 symbols")
    private final String name;

    public static Customer of(final Long id) {
        return new Customer(id, null);
    }
}
