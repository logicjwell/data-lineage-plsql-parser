package com.lineage.plsql.parser.visitor;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;

import com.lineage.plsql.parser.model.ColumnLineage;
import com.lineage.plsql.parser.model.SqlLineageResult;
import com.lineage.plsql.parser.model.TableLineage;

import java.io.StringReader;
import java.util.*;

/**
 * PL/SQL血缘关系访问器
 * 用于解析SQL语句并提取数据血缘关系
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
@Slf4j
public class PlSqlLineageVisitor extends com.lineage.plsql.parser.PlSqlParserBaseVisitor<Object> {

    /**
     * SQL血缘解析结果
     */
    @Getter
    private SqlLineageResult result;

    /**
     * 当前语句ID
     */
    private String statementId;

    /**
     * 构造函数 - 初始化访问器
     */
    public PlSqlLineageVisitor() {
        this.result = SqlLineageResult.builder()
                .sqlType("UNKNOWN")
                .tableLineages(new ArrayList<>())
                .columnLineages(new ArrayList<>())
                .parseSuccess(true)
                .build();
    }

    /**
     * 解析SQL语句并返回血缘结果
     *
     * @param sql 要解析的SQL语句
     * @return 血缘解析结果
     */
    public static SqlLineageResult parse(String sql) {
        return parse(sql, UUID.randomUUID().toString());
    }

    /**
     * 解析SQL语句并返回血缘结果（带指定语句ID）
     *
     * @param sql 要解析的SQL语句
     * @param statementId 语句唯一标识
     * @return 血缘解析结果
     */
    public static SqlLineageResult parse(String sql, String statementId) {
        long startTime = System.currentTimeMillis();
        PlSqlLineageVisitor visitor = new PlSqlLineageVisitor();
        visitor.statementId = statementId;

        try {
            // 词法分析 - 将SQL转换为大写以适配PL/SQL词法分析器
            CharStream charStream = CharStreams.fromReader(new StringReader(sql.toUpperCase()));
            com.lineage.plsql.parser.PlSqlLexer lexer = new com.lineage.plsql.parser.PlSqlLexer(charStream);

            // 词法符号流
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 语法分析
            com.lineage.plsql.parser.PlSqlParser parser = new com.lineage.plsql.parser.PlSqlParser(tokens);
            parser.setErrorHandler(new BailErrorStrategy());

            // 开始解析
            ParseTree tree = parser.sql_script();
            log.debug("SQL解析树: {}", tree.toStringTree(parser));

            // 遍历语法树
            visitor.visit(tree);

            // 设置解析耗时
            visitor.result.setParseTime(System.currentTimeMillis() - startTime);
            visitor.result.setOriginalSql(sql);
            visitor.result.setStatementId(statementId);
            visitor.result.setDatabaseType("ORACLE");

            log.info("SQL解析成功，耗时: {}ms", visitor.result.getParseTime());

        } catch (Exception e) {
            log.error("SQL解析失败: {}", e.getMessage(), e);
            visitor.result.setParseFailed(e.getMessage());
            visitor.result.setParseTime(System.currentTimeMillis() - startTime);
        }

        return visitor.result;
    }

    /**
     * 创建表血缘关系
     */
    private TableLineage createTableLineage(String sourceTable, String targetTable, String sqlType) {
        return TableLineage.builder()
                .sourceTable(sourceTable)
                .targetTable(targetTable)
                .sqlType(sqlType)
                .statementId(statementId)
                .databaseType("ORACLE")
                .createTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 规范化表名
     */
    private String normalizeTableName(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            return null;
        }
        String normalized = tableName.replace("\"", "").replace("'", "").trim();
        if (normalized.contains(".")) {
            normalized = normalized.substring(normalized.lastIndexOf(".") + 1);
        }
        return normalized.toUpperCase();
    }

    /**
     * 从Tableview_nameContext获取表名
     */
    private String extractTableName(com.lineage.plsql.parser.PlSqlParser.Tableview_nameContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.identifier() != null) {
            return normalizeTableName(ctx.identifier().getText());
        }
        if (ctx.id_expression() != null) {
            return normalizeTableName(ctx.id_expression().getText());
        }
        return null;
    }

    /**
     * 从Dml_table_expression_clauseContext获取表名
     */
    private String extractTableNameFromDmlTableExpr(com.lineage.plsql.parser.PlSqlParser.Dml_table_expression_clauseContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.tableview_name() != null) {
            return extractTableName(ctx.tableview_name());
        }
        return null;
    }

    /**
     * 从General_table_refContext获取表名
     */
    private String extractTableNameFromGeneralTableRef(com.lineage.plsql.parser.PlSqlParser.General_table_refContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.dml_table_expression_clause() != null) {
            return extractTableNameFromDmlTableExpr(ctx.dml_table_expression_clause());
        }
        return null;
    }

    /**
     * 创建字段血缘关系
     */
    private ColumnLineage createColumnLineage(String sourceTable, String sourceColumn, String targetTable, String targetColumn, String transformExpression) {
        return ColumnLineage.builder()
                .sourceTable(sourceTable)
                .sourceColumn(sourceColumn)
                .targetTable(targetTable)
                .targetColumn(targetColumn)
                .transformExpression(transformExpression)
                .sqlType("SELECT")
                .statementId(statementId)
                .createTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 处理SELECT语句
     */
    @Override
    public Object visitSelect_statement(com.lineage.plsql.parser.PlSqlParser.Select_statementContext ctx) {
        log.debug("处理SELECT语句");

        try {
            // 获取select_only_statement
            if (ctx.select_only_statement() != null) {
                com.lineage.plsql.parser.PlSqlParser.Select_only_statementContext selectOnlyCtx = ctx.select_only_statement();

                // 获取subquery
                if (selectOnlyCtx.subquery() != null) {
                    com.lineage.plsql.parser.PlSqlParser.SubqueryContext subqueryCtx = selectOnlyCtx.subquery();

                    // 处理subquery_basic_elements
                    if (subqueryCtx.subquery_basic_elements() != null) {
                        com.lineage.plsql.parser.PlSqlParser.Subquery_basic_elementsContext basicCtx = subqueryCtx.subquery_basic_elements();

                        // 处理query_block获取FROM子句中的表
                        if (basicCtx.query_block() != null) {
                            com.lineage.plsql.parser.PlSqlParser.Query_blockContext queryCtx = basicCtx.query_block();

                            // 构建表别名映射
                            Map<String, String> aliasMap = new HashMap<>();
                            // 用于存储子查询别名到其列映射的对应关系
                            Map<String, Map<String, String>> subqueryAliasToColumnMapping = new HashMap<>();

                            if (queryCtx.from_clause() != null) {
                                com.lineage.plsql.parser.PlSqlParser.From_clauseContext fromCtx = queryCtx.from_clause();
                                if (fromCtx.table_ref_list() != null) {
                                    // 第一遍：识别所有子查询并处理它们，获取列映射
                                    for (com.lineage.plsql.parser.PlSqlParser.Table_refContext tableRefCtx : fromCtx.table_ref_list().table_ref()) {
                                        // 检查是否是子查询
                                        if (isSubqueryTableRef(tableRefCtx)) {
                                            String subqueryAlias = getSubqueryAlias(tableRefCtx);
                                            if (subqueryAlias != null) {
                                                // 处理子查询，获取其列映射
                                                Map<String, String> columnMapping = new HashMap<>();
                                                processSubqueryForColumnMapping(tableRefCtx, columnMapping);
                                                subqueryAliasToColumnMapping.put(subqueryAlias, columnMapping);
                                            }
                                        }
                                    }
                                    // 第二遍：处理普通表引用
                                    for (com.lineage.plsql.parser.PlSqlParser.Table_refContext tableRefCtx : fromCtx.table_ref_list().table_ref()) {
                                        if (!isSubqueryTableRef(tableRefCtx)) {
                                            processTableRefForSelect(tableRefCtx);
                                            aliasMap.putAll(extractTableAliasMap(tableRefCtx));
                                        }
                                    }
                                }
                            }

                            // 处理SELECT列表，使用子查询列映射处理子查询列引用
                            if (queryCtx.selected_list() != null) {
                                processSelectListWithSubqueryAlias(queryCtx.selected_list(), aliasMap, subqueryAliasToColumnMapping);
                            }
                        }

                        // 处理内嵌子查询
                        if (basicCtx.subquery() != null) {
                            visitSubquery(basicCtx.subquery());
                        }
                    }
                }
            }

            result.setSqlType("SELECT");

        } catch (Exception e) {
            log.error("处理SELECT语句异常: {}", e.getMessage(), e);
        }

        return super.visitSelect_statement(ctx);
    }

    /**
     * 检查tableRef是否是子查询
     */
    private boolean isSubqueryTableRef(com.lineage.plsql.parser.PlSqlParser.Table_refContext tableRefCtx) {
        if (tableRefCtx == null || tableRefCtx.table_ref_aux() == null ||
            tableRefCtx.table_ref_aux().table_ref_aux_internal() == null) {
            return false;
        }
        com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internalContext internalCtx =
            tableRefCtx.table_ref_aux().table_ref_aux_internal();
        if (internalCtx instanceof com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) {
            com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext oneCtx =
                    (com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) internalCtx;
            return oneCtx.dml_table_expression_clause() != null &&
                   oneCtx.dml_table_expression_clause().select_statement() != null;
        }
        return false;
    }

    /**
     * 获取子查询的别名
     */
    private String getSubqueryAlias(com.lineage.plsql.parser.PlSqlParser.Table_refContext tableRefCtx) {
        if (tableRefCtx.table_ref_aux() != null &&
            tableRefCtx.table_ref_aux().table_alias() != null) {
            return tableRefCtx.table_ref_aux().table_alias().getText().toUpperCase();
        }
        return null;
    }

    /**
     * 处理子查询并构建列映射
     */
    private void processSubqueryForColumnMapping(com.lineage.plsql.parser.PlSqlParser.Table_refContext tableRefCtx,
                                                  Map<String, String> columnMapping) {
        if (tableRefCtx.table_ref_aux() == null ||
            tableRefCtx.table_ref_aux().table_ref_aux_internal() == null) {
            return;
        }
        com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internalContext internalCtx =
            tableRefCtx.table_ref_aux().table_ref_aux_internal();
        if (internalCtx instanceof com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) {
            com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext oneCtx =
                    (com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) internalCtx;
            if (oneCtx.dml_table_expression_clause() != null &&
                oneCtx.dml_table_expression_clause().select_statement() != null) {
                // 处理子查询的SELECT语句
                com.lineage.plsql.parser.PlSqlParser.Select_statementContext selectCtx =
                    oneCtx.dml_table_expression_clause().select_statement();
                if (selectCtx.select_only_statement() != null &&
                    selectCtx.select_only_statement().subquery() != null) {
                    com.lineage.plsql.parser.PlSqlParser.SubqueryContext subqueryCtx =
                        selectCtx.select_only_statement().subquery();
                    buildColumnMappingFromSubquery(subqueryCtx, columnMapping);
                }
            }
        }
    }

    /**
     * 从子查询构建列映射
     */
    private void buildColumnMappingFromSubquery(com.lineage.plsql.parser.PlSqlParser.SubqueryContext subqueryCtx,
                                                 Map<String, String> columnMapping) {
        if (subqueryCtx == null || subqueryCtx.subquery_basic_elements() == null) {
            return;
        }
        com.lineage.plsql.parser.PlSqlParser.Subquery_basic_elementsContext basicCtx = subqueryCtx.subquery_basic_elements();
        if (basicCtx.query_block() != null) {
            com.lineage.plsql.parser.PlSqlParser.Query_blockContext queryCtx = basicCtx.query_block();

            // 构建内层表的别名映射
            Map<String, String> aliasMap = new HashMap<>();
            if (queryCtx.from_clause() != null &&
                queryCtx.from_clause().table_ref_list() != null) {
                for (com.lineage.plsql.parser.PlSqlParser.Table_refContext tableRefCtx : queryCtx.from_clause().table_ref_list().table_ref()) {
                    aliasMap.putAll(extractTableAliasMap(tableRefCtx));
                }
            }

            // 构建SELECT列表中每个列到其源表的映射
            if (queryCtx.selected_list() != null) {
                for (com.lineage.plsql.parser.PlSqlParser.Select_list_elementsContext selCtx : queryCtx.selected_list().select_list_elements()) {
                    if (selCtx == null) {
                        continue;
                    }

                    // 获取输出列名（别名优先）
                    String outputColumn = null;
                    if (selCtx.column_alias() != null) {
                        outputColumn = selCtx.column_alias().getText().toUpperCase();
                        if (outputColumn.startsWith("AS ")) {
                            outputColumn = outputColumn.substring(3).trim();
                        }
                    } else if (selCtx.expression() != null) {
                        // 如果表达式包含标量子查询，使用 extractColumnNameFromExpression
                        if (containsScalarSubquery(selCtx.expression())) {
                            outputColumn = extractColumnNameFromExpression(selCtx.expression());
                        } else {
                            outputColumn = selCtx.expression().getText().toUpperCase();
                        }
                    }

                    if (outputColumn == null) {
                        continue;
                    }

                    // 从表达式中提取列引用
                    if (selCtx.expression() != null) {
                        String srcColumn = extractSingleColumnReference(selCtx.expression(), aliasMap);
                        if (srcColumn != null && srcColumn.contains(":")) {
                            columnMapping.put(outputColumn, srcColumn);
                        }
                    }
                }
            }

            // 递归处理内嵌子查询
            if (basicCtx.subquery() != null) {
                buildColumnMappingFromSubquery(basicCtx.subquery(), columnMapping);
            }
        }
    }

    /**
     * 从表达式中提取单个列引用（用于列映射）
     */
    private String extractSingleColumnReference(com.lineage.plsql.parser.PlSqlParser.ExpressionContext exprCtx,
                                                Map<String, String> aliasMap) {
        if (exprCtx == null) {
            return null;
        }

        for (int i = 0; i < exprCtx.getChildCount(); i++) {
            ParseTree child = exprCtx.getChild(i);
            // 跳过标量子查询
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    String alias = parts[0];
                    String column = parts[1];
                    // 只在aliasMap中查找，aliasMap中不存在的是外层子查询别名，跳过
                    String tableName = aliasMap.get(alias);
                    if (tableName != null && StrUtil.isNotBlank(column)) {
                        return tableName + ":" + column;
                    }
                }
            } else if (child instanceof com.lineage.plsql.parser.PlSqlParser.ConstantContext) {
                // 忽略常量，但检查其子节点
                for (int j = 0; j < child.getChildCount(); j++) {
                    String result = extractSingleColumnReferenceFromTree(child.getChild(j), aliasMap);
                    if (result != null) {
                        return result;
                    }
                }
            } else {
                String result = extractSingleColumnReferenceFromTree(child, aliasMap);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 从树中提取单个列引用
     */
    private String extractSingleColumnReferenceFromTree(ParseTree tree, Map<String, String> aliasMap) {
        if (tree == null) {
            return null;
        }
        // 跳过标量子查询
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            return null;
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            // 跳过标量子查询
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    String alias = parts[0];
                    String column = parts[1];
                    // 只在aliasMap中查找，aliasMap中不存在的是外层子查询别名，跳过
                    String tableName = aliasMap.get(alias);
                    if (tableName != null && StrUtil.isNotBlank(column)) {
                        return tableName + ":" + column;
                    }
                }
            } else {
                String result = extractSingleColumnReferenceFromTree(child, aliasMap);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 处理SELECT列表（使用子查询列映射）
     */
    private void processSelectListWithSubqueryAlias(
            com.lineage.plsql.parser.PlSqlParser.Selected_listContext selectedListCtx,
            Map<String, String> aliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping) {
        if (selectedListCtx == null) {
            return;
        }

        for (com.lineage.plsql.parser.PlSqlParser.Select_list_elementsContext selCtx : selectedListCtx.select_list_elements()) {
            if (selCtx == null) {
                continue;
            }

            // 获取目标列名
            String targetColumn = null;

            if (selCtx.column_alias() != null) {
                targetColumn = selCtx.column_alias().getText().toUpperCase();
                if (targetColumn.startsWith("AS ")) {
                    targetColumn = targetColumn.substring(3).trim();
                }
            }

            if (selCtx.expression() != null) {
                // 检查是否包含标量子查询
                boolean hasScalarSubquery = containsScalarSubquery(selCtx.expression());
                // 即使 containsScalarSubquery 返回 false，也要尝试从表达式中提取列名
                // 如果表达式包含标量子查询，extractColumnNameFromExpression 会提取出标量子查询返回的列名
                if (targetColumn == null) {
                    // 尝试从表达式中提取列名
                    targetColumn = extractColumnNameFromExpression(selCtx.expression());
                }
                // 处理标量子查询（如 SELECT (SELECT ... FROM ...) FROM ...）
                // 无论 hasScalarSubquery 结果如何，都调用此方法处理标量子查询
                processScalarSubqueryInExpression(
                    selCtx.expression(), aliasMap, subqueryAliasToColumnMapping, targetColumn, null);
                // 处理表达式中的普通列引用（只有非标量子查询的列引用才由这里处理）
                // 传递 null 作为 transformExpression，因为标量子查询已经由上面处理
                if (!hasScalarSubquery) {
                    extractColumnReferencesFromExpressionWithSubquery(
                        selCtx.expression(), aliasMap, subqueryAliasToColumnMapping, targetColumn, selCtx.expression().getText());
                }
            }

            // 处理没有别名的简单列引用（直接在SELECT列表中的column_name）
            if (targetColumn == null && selCtx.expression() == null) {
                // 可能是直接的列名
                if (selCtx.getChildCount() > 0) {
                    String text = selCtx.getText().toUpperCase();
                    if (!text.contains(".")) {
                        // 简单的列名引用
                    }
                }
            }
        }
    }

    /**
     * 从表达式中提取列名（用于目标列）
     * 如果表达式是简单的 table.column 或 column 形式，返回列名部分
     */
    private String extractColumnNameFromExpression(com.lineage.plsql.parser.PlSqlParser.ExpressionContext exprCtx) {
        if (exprCtx == null) {
            return null;
        }
        // 先检查是否包含标量子查询
        String scalarSubqueryColumn = extractColumnFromScalarSubquery(exprCtx);
        if (scalarSubqueryColumn != null) {
            return scalarSubqueryColumn;
        }
        // 遍历表达式树，查找Variable_name
        for (int i = 0; i < exprCtx.getChildCount(); i++) {
            ParseTree child = exprCtx.getChild(i);
            // 跳过标量子查询
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    return parts[1]; // 返回列名部分
                } else {
                    return fullName; // 直接是列名
                }
            } else if (child instanceof com.lineage.plsql.parser.PlSqlParser.Id_expressionContext) {
                return child.getText().toUpperCase();
            } else {
                // 继续在子树中查找
                String result = extractColumnNameFromTree(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 从标量子查询表达式中提取返回列名
     * 例如：SELECT (SELECT DLER_NAME FROM ...) FROM ...
     */
    private String extractColumnFromScalarSubquery(com.lineage.plsql.parser.PlSqlParser.ExpressionContext exprCtx) {
        if (exprCtx == null) {
            return null;
        }
        // 检查是否有 subquery 子节点
        for (int i = 0; i < exprCtx.getChildCount(); i++) {
            ParseTree child = exprCtx.getChild(i);
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                com.lineage.plsql.parser.PlSqlParser.SubqueryContext subqueryCtx =
                        (com.lineage.plsql.parser.PlSqlParser.SubqueryContext) child;
                // 获取子查询的SELECT列表列
                if (subqueryCtx.subquery_basic_elements() != null &&
                    subqueryCtx.subquery_basic_elements().query_block() != null) {
                    com.lineage.plsql.parser.PlSqlParser.Query_blockContext queryCtx =
                            subqueryCtx.subquery_basic_elements().query_block();
                    if (queryCtx.selected_list() != null &&
                        !queryCtx.selected_list().select_list_elements().isEmpty()) {
                        com.lineage.plsql.parser.PlSqlParser.Select_list_elementsContext selCtx =
                                queryCtx.selected_list().select_list_elements().get(0);
                        // 如果有别名，返回别名
                        if (selCtx.column_alias() != null) {
                            String alias = selCtx.column_alias().getText().toUpperCase();
                            if (alias.startsWith("AS ")) {
                                alias = alias.substring(3).trim();
                            }
                            return alias;
                        }
                        // 没有别名，尝试从表达式中提取列名
                        if (selCtx.expression() != null) {
                            return extractColumnNameFromExpression(selCtx.expression());
                        }
                    }
                }
            }
            // 递归检查子节点
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.ExpressionContext) {
                String result = extractColumnFromScalarSubquery((com.lineage.plsql.parser.PlSqlParser.ExpressionContext) child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 检查表达式是否包含标量子查询
     */
    private boolean containsScalarSubquery(ParseTree tree) {
        if (tree == null) {
            return false;
        }
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            log.debug("Found SubqueryContext: {}", tree.getText());
            return true;
        }
        // 递归检查所有子节点
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            log.debug("containsScalarSubquery traversing: {} - {}", child.getClass().getSimpleName(), truncateText(child.getText(), 100));
            if (containsScalarSubquery(child)) {
                return true;
            }
        }
        return false;
    }

    private String truncateText(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 从树中提取列名
     */
    private String extractColumnNameFromTree(ParseTree tree) {
        if (tree == null) {
            return null;
        }
        // 跳过标量子查询 - 应该由 extractColumnFromScalarSubquery 处理
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            return null;
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            // 跳过标量子查询
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    return parts[1];
                } else {
                    return fullName;
                }
            } else if (child instanceof com.lineage.plsql.parser.PlSqlParser.Id_expressionContext) {
                return child.getText().toUpperCase();
            } else {
                String result = extractColumnNameFromTree(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 检查表达式是否包含标量子查询，并处理其中的列引用
     * 例如：(SELECT DLER_NAME FROM MDM_DEALER_HEAD_FIN WHERE ROWID_OBJECT = T2.LINKED_DEALER_ID)
     */
    private void processScalarSubqueryInExpression(
            com.lineage.plsql.parser.PlSqlParser.ExpressionContext exprCtx,
            Map<String, String> aliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping,
            String targetColumn,
            String transformExpression) {
        if (exprCtx == null) {
            return;
        }

        // 在表达式树中查找标量子查询
        findAndProcessScalarSubquery(exprCtx, aliasMap, subqueryAliasToColumnMapping, targetColumn, transformExpression);
    }

    /**
     * 在表达式树中查找标量子查询并处理
     */
    private void findAndProcessScalarSubquery(
            ParseTree tree,
            Map<String, String> aliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping,
            String targetColumn,
            String transformExpression) {
        if (tree == null) {
            return;
        }

        // 检查是否是子查询上下文
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            processSubqueryForLineage((com.lineage.plsql.parser.PlSqlParser.SubqueryContext) tree,
                    aliasMap, subqueryAliasToColumnMapping, targetColumn, transformExpression);
            return; // 子查询内部已处理，不再继续遍历其子节点
        }

        // 继续遍历子节点
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            // 跳过某些不需要检查的节点类型
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.ConstantContext) {
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                processSubqueryForLineage((com.lineage.plsql.parser.PlSqlParser.SubqueryContext) child,
                        aliasMap, subqueryAliasToColumnMapping, targetColumn, transformExpression);
            } else {
                findAndProcessScalarSubquery(child, aliasMap, subqueryAliasToColumnMapping, targetColumn, transformExpression);
            }
        }
    }

    /**
     * 处理子查询的血缘关系
     */
    private void processSubqueryForLineage(
            com.lineage.plsql.parser.PlSqlParser.SubqueryContext subqueryCtx,
            Map<String, String> aliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping,
            String targetColumn,
            String transformExpression) {
        if (subqueryCtx == null || subqueryCtx.subquery_basic_elements() == null) {
            return;
        }

        com.lineage.plsql.parser.PlSqlParser.Subquery_basic_elementsContext basicCtx = subqueryCtx.subquery_basic_elements();
        if (basicCtx.query_block() == null) {
            return;
        }

        com.lineage.plsql.parser.PlSqlParser.Query_blockContext queryCtx = basicCtx.query_block();

        // 构建当前子查询自己的别名映射（包含自己的FROM子句中的表）
        Map<String, String> localAliasMap = new HashMap<>();
        if (queryCtx.from_clause() != null && queryCtx.from_clause().table_ref_list() != null) {
            for (com.lineage.plsql.parser.PlSqlParser.Table_refContext tableRefCtx : queryCtx.from_clause().table_ref_list().table_ref()) {
                localAliasMap.putAll(extractTableAliasMap(tableRefCtx));
            }
        }
        log.debug("processSubqueryForLineage: localAliasMap={}", localAliasMap);

        // 获取子查询的SELECT列表信息
        // 标量子查询的SELECT列表通常只有一个元素（返回单行单列）
        String subquerySelectColumn = null;     // 子查询SELECT的返回列（可能是别名或表达式名）
        String subquerySelectExprColumn = null; // SELECT表达式中的源列（如DLER_NAME）
        boolean hasAlias = false;               // 是否有显式别名

        if (queryCtx.selected_list() != null && !queryCtx.selected_list().select_list_elements().isEmpty()) {
            com.lineage.plsql.parser.PlSqlParser.Select_list_elementsContext selCtx =
                    queryCtx.selected_list().select_list_elements().get(0);
            if (selCtx.expression() != null) {
                // 检查是否有别名
                if (selCtx.column_alias() != null) {
                    subquerySelectColumn = selCtx.column_alias().getText().toUpperCase();
                    if (subquerySelectColumn.startsWith("AS ")) {
                        subquerySelectColumn = subquerySelectColumn.substring(3).trim();
                    }
                    hasAlias = true;
                }
                // 提取SELECT表达式中的列引用
                subquerySelectExprColumn = extractColumnNameFromExpression(selCtx.expression());
            }
        }

        // 如果没有别名，使用表达式中的列名作为子查询的返回列
        if (!hasAlias && subquerySelectColumn == null && StrUtil.isNotBlank(subquerySelectExprColumn)) {
            subquerySelectColumn = subquerySelectExprColumn;
        }

        // 如果没有WHERE条件，从SELECT表达式直接提取列引用
        if (queryCtx.where_clause() == null || queryCtx.where_clause().expression() == null) {
            if (StrUtil.isNotBlank(subquerySelectExprColumn)) {
                // 没有WHERE条件时，直接建立从源列到目标列的血缘
                String finalTargetColumn = hasAlias ? subquerySelectColumn : targetColumn;
                if (StrUtil.isNotBlank(finalTargetColumn)) {
                    result.addColumnLineage(createColumnLineage(null, subquerySelectExprColumn, null, finalTargetColumn, null));
                }
            }
            return;
        }

        // 查找WHERE子句，提取连接条件
        com.lineage.plsql.parser.PlSqlParser.ExpressionContext whereExpr = queryCtx.where_clause().expression();

        // 提取WHERE条件中的所有列引用（使用合并后的aliasMap，包含内层和外层的别名）
        Map<String, String> mergedAliasMap = new HashMap<>(aliasMap);
        mergedAliasMap.putAll(localAliasMap);
        List<ColumnRefInfo> whereColumnRefs = extractAllColumnRefsFromWhere(whereExpr, mergedAliasMap);
        log.debug("processSubqueryForLineage: mergedAliasMap={}, whereColumnRefs={}", mergedAliasMap, whereColumnRefs);

        // 区分内层列引用和外层列引用
        // 内层引用：来自当前子查询源表的列（如 ROWID_OBJECT）
        // 外层引用：引用外层子查询或外层表的列（如 T2.LINKED_DEALER_ID）
        ColumnRefInfo innerColumnRef = null;  // 内层子查询的列（等号左边）
        ColumnRefInfo outerColumnRef = null;   // 外层引用的列（等号右边）

        for (ColumnRefInfo ref : whereColumnRefs) {
            // 判断这个引用是内层还是外层
            // 如果 ref.table 为 null（没有表前缀），说明这是子查询FROM表的列
            // 如果 ref.table 在 localAliasMap 中，也是内层引用
            if (ref.table == null || localAliasMap.containsKey(ref.table)) {
                if (innerColumnRef == null) {  // 只取第一个
                    innerColumnRef = ref;
                }
            } else {
                outerColumnRef = ref;
            }
        }
        log.debug("processSubqueryForLineage: innerColumnRef={}, outerColumnRef={}", innerColumnRef, outerColumnRef);

        // 建立血缘关系
        // 情况1：内层源表.源列 -> 子查询SELECT列（中间结果）
        if (innerColumnRef != null && StrUtil.isNotBlank(subquerySelectColumn)) {
            // 如果 innerColumnRef.table 为 null，但列名存在，尝试推断表名
            String innerTable = innerColumnRef.table;
            if (innerTable == null && StrUtil.isNotBlank(innerColumnRef.column)) {
                // 尝试从 localAliasMap 的 value（物理表名）中推断
                // 这里简化处理：如果 localAliasMap 只有一张表，就用那张表
                if (localAliasMap.size() == 1) {
                    innerTable = localAliasMap.values().iterator().next();
                    log.debug("processSubqueryForLineage: inferred innerTable={} from localAliasMap", innerTable);
                }
            }
            if (StrUtil.isNotBlank(innerTable)) {
                result.addColumnLineage(createColumnLineage(
                        innerTable, innerColumnRef.column,
                        null, subquerySelectColumn, null));
            }
        }

        // 情况2：子查询SELECT列（中间结果）-> 外层目标列（targetColumn）
        // 这里 subquerySelectColumn 是子查询返回的列（DLER_NAME）
        // targetColumn 是外层指定的别名（LINKED_DEALER_NAME）
        // 血缘关系：DLER_NAME（中间列）-> LINKED_DEALER_NAME
        if (StrUtil.isNotBlank(subquerySelectColumn) && StrUtil.isNotBlank(targetColumn)) {
            result.addColumnLineage(createColumnLineage(
                    null, subquerySelectColumn,  // 源是子查询返回的列（中间列，无物理表）
                    null, targetColumn, null));   // 目标是外层别名
        }

        // 新增：当存在内层物理列和子查询->外层映射时，添加一条合并映射
        // 直接将内层物理表.列 -> 外层目标列（避免仅保留中间结果）
        if (innerColumnRef != null && StrUtil.isNotBlank(targetColumn)) {
            String innerTable = innerColumnRef.table;
            if (innerTable == null && StrUtil.isNotBlank(innerColumnRef.column)) {
                if (localAliasMap.size() == 1) {
                    innerTable = localAliasMap.values().iterator().next();
                    log.debug("processSubqueryForLineage: inferred innerTable={} for merged mapping", innerTable);
                }
            }
            if (StrUtil.isNotBlank(innerTable) && StrUtil.isNotBlank(innerColumnRef.column)) {
                result.addColumnLineage(createColumnLineage(innerTable, innerColumnRef.column, null, targetColumn, null));
            }
        }

        // 情况3：如果有外层引用，并且它引用的是物理表（不在subqueryAliasToColumnMapping中）
        // 建立外层源表.源列 -> 外层目标列 的血缘
        if (outerColumnRef != null && !subqueryAliasToColumnMapping.containsKey(outerColumnRef.table)) {
            if (StrUtil.isNotBlank(outerColumnRef.column)) {
                result.addColumnLineage(createColumnLineage(
                        outerColumnRef.table, outerColumnRef.column,
                        null, targetColumn, null));
            }
        }
    }

    /**
     * 从WHERE条件中提取所有列引用
     */
    private List<ColumnRefInfo> extractAllColumnRefsFromWhere(
            com.lineage.plsql.parser.PlSqlParser.ExpressionContext whereExpr,
            Map<String, String> aliasMap) {
        List<ColumnRefInfo> refs = new ArrayList<>();
        if (whereExpr == null) {
            return refs;
        }
        extractColumnRefsRecursively(whereExpr, aliasMap, refs);
        return refs;
    }

    /**
     * 递归提取列引用
     */
    private void extractColumnRefsRecursively(ParseTree tree, Map<String, String> aliasMap, List<ColumnRefInfo> refs) {
        if (tree == null) {
            return;
        }
        // 跳过 SubqueryContext - 标量子查询由 processSubqueryForLineage 处理
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            return;
        }

        // 检查是否是 Variable_name
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
            String fullName = tree.getText().toUpperCase();
            log.debug("extractColumnRefsRecursively: Variable_name = {}", fullName);
            if (fullName.contains(".")) {
                String[] parts = fullName.split("\\.");
                String alias = parts[0];
                String column = parts[1];
                ColumnRefInfo info = new ColumnRefInfo();
                info.table = alias;
                info.column = column;
                refs.add(info);
                log.debug("extractColumnRefsRecursively: Added ref {}:{}", alias, column);
            } else {
                // 没有表前缀的列引用，将其添加到 refs 中，table 为 null
                // 调用者需要根据上下文判断这个列来自哪个表
                ColumnRefInfo info = new ColumnRefInfo();
                info.table = null;  // 表示没有表前缀
                info.column = fullName;
                refs.add(info);
                log.debug("extractColumnRefsRecursively: Added ref (no table prefix) {}", fullName);
            }
            return;
        }

        // 继续遍历子节点
        for (int i = 0; i < tree.getChildCount(); i++) {
            extractColumnRefsRecursively(tree.getChild(i), aliasMap, refs);
        }
    }

    /**
     * 列引用对（用于WHERE条件中的等号两侧）
     */
    private static class ColumnRefPair {
        String leftTable;      // 等号左侧的表
        String leftColumn;     // 等号左侧的列
        String rightColumn;    // 等号右侧的完整引用（如 T2.LINKED_DEALER_ID）
        String rightSourceTable; // 等号右侧引用的源表
        String rightSourceColumn; // 等号右侧引用的源列
    }

    /**
     * 从WHERE条件中提取列引用对（处理等号连接的列引用）
     */
    private List<ColumnRefPair> extractColumnRefPairsFromWhere(
            com.lineage.plsql.parser.PlSqlParser.ExpressionContext whereExpr,
            Map<String, String> aliasMap) {
        List<ColumnRefPair> pairs = new ArrayList<>();
        if (whereExpr == null) {
            return pairs;
        }

        // 递归遍历表达式树找到等号
        findEqualityPairs(whereExpr, aliasMap, pairs);
        return pairs;
    }

    /**
     * 在表达式树中查找等号条件并提取列引用对
     */
    private void findEqualityPairs(ParseTree tree, Map<String, String> aliasMap, List<ColumnRefPair> pairs) {
        if (tree == null) {
            return;
        }
        // 跳过 SubqueryContext
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            return;
        }

        // 检查是否是等号操作
        String nodeText = tree.getText().toUpperCase();
        if (nodeText.equals("=") || nodeText.contains("=")) {
            // 找到等号，分析左右两侧
            if (tree.getChildCount() >= 2) {
                ParseTree leftChild = tree.getChild(0);
                ParseTree rightChild = tree.getChild(1);

                ColumnRefPair pair = new ColumnRefPair();

                // 解析左侧列引用
                ColumnRefInfo leftRef = extractColumnRefFromTree(leftChild, aliasMap);
                if (leftRef != null) {
                    pair.leftTable = leftRef.table;
                    pair.leftColumn = leftRef.column;
                }

                // 解析右侧列引用
                ColumnRefInfo rightRef = extractColumnRefFromTree(rightChild, aliasMap);
                if (rightRef != null) {
                    pair.rightColumn = rightRef.table + ":" + rightRef.column;
                    pair.rightSourceTable = rightRef.table;
                    pair.rightSourceColumn = rightRef.column;
                }

                if (pair.leftColumn != null && pair.rightColumn != null) {
                    pairs.add(pair);
                }
            }
            return;
        }

        // 继续遍历子节点
        for (int i = 0; i < tree.getChildCount(); i++) {
            findEqualityPairs(tree.getChild(i), aliasMap, pairs);
        }
    }

    /**
     * 列引用信息
     */
    private static class ColumnRefInfo {
        String table;
        String column;
    }

    /**
     * 从树中提取列引用信息
     */
    private ColumnRefInfo extractColumnRefFromTree(ParseTree tree, Map<String, String> aliasMap) {
        ColumnRefInfo info = new ColumnRefInfo();
        if (tree == null) {
            return null;
        }
        // 跳过 SubqueryContext
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            return null;
        }

        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
            String fullName = tree.getText().toUpperCase();
            if (fullName.contains(".")) {
                String[] parts = fullName.split("\\.");
                String alias = parts[0];
                String column = parts[1];

                // 查找表名
                String tableName = aliasMap.get(alias);
                if (tableName == null) {
                    // alias不在aliasMap中，可能是外层子查询的别名
                    // 直接使用alias作为表名
                    tableName = alias;
                }
                info.table = tableName;
                info.column = column;
                return info;
            }
        } else {
            // 继续在子节点中查找
            for (int i = 0; i < tree.getChildCount(); i++) {
                ColumnRefInfo childInfo = extractColumnRefFromTree(tree.getChild(i), aliasMap);
                if (childInfo != null && childInfo.table != null) {
                    return childInfo;
                }
            }
        }
        return null;
    }

    /**
     * 从表达式中简单提取列引用（用于没有WHERE条件的子查询）
     * 注意：如果表达式包含标量子查询，应该由 processScalarSubqueryInExpression 处理
     */
    private void extractColumnRefsFromExpressionSimple(
            com.lineage.plsql.parser.PlSqlParser.ExpressionContext exprCtx,
            Map<String, String> aliasMap,
            String targetColumn) {
        if (exprCtx == null) {
            return;
        }

        for (int i = 0; i < exprCtx.getChildCount(); i++) {
            ParseTree child = exprCtx.getChild(i);
            // 跳过 SubqueryContext - 标量子查询由 processScalarSubqueryInExpression 处理
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    String alias = parts[0];
                    String column = parts[1];

                    String tableName = aliasMap.get(alias);
                    if (tableName == null) {
                        tableName = alias;
                    }
                    if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(column)) {
                        result.addColumnLineage(createColumnLineage(tableName, column, null, targetColumn, null));
                    }
                }
            } else if (child instanceof com.lineage.plsql.parser.PlSqlParser.ExpressionContext) {
                extractColumnRefsFromExpressionSimple((com.lineage.plsql.parser.PlSqlParser.ExpressionContext) child, aliasMap, targetColumn);
            }
        }
    }

    /**
     * 从WHERE条件中提取列引用
     */
    private void extractColumnRefsFromWhereClause(
            com.lineage.plsql.parser.PlSqlParser.Query_blockContext queryCtx,
            Map<String, String> outerAliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping,
            String targetColumn,
            String transformExpression,
            String subquerySelectColumn) {
        // 查找WHERE子句
        if (queryCtx.where_clause() == null) {
            return;
        }

        com.lineage.plsql.parser.PlSqlParser.Where_clauseContext whereCtx = queryCtx.where_clause();
        if (whereCtx.expression() == null) {
            return;
        }

        // 从WHERE条件表达式中提取所有列引用
        com.lineage.plsql.parser.PlSqlParser.ExpressionContext whereExpr = whereCtx.expression();
        extractColumnRefsFromExpression(whereExpr, outerAliasMap, subqueryAliasToColumnMapping,
                targetColumn, transformExpression, subquerySelectColumn);
    }

    /**
     * 在条件树中查找列引用
     */
    private void findColumnRefsInConditionTree(
            ParseTree tree,
            Map<String, String> aliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping,
            String targetColumn,
            String transformExpression,
            String subquerySelectColumn) {
        if (tree == null) {
            return;
        }

        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    String alias = parts[0];
                    String column = parts[1];

                    // 检查是否是子查询别名
                    if (subqueryAliasToColumnMapping.containsKey(alias)) {
                        Map<String, String> columnMapping = subqueryAliasToColumnMapping.get(alias);
                        String sourceMapping = columnMapping.get(column);
                        if (sourceMapping != null && sourceMapping.contains(":")) {
                            String[] srcParts = sourceMapping.split(":");
                            String sourceTable = srcParts[0];
                            String sourceCol = srcParts[1];
                            // 子查询的SELECT列作为中间结果，最终目标是外层的targetColumn
                            if (StrUtil.isNotBlank(subquerySelectColumn)) {
                                result.addColumnLineage(createColumnLineage(sourceTable, sourceCol, null, subquerySelectColumn, null));
                            }
                        }
                    } else {
                        // 普通物理表列引用
                        String tableName = aliasMap.get(alias);
                        if (tableName == null) {
                            tableName = alias;
                        }
                        if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(column)) {
                            // 子查询的SELECT列作为中间结果，最终目标是外层的targetColumn
                            if (StrUtil.isNotBlank(subquerySelectColumn)) {
                                result.addColumnLineage(createColumnLineage(tableName, column, null, subquerySelectColumn, null));
                            }
                        }
                    }
                }
            } else if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                // 跳过标量子查询，由 processScalarSubqueryInExpression 处理
                continue;
            } else {
                findColumnRefsInConditionTree(child, aliasMap, subqueryAliasToColumnMapping,
                        targetColumn, transformExpression, subquerySelectColumn);
            }
        }
    }

    /**
     * 从表达式中提取列引用（用于标量子查询）
     */
    private void extractColumnRefsFromExpression(
            com.lineage.plsql.parser.PlSqlParser.ExpressionContext exprCtx,
            Map<String, String> aliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping,
            String targetColumn,
            String transformExpression,
            String subquerySelectColumn) {
        if (exprCtx == null) {
            return;
        }

        for (int i = 0; i < exprCtx.getChildCount(); i++) {
            ParseTree child = exprCtx.getChild(i);
            // 跳过标量子查询
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    String alias = parts[0];
                    String column = parts[1];

                    if (subqueryAliasToColumnMapping.containsKey(alias)) {
                        Map<String, String> columnMapping = subqueryAliasToColumnMapping.get(alias);
                        String sourceMapping = columnMapping.get(column);
                        if (sourceMapping != null && sourceMapping.contains(":")) {
                            String[] srcParts = sourceMapping.split(":");
                            String sourceTable = srcParts[0];
                            String sourceCol = srcParts[1];
                            if (StrUtil.isNotBlank(subquerySelectColumn)) {
                                result.addColumnLineage(createColumnLineage(sourceTable, sourceCol, null, subquerySelectColumn, null));
                            }
                        }
                    } else {
                        String tableName = aliasMap.get(alias);
                        if (tableName == null) {
                            tableName = alias;
                        }
                        if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(column)) {
                            if (StrUtil.isNotBlank(subquerySelectColumn)) {
                                result.addColumnLineage(createColumnLineage(tableName, column, null, subquerySelectColumn, null));
                            }
                        }
                    }
                }
            } else {
                findColumnRefsInConditionTree(child, aliasMap, subqueryAliasToColumnMapping,
                        targetColumn, transformExpression, subquerySelectColumn);
            }
        }
    }

    /**
     * 从表达式中提取列引用（支持子查询别名）
     */
    private void extractColumnReferencesFromExpressionWithSubquery(
            com.lineage.plsql.parser.PlSqlParser.ExpressionContext exprCtx,
            Map<String, String> aliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping,
            String targetColumn,
            String transformExpression) {
        if (exprCtx == null) {
            return;
        }

        for (int i = 0; i < exprCtx.getChildCount(); i++) {
            ParseTree child = exprCtx.getChild(i);
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                // 跳过标量子查询
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    String alias = parts[0];
                    String column = parts[1];

                    // 检查是否是子查询别名
                    if (subqueryAliasToColumnMapping.containsKey(alias)) {
                        Map<String, String> columnMapping = subqueryAliasToColumnMapping.get(alias);
                        String sourceMapping = columnMapping.get(column);
                        if (sourceMapping != null && sourceMapping.contains(":")) {
                            String[] srcParts = sourceMapping.split(":");
                            String sourceTable = srcParts[0];
                            String sourceCol = srcParts[1];
                            result.addColumnLineage(createColumnLineage(sourceTable, sourceCol, null, targetColumn, transformExpression));
                        }
                    } else {
                        // 普通物理表列引用
                        String tableName = aliasMap.get(alias);
                        if (tableName == null) {
                            tableName = alias;
                        }
                        if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(column)) {
                            result.addColumnLineage(createColumnLineage(tableName, column, null, targetColumn, transformExpression));
                        }
                    }
                }
            } else if (child instanceof com.lineage.plsql.parser.PlSqlParser.ExpressionContext) {
                // 递归处理嵌套的表达式，但传递 insideSubquery=false 因为 ExpressionContext 不是 SubqueryContext
                extractColumnReferencesFromTreeWithSubquery(child, aliasMap, subqueryAliasToColumnMapping, targetColumn, transformExpression, false);
            } else if (!(child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext)) {
                extractColumnReferencesFromTreeWithSubquery(child, aliasMap, subqueryAliasToColumnMapping, targetColumn, transformExpression, false);
            }
        }
    }

    /**
     * 在树中查找列引用（支持子查询别名）
     */
    private void extractColumnReferencesFromTreeWithSubquery(
            ParseTree tree,
            Map<String, String> aliasMap,
            Map<String, Map<String, String>> subqueryAliasToColumnMapping,
            String targetColumn,
            String transformExpression,
            boolean insideSubquery) {
        if (tree == null) {
            return;
        }
        // 跳过标量子查询
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            return;
        }

        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            // 跳过标量子查询
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
                // 标记为在 SubqueryContext 内部，传递给递归调用
                extractColumnReferencesFromTreeWithSubquery(child, aliasMap, subqueryAliasToColumnMapping, targetColumn, transformExpression, true);
                continue;
            }
            // 如果已经在 SubqueryContext 内部，跳过处理（列引用由标量子查询处理）
            if (insideSubquery) {
                continue;
            }
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    String alias = parts[0];
                    String column = parts[1];

                    if (subqueryAliasToColumnMapping.containsKey(alias)) {
                        Map<String, String> columnMapping = subqueryAliasToColumnMapping.get(alias);
                        String sourceMapping = columnMapping.get(column);
                        if (sourceMapping != null && sourceMapping.contains(":")) {
                            String[] srcParts = sourceMapping.split(":");
                            String sourceTable = srcParts[0];
                            String sourceCol = srcParts[1];
                            result.addColumnLineage(createColumnLineage(sourceTable, sourceCol, null, targetColumn, transformExpression));
                        }
                    } else {
                        String tableName = aliasMap.get(alias);
                        if (tableName == null) {
                            tableName = alias;
                        }
                        if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(column)) {
                            result.addColumnLineage(createColumnLineage(tableName, column, null, targetColumn, transformExpression));
                        }
                    }
                }
            } else if (!(child instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext)) {
                extractColumnReferencesFromTreeWithSubquery(child, aliasMap, subqueryAliasToColumnMapping, targetColumn, transformExpression, insideSubquery);
            }
        }
    }

    /**
     * 处理子查询
     */
    @Override
    public Object visitSubquery(com.lineage.plsql.parser.PlSqlParser.SubqueryContext ctx) {
        if (ctx == null) {
            return super.visitSubquery(ctx);
        }

        try {
            if (ctx.subquery_basic_elements() != null) {
                com.lineage.plsql.parser.PlSqlParser.Subquery_basic_elementsContext basicCtx = ctx.subquery_basic_elements();

                // 处理query_block获取FROM子句中的表
                if (basicCtx.query_block() != null) {
                    com.lineage.plsql.parser.PlSqlParser.Query_blockContext queryCtx = basicCtx.query_block();

                    // 构建表别名映射
                    Map<String, String> aliasMap = new HashMap<>();
                    if (queryCtx.from_clause() != null) {
                        com.lineage.plsql.parser.PlSqlParser.From_clauseContext fromCtx = queryCtx.from_clause();
                        if (fromCtx.table_ref_list() != null) {
                            for (com.lineage.plsql.parser.PlSqlParser.Table_refContext tableRefCtx : fromCtx.table_ref_list().table_ref()) {
                                processTableRefForSelect(tableRefCtx);
                                aliasMap.putAll(extractTableAliasMap(tableRefCtx));
                            }
                        }
                    }

                    // 处理SELECT列表提取字段血缘
                    if (queryCtx.selected_list() != null) {
                        processSelectList(queryCtx.selected_list(), aliasMap, null);
                    }
                }

                // 处理内嵌子查询
                if (basicCtx.subquery() != null) {
                    visitSubquery(basicCtx.subquery());
                }
            }
        } catch (Exception e) {
            log.error("处理子查询异常: {}", e.getMessage(), e);
        }

        return super.visitSubquery(ctx);
    }

    /**
     * 处理SELECT语句中的表引用
     */
    private void processTableRefForSelect(com.lineage.plsql.parser.PlSqlParser.Table_refContext ctx) {
        if (ctx == null) {
            return;
        }

        // 处理table_ref_aux
        if (ctx.table_ref_aux() != null) {
            com.lineage.plsql.parser.PlSqlParser.Table_ref_auxContext auxCtx = ctx.table_ref_aux();
            if (auxCtx.table_ref_aux_internal() != null) {
                com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internalContext internalCtx = auxCtx.table_ref_aux_internal();

                // Table_ref_aux_internal_one: dml_table_expression_clause (pivot_clause | unpivot_clause)?
                if (internalCtx instanceof com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) {
                    com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext oneCtx =
                            (com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) internalCtx;
                    if (oneCtx.dml_table_expression_clause() != null) {
                        String tableName = extractTableNameFromDmlTableExpr(oneCtx.dml_table_expression_clause());
                        if (StrUtil.isNotBlank(tableName)) {
                            log.debug("发现源表: {}", tableName);
                            result.addTableLineage(createTableLineage(tableName, null, "SELECT"));
                        }
                    }
                }
                // Table_ref_aux_internal_three: ONLY '(' dml_table_expression_clause ')'
                else if (internalCtx instanceof com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_threeContext) {
                    com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_threeContext threeCtx =
                            (com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_threeContext) internalCtx;
                    if (threeCtx.dml_table_expression_clause() != null) {
                        String tableName = extractTableNameFromDmlTableExpr(threeCtx.dml_table_expression_clause());
                        if (StrUtil.isNotBlank(tableName)) {
                            log.debug("发现源表: {}", tableName);
                            result.addTableLineage(createTableLineage(tableName, null, "SELECT"));
                        }
                    }
                }
            }
        }

        // 处理JOIN子句
        if (ctx.join_clause() != null && !ctx.join_clause().isEmpty()) {
            for (com.lineage.plsql.parser.PlSqlParser.Join_clauseContext joinCtx : ctx.join_clause()) {
                processJoinClause(joinCtx);
            }
        }
    }

    /**
     * 处理JOIN子句
     */
    private void processJoinClause(com.lineage.plsql.parser.PlSqlParser.Join_clauseContext ctx) {
        if (ctx == null) {
            return;
        }

        if (ctx.table_ref_aux() != null) {
            processTableRefAuxForSelect(ctx.table_ref_aux());
        }
    }

    /**
     * 处理Table_ref_auxContext获取表名（用于SELECT）
     */
    private void processTableRefAuxForSelect(com.lineage.plsql.parser.PlSqlParser.Table_ref_auxContext ctx) {
        if (ctx == null) {
            return;
        }

        if (ctx.table_ref_aux_internal() != null) {
            com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internalContext internalCtx = ctx.table_ref_aux_internal();

            // Table_ref_aux_internal_one: dml_table_expression_clause (pivot_clause | unpivot_clause)?
            if (internalCtx instanceof com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) {
                com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext oneCtx =
                        (com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) internalCtx;
                if (oneCtx.dml_table_expression_clause() != null) {
                    String tableName = extractTableNameFromDmlTableExpr(oneCtx.dml_table_expression_clause());
                    if (StrUtil.isNotBlank(tableName)) {
                        log.debug("发现源表(JOIN): {}", tableName);
                        result.addTableLineage(createTableLineage(tableName, null, "SELECT"));
                    }
                }
            }
            // Table_ref_aux_internal_three: ONLY '(' dml_table_expression_clause ')'
            else if (internalCtx instanceof com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_threeContext) {
                com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_threeContext threeCtx =
                        (com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_threeContext) internalCtx;
                if (threeCtx.dml_table_expression_clause() != null) {
                    String tableName = extractTableNameFromDmlTableExpr(threeCtx.dml_table_expression_clause());
                    if (StrUtil.isNotBlank(tableName)) {
                        log.debug("发现源表(JOIN): {}", tableName);
                        result.addTableLineage(createTableLineage(tableName, null, "SELECT"));
                    }
                }
            }
        }
    }

    /**
     * 从table_ref提取表名和别名映射
     */
    private Map<String, String> extractTableAliasMap(com.lineage.plsql.parser.PlSqlParser.Table_refContext ctx) {
        Map<String, String> aliasMap = new HashMap<>();
        if (ctx == null || ctx.table_ref_aux() == null) {
            return aliasMap;
        }

        com.lineage.plsql.parser.PlSqlParser.Table_ref_auxContext auxCtx = ctx.table_ref_aux();
        String tableName = null;

        if (auxCtx.table_ref_aux_internal() != null) {
            com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internalContext internalCtx = auxCtx.table_ref_aux_internal();
            if (internalCtx instanceof com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) {
                com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext oneCtx =
                        (com.lineage.plsql.parser.PlSqlParser.Table_ref_aux_internal_oneContext) internalCtx;
                if (oneCtx.dml_table_expression_clause() != null) {
                    tableName = extractTableNameFromDmlTableExpr(oneCtx.dml_table_expression_clause());
                }
            }
        }

        if (auxCtx.table_alias() != null) {
            // 有显式别名
            String alias = auxCtx.table_alias().getText().toUpperCase();
            if (StrUtil.isNotBlank(tableName)) {
                aliasMap.put(alias, tableName);
            }
        } else if (StrUtil.isNotBlank(tableName)) {
            // 没有显式别名，使用表名本身作为别名
            aliasMap.put(tableName, tableName);
        }
        return aliasMap;
    }

    /**
     * 处理SELECT列表，提取字段血缘
     */
    private void processSelectList(com.lineage.plsql.parser.PlSqlParser.Selected_listContext selectedListCtx,
                                   Map<String, String> aliasMap, String defaultTable) {
        if (selectedListCtx == null) {
            return;
        }

        for (com.lineage.plsql.parser.PlSqlParser.Select_list_elementsContext selCtx : selectedListCtx.select_list_elements()) {
            if (selCtx == null) {
                continue;
            }

            // 获取目标列名（别名或表达式）
            String targetColumn = null;
            String transformExpression = null;

            if (selCtx.column_alias() != null) {
                targetColumn = selCtx.column_alias().getText().toUpperCase();
                // 去掉AS关键字
                if (targetColumn.startsWith("AS ")) {
                    targetColumn = targetColumn.substring(3).trim();
                }
            }

            // 获取表达式文本作为变换表达式
            if (selCtx.expression() != null) {
                // 检查是否包含标量子查询
                if (containsScalarSubquery(selCtx.expression())) {
                    transformExpression = null;
                    if (targetColumn == null) {
                        // 从标量子查询中提取列名
                        targetColumn = extractColumnNameFromExpression(selCtx.expression());
                    }
                } else {
                    transformExpression = selCtx.expression().getText();
                    if (targetColumn == null) {
                        // 如果没有别名，使用表达式文本作为目标列
                        targetColumn = transformExpression;
                    }
                }
            }

            // 从表达式中提取列引用
            extractColumnReferencesFromExpression(selCtx.expression(), aliasMap, defaultTable, null, targetColumn, transformExpression);
        }
    }

    /**
     * 从表达式中提取列引用
     */
    private void extractColumnReferencesFromExpression(com.lineage.plsql.parser.PlSqlParser.ExpressionContext exprCtx,
                                                     Map<String, String> aliasMap, String defaultTable,
                                                     String sourceTable, String targetColumn, String transformExpression) {
        if (exprCtx == null) {
            return;
        }

        // 递归遍历表达式树查找列引用
        findColumnReferencesInTree(exprCtx, aliasMap, defaultTable, sourceTable, targetColumn, transformExpression);
    }

    /**
     * 在表达式树中查找列引用
     */
    private void findColumnReferencesInTree(ParseTree tree, Map<String, String> aliasMap, String defaultTable,
                                         String sourceTable, String targetColumn, String transformExpression) {
        if (tree == null) {
            return;
        }
        // 跳过 SubqueryContext - 标量子查询由 processScalarSubqueryInExpression 处理
        if (tree instanceof com.lineage.plsql.parser.PlSqlParser.SubqueryContext) {
            return;
        }

        // 获取节点的文本内容（转换为大写以便匹配）
        String nodeText = tree.getText().toUpperCase();

        // 检查是否是 table.column 形式（如 T.ROWID_OBJECT）
        if (nodeText.contains(".")) {
            String[] parts = nodeText.split("\\.");
            if (parts.length >= 2) {
                String alias = parts[0];
                String column = parts[1];
                // 跳过 T1, T2 等子查询别名的形式
                if (alias.matches("T\\d+") || alias.matches("T$")) {
                    // 这是一个子查询结果列的引用
                    if (StrUtil.isNotBlank(column) && !"ACCT_CODE".equals(column)) {
                        result.addColumnLineage(createColumnLineage("SUBQUERY", column, sourceTable, targetColumn, transformExpression));
                    }
                    return;
                }
                String tableName = aliasMap.get(alias);
                if (tableName == null) {
                    tableName = alias;
                }
                if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(column)) {
                    result.addColumnLineage(createColumnLineage(tableName, column, sourceTable, targetColumn, transformExpression));
                }
                return;
            }
        }

        // 继续遍历子节点
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof com.lineage.plsql.parser.PlSqlParser.Variable_nameContext) {
                // Variable_name 类型
                String fullName = child.getText().toUpperCase();
                if (fullName.contains(".")) {
                    String[] parts = fullName.split("\\.");
                    String alias = parts[0];
                    String column = parts[1];
                    // 跳过子查询别名 T, T1, T2 等
                    if (alias.matches("T\\d*") && alias.length() <= 2) {
                        if (StrUtil.isNotBlank(column)) {
                            result.addColumnLineage(createColumnLineage("SUBQUERY", column, sourceTable, targetColumn, transformExpression));
                        }
                    } else {
                        String tableName = aliasMap.get(alias);
                        if (tableName == null) {
                            tableName = alias;
                        }
                        if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(column)) {
                            result.addColumnLineage(createColumnLineage(tableName, column, sourceTable, targetColumn, transformExpression));
                        }
                    }
                }
            } else if (child instanceof com.lineage.plsql.parser.PlSqlParser.Id_expressionContext) {
                // 处理 id_expression（可能是列名）
                String text = child.getText().toUpperCase();
                // 跳过子查询别名 T, T1, T2 和数字常量
                if (!text.matches("T\\d*") && !text.matches("\\d+") && !text.matches("'.*'")) {
                    if (StrUtil.isNotBlank(defaultTable) && StrUtil.isNotBlank(text)) {
                        result.addColumnLineage(createColumnLineage(defaultTable, text, sourceTable, targetColumn, transformExpression));
                    }
                }
            } else if (child instanceof com.lineage.plsql.parser.PlSqlParser.ConstantContext) {
                // 忽略常量 - 但需要继续检查常量内部的variable_name
                for (int j = 0; j < child.getChildCount(); j++) {
                    ParseTree constantChild = child.getChild(j);
                    findColumnReferencesInTree(constantChild, aliasMap, defaultTable, sourceTable, targetColumn, transformExpression);
                }
            } else {
                // 继续递归检查其他子节点
                findColumnReferencesInTree(child, aliasMap, defaultTable, sourceTable, targetColumn, transformExpression);
            }
        }
    }

    /**
     * 处理INSERT语句
     */
    @Override
    public Object visitInsert_statement(com.lineage.plsql.parser.PlSqlParser.Insert_statementContext ctx) {
        log.debug("处理INSERT语句");

        try {
            String targetTable = null;

            // 获取single_table_insert
            if (ctx.single_table_insert() != null) {
                com.lineage.plsql.parser.PlSqlParser.Single_table_insertContext singleCtx = ctx.single_table_insert();

                // 获取目标表
                if (singleCtx.insert_into_clause() != null) {
                    com.lineage.plsql.parser.PlSqlParser.Insert_into_clauseContext intoCtx = singleCtx.insert_into_clause();
                    targetTable = extractTableNameFromGeneralTableRef(intoCtx.general_table_ref());
                }

                if (StrUtil.isNotBlank(targetTable)) {
                    log.debug("发现目标表: {}", targetTable);
                }

                // 处理SELECT语句（获取源表）
                if (singleCtx.select_statement() != null) {
                    visitSelect_statement(singleCtx.select_statement());

                    // 然后建立从源表到目标表的映射
                    List<String> sourceTables = result.getSourceTableNames();
                    for (String sourceTable : sourceTables) {
                        result.addTableLineage(createTableLineage(sourceTable, targetTable, "INSERT"));
                    }
                }
            }

            result.setSqlType("INSERT");

        } catch (Exception e) {
            log.error("处理INSERT语句异常: {}", e.getMessage(), e);
        }

        return super.visitInsert_statement(ctx);
    }

    /**
     * 处理UPDATE语句
     */
    @Override
    public Object visitUpdate_statement(com.lineage.plsql.parser.PlSqlParser.Update_statementContext ctx) {
        log.debug("处理UPDATE语句");

        try {
            String targetTable = null;
            if (ctx.general_table_ref() != null) {
                targetTable = extractTableNameFromGeneralTableRef(ctx.general_table_ref());
            }

            if (StrUtil.isNotBlank(targetTable)) {
                log.debug("发现目标表: {}", targetTable);
                result.addTableLineage(createTableLineage(null, targetTable, "UPDATE"));
            }

            result.setSqlType("UPDATE");

        } catch (Exception e) {
            log.error("处理UPDATE语句异常: {}", e.getMessage(), e);
        }

        return super.visitUpdate_statement(ctx);
    }

    /**
     * 处理DELETE语句
     */
    @Override
    public Object visitDelete_statement(com.lineage.plsql.parser.PlSqlParser.Delete_statementContext ctx) {
        log.debug("处理DELETE语句");

        try {
            String targetTable = null;
            if (ctx.general_table_ref() != null) {
                targetTable = extractTableNameFromGeneralTableRef(ctx.general_table_ref());
            }

            if (StrUtil.isNotBlank(targetTable)) {
                log.debug("发现目标表: {}", targetTable);
                result.addTableLineage(createTableLineage(null, targetTable, "DELETE"));
            }

            result.setSqlType("DELETE");

        } catch (Exception e) {
            log.error("处理DELETE语句异常: {}", e.getMessage(), e);
        }

        return super.visitDelete_statement(ctx);
    }

    /**
     * 处理CREATE VIEW语句
     */
    @Override
    public Object visitCreate_view(com.lineage.plsql.parser.PlSqlParser.Create_viewContext ctx) {
        log.debug("处理CREATE VIEW语句");

        try {
            String viewName = null;
            if (ctx.tableview_name() != null) {
                viewName = extractTableName(ctx.tableview_name());
            }

            if (StrUtil.isNotBlank(viewName)) {
                log.debug("发现视图: {}", viewName);
            }

            // 处理SELECT语句
            if (ctx.select_only_statement() != null) {
                com.lineage.plsql.parser.PlSqlParser.Select_only_statementContext selectOnlyCtx = ctx.select_only_statement();
                if (selectOnlyCtx.subquery() != null) {
                    visitSubquery(selectOnlyCtx.subquery());

                    // 建立从源表到视图的映射
                    List<String> sourceTables = result.getSourceTableNames();
                    for (String sourceTable : sourceTables) {
                        TableLineage tl = createTableLineage(sourceTable, viewName, "CREATE_VIEW");
                        tl.setIsView(true);
                        result.addTableLineage(tl);
                    }
                }
            }

            result.setSqlType("CREATE_VIEW");

        } catch (Exception e) {
            log.error("处理CREATE VIEW语句异常: {}", e.getMessage(), e);
        }

        return super.visitCreate_view(ctx);
    }

    /**
     * 处理CREATE TABLE语句
     */
    @Override
    public Object visitCreate_table(com.lineage.plsql.parser.PlSqlParser.Create_tableContext ctx) {
        log.debug("处理CREATE TABLE语句");

        try {
            String targetTable = null;
            if (ctx.tableview_name() != null) {
                targetTable = extractTableName(ctx.tableview_name());
            }

            if (StrUtil.isNotBlank(targetTable)) {
                log.debug("发现目标表: {}", targetTable);
            }

            // 处理AS SELECT子句
            if (ctx.select_only_statement() != null) {
                com.lineage.plsql.parser.PlSqlParser.Select_only_statementContext selectOnlyCtx = ctx.select_only_statement();
                if (selectOnlyCtx.subquery() != null) {
                    visitSubquery(selectOnlyCtx.subquery());

                    // 建立从源表到目标表的映射
                    List<String> sourceTables = result.getSourceTableNames();
                    for (String sourceTable : sourceTables) {
                        result.addTableLineage(createTableLineage(sourceTable, targetTable, "CREATE_TABLE"));
                    }
                }
            }

            result.setSqlType("CREATE_TABLE");

        } catch (Exception e) {
            log.error("处理CREATE TABLE语句异常: {}", e.getMessage(), e);
        }

        return super.visitCreate_table(ctx);
    }

    /**
     * 处理ALTER TABLE语句
     */
    @Override
    public Object visitAlter_table(com.lineage.plsql.parser.PlSqlParser.Alter_tableContext ctx) {
        log.debug("处理ALTER TABLE语句");

        try {
            String tableName = null;
            if (ctx.tableview_name() != null) {
                tableName = extractTableName(ctx.tableview_name());
            }

            if (StrUtil.isNotBlank(tableName)) {
                log.debug("发现表: {}", tableName);
                result.addTableLineage(createTableLineage(null, tableName, "ALTER_TABLE"));
            }

            result.setSqlType("ALTER_TABLE");

        } catch (Exception e) {
            log.error("处理ALTER TABLE语句异常: {}", e.getMessage(), e);
        }

        return super.visitAlter_table(ctx);
    }

    /**
     * 处理错误节点
     */
    @Override
    public Object visitErrorNode(ErrorNode node) {
        log.warn("解析错误节点: {}", node.getText());
        return super.visitErrorNode(node);
    }
}