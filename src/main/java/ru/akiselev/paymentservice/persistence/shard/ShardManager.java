package ru.akiselev.paymentservice.persistence.shard;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.checkIsEntity;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.isShardId;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.tableNameForClass;

public abstract class ShardManager {

    protected final Map<Integer, Shard> shards;

    protected ShardManager(final List<Shard> shardList) {
        Preconditions.checkNotNull(shardList);
        this.shards = shardList.stream().collect(Collectors.toMap(Shard::getId, Function.identity()));
    }

    public abstract <T> int evaluateShardIdFor(final T entity);

    public abstract Shard evaluateShard(final Object id);

    public abstract <T, ID> ID save(final T entity, final Class<T> tClass);

    public abstract <T, ID> List<ID> saveAllInBatch(final List<T> entities, final Class<T> tClass);

    public abstract <T, ID> List<ID> executeQuery(final String query, final Class<T> tClass);

    protected abstract <T> List<T> findAll(final Class<? extends T> tClass, final String condition);

    protected abstract <T> T findOne(final Class<? extends T> entity, final String condition, final Object shardId);

    protected abstract <ID> ID executeQuery(final String condition, final Class<? extends ID> idClass, final Object shardId);

    public <T> SelectQuery<T> find(final Class<? extends T> tClass) {
        checkIsEntity(tClass, format("This is not an entity class: %s.", tClass.getName()));
        return new SelectQuery<>(tClass);
    }

    public <T> SumQuery<T> sum(final Class<T> tClass, final String parameter) {
        checkIsEntity(tClass, format("This is not an entity class: %s.", tClass.getName()));
        return new SumQuery<>(tClass, parameter);
    }

    public class SelectQuery<T> extends BaseQuery<T> implements Query<T> {

        SelectQuery(final Class<? extends T> tClass) {
            super(tClass);
        }

        public SelectQueryCondition where() {
            return new SelectQueryCondition("where ");
        }

        @Override
        public List<T> all() {
            return findAll(tClass, "");
        }

        public class SelectQueryCondition extends BaseQueryCondition {

            private SelectQueryCondition(final String condition) {
                super(condition);
            }

            private SelectQueryCondition(final String condition, final Object shardId) {
                super(condition, shardId);
            }

            public SelectQueryCondition shardId(final Object shardId) {
                return new SelectQueryCondition(condition, shardId);
            }

            public SelectQueryCondition eq(final String parameter, final Object o) {
                final String template;
                if (o instanceof String)
                    template = "%s %s=\"%s\"";
                else
                    template = "%s %s=%s";
                if (isShardId(tClass, parameter)) {
                    return new SelectQueryCondition(format(template, condition, parameter, o), o);
                } else {
                    return new SelectQueryCondition(format(template, condition, parameter, o), shardId);
                }
            }

            public T one() {
                return findOne(tClass, condition, shardId);
            }

            public List<T> list() {
                return findAll(tClass, condition);
            }
        }
    }

    public class SumQuery<T> extends BaseQuery<T> {
        private final String query;

        public SumQuery(final Class<? extends T> tClass, final String parameter) {
            super(tClass);
            this.query = format("select sum(%s) from %s", parameter, tableNameForClass(tClass));
        }

        public SumQueryCondition where() {
            return new SumQueryCondition(format("%s %s", query, "where "));
        }

        public class SumQueryCondition extends BaseQueryCondition {

            private SumQueryCondition(final String condition) {
                super(condition);
            }

            private SumQueryCondition(final String condition, final Object shardId) {
                super(condition, shardId);
            }

            public SumQueryCondition shardId(final long shardId) {
                return new SumQueryCondition(this.condition, shardId);
            }

            public SumQueryCondition eq(final String parameter, final Object o) {
                final String template;
                if (o instanceof String)
                    template = "%s %s=\"%s\"";
                else
                    template = "%s %s=%s";
                if (isShardId(tClass, parameter)) {
                    return new SumQueryCondition(format(template, condition, parameter, o), o);
                } else {
                    return new SumQueryCondition(format(template, condition, parameter, o, shardId));
                }
            }

            public <R> R exec(final Class<? extends R> resType) {
                return executeQuery(condition, resType, shardId);
            }
        }
    }

}

interface Query<T> {
    List<T> all();
}

abstract class BaseQuery<T> {

    protected final Class<? extends T> tClass;

    protected BaseQuery(final Class<? extends T> tClass) {
        this.tClass = tClass;
    }
}

abstract class BaseQueryCondition {

    protected final String condition;
    protected Object shardId;

    protected BaseQueryCondition(final String condition) {
        this.condition = condition;
    }

    protected BaseQueryCondition(final String condition, final Object shardId) {
        this.condition = condition;
        this.shardId = shardId;
    }
}