package com.happy.learning.zh.auth.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
// JsonTypeHandler.java
public abstract class JsonTypeHandler<T> extends BaseTypeHandler<T> {
    private final Type type;
    private static final ObjectMapper mapper = new ObjectMapper();

    public JsonTypeHandler(Type type) {
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    T parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, mapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON 序列化失败", e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    public T parse(String json) {
        try {
            return mapper.readValue(json, mapper.constructType(type));
        } catch (IOException e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
    }

    // SetTypeHandler.java
    public class SetTypeHandler extends JsonTypeHandler<Set<String>> {
        public SetTypeHandler() {
                super(constructParametricType());
            }

        private static Type constructParametricType() {
            return TypeFactory.defaultInstance()
                    .constructCollectionType(Set.class, String.class);
        }
    }

    // MapTypeHandler.java
    public static class MapTypeHandler extends JsonTypeHandler<Map<String, Object>> {
        public MapTypeHandler() {
                super(constructParametricType());
            }

        private static Type constructParametricType() {
            return TypeFactory.defaultInstance()
                    .constructMapType(Map.class, String.class, Object.class);
        }
    }

}