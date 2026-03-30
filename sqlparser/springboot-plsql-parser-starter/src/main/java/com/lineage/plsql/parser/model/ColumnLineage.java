package com.lineage.plsql.parser.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;

/**
 * 字段血缘关系模型
 * 用于描述字段级别的数据血缘关系，包括源字段和目标字段的映射信息
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnLineage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源表名称
     */
    private String sourceTable;

    /**
     * 源字段名称
     */
    private String sourceColumn;

    /**
     * 目标表名称
     */
    private String targetTable;

    /**
     * 目标字段名称
     */
    private String targetColumn;

    /**
     * 转换表达式（如有）
     * 例如: NVL(T2.ACCT_CODE,T2.DLER_CODE)
     */
    private String transformExpression;

    /**
     * SQL语句类型：SELECT、INSERT、UPDATE、DELETE、CREATE、ALTER等
     */
    private String sqlType;

    /**
     * 血缘方向：IN（输入）、OUT（输出）
     */
    private String direction;

    /**
     * 所属SQL语句的唯一标识
     */
    private String statementId;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 无参构造方法
     */
    public ColumnLineage(String sourceTable, String sourceColumn, String targetTable, String targetColumn) {
        this.sourceTable = sourceTable;
        this.sourceColumn = sourceColumn;
        this.targetTable = targetTable;
        this.targetColumn = targetColumn;
        this.createTime = System.currentTimeMillis();
    }
}
