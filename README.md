# SQL Parser - 数据血缘解析系统

基于ANTLR4的Oracle SQL解析器，用于提取数据血缘关系。

## 项目结构

```
sqlparser/
├── pom.xml                           # 父项目POM文件
├── springboot-plsql-parser-starter/  # Spring Boot Starter模块
│   ├── pom.xml
│   ├── src/main/
│   │   ├── antlr4/                   # ANTLR4语法文件
│   │   │   └── com/lineage/plsql/parser/
│   │   │       ├── PlSqlLexer.g4    # 词法分析器
│   │   │       └── PlSqlParser.g4   # 语法分析器
│   │   ├── java/                     # Java源代码
│   │   │   └── com/lineage/plsql/parser/
│   │   │       ├── config/           # 配置类
│   │   │       ├── model/            # 数据模型
│   │   │       ├── service/          # 服务类
│   │   │       └── visitor/          # 血缘解析访问器
│   │   └── resources/
│   │       └── META-INF/             # Spring配置
│   └── db/                           # 数据库建表语句
│       ├── oracle/data_lineage.sql   # Oracle建表语句
│       └── mysql/data_lineage.sql    # MySQL建表语句
│
└── sqlparser-demo/                   # 使用示例模块
    ├── pom.xml
    └── src/main/
        ├── java/demo/
        │   └── TestExample1.java     # 示例测试类
        └── resources/
            ├── application.properties
            └── logback.xml
```

## 模块说明

### 1. springboot-plsql-parser-starter

核心解析模块，提供SQL血缘关系解析功能。

**主要功能：**
- 解析SELECT、INSERT、UPDATE、DELETE语句
- 解析CREATE VIEW、CREATE TABLE语句
- 提取表级别血缘关系
- 提取字段级别血缘关系

**技术栈：**
- ANTLR4 4.7.2
- Spring Boot 2.7.18
- Lombok
- Hutool
- SLF4J

### 2. sqlparser-demo

使用示例模块，展示如何调用解析器。

## 编译说明

### 环境要求

- JDK 1.8+
- Maven 3.6+

### 编译步骤

1. **完整编译（包含ANTLR代码生成）：**
```bash
cd sqlparser
mvn clean compile -DskipTests
```

2. **仅编译Java代码（如ANTLR已生成）：**
```bash
mvn compile -DskipTests
```

3. **安装到本地Maven仓库：**
```bash
mvn clean install -DskipTests
```

### 注意事项

1. **ANTLR代码生成**：
   
   - ANTLR会从`PlSqlLexer.g4`和`PlSqlParser.g4`生成Java代码
   - 生成的代码位于`target/generated-sources/antlr4`目录
   - PlSqlParser.java文件较大（约8MB），编译需要较长的时间
   
2. **内存配置**：
   - 建议配置Maven内存：
   ```bash
   set MAVEN_OPTS=-Xmx2g
   ```

## 数据库设置

### Oracle数据库

执行`db/oracle/data_lineage.sql`创建所需表：

```sql
-- 在SQLPlus或SQL Developer中执行
@db/oracle/data_lineage.sql
```

### MySQL数据库

执行`db/mysql/data_lineage.sql`创建所需表：

```sql
-- 在MySQL客户端中执行
source db/mysql/data_lineage.sql
```

## 使用示例

### 解析SQL血缘关系

```java
import com.lineage.plsql.parser.service.SqlLineageService;
import com.lineage.plsql.parser.model.SqlLineageResult;

public class Demo {
    public static void main(String[] args) {
        // 创建解析服务
        SqlLineageService service = new SqlLineageService();

        // 要解析的SQL
        String sql = "SELECT T1.COL1, T2.COL2 FROM TABLE1 T1 " +
                     "JOIN TABLE2 T2 ON T1.ID = T2.ID";

        // 解析血缘关系
        SqlLineageResult result = service.parseLineage(sql);

        // 输出结果
        System.out.println("源表: " + result.getSourceTableNames());
        System.out.println("目标表: " + result.getTargetTableNames());
        System.out.println("字段血缘数: " + result.getColumnLineages().size());
    }
}
```

### 运行Demo测试

```bash
cd sqlparser-demo
mvn compile exec:java -Dexec.mainClass="demo.TestExample1"
```

## 数据血缘模型

### TableLineage - 表血缘关系

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| sourceTable | String | 源表名称 |
| sourceAlias | String | 源表别名 |
| targetTable | String | 目标表名称 |
| targetAlias | String | 目标表别名 |
| sqlType | String | SQL类型(SELECT/INSERT/UPDATE/DELETE) |
| operationType | String | 操作类型(READ/WRITE/READ_WRITE) |
| isView | Boolean | 是否为视图 |
| statementId | String | 语句唯一标识 |
| createTime | Long | 创建时间 |

### ColumnLineage - 字段血缘关系

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| sourceTable | String | 源表名称 |
| sourceColumn | String | 源字段名称 |
| targetTable | String | 目标表名称 |
| targetColumn | String | 目标字段名称 |
| transformExpression | String | 转换表达式 |
| sqlType | String | SQL类型 |
| direction | String | 血缘方向(IN/OUT) |
| statementId | String | 语句唯一标识 |
| createTime | Long | 创建时间 |

## 支持的SQL类型

- **SELECT** - 查询语句血缘解析
- **INSERT** - 插入语句血缘解析（包含SELECT子查询）
- **UPDATE** - 更新语句血缘解析
- **DELETE** - 删除语句血缘解析
- **CREATE_VIEW** - 创建视图血缘解析
- **CREATE_TABLE** - 创建表（AS SELECT）血缘解析
- **ALTER_TABLE** - 修改表血缘解析

## 测试日志

### 测试SQL

```sql
select * from 
  (SELECT
  (SELECT DLER_NAME FROM MDM_DEALER_HEAD_FIN WHERE ROWID_OBJECT = T2.LINKED_DEALER_ID) LINKED_DEALER_NAME,
  (SELECT DLER_CODE FROM MDM_DEALER_HEAD_FIN WHERE ROWID_OBJECT = T2.LINKED_DEALER_ID) LINKED_DEALER_CODE,
   (SELECT WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE, 'NAME', '_')) FROM TABLE(F_SPLIT(T1.BUSINESS_SCOPE_CODE,','))) BUSINESS_SCOPE_NAME
   FROM MDM_DEALER_CHILD_FIN T1) T
```

### 测试结果

