package com.example;

import com.example.annotation.Column;
import com.example.annotation.Id;
import com.example.annotation.Table;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Session {
    private final DataSource dataSource;
    private final Map<EntityKey, Object> entityCacheMap;
    private final Map<EntityKey, Map<String, Object>> entitySnapshotMap;

    public Session(DataSource dataSource) {
        this.dataSource = dataSource;
        entityCacheMap = new HashMap<>();
        entitySnapshotMap = new HashMap<>();
    }

    @SneakyThrows
    public <T> T find(Class<T> entityType, Object id) {
        EntityKey entityKey = new EntityKey(id, entityType);
        if (entityCacheMap.containsKey(entityKey)) {
            return entityType.cast(entityCacheMap.get(entityKey));
        }
        try (Connection connection = dataSource.getConnection()) {
            String tableName = getTableName(entityType);
            String idColumnName = getColumnName(getIdField(entityType));
            String selectByIdQuery = "SELECT * FROM %s WHERE %s = ?" .formatted(tableName, idColumnName);
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectByIdQuery)) {
                preparedStatement.setObject(1, id);
                ResultSet resultSet = preparedStatement.executeQuery();
                T entity = mapResultSetToEntity(resultSet, entityType);
                entityCacheMap.put(entityKey, entity);
                entitySnapshotMap.put(entityKey, createFieldNameToValueMap(entity));
                return entity;
            }
        }
    }

    @SneakyThrows
    private static <T> Map<String, Object> createFieldNameToValueMap(T entity) {
        Map<String, Object> map = new HashMap<>();
        for (Field field : entity.getClass().getDeclaredFields()) {
            Object value = field.get(entity);
            map.put(field.getName(), value);
        }
        return map;
    }

    @SneakyThrows
    private <T> T mapResultSetToEntity(ResultSet resultSet, Class<T> entityType) {
        Field[] declaredFields = entityType.getDeclaredFields();
        T entityInstance = entityType.getConstructor().newInstance();
        for (Field field : declaredFields) {
            String name = getColumnName(field);
            Object value = resultSet.getObject(name, field.getType());
            field.setAccessible(true);
            field.set(entityInstance, value);
        }
        return entityInstance;
    }

    private static String getColumnName(Field field) {
        return Optional.ofNullable(field.getAnnotation(Column.class))
                .map(Column::value)
                .orElseGet(field::getName);
    }

    private static <T> Field getIdField(Class<T> entityType) {
        return Arrays.stream(entityType.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow();
    }

    private static <T> String getTableName(Class<T> entityType) {
        return Optional.ofNullable(entityType.getAnnotation(Table.class))
                .map(Table::value)
                .orElseGet(entityType::getName);
    }

    public void close() {
        entitySnapshotMap.keySet()
                .forEach(this::doEntityDirtyCheck);
    }

    @SneakyThrows
    private void doEntityDirtyCheck(EntityKey k) {
        Map<String, Object> columnsToUpdate = new HashMap<>();
        if (entityCacheMap.containsKey(k)) {
            Object entityInstance = entityCacheMap.get(k);
            Field[] declaredFields = entityInstance.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                Object fieldValue = field.get(entityInstance);
                Object fieldSnapshotValue = entitySnapshotMap.get(k).get(field.getName());
                if (fieldValue != fieldSnapshotValue) {
                    columnsToUpdate.put(getColumnName(field), fieldValue);
                }
            }
            if (!columnsToUpdate.isEmpty()) {
                update(entityInstance, columnsToUpdate);
            }
        }
    }

    @SneakyThrows
    private void update(Object entityInstance, Map<String, Object> columnsToUpdate) {
        try (Connection connection = dataSource.getConnection()) {
            Class<?> entityType = entityInstance.getClass();
            String tableName = getTableName(entityType);
            String idColumnName = getColumnName(getIdField(entityType));
            String setClauseString = mapToSetClauseString(columnsToUpdate);
            String updateByIdQuery = "UPDATE %s SET %s WHERE %s = " .formatted(tableName, setClauseString, idColumnName);
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateByIdQuery)) {
                preparedStatement.setObject(1, getIdField(entityType).get(entityInstance));
                preparedStatement.executeUpdate();
            }
        }
    }

    private static String mapToSetClauseString(Map<String, Object> columnsToUpdate) {
        return columnsToUpdate.entrySet()
                .stream().map(e -> "%s = %s" .formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
    }
}
