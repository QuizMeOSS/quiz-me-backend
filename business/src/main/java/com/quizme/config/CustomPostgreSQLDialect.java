package com.quizme.config;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

public class CustomPostgreSQLDialect extends PostgreSQLDialect {

    @Override
    public JdbcType resolveSqlTypeDescriptor(
            String columnTypeName,
            int jdbcTypeCode,
            int precision,
            int scale,
            JdbcTypeRegistry jdbcTypeRegistry) {

        // hibernate validation fails to resolve "citext" datatype, so we need to explicitly tell it
        // to map it to varchar
        if ("citext".equalsIgnoreCase(columnTypeName)) {
            return jdbcTypeRegistry.getDescriptor(Types.VARCHAR);
        }

        return super.resolveSqlTypeDescriptor(
                columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry);
    }
}