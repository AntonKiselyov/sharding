package ru.akiselev.paymentservice.persistence.shard;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.akiselev.paymentservice.persistence.PersistenceUtils;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.entityClassFor;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.extractValueFor;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.findConstructorFor;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.getValue;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.idFieldFor;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.idNameFor;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.idTypeFor;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.isAnyFieldNull;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.isEntity;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.isFieldNull;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.referenceKeyColumnName;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.tableNameForClass;

@RequiredArgsConstructor
@Getter
public class ShardServer implements Shard {

    public static final String QUERY_HL = "QUERY: ";

    private final int id;
    private final DataSource dataSource;

    @Override
    public <T, ID> ID insert(final T entity, final Class<T> tClass) {
        final String paramAsStr = collectParametersAsString(tClass, entity);
        final String valuesAsStr = collectValuesAsString(tClass, entity);
        final String query = format("insert into %s(%s) values(%s)", tableNameForClass(tClass), paramAsStr, valuesAsStr);
        return insert(query, tClass);
    }

    private <T, ID> ID insert(final String query, final Class<T> tClass) {
        System.out.println(QUERY_HL + query);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            statement.executeUpdate();
            try (final ResultSet generatedKeys = statement.getGeneratedKeys()) {

                Preconditions.checkState(generatedKeys.next());
                final Class<? extends ID> idType = idTypeFor(tClass);
                return generatedKeys.getObject(idNameFor(tClass), idType);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> String collectParametersAsString(final  Class<T> tClass, final T entity) {
        return stream(tClass.getDeclaredFields())
                .filter(field -> !isFieldNull(field, entity))
                .map(PersistenceUtils::columnNameFor)
                .collect(Collectors.joining(","));
    }

    private <T> String collectValuesAsString(final Class<T> tClass, final T entity) {
        return stream(tClass.getDeclaredFields())
                .filter(field -> !isFieldNull(field, entity))
                .map(field -> {
                    Object value;
                    if (isEntity(field)) {
                        final Object entityValue = getValue(field, entity);
                        final Field idField = idFieldFor(field.getType());
                        value = getValue(idField, entityValue);
                    } else {
                        value = getValue(field, entity);
                    }
                    if (value != null) {
                        if (field.getType().equals(String.class)) {
                            return format("'%s'", value);
                        } else if (field.getType().equals(Boolean.class)) {
                            return String.valueOf((Boolean) value ? 1 : 0);
                        } else {
                            return format("%s", value);
                        }
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    @Override
    public <T, ID> List<ID> executeQuery(final String query, final Class<T> tClass) {
        System.out.println(QUERY_HL + query);
        try (final Connection connection = dataSource.getConnection();
             final Statement statement = connection.createStatement();
             final ResultSet resultSet = statement.executeQuery(query)) {

            final List<ID> ids = newArrayList();
            final String idName = idNameFor(tClass);
            final Class<? extends ID> idType = idTypeFor(tClass);
            while (resultSet.next()) {
                final ID id = resultSet.getObject(idName, idType);
                ids.add(id);
            }
            return ids;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T selectSingle(final String query, final Class<T> tClass) {
        System.out.println(QUERY_HL + query);
        try (final Connection connection = dataSource.getConnection();
             final Statement statement = connection.createStatement();
             final ResultSet resultSet = statement.executeQuery(query)) {

            Preconditions.checkState(resultSet.next());
            return resultSet.getObject(1, tClass);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <ID, T> List<ID> insertAllInBatch(final List<T> entities, final Class<T> tClass) {
        try (final Connection connection = dataSource.getConnection()) {

            final String query = batchInsertFor(tClass, entities);
            System.out.println(QUERY_HL + query);

            try (final PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                final List<Field> fields = getColumnFields(tClass, entities);
                for (final T entity : entities) {
                    for (int i = 0; i < fields.size(); i++) {
                        final Field field = fields.get(i);
                        Object value;
                        if (isEntity(field)) {
                            final Object entityValue = getValue(field, entity);
                            final Field idField = idFieldFor(field.getType());
                            value = getValue(idField, entityValue);
                        } else {
                            value = getValue(field, entity);
                        }
                        if (value != null) {
                            statement.setObject(i + 1, value);
                        }
                    }
                    statement.addBatch();
                }
                statement.executeBatch();

                try (final ResultSet resultSet = statement.getGeneratedKeys()) {
                    final List<ID> ids = newArrayList();
                    final String idName = idNameFor(tClass);
                    final Class<? extends ID> idType = idTypeFor(tClass);
                    while (resultSet.next()) {
                        ids.add(resultSet.getObject(idName, idType));
                    }
                    return ids;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> List<Field> getColumnFields(final Class<?> clazz, final List<? extends T> entities) {
        Preconditions.checkNotNull(entities);
        final List<Field> columnFields = newArrayList();
        for (final Field field : clazz.getDeclaredFields()) {
            if (!isAnyFieldNull(field, entities)) {
                columnFields.add(field);
            }
        }
        return columnFields;
    }

    private String batchInsertFor(final Class<?> tClass, final List<?> entities) {
        final String parameters = String.join(",", collectParametersAsString(tClass, entities));
        final String args = fieldsAsEmptyArgs(tClass, entities);
        return format("insert into %s (%s) values(%s)", tableNameForClass(tClass), parameters, args);
    }

    private String collectParametersAsString(final Class<?> clazz, final List<?> entities) {
        Preconditions.checkNotNull(entities);
        return stream(clazz.getDeclaredFields())
                .filter(field -> !isAnyFieldNull(field, entities))
                .map(PersistenceUtils::columnNameFor)
                .collect(Collectors.joining(","));
    }

    private String fieldsAsEmptyArgs(final Class<?> clazz, final List<?> entities) {
        Preconditions.checkNotNull(entities);
        return stream(clazz.getDeclaredFields())
                .filter(field -> !isAnyFieldNull(field, entities))
                .map(field -> "?")
                .collect(Collectors.joining(","));
    }

    @Override
    public <T> List<T> findAll(final Class<? extends T> tClass, final String condition) {
        final String query = queryForClassWithCondition(tClass, condition);
        System.out.println(QUERY_HL + query);
        try (final Connection connection = dataSource.getConnection();
             final Statement statement = connection.createStatement();
             final ResultSet selected = statement.executeQuery(query)) {

            final List<T> entities = newArrayList();

            try {
                while (selected.next()) {
                    T entity = map(selected, tClass);
                    entities.add(entity);
                }
            } catch (SQLException e) {
                throw new RuntimeException(format(
                        "Error while fetching all data for class %s %s.",
                        tClass.getSimpleName(), condition
                ));
            }
            return entities;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T findOne(final Class<T> tClass, final String condition) {
        final String query = queryForClassWithCondition(tClass, condition);
        System.out.println(QUERY_HL + query);
        try (final Connection connection = dataSource.getConnection();
             final Statement statement = connection.createStatement();
             final ResultSet resultSet = statement.executeQuery(query)) {

            Verify.verify(resultSet.next());
            final T mapped = map(resultSet, tClass);
            Verify.verify(!resultSet.next(), "Expected unique instance, but got more");
            return mapped;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String queryForClassWithCondition(final Class<?> tClass, final String condition) {
        return format("select * from %s %s", tableNameForClass(tClass), condition);
    }

    private <T> T map(final ResultSet resultSet, final Class<? extends T> tClass) {
        final Constructor<?> constructor = findConstructorFor(tClass);
        final Object[] args = extractArgsForConstructor(resultSet, constructor);
        try {
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(format("Cannot create instance of type %s using constructor %s.", tClass, constructor));
        }
    }

    private Object[] extractArgsForConstructor(final ResultSet resultSet, final Constructor<?> constructor) {
        final Parameter[] parameters = constructor.getParameters();
        final Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Preconditions.checkState(parameters[i].isNamePresent(), "Parameter name is not presented. Please use -parameters options for javac.");
            args[i] = extractArgsForConstructorParameters(resultSet, parameters[i]);
        }
        return args;
    }

    private Object extractArgsForConstructorParameters(final ResultSet rs, final Parameter parameter) {
        final Class<?> parameterType = parameter.getType();
        if (isEntity(parameterType)) {
            final String referenceKeyColumnName = referenceKeyColumnName(parameter.getName());
            final Object extracted = extractValueFor(rs, referenceKeyColumnName);
            if (extracted == null) return null;
            final Class<?> entityParameterType = entityClassFor(parameterType);
            return selectSingle(format("select * from %s", tableNameForClass(entityParameterType)), entityParameterType);
        } else {
            final Object extracted = extractValueFor(rs, parameter.getName());
            if (!parameterType.isAssignableFrom(extracted.getClass())) {
                if (extracted instanceof Number) {
                    if (parameterType.equals(int.class)) {
                        return ((Number) extracted).intValue();
                    } else if (parameterType.equals(boolean.class)) {
                        final int extractedAsInt = ((Number) extracted).intValue();
                        return extractedAsInt == 1;
                    } else if (parameterType.equals(long.class)) {
                        return ((Number) extracted).longValue();
                    } else {
                        throw new RuntimeException(
                                format("Cannot cast %s to type %s to parameter %s of type %s",
                                        extracted,
                                        extracted.getClass().getSimpleName(),
                                        parameter.getName(),
                                        parameterType.getSimpleName())
                        );
                    }
                } else {
                    throw new RuntimeException(
                            format(
                                    "Cannot cast %s of type %s to parameter %s of type %s",
                                    extracted,
                                    extracted.getClass().getSimpleName(),
                                    parameter.getName(),
                                    parameterType.getSimpleName()
                            )
                    );
                }
            }
            return extracted;
        }
    }

}
