package com.lineage.plsql.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;

/**
 * PL/SQL Parser基础类
 * 为ANTLR生成的Parser提供必要的方法
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
public class PlSqlParserBase extends Parser {

    /**
     * self引用，用于兼容C++风格的grammar代码
     * 在C++中self是指向当前对象的指针，Java中等价于this
     */
    protected PlSqlParserBase self = this;

    /**
     * 构造函数
     *
     * @param input 输入的token流
     */
    protected PlSqlParserBase(TokenStream input) {
        super(input);
    }

    /**
     * 检查是否为版本12
     * 用于PL/SQL语法兼容性检查
     *
     * @return 版本标志
     */
    public boolean isVersion12() {
        return true;
    }

    /**
     * 检查是否为版本10
     * 用于PL/SQL语法兼容性检查
     *
     * @return 版本标志
     */
    public boolean isVersion10() {
        return true;
    }

    /**
     * 获取ATN（增强转换器-神经网络）自动机
     * 子类会覆盖此方法返回实际的ATN
     */
    @Override
    public ATN getATN() {
        throw new UnsupportedOperationException("ATN should be provided by generated parser");
    }

    /**
     * 获取Token名称数组
     * 子类会覆盖此方法返回实际的Token名称
     */
    @Override
    public String[] getTokenNames() {
        throw new UnsupportedOperationException("Token names should be provided by generated parser");
    }

    /**
     * 获取规则名称数组
     * 子类会覆盖此方法返回实际的规则名称
     */
    @Override
    public String[] getRuleNames() {
        throw new UnsupportedOperationException("Rule names should be provided by generated parser");
    }

    /**
     * 获取语法文件名
     * 子类会覆盖此方法返回实际的语法文件名
     */
    @Override
    public String getGrammarFileName() {
        throw new UnsupportedOperationException("Grammar file name should be provided by generated parser");
    }
}