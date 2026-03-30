package com.lineage.plsql.parser.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;

/**
 * 表血缘关系模型
 * 用于描述表级别的数据血缘关系，包括源表和目标表的映射信息
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableLineage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 源表名称
     */
    private String sourceTable;

    /**
     * 源表别名（如有）
     */
    private String sourceAlias;

    /**
     * 目标表名称
     */
    private String targetTable;

    /**
     * 目标表别名（如有）
     */
    private String targetAlias;

    /**
     * SQL语句类型：SELECT、INSERT、UPDATE、DELETE、CREATE、ALTER等
     */
    private String sqlType;

    /**
     * 操作类型：READ、WRITE、READ_WRITE
     */
    private String operationType;

    /**
     * SQL语句内容（可能较长）
     */
    private String sqlText;

    /**
     * 是否为视图：true-是视图，false-不是视图
     */
    private Boolean isView;

    /**
     * 视图定义（如果是视图）
     */
    private String viewDefinition;

    /**
     * 所属SQL语句的唯一标识
     */
    private String statementId;

    /**
     * 数据库类型：ORACLE、MYSQL
     */
    private String databaseType;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 无参构造方法
     */
    public TableLineage(String sourceTable, String targetTable, String sqlType) {
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.sqlType = sqlType;
        this.createTime = System.currentTimeMillis();
    }
}