```markdown
2026-03-30 15:01:30.940 [main] INFO  demo.TestExample1 - ========== Example1 SQL血缘解析测试开始 ==========
2026-03-30 15:01:30.948 [main] INFO  demo.TestExample1 - 开始解析SQL血缘关系...
2026-03-30 15:01:30.948 [main] INFO  c.l.p.p.service.SqlLineageService - 开始解析SQL血缘关系, 语句ID: EXAMPLE1_TEST
2026-03-30 15:02:32.046 [main] INFO  demo.TestExample1 - ========== Example1 SQL血缘解析测试开始 ==========
2026-03-30 15:02:32.050 [main] INFO  demo.TestExample1 - 开始解析SQL血缘关系...
2026-03-30 15:02:32.051 [main] INFO  c.l.p.p.service.SqlLineageService - 开始解析SQL血缘关系, 语句ID: EXAMPLE1_TEST
2026-03-30 15:02:33.792 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - SQL解析树: (sql_script (unit_statement (data_manipulation_language_statements (select_statement (select_only_statement (subquery (subquery_basic_elements (query_block SELECT (selected_list *) (from_clause FROM (table_ref_list (table_ref (table_ref_aux (table_ref_aux_internal (dml_table_expression_clause ( (select_statement (select_only_statement (subquery (subquery_basic_elements (query_block SELECT (selected_list (select_list_elements (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom ( (subquery (subquery_basic_elements (query_block SELECT (selected_list (select_list_elements (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string (variable_name (id_expression (regular_id DLER_NAME))))))))))))))))) (from_clause FROM (table_ref_list (table_ref (table_ref_aux (table_ref_aux_internal (dml_table_expression_clause (tableview_name (identifier (id_expression (regular_id MDM_DEALER_HEAD_FIN)))))))))) (where_clause WHERE (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string (variable_name (id_expression (regular_id ROWID_OBJECT))))))))))) (relational_operator =) (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string (variable_name (id_expression (regular_id T2)) . (id_expression (regular_id LINKED_DEALER_ID)))))))))))))))))))) ))))))))))) (column_alias (identifier (id_expression (regular_id LINKED_DEALER_NAME))))) , (select_list_elements (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom ( (subquery (subquery_basic_elements (query_block SELECT (selected_list (select_list_elements (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string (variable_name (id_expression (regular_id DLER_CODE))))))))))))))))) (from_clause FROM (table_ref_list (table_ref (table_ref_aux (table_ref_aux_internal (dml_table_expression_clause (tableview_name (identifier (id_expression (regular_id MDM_DEALER_HEAD_FIN)))))))))) (where_clause WHERE (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string (variable_name (id_expression (regular_id ROWID_OBJECT))))))))))) (relational_operator =) (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string (variable_name (id_expression (regular_id T2)) . (id_expression (regular_id LINKED_DEALER_ID)))))))))))))))))))) ))))))))))) (column_alias (identifier (id_expression (regular_id LINKED_DEALER_CODE))))) , (select_list_elements (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom ( (subquery (subquery_basic_elements (query_block SELECT (selected_list (select_list_elements (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (general_element (general_element_part (id_expression (regular_id WM_CONCAT)) (function_argument ( (argument (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (general_element (general_element_part (id_expression (regular_id APP_ORS)) . (id_expression (regular_id F_GET_VALUE_BY_ROWID)) (function_argument ( (argument (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string (variable_name (id_expression (regular_id (non_reserved_keywords_pre12c COLUMN_VALUE))))))))))))))))) , (argument (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string 'NAME'))))))))))))) , (argument (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string '_'))))))))))))) ))))))))))))))) )))))))))))))))) (from_clause FROM (table_ref_list (table_ref (table_ref_aux (table_ref_aux_internal (dml_table_expression_clause (table_collection_expression TABLE ( (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (general_element (general_element_part (id_expression (regular_id F_SPLIT)) (function_argument ( (argument (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string (variable_name (id_expression (regular_id T1)) . (id_expression (regular_id BUSINESS_SCOPE_CODE)))))))))))))))) , (argument (expression (logical_expression (unary_logical_expression (multiset_expression (relational_expression (compound_expression (concatenation (model_expression (unary_expression (atom (constant (quoted_string ','))))))))))))) )))))))))))))) ))))))))))) ))))))))))) (column_alias (identifier (id_expression (regular_id BUSINESS_SCOPE_NAME)))))) (from_clause FROM (table_ref_list (table_ref (table_ref_aux (table_ref_aux_internal (dml_table_expression_clause (tableview_name (identifier (id_expression (regular_id MDM_DEALER_CHILD_FIN)))))) (table_alias (identifier (id_expression (regular_id T1))))))))))))) ))) (table_alias (identifier (id_expression (regular_id T))))))))))))))) <EOF>)
2026-03-30 15:02:33.793 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - 处理SELECT语句
2026-03-30 15:02:33.793 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - 处理SELECT语句
2026-03-30 15:02:33.794 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - 发现源表: MDM_DEALER_CHILD_FIN
2026-03-30 15:02:33.795 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.796 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.796 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.796 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.796 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.796 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.796 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.796 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.797 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.797 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - (
2026-03-30 15:02:33.797 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: SubqueryContext - SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID
2026-03-30 15:02:33.797 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - Found SubqueryContext: SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID
2026-03-30 15:02:33.797 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: localAliasMap={MDM_DEALER_HEAD_FIN=MDM_DEALER_HEAD_FIN}
2026-03-30 15:02:33.797 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - extractColumnRefsRecursively: Variable_name = ROWID_OBJECT
2026-03-30 15:02:33.798 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - extractColumnRefsRecursively: Added ref (no table prefix) ROWID_OBJECT
2026-03-30 15:02:33.798 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - extractColumnRefsRecursively: Variable_name = T2.LINKED_DEALER_ID
2026-03-30 15:02:33.798 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - extractColumnRefsRecursively: Added ref T2:LINKED_DEALER_ID
2026-03-30 15:02:33.798 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: mergedAliasMap={MDM_DEALER_HEAD_FIN=MDM_DEALER_HEAD_FIN, T1=MDM_DEALER_CHILD_FIN}, whereColumnRefs=[com.lineage.plsql.parser.visitor.PlSqlLineageVisitor$ColumnRefInfo@5badeda0, com.lineage.plsql.parser.visitor.PlSqlLineageVisitor$ColumnRefInfo@56a9a7b5]
2026-03-30 15:02:33.798 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: innerColumnRef=com.lineage.plsql.parser.visitor.PlSqlLineageVisitor$ColumnRefInfo@5badeda0, outerColumnRef=com.lineage.plsql.parser.visitor.PlSqlLineageVisitor$ColumnRefInfo@56a9a7b5
2026-03-30 15:02:33.798 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: inferred innerTable=MDM_DEALER_HEAD_FIN from localAliasMap
2026-03-30 15:02:33.800 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: inferred innerTable=MDM_DEALER_HEAD_FIN for merged mapping
2026-03-30 15:02:33.800 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.800 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.800 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.800 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.800 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.800 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.800 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.801 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.801 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.801 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - (
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: SubqueryContext - SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - Found SubqueryContext: SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: localAliasMap={MDM_DEALER_HEAD_FIN=MDM_DEALER_HEAD_FIN}
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - extractColumnRefsRecursively: Variable_name = ROWID_OBJECT
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - extractColumnRefsRecursively: Added ref (no table prefix) ROWID_OBJECT
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - extractColumnRefsRecursively: Variable_name = T2.LINKED_DEALER_ID
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - extractColumnRefsRecursively: Added ref T2:LINKED_DEALER_ID
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: mergedAliasMap={MDM_DEALER_HEAD_FIN=MDM_DEALER_HEAD_FIN, T1=MDM_DEALER_CHILD_FIN}, whereColumnRefs=[com.lineage.plsql.parser.visitor.PlSqlLineageVisitor$ColumnRefInfo@1dd247b, com.lineage.plsql.parser.visitor.PlSqlLineageVisitor$ColumnRefInfo@338270ea]
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: innerColumnRef=com.lineage.plsql.parser.visitor.PlSqlLineageVisitor$ColumnRefInfo@1dd247b, outerColumnRef=com.lineage.plsql.parser.visitor.PlSqlLineageVisitor$ColumnRefInfo@338270ea
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: inferred innerTable=MDM_DEALER_HEAD_FIN from localAliasMap
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: inferred innerTable=MDM_DEALER_HEAD_FIN for merged mapping
2026-03-30 15:02:33.802 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.803 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.803 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.803 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.803 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.803 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.803 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.803 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.804 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.804 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - (
2026-03-30 15:02:33.804 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: SubqueryContext - SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS_...
2026-03-30 15:02:33.804 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - Found SubqueryContext: SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS_SCOPE_CODE,','))
2026-03-30 15:02:33.804 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - processSubqueryForLineage: localAliasMap={}
2026-03-30 15:02:33.805 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - 发现源表: MDM_DEALER_CHILD_FIN
2026-03-30 15:02:33.805 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.805 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - (
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: SubqueryContext - SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID
2026-03-30 15:02:33.806 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - Found SubqueryContext: SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID)
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - (
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: SubqueryContext - SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID
2026-03-30 15:02:33.807 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - Found SubqueryContext: SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2.LINKED_DEALER_ID
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - (SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - (
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: SubqueryContext - SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS_...
2026-03-30 15:02:33.808 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - Found SubqueryContext: SELECTWM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1.BUSINESS_SCOPE_CODE,','))
2026-03-30 15:02:33.809 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - 发现源表: MDM_DEALER_HEAD_FIN
2026-03-30 15:02:33.809 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - DLER_NAME
2026-03-30 15:02:33.809 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - DLER_NAME
2026-03-30 15:02:33.809 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - DLER_NAME
2026-03-30 15:02:33.809 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - DLER_NAME
2026-03-30 15:02:33.809 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - DLER_NAME
2026-03-30 15:02:33.810 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - DLER_NAME
2026-03-30 15:02:33.810 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - DLER_NAME
2026-03-30 15:02:33.810 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - DLER_NAME
2026-03-30 15:02:33.810 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - DLER_NAME
2026-03-30 15:02:33.810 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConstantContext - DLER_NAME
2026-03-30 15:02:33.811 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Quoted_stringContext - DLER_NAME
2026-03-30 15:02:33.811 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Variable_nameContext - DLER_NAME
2026-03-30 15:02:33.811 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Id_expressionContext - DLER_NAME
2026-03-30 15:02:33.811 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Regular_idContext - DLER_NAME
2026-03-30 15:02:33.811 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - DLER_NAME
2026-03-30 15:02:33.811 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - 发现源表: MDM_DEALER_HEAD_FIN
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConstantContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Quoted_stringContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Variable_nameContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Id_expressionContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Regular_idContext - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - DLER_CODE
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.812 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: General_elementContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: General_element_partContext - WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Id_expressionContext - WM_CONCAT
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Regular_idContext - WM_CONCAT
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - WM_CONCAT
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Function_argumentContext - (APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - (
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ArgumentContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ExpressionContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.813 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: General_elementContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: General_element_partContext - APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Id_expressionContext - APP_ORS
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Regular_idContext - APP_ORS
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - APP_ORS
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - .
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Id_expressionContext - F_GET_VALUE_BY_ROWID
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Regular_idContext - F_GET_VALUE_BY_ROWID
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - F_GET_VALUE_BY_ROWID
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Function_argumentContext - (COLUMN_VALUE,'NAME','_')
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - (
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ArgumentContext - COLUMN_VALUE
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ExpressionContext - COLUMN_VALUE
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - COLUMN_VALUE
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - COLUMN_VALUE
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - COLUMN_VALUE
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - COLUMN_VALUE
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - COLUMN_VALUE
2026-03-30 15:02:33.814 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConstantContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Quoted_stringContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Variable_nameContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Id_expressionContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Regular_idContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Non_reserved_keywords_pre12cContext - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - COLUMN_VALUE
2026-03-30 15:02:33.815 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - ,
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ArgumentContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ExpressionContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConstantContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Quoted_stringContext - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - 'NAME'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - ,
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ArgumentContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ExpressionContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Logical_expressionContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_logical_expressionContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Multiset_expressionContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Relational_expressionContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Compound_expressionContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConcatenationContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Model_expressionContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Unary_expressionContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: AtomContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: ConstantContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: Quoted_stringContext - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - '_'
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - )
2026-03-30 15:02:33.816 [main] DEBUG c.l.p.p.visitor.PlSqlLineageVisitor - containsScalarSubquery traversing: TerminalNodeImpl - )
2026-03-30 15:02:33.817 [main] INFO  c.l.p.p.visitor.PlSqlLineageVisitor - SQL解析成功，耗时: 1731ms
2026-03-30 15:02:33.817 [main] INFO  c.l.p.p.service.SqlLineageService - SQL血缘关系解析完成, 语句ID: EXAMPLE1_TEST, 源表数: 2, 目标表数: 0, 字段血缘数: 13
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 - === 解析基本信息 ===
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 - SQL类型: SELECT
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 - 解析成功: true
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 - 解析耗时: 1731 ms
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 - 语句ID: EXAMPLE1_TEST
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 - === 表血缘关系 ===
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 - 共发现 4 条表血缘关系:
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 -   源表: MDM_DEALER_CHILD_FIN -> 目标表: null (SQL类型: SELECT)
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 -   源表: MDM_DEALER_CHILD_FIN -> 目标表: null (SQL类型: SELECT)
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 -   源表: MDM_DEALER_HEAD_FIN -> 目标表: null (SQL类型: SELECT)
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 -   源表: MDM_DEALER_HEAD_FIN -> 目标表: null (SQL类型: SELECT)
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 - === 源表列表 ===
2026-03-30 15:02:33.817 [main] INFO  demo.TestExample1 -   - MDM_DEALER_CHILD_FIN
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   - MDM_DEALER_HEAD_FIN
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 - === 目标表列表 ===
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 - 未发现目标表
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 - === 字段血缘关系 ===
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 - 共发现 13 条字段血缘关系:
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   1. MDM_DEALER_HEAD_FIN:ROWID_OBJECT -> null:DLER_NAME
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   2. null:DLER_NAME -> null:LINKED_DEALER_NAME
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   3. MDM_DEALER_HEAD_FIN:ROWID_OBJECT -> null:LINKED_DEALER_NAME
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   4. T2:LINKED_DEALER_ID -> null:LINKED_DEALER_NAME
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   5. MDM_DEALER_HEAD_FIN:ROWID_OBJECT -> null:DLER_CODE
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   6. null:DLER_CODE -> null:LINKED_DEALER_CODE
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   7. MDM_DEALER_HEAD_FIN:ROWID_OBJECT -> null:LINKED_DEALER_CODE
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   8. T2:LINKED_DEALER_ID -> null:LINKED_DEALER_CODE
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   9. null:WM_CONCAT -> null:BUSINESS_SCOPE_NAME
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   10. (SELECTDLER_NAMEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2:LINKED_DEALER_ID) -> null:LINKED_DEALER_NAME
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   11. (SELECTDLER_CODEFROMMDM_DEALER_HEAD_FINWHEREROWID_OBJECT=T2:LINKED_DEALER_ID) -> null:LINKED_DEALER_CODE
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   12. (SELECTWM_CONCAT(APP_ORS:F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))FROMTABLE(F_SPLIT(T1 -> null:BUSINESS_SCOPE_NAME
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 -   13. WM_CONCAT(APP_ORS:F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_')) (WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))) -> null:WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE,'NAME','_'))
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 - === 原始SQL ===
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 - SQL内容: select * from 
  (SELECT
  (SELECT DLER_NAME FROM MDM_DEALER_HEAD_FIN WHERE ROWID_OBJECT = T2.LINKED_DEALER_ID) LINKED_DEALER_NAME,
  (SELECT DLER_CODE FROM MDM_DEALER_HEAD_FIN WHERE ROWID_OBJECT = T2.LINKED_DEALER_ID) LINKED_DEALER_CODE,
   (SELECT WM_CONCAT(APP_ORS.F_GET_VALUE_BY_ROWID(COLUMN_VALUE, 'NAME', '_')) FROM TABLE(F_SPLIT(T1.BUSINESS_SCOPE_CODE,','))) BUSINESS_SCOPE_NAME
   FROM MDM_DEALER_CHILD_FIN T1) T
2026-03-30 15:02:33.818 [main] INFO  demo.TestExample1 - ========== Example1 SQL血缘解析测试完成 ==========
```

