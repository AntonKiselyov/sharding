package ru.akiselev.paymentservice.persistence.shard;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static ru.akiselev.paymentservice.persistence.PersistenceUtils.shardIdValueFor;

public class HashShardManager extends ShardManager {

    public HashShardManager(final List<Shard> shards) {
        super(shards);
    }

    @Override
    public <T> int evaluateShardIdFor(final T entity) {
        final Object shardIdValue = shardIdValueFor(entity.getClass(), entity);
        final int hashKey = HashGen.hashKey(shardIdValue);
        return evaluateShardId(hashKey);
    }

    @Override
    public Shard evaluateShard(final Object id) {
        final int hashKey = HashGen.hashKey(id);
        final int shardId = evaluateShardId(hashKey);
        return shards.get(shardId);
    }

    private int evaluateShardId(final int id) {
        final var shardCount = shards.size();
        final var hash = id % shardCount;
        return hash == 0 ? hash + shardCount : hash;
    }

    @Override
    public <T, ID> ID save(final T entity, final Class<T> tClass) {
        final Object shardIdValue = shardIdValueFor(tClass, entity);
        final Shard shard = evaluateShard(shardIdValue);
        return shard.insert(entity, tClass);
    }

    @Override
    public <T, ID> List<ID> saveAllInBatch(final List<T> entities, final Class<T> tClass) {
        final List<ID> ids = newArrayList();
        final Map<Integer, List<T>> elementMap = entities.stream().collect(groupingBy(this::evaluateShardIdFor));
        for (final Map.Entry<Integer, List<T>> entry : elementMap.entrySet()) {
            final Shard shardServer = shards.get(entry.getKey());
            ids.addAll(shardServer.insertAllInBatch(entry.getValue(), tClass));
        }
        return ids;
    }

    @Override
    public <T, ID> List<ID> executeQuery(final String query, final Class<T> tClass) {
        final List<ID> result = newArrayList();
        for (final Shard shard : shards.values()) {
            final List<ID> elements = shard.executeQuery(query, tClass);
            result.addAll(elements);
        }
        return result;
    }

    @Override
    protected  <T> List<T> findAll(final Class<? extends T> tClass, final String s) {
        final List<T> selected = newArrayList();
        for (final Shard shard : shards.values()) {
            final List<T> portion = shard.findAll(tClass, s);
            selected.addAll(portion);
        }
        return selected;
    }

    @Override
    protected  <T> T findOne(final Class<? extends T> tClass, final String s, final Object shardId) {
        Preconditions.checkNotNull(shardId, format("Shard id cannot be null for %s.", tClass.getName()));
        final Shard shard = evaluateShard(shardId);
        return shard.findOne(tClass, s);
    }

    @Override
    protected <ID> ID executeQuery(final String query, final Class<? extends ID> idClass, final Object shardId) {
        Preconditions.checkNotNull(shardId, "Shard id cannot be null.");
        final Shard shard = evaluateShard(shardId);
        return shard.selectSingle(query, idClass);
    }

    static class HashGen {
        public static int hashKey(final Object o) {
            return o.hashCode();
        }
    }
}
