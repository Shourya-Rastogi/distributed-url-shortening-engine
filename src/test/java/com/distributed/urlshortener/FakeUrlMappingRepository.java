package com.distributed.urlshortener;

import com.distributed.urlshortener.domain.entity.UrlMapping;
import com.distributed.urlshortener.repository.UrlMappingRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory test double implementation of UrlMappingRepository.
 * Provides ultra-fast zero-overhead execution without bytecode proxying.
 */
public class FakeUrlMappingRepository implements UrlMappingRepository {

    private final Map<String, UrlMapping> shortCodeIndex = new ConcurrentHashMap<>();
    private final Map<Long, UrlMapping> idIndex = new ConcurrentHashMap<>();
    private long idSeq = 1;

    @Override
    public Optional<UrlMapping> findByShortCode(String shortCode) {
        return Optional.ofNullable(shortCodeIndex.get(shortCode));
    }

    @Override
    public boolean existsByShortCode(String shortCode) {
        return shortCodeIndex.containsKey(shortCode);
    }

    @Override
    public int incrementClickCount(String shortCode) {
        UrlMapping mapping = shortCodeIndex.get(shortCode);
        if (mapping != null) {
            mapping.setTotalClicks(mapping.getTotalClicks() + 1);
            return 1;
        }
        return 0;
    }

    @Override
    public int deactivateExpiredUrls(Instant now) {
        int count = 0;
        for (UrlMapping mapping : shortCodeIndex.values()) {
            if (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(now) && mapping.isActive()) {
                mapping.setActive(false);
                count++;
            }
        }
        return count;
    }

    @Override
    public <S extends UrlMapping> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idSeq++);
        }
        shortCodeIndex.put(entity.getShortCode(), entity);
        idIndex.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends UrlMapping> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S e : entities) {
            result.add(save(e));
        }
        return result;
    }

    @Override
    public Optional<UrlMapping> findById(Long id) {
        return Optional.ofNullable(idIndex.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return idIndex.containsKey(id);
    }

    @Override
    public List<UrlMapping> findAll() {
        return new ArrayList<>(shortCodeIndex.values());
    }

    @Override
    public List<UrlMapping> findAllById(Iterable<Long> ids) {
        List<UrlMapping> list = new ArrayList<>();
        for (Long id : ids) {
            if (idIndex.containsKey(id)) list.add(idIndex.get(id));
        }
        return list;
    }

    @Override
    public long count() {
        return shortCodeIndex.size();
    }

    @Override
    public void deleteById(Long id) {
        UrlMapping m = idIndex.remove(id);
        if (m != null) shortCodeIndex.remove(m.getShortCode());
    }

    @Override
    public void delete(UrlMapping entity) {
        if (entity != null) {
            shortCodeIndex.remove(entity.getShortCode());
            if (entity.getId() != null) idIndex.remove(entity.getId());
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends UrlMapping> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        shortCodeIndex.clear();
        idIndex.clear();
    }

    @Override
    public void flush() {}

    @Override
    public <S extends UrlMapping> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends UrlMapping> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<UrlMapping> entities) {
        deleteAll(entities);
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        deleteAllById(ids);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public UrlMapping getOne(Long id) {
        return idIndex.get(id);
    }

    @Override
    public UrlMapping getById(Long id) {
        return idIndex.get(id);
    }

    @Override
    public UrlMapping getReferenceById(Long id) {
        return idIndex.get(id);
    }

    @Override
    public <S extends UrlMapping> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends UrlMapping> List<S> findAll(Example<S> example) {
        return Collections.emptyList();
    }

    @Override
    public <S extends UrlMapping> List<S> findAll(Example<S> example, Sort sort) {
        return Collections.emptyList();
    }

    @Override
    public <S extends UrlMapping> Page<S> findAll(Example<S> example, Pageable pageable) {
        return Page.empty();
    }

    @Override
    public <S extends UrlMapping> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends UrlMapping> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends UrlMapping, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public List<UrlMapping> findAll(Sort sort) {
        return findAll();
    }

    @Override
    public Page<UrlMapping> findAll(Pageable pageable) {
        return Page.empty();
    }
}
