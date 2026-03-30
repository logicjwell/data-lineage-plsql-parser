package com.lineage.plsql.parser.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL血缘解析结果模型
 * 用于封装SQL语句解析后的完整血缘关系结果
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SqlLineageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * SQL语句类型：SELECT、INSERT、UPDATE、DELETE、CREATE、ALTER、CREATE_VIEW等
     */
    private String sqlType;

    /**
     * 原始SQL语句
     */
    private String originalSql;

    /**
     * 表血缘关系列表
     */
    @Builder.Default
    private List<TableLineage> tableLineages = new ArrayList<>();

    /**
     * 字段血缘关系列表
     */
    @Builder.Default
    private List<ColumnLineage> columnLineages = new ArrayList<>();

    /**
     * 是否解析成功
     */
    @Builder.Default
    private Boolean parseSuccess = true;

    /**
     * 错误信息（如果解析失败）
     */
    private String errorMessage;

    /**
     * SQL语句的唯一标识
     */
    private String statementId;

    /**
     * 解析耗时（毫秒）
     */
    private Long parseTime;

    /**
     * 数据库类型：ORACLE、MYSQL
     */
    private String databaseType;

    /**
     * 源表列表（去重）
     */
    private List<String> sourceTables;

    /**
     * 目标表列表（去重）
     */
    private List<String> targetTables;

    /**
     * 添加表血缘关系
     *
     * @param tableLineage 表血缘关系
     */
    public void addTableLineage(TableLineage tableLineage) {
        if (this.tableLineages == null) {
            this.tableLineages = new ArrayList<>();
        }
        this.tableLineages.add(tableLineage);
    }

    /**
     * 添加字段血缘关系
     *
     * @param columnLineage 字段血缘关系
     */
    public void addColumnLineage(ColumnLineage columnLineage) {
        if (this.columnLineages == null) {
            this.columnLineages = new ArrayList<>();
        }
        this.columnLineages.add(columnLineage);
    }

    /**
     * 获取源表名称列表
     *
     * @return 源表名称列表
     */
    public List<String> getSourceTableNames() {
        List<String> tables = new ArrayList<>();
        if (tableLineages != null) {
            for (TableLineage tl : tableLineages) {
                if (tl.getSourceTable() != null && !tables.contains(tl.getSourceTable())) {
                    tables.add(tl.getSourceTable());
                }
            }
        }
        return tables;
    }

    /**
     * 获取目标表名称列表
     *
     * @return 目标表名称列表
     */
    public List<String> getTargetTableNames() {
        List<String> tables = new ArrayList<>();
        if (tableLineages != null) {
            for (TableLineage tl : tableLineages) {
                if (tl.getTargetTable() != null && !tables.contains(tl.getTargetTable())) {
                    tables.add(tl.getTargetTable());
                }
            }
        }
        return tables;
    }

    /**
     * 设置解析失败状态
     *
     * @param errorMessage 错误信息
     */
    public void setParseFailed(String errorMessage) {
        this.parseSuccess = false;
        this.errorMessage = errorMessage;
    }

    /**
     * 判断是否解析成功
     *
     * @return true-成功，false-失败
     */
    public boolean isSuccess() {
        return this.parseSuccess != null && this.parseSuccess;
    }
}
