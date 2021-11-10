package ru.akiselev.paymentservice.persistence.shard;

import java.util.List;

public interface Shard {

    int getId();

    <T, ID> ID insert(final T entity, final Class<T> tClass);

    <T, ID> List<ID> executeQuery(final String query, final Class<T> idClass);

    <T> T selectSingle(final String sqlQuery, final Class<T> tClass);

    <ID, T> List<ID> insertAllInBatch(final List<T> entities, final Class<T> tClass);

    <T> List<T> findAll(final Class<? extends T> tClass, final String s);

    <T> T findOne(final Class<T> tClass, final String s);


}
