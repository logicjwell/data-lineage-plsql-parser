package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SQL Parser Demo应用程序入口类
 *
 * <p>功能说明：
 * 本程序用于演示如何使用PL/SQL血缘解析器来解析SQL语句，
 * 并提取其中的表级别和字段级别血缘关系。
 *
 * <p>使用方法：
 * 直接运行main方法即可执行测试
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
@SpringBootApplication
public class SqlParserDemoApplication {

    /**
     * 应用程序入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SqlParserDemoApplication.class, args);

        // 运行测试
        TestExample1.main(args);
    }
}
