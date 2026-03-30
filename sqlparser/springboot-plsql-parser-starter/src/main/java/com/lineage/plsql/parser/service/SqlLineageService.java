package com.lineage.plsql.parser.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lineage.plsql.parser.model.ColumnLineage;
import com.lineage.plsql.parser.model.SqlLineageResult;
import com.lineage.plsql.parser.model.TableLineage;
import com.lineage.plsql.parser.visitor.PlSqlLineageVisitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/**
 * SQL血缘解析服务
 * 提供SQL血缘关系解析的核心业务逻辑
 *
 * <p>主要功能：
 * <ul>
 *   <li>解析单条SQL语句的血缘关系</li>
 *   <li>批量解析多条SQL语句的血缘关系</li>
 *   <li>提取表级别血缘关系</li>
 *   <li>提取字段级别血缘关系</li>
 * </ul>
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
@Slf4j
@Service
public class SqlLineageService {

    /**
     * 解析单条SQL语句的血缘关系
     *
     * @param sql 要解析的SQL语句
     * @return 血缘解析结果
     */
    public SqlLineageResult parseLineage(String sql) {
        return parseLineage(sql, UUID.randomUUID().toString());
    }

    /**
     * 解析单条SQL语句的血缘关系（带指定语句ID）
     *
     * @param sql 要解析的SQL语句
     * @param statementId 语句唯一标识
     * @return 血缘解析结果
     */
    public SqlLineageResult parseLineage(String sql, String statementId) {
        log.info("开始解析SQL血缘关系, 语句ID: {}", statementId);

        // 参数校验
        if (StrUtil.isBlank(sql)) {
            log.warn("SQL语句为空");
            return buildEmptyResult("SQL语句不能为空");
        }

        try {
            // 调用ANTLR解析器解析SQL
            SqlLineageResult result = PlSqlLineageVisitor.parse(sql, statementId);

            log.info("SQL血缘关系解析完成, 语句ID: {}, 源表数: {}, 目标表数: {}, 字段血缘数: {}",
                    statementId,
                    result.getSourceTableNames().size(),
                    result.getTargetTableNames().size(),
                    result.getColumnLineages().size());

            return result;

        } catch (Exception e) {
            log.error("SQL血缘关系解析异常: {}", e.getMessage(), e);
            return buildErrorResult(statementId, e.getMessage());
        }
    }

    /**
     * 批量解析多条SQL语句的血缘关系
     *
     * @param sqlList SQL语句列表
     * @return 血缘解析结果列表
     */
    public List<SqlLineageResult> parseLineageBatch(List<String> sqlList) {
        return parseLineageBatch(sqlList, null);
    }

    /**
     * 批量解析多条SQL语句的血缘关系（带语句ID前缀）
     *
     * @param sqlList SQL语句列表
     * @param idPrefix 语句ID前缀
     * @return 血缘解析结果列表
     */
    public List<SqlLineageResult> parseLineageBatch(List<String> sqlList, String idPrefix) {
        log.info("开始批量解析SQL血缘关系, SQL数量: {}", sqlList != null ? sqlList.size() : 0);

        List<SqlLineageResult> results = new ArrayList<>();

        if (CollUtil.isEmpty(sqlList)) {
            log.warn("SQL列表为空");
            return results;
        }

        for (int i = 0; i < sqlList.size(); i++) {
            String sql = sqlList.get(i);
            String statementId = StrUtil.isNotBlank(idPrefix)
                    ? idPrefix + "_" + i
                    : String.valueOf(i);

            SqlLineageResult result = parseLineage(sql, statementId);
            results.add(result);
        }

        log.info("批量解析完成, 成功: {}, 失败: {}",
                results.stream().filter(SqlLineageResult::isSuccess).count(),
                results.stream().filter(r -> !r.isSuccess()).count());

        return results;
    }

    /**
     * 从血缘结果中提取所有源表名称
     *
     * @param result 血缘解析结果
     * @return 源表名称列表（去重）
     */
    public List<String> extractSourceTables(SqlLineageResult result) {
        if (result == null || CollUtil.isEmpty(result.getTableLineages())) {
            return new ArrayList<>();
        }

        List<String> sourceTables = new ArrayList<>();
        for (TableLineage tl : result.getTableLineages()) {
            if (StrUtil.isNotBlank(tl.getSourceTable()) && !sourceTables.contains(tl.getSourceTable())) {
                sourceTables.add(tl.getSourceTable());
            }
        }
        return sourceTables;
    }

    /**
     * 从血缘结果中提取所有目标表名称
     *
     * @param result 血缘解析结果
     * @return 目标表名称列表（去重）
     */
    public List<String> extractTargetTables(SqlLineageResult result) {
        if (result == null || CollUtil.isEmpty(result.getTableLineages())) {
            return new ArrayList<>();
        }

        List<String> targetTables = new ArrayList<>();
        for (TableLineage tl : result.getTableLineages()) {
            if (StrUtil.isNotBlank(tl.getTargetTable()) && !targetTables.contains(tl.getTargetTable())) {
                targetTables.add(tl.getTargetTable());
            }
        }
        return targetTables;
    }

    /**
     * 获取指定表的字段血缘关系
     *
     * @param result 血缘解析结果
     * @param tableName 表名
     * @param isSource true-作为源表, false-作为目标表
     * @return 字段血缘关系列表
     */
    public List<ColumnLineage> getColumnLineagesForTable(SqlLineageResult result, String tableName, boolean isSource) {
        if (result == null || StrUtil.isBlank(tableName) || CollUtil.isEmpty(result.getColumnLineages())) {
            return new ArrayList<>();
        }

        List<ColumnLineage> columnLineages = new ArrayList<>();
        for (ColumnLineage cl : result.getColumnLineages()) {
            if (isSource) {
                if (tableName.equalsIgnoreCase(cl.getSourceTable())) {
                    columnLineages.add(cl);
                }
            } else {
                if (tableName.equalsIgnoreCase(cl.getTargetTable())) {
                    columnLineages.add(cl);
                }
            }
        }
        return columnLineages;
    }

    /**
     * 构建空结果
     */
    private SqlLineageResult buildEmptyResult(String message) {
        return SqlLineageResult.builder()
                .sqlType("UNKNOWN")
                .parseSuccess(false)
                .errorMessage(message)
                .tableLineages(new ArrayList<>())
                .columnLineages(new ArrayList<>())
                .build();
    }

    /**
     * 构建错误结果
     */
    private SqlLineageResult buildErrorResult(String statementId, String errorMessage) {
        return SqlLineageResult.builder()
                .sqlType("UNKNOWN")
                .parseSuccess(false)
                .errorMessage(errorMessage)
                .statementId(statementId)
                .tableLineages(new ArrayList<>())
                .columnLineages(new ArrayList<>())
                .build();
    }
}
