package com.lineage.plsql.parser.config;

import com.lineage.plsql.parser.service.SqlLineageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PL/SQL解析器自动配置类
 * 当项目中存在SqlLineageService时，自动配置解析器相关Bean
 *
 * <p>配置内容：
 * <ul>
 *   <li>SqlLineageService - SQL血缘解析服务</li>
 * </ul>
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(SqlLineageService.class)
public class PlSqlParserAutoConfiguration {

    /**
     * 配置SQL血缘解析服务
     *
     * @return SqlLineageService实例
     */
    @Bean
    @ConditionalOnMissingBean(SqlLineageService.class)
    public SqlLineageService sqlLineageService() {
        log.info("初始化SQL血缘解析服务");
        return new SqlLineageService();
    }
}
