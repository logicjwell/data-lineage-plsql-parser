package demo;

import cn.hutool.core.io.FileUtil;
import com.lineage.plsql.parser.model.ColumnLineage;
import com.lineage.plsql.parser.model.SqlLineageResult;
import com.lineage.plsql.parser.model.TableLineage;
import com.lineage.plsql.parser.service.SqlLineageService;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Example1 SQL血缘解析测试类
 *
 * <p>测试文件: examples/Example1.sql
 * <p>功能说明: 解析经销商数据查询SQL，提取表级别和字段级别的血缘关系
 *
 * <p>涉及的源表:
 * <ul>
 *   <li>MDM_DEALER_CHILD_FIN - 经销商子表</li>
 *   <li>MDM_DEALER_HEAD_FIN - 经销商主表</li>
 *   <li>APP_ORS.C_BO_DEPARTMENT - 部门表</li>
 *   <li>MDM_ERP_ID_MAPPING - ERP ID映射表</li>
 *   <li>MDM_INVOICE_INFO - 发票信息表</li>
 * </ul>
 *
 * <p>SQL特点:
 * <ul>
 *   <li>包含多个INNER JOIN和LEFT JOIN连接</li>
 *   <li>使用NVL函数处理空值</li>
 *   <li>使用多个自定义函数（F_GET_NAME_BY_CODE、F_GET_GEOREGION_INFO等）</li>
 *   <li>包含子查询在SELECT列表中</li>
 * </ul>
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
@Slf4j
public class TestExample1 {

    /**
     * 测试用的SQL语句（截取自Example1.sql的核心部分）
     * 由于完整SQL较长，这里使用简化版本进行测试
     */
    private static String TEST_SQL = "select * from \n" +
            "  (SELECT\n" +
            "  (SELECT DLER_NAME FROM MDM_DEALER_HEAD_FIN WHERE ROWID_OBJECT = T2.LINKED_DEALER_ID) LINKED_DEALER_NAME,\n" +
            "  (SELECT DLER_CODE FROM MDM_DEALER_HEAD_FIN WHERE ROWID_OBJECT = T2.LINKED_DEALER_ID) LINKED_DEALER_CODE,\n" +
            "   (SELECT WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE, 'NAME', '_')) FROM TABLE(F_SPLIT(T1.BUSINESS_SCOPE_CODE,','))) BUSINESS_SCOPE_NAME\n" +
            "   FROM MDM_DEALER_CHILD_FIN T1) T";
//    private static final String TEST_SQL = "select * FROM TEST;";

    /**
     * 主函数 - 运行测试
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        log.info("========== Example1 SQL血缘解析测试开始 ==========");
//        TEST_SQL = FileUtil.readString("C:\\AI\\lineage\\examples\\Example1.sql", Charset.defaultCharset());
        try {
            // 初始化解析服务
            SqlLineageService sqlLineageService = new SqlLineageService();

            // 执行血缘解析
            log.info("开始解析SQL血缘关系...");
            SqlLineageResult result = sqlLineageService.parseLineage(TEST_SQL, "EXAMPLE1_TEST");

            // 输出解析结果
            printParseResult(result);

            log.info("========== Example1 SQL血缘解析测试完成 ==========");

        } catch (Exception e) {
            log.error("测试执行异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 打印解析结果
     *
     * @param result 血缘解析结果
     */
    private static void printParseResult(SqlLineageResult result) {
        if (result == null) {
            log.error("解析结果为空");
            return;
        }

        // 基本信息
        log.info("=== 解析基本信息 ===");
        log.info("SQL类型: {}", result.getSqlType());
        log.info("解析成功: {}", result.isSuccess());
        log.info("解析耗时: {} ms", result.getParseTime());
        log.info("语句ID: {}", result.getStatementId());

        // 如果解析失败，输出错误信息
        if (!result.isSuccess()) {
            log.error("解析失败原因: {}", result.getErrorMessage());
            return;
        }

        // 表血缘关系
        log.info("=== 表血缘关系 ===");
        List<TableLineage> tableLineages = result.getTableLineages();
        if (tableLineages != null && !tableLineages.isEmpty()) {
            log.info("共发现 {} 条表血缘关系:", tableLineages.size());
            for (TableLineage tl : tableLineages) {
                log.info("  源表: {} -> 目标表: {} (SQL类型: {})",
                        tl.getSourceTable(),
                        tl.getTargetTable(),
                        tl.getSqlType());
            }
        } else {
            log.info("未发现表血缘关系");
        }

        // 源表列表
        log.info("=== 源表列表 ===");
        List<String> sourceTables = result.getSourceTableNames();
        if (sourceTables != null && !sourceTables.isEmpty()) {
            for (String table : sourceTables) {
                log.info("  - {}", table);
            }
        } else {
            log.info("未发现源表");
        }

        // 目标表列表
        log.info("=== 目标表列表 ===");
        List<String> targetTables = result.getTargetTableNames();
        if (targetTables != null && !targetTables.isEmpty()) {
            for (String table : targetTables) {
                log.info("  - {}", table);
            }
        } else {
            log.info("未发现目标表");
        }

        // 字段血缘关系（只显示前20条）
        log.info("=== 字段血缘关系 ===");
        List<ColumnLineage> columnLineages = result.getColumnLineages();
        if (columnLineages != null && !columnLineages.isEmpty()) {
            log.info("共发现 {} 条字段血缘关系:", columnLineages.size());
            int count = 0;
            for (ColumnLineage cl : columnLineages) {

                log.info("  {}. {}:{}{} -> {}:{}",
                        count + 1,
                        cl.getSourceTable(),
                        cl.getSourceColumn(),
                        StrUtil.isNotBlank(cl.getTransformExpression()) ? " (" + cl.getTransformExpression() + ")" : "",
                        cl.getTargetTable(),
                        cl.getTargetColumn());
                count++;
            }
        } else {
            log.info("未发现字段血缘关系");
        }

        // 原始SQL（截断显示）
        log.info("=== 原始SQL ===");
        String originalSql = result.getOriginalSql();
        if (StrUtil.isNotBlank(originalSql)) {
            String displaySql = originalSql;
            log.info("SQL内容: {}", displaySql);
        }
    }

    // StrUtil引用，用于字段血缘输出的空值判断
    private static class StrUtil {
        public static boolean isNotBlank(String str) {
            return str != null && !str.trim().isEmpty();
        }
    }
}
