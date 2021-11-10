package ru.akiselev.paymentservice.persistence;

import com.google.common.base.Verify;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;

public class PersistenceUtils {

    private PersistenceUtils() {

    }

    public static <T> Object getValue(final Field field, final T entity) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot get field value.", e);
        }
    }

    public static <T> Long getLongValue(final Field field, final T entity) {
        try {
            field.setAccessible(true);
            return (Long) field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot get field value.", e);
        }
    }

    public static <T> void setValueIfNull(final Field field, final T entity, final Object value) {
        try {
            field.setAccessible(true);
            if (field.get(entity) == null) return;
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(format("Cannot set field value for %s.", value), e);
        }
    }

    public static String idNameFor(final Class<?> tClass) {
        final Field[] declaredFields = tClass.getDeclaredFields();
        for (final Field field : declaredFields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field.getName();
            }
        }
        throw new RuntimeException(format("Cannot get id name for %s", tClass.getName()));
    }

    public static <T> boolean isAnyFieldNull(final Field field, final List<T> entities) {
        return entities.stream().anyMatch(entity -> isFieldNull(field, entity));
    }

    public static <T> boolean isFieldNull(final Field field, final T entity) {
        try {
            field.setAccessible(true);
            final Object o = field.get(entity);
            return o == null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot extract field.", e);
        }
    }

    public static String tableNameForClass(final Class<?> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            return clazz.getAnnotation(Table.class).name();
        } else {
            return format("%ss", clazz.getSimpleName());
        }
    }

    public static Field idFieldFor(final Class<?> clazz) {
        for (final Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new RuntimeException(format("No id field for %s.", clazz.getName()));
    }

    public static <ID> Class<? extends ID> idTypeFor(final Class<?> tClass) {
        for (final Field field : tClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return (Class<? extends ID>) field.getType();
            }
        }
        throw new RuntimeException(format("Cannot evaluate id type for %s", tClass.getName()));
    }

    public static String columnNameFor(final Field field) {
        final Class<?> type = field.getType();
        if (isEntity(type)) {
            return referenceKeyColumnName(field.getName());
        } else {
            if (field.isAnnotationPresent(Column.class)) {
                return field.getDeclaredAnnotation(Column.class).name();
            } else {
                return field.getName();
            }
        }
    }

    public static <T> Object shardIdValueFor(final Class<? extends T> clazz, final T entity) {
        for (final Field field : clazz.getDeclaredFields()) {
            if (isEntity(field)) {
                return shardIdValueFor(field.getType(), getValue(field, entity));
            } else if (field.isAnnotationPresent(ShardId.class)) {
                return getValue(field, entity);
            }
        }
        throw new RuntimeException(format("No shard id value for %s", clazz));
    }

    public static boolean isShardId(final Class<?> clazz, final String fieldName) {
        for (final Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ShardId.class)) {
                if (isEntity(field) && referenceKeyColumnName(field.getName()).equals(fieldName)) {
                    return true;
                } else if (field.isAnnotationPresent(Id.class) && field.getName().equals(fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String referenceKeyColumnName(final String referenceColumnName) {
        return format("%s_id", referenceColumnName);
    }

    public static Field shardIdFor(final Class<?> clazz) {
        for (final Field field : clazz.getDeclaredFields()) {
            if (isEntity(field)) {
                return shardIdFor(field.getType());
            } else if (field.isAnnotationPresent(ShardId.class)) {
                return field;
            }
        }
        throw new RuntimeException(format("No shardId field. ShardId field is required for %s.", clazz));
    }


    public static <T> void checkIsEntity(final Class<T> tClass, final String msg) {
        if (!isEntity(tClass)) {
            throw new RuntimeException(msg);
        }
    }

    public static boolean isEntity(final Field field) {
        return isEntity(field.getType());
    }

    public static <T> boolean isEntity(final Class<T> tClass) {
        return tClass.isAnnotationPresent(Entity.class);
    }

    public static Constructor<?> findConstructorFor(final Class<?> clazz) {
        final Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        Verify.verify(declaredConstructors.length == 1, "Expected the only 1 constructor for %, but got %d",
                clazz.getSimpleName(), declaredConstructors.length);
        return declaredConstructors[0];
    }

    public static Object extractValueFor(final ResultSet resultSet, final String columnName) {
        try {
            return resultSet.getObject(columnName);
        } catch (SQLException e) {
            throw new RuntimeException(format("Cannot extract value from Result set for column %s", columnName), e);
        }
    }

    public static <T> Class<? extends T> entityClassFor(final Class<T> clazz) {
        if (isEntity(clazz))
            return clazz;
        else
            throw new RuntimeException(format("No entity class for %s.", clazz.getName()));
    }
}
