-- =====================================================
-- 数据血缘表结构 - Oracle版本
-- 适用于Oracle 11g及以上版本
-- 创建时间: 2026-03-29
-- =====================================================

-- 删除已存在的序列和表（如果需要重建）
-- DROP TABLE data_lineage.table_lineage CASCADE CONSTRAINTS;
-- DROP TABLE data_lineage.column_lineage CASCADE CONSTRAINTS;
-- DROP SEQUENCE data_lineage.seq_table_lineage;
-- DROP SEQUENCE data_lineage.seq_column_lineage;

-- 创建表血缘关系表
CREATE TABLE data_lineage.table_lineage (
    -- 主键ID
    id              NUMBER(20) NOT NULL,
    -- 源表名称
    source_table    VARCHAR2(200) NOT NULL,
    -- 源表别名
    source_alias    VARCHAR2(100),
    -- 目标表名称
    target_table    VARCHAR2(200) NOT NULL,
    -- 目标表别名
    target_alias    VARCHAR2(100),
    -- SQL类型: SELECT/INSERT/UPDATE/DELETE/CREATE/ALTER
    sql_type        VARCHAR2(50) NOT NULL,
    -- 操作类型: READ/WRITE/READ_WRITE
    operation_type  VARCHAR2(50),
    -- SQL文本内容
    sql_text        CLOB,
    -- 是否为视图: 1-是, 0-否
    is_view         CHAR(1) DEFAULT '0',
    -- 视图定义
    view_definition CLOB,
    -- 语句唯一标识
    statement_id    VARCHAR2(100),
    -- 数据库类型
    database_type   VARCHAR2(50) DEFAULT 'ORACLE',
    -- 创建时间
    create_time     NUMBER(20) DEFAULT TO_NUMBER(TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')),
    -- 备注
    remark          VARCHAR2(500),
    -- 状态: 1-有效, 0-无效
    status          CHAR(1) DEFAULT '1',
    -- 分区字段
    partition_date  VARCHAR2(8),
    -- 租户字段
    tenant_id       VARCHAR2(50),
    CONSTRAINT pk_table_lineage PRIMARY KEY (id)
) TABLESPACE DATA_LINEAGE_TBS
PARTITION BY RANGE (partition_date) (
    PARTITION P_DEFAULT VALUES LESS THAN ('99999999')
);

-- 创建字段血缘关系表
CREATE TABLE data_lineage.column_lineage (
    -- 主键ID
    id                    NUMBER(20) NOT NULL,
    -- 源表名称
    source_table          VARCHAR2(200) NOT NULL,
    -- 源字段名称
    source_column         VARCHAR2(200) NOT NULL,
    -- 目标表名称
    target_table          VARCHAR2(200) NOT NULL,
    -- 目标字段名称
    target_column         VARCHAR2(200) NOT NULL,
    -- 转换表达式
    transform_expression  VARCHAR2(1000),
    -- SQL类型
    sql_type              VARCHAR2(50),
    -- 血缘方向: IN/OUT
    direction             VARCHAR2(10),
    -- 语句唯一标识
    statement_id          VARCHAR2(100),
    -- 备注
    remark                VARCHAR2(500),
    -- 创建时间
    create_time           NUMBER(20) DEFAULT TO_NUMBER(TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')),
    -- 状态: 1-有效, 0-无效
    status                CHAR(1) DEFAULT '1',
    -- 分区字段
    partition_date        VARCHAR2(8),
    -- 租户字段
    tenant_id             VARCHAR2(50),
    -- 关联的表血缘ID
    table_lineage_id      NUMBER(20),
    CONSTRAINT pk_column_lineage PRIMARY KEY (id),
    CONSTRAINT fk_column_table_lineage FOREIGN KEY (table_lineage_id)
        REFERENCES data_lineage.table_lineage(id) ON DELETE CASCADE
) TABLESPACE DATA_LINEAGE_TBS
PARTITION BY RANGE (partition_date) (
    PARTITION P_DEFAULT VALUES LESS THAN ('99999999')
);

-- 创建序列
CREATE SEQUENCE data_lineage.seq_table_lineage
    START WITH 1
    INCREMENT BY 1
    NOMAXVALUE
    NOCYCLE
    NOCACHE;

CREATE SEQUENCE data_lineage.seq_column_lineage
    START WITH 1
    INCREMENT BY 1
    NOMAXVALUE
    NOCYCLE
    NOCACHE;

-- 创建索引
CREATE INDEX idx_table_source ON data_lineage.table_lineage(source_table);
CREATE INDEX idx_table_target ON data_lineage.table_lineage(target_table);
CREATE INDEX idx_table_sql_type ON data_lineage.table_lineage(sql_type);
CREATE INDEX idx_table_create_time ON data_lineage.table_lineage(create_time);
CREATE INDEX idx_table_statement_id ON data_lineage.table_lineage(statement_id);

CREATE INDEX idx_column_source ON data_lineage.column_lineage(source_table, source_column);
CREATE INDEX idx_column_target ON data_lineage.column_lineage(target_table, target_column);
CREATE INDEX idx_column_table_lineage_id ON data_lineage.column_lineage(table_lineage_id);
CREATE INDEX idx_column_create_time ON data_lineage.column_lineage(create_time);

-- 添加注释
COMMENT ON TABLE data_lineage.table_lineage IS '表血缘关系表';
COMMENT ON COLUMN data_lineage.table_lineage.id IS '主键ID';
COMMENT ON COLUMN data_lineage.table_lineage.source_table IS '源表名称';
COMMENT ON COLUMN data_lineage.table_lineage.source_alias IS '源表别名';
COMMENT ON COLUMN data_lineage.table_lineage.target_table IS '目标表名称';
COMMENT ON COLUMN data_lineage.table_lineage.target_alias IS '目标表别名';
COMMENT ON COLUMN data_lineage.table_lineage.sql_type IS 'SQL类型: SELECT/INSERT/UPDATE/DELETE/CREATE/ALTER';
COMMENT ON COLUMN data_lineage.table_lineage.operation_type IS '操作类型: READ/WRITE/READ_WRITE';
COMMENT ON COLUMN data_lineage.table_lineage.sql_text IS 'SQL文本内容';
COMMENT ON COLUMN data_lineage.table_lineage.is_view IS '是否为视图: 1-是, 0-否';
COMMENT ON COLUMN data_lineage.table_lineage.view_definition IS '视图定义';
COMMENT ON COLUMN data_lineage.table_lineage.statement_id IS '语句唯一标识';
COMMENT ON COLUMN data_lineage.table_lineage.database_type IS '数据库类型';
COMMENT ON COLUMN data_lineage.table_lineage.create_time IS '创建时间';
COMMENT ON COLUMN data_lineage.table_lineage.remark IS '备注';

COMMENT ON TABLE data_lineage.column_lineage IS '字段血缘关系表';
COMMENT ON COLUMN data_lineage.column_lineage.id IS '主键ID';
COMMENT ON COLUMN data_lineage.column_lineage.source_table IS '源表名称';
COMMENT ON COLUMN data_lineage.column_lineage.source_column IS '源字段名称';
COMMENT ON COLUMN data_lineage.column_lineage.target_table IS '目标表名称';
COMMENT ON COLUMN data_lineage.column_lineage.target_column IS '目标字段名称';
COMMENT ON COLUMN data_lineage.column_lineage.transform_expression IS '转换表达式';
COMMENT ON COLUMN data_lineage.column_lineage.sql_type IS 'SQL类型';
COMMENT ON COLUMN data_lineage.column_lineage.direction IS '血缘方向: IN/OUT';
COMMENT ON COLUMN data_lineage.column_lineage.statement_id IS '语句唯一标识';
COMMENT ON COLUMN data_lineage.column_lineage.table_lineage_id IS '关联的表血缘ID';
COMMENT ON COLUMN data_lineage.column_lineage.create_time IS '创建时间';
COMMENT ON COLUMN data_lineage.column_lineage.remark IS '备注';
