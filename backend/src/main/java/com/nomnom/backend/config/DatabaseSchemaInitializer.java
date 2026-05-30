package com.nomnom.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        addColumnIfMissing("dishes", "stock", "INT DEFAULT 999 COMMENT '库存数量'");
        addColumnIfMissing("dishes", "specs", "TEXT COMMENT '菜品规格口味，存储为JSON字符串'");
        addColumnIfMissing("dishes", "is_recommend", "TINYINT(1) DEFAULT 0 COMMENT '是否店长推荐（0：否；1：是）'");
        addColumnIfMissing("dishes", "recommend_ids", "TEXT COMMENT '推荐搭配菜品ID列表，JSON数组格式'");
        addColumnIfMissing("dishes_evaluations", "orders_id", "INT COMMENT '关联订单ID，防止刷评'");
        addColumnIfMissing("dishes_table", "area", "VARCHAR(50) COMMENT '桌台区域兼容字段'");
        addColumnIfMissing("dishes_table", "amount", "DECIMAL(10, 2) DEFAULT 0.00 COMMENT '当前桌台消费总额'");
        addColumnIfMissing("dishes_table", "min_spend", "DECIMAL(10, 2) DEFAULT 0.00 COMMENT '最低消费'");
        addColumnIfMissing("dishes_table", "occupied_by", "INT COMMENT '当前占用用户ID'");
        addColumnIfMissing("orders", "table_id", "INT COMMENT '桌台ID'");
        addColumnIfMissing("orders", "table_number", "VARCHAR(20) COMMENT '桌台号快照'");
        addColumnIfMissing("shopping_car", "dishes_id", "INT COMMENT '菜品ID，冗余字段便于列表展示'");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) throws SQLException {
        if (hasColumn(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private boolean hasColumn(String tableName, String columnName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            if (hasColumn(metadata, catalog, tableName, columnName)) {
                return true;
            }
            return hasColumn(metadata, catalog, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT));
        }
    }

    private boolean hasColumn(DatabaseMetaData metadata, String catalog, String tableName, String columnName)
            throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, null, tableName, columnName)) {
            return columns.next();
        }
    }
}
