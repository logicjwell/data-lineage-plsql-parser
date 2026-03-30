-- =====================================================
-- 数据血缘表结构 - MySQL版本
-- 适用于MySQL 5.7及以上版本
-- 创建时间: 2026-03-29
-- =====================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS data_lineage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE data_lineage;

-- 删除已存在的表（如果需要重建）
-- DROP TABLE IF EXISTS table_lineage;
-- DROP TABLE IF EXISTS column_lineage;

-- 创建表血缘关系表
CREATE TABLE table_lineage (
    -- 主键ID
    id              BIGINT(20) NOT NULL AUTO_INCREMENT,
    -- 源表名称
    source_table    VARCHAR(200) NOT NULL COMMENT '源表名称',
    -- 源表别名
    source_alias    VARCHAR(100) COMMENT '源表别名',
    -- 目标表名称
    target_table    VARCHAR(200) NOT NULL COMMENT '目标表名称',
    -- 目标表别名
    target_alias    VARCHAR(100) COMMENT '目标表别名',
    -- SQL类型: SELECT/INSERT/UPDATE/DELETE/CREATE/ALTER
    sql_type        VARCHAR(50) NOT NULL COMMENT 'SQL类型',
    -- 操作类型: READ/WRITE/READ_WRITE
    operation_type  VARCHAR(50) COMMENT '操作类型',
    -- SQL文本内容
    sql_text        LONGTEXT COMMENT 'SQL文本内容',
    -- 是否为视图: 1-是, 0-否
    is_view         TINYINT(1) DEFAULT 0 COMMENT '是否为视图: 1-是, 0-否',
    -- 视图定义
    view_definition LONGTEXT COMMENT '视图定义',
    -- 语句唯一标识
    statement_id    VARCHAR(100) COMMENT '语句唯一标识',
    -- 数据库类型
    database_type   VARCHAR(50) DEFAULT 'MYSQL' COMMENT '数据库类型',
    -- 创建时间
    create_time     BIGINT(20) DEFAULT UNIX_TIMESTAMP() COMMENT '创建时间',
    -- 备注
    remark          VARCHAR(500) COMMENT '备注',
    -- 状态: 1-有效, 0-无效
    status          TINYINT(1) DEFAULT 1 COMMENT '状态: 1-有效, 0-无效',
    -- 分区字段
    partition_date  VARCHAR(8) COMMENT '分区日期',
    -- 租户字段
    tenant_id       VARCHAR(50) COMMENT '租户ID',
    PRIMARY KEY (id),
    -- 索引
    INDEX idx_source_table (source_table),
    INDEX idx_target_table (target_table),
    INDEX idx_sql_type (sql_type),
    INDEX idx_create_time (create_time),
    INDEX idx_statement_id (statement_id),
    INDEX idx_partition_date (partition_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表血缘关系表';

-- 创建字段血缘关系表
CREATE TABLE column_lineage (
    -- 主键ID
    id                    BIGINT(20) NOT NULL AUTO_INCREMENT,
    -- 源表名称
    source_table          VARCHAR(200) NOT NULL COMMENT '源表名称',
    -- 源字段名称
    source_column         VARCHAR(200) NOT NULL COMMENT '源字段名称',
    -- 目标表名称
    target_table          VARCHAR(200) NOT NULL COMMENT '目标表名称',
    -- 目标字段名称
    target_column         VARCHAR(200) NOT NULL COMMENT '目标字段名称',
    -- 转换表达式
    transform_expression  VARCHAR(1000) COMMENT '转换表达式',
    -- SQL类型
    sql_type              VARCHAR(50) COMMENT 'SQL类型',
    -- 血缘方向: IN/OUT
    direction             VARCHAR(10) COMMENT '血缘方向: IN/OUT',
    -- 语句唯一标识
    statement_id          VARCHAR(100) COMMENT '语句唯一标识',
    -- 备注
    remark                VARCHAR(500) COMMENT '备注',
    -- 创建时间
    create_time           BIGINT(20) DEFAULT UNIX_TIMESTAMP() COMMENT '创建时间',
    -- 状态: 1-有效, 0-无效
    status                TINYINT(1) DEFAULT 1 COMMENT '状态: 1-有效, 0-无效',
    -- 分区字段
    partition_date        VARCHAR(8) COMMENT '分区日期',
    -- 租户字段
    tenant_id             VARCHAR(50) COMMENT '租户ID',
    -- 关联的表血缘ID
    table_lineage_id      BIGINT(20) COMMENT '关联的表血缘ID',
    PRIMARY KEY (id),
    -- 索引
    INDEX idx_source (source_table, source_column),
    INDEX idx_target (target_table, target_column),
    INDEX idx_table_lineage_id (table_lineage_id),
    INDEX idx_create_time (create_time),
    INDEX idx_statement_id (statement_id),
    -- 外键约束
    CONSTRAINT fk_column_table_lineage FOREIGN KEY (table_lineage_id)
        REFERENCES table_lineage(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字段血缘关系表';

-- =====================================================
-- 视图定义 - 方便查询血缘关系
-- =====================================================

-- 表血缘关系视图
CREATE OR REPLACE VIEW v_table_lineage AS
SELECT
    id,
    source_table,
    source_alias,
    target_table,
    target_alias,
    sql_type,
    operation_type,
    is_view,
    statement_id,
    database_type,
    FROM_UNIXTIME(create_time) AS create_time_str,
    remark
FROM table_lineage
WHERE status = 1;

-- 字段血缘关系视图
CREATE OR REPLACE VIEW v_column_lineage AS
SELECT
    c.id,
    c.source_table,
    c.source_column,
    c.target_table,
    c.target_column,
    c.transform_expression,
    c.sql_type,
    c.direction,
    c.statement_id,
    c.table_lineage_id,
    t.source_table AS parent_source_table,
    t.target_table AS parent_target_table,
    FROM_UNIXTIME(c.create_time) AS create_time_str,
    c.remark
FROM column_lineage c
LEFT JOIN table_lineage t ON c.table_lineage_id = t.id
WHERE c.status = 1;

-- 完整的血缘链路视图（表级别）
CREATE OR REPLACE VIEW v_lineage_chain AS
SELECT
    tl.id AS lineage_id,
    tl.source_table,
    tl.target_table,
    tl.sql_type,
    tl.operation_type,
    COUNT(DISTINCT cl.id) AS column_count,
    tl.statement_id,
    FROM_UNIXTIME(tl.create_time) AS create_time_str
FROM table_lineage tl
LEFT JOIN column_lineage cl ON tl.id = cl.table_lineage_id
WHERE tl.status = 1
GROUP BY tl.id, tl.source_table, tl.target_table, tl.sql_type, tl.operation_type, tl.statement_id, tl.create_time;

-- 创建时间分区表（可选，用于大数据量场景）
-- ALTER TABLE table_lineage PARTITION BY RANGE (partition_date) (
--     PARTITION p_202601 VALUES LESS THAN ('20260201'),
--     PARTITION p_202602 VALUES LESS THAN ('20260301'),
--     PARTITION p_202603 VALUES LESS THAN ('20260401'),
--     PARTITION p_max VALUES LESS THAN MAXVALUE
-- );
