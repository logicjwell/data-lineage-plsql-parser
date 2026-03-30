package com.lineage.plsql.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;

/**
 * PL/SQL Lexer基础类
 * 为ANTLR生成的Lexer提供必要的方法
 *
 * @author YuQun(logicjwell@126.com)
 * @version 1.0.0
 */
public class PlSqlLexerBase extends Lexer {

    /**
     * self引用，用于兼容C++风格的grammar代码
     * 在C++中self是指向当前对象的指针，Java中等价于this
     */
    protected PlSqlLexerBase self = this;

    /**
     * 构造函数
     *
     * @param input 输入的字符流
     */
    protected PlSqlLexerBase(CharStream input) {
        super(input);
    }

    /**
     * 检查指定位置是否为换行符
     * 用于REMARK_COMMENT和PROMPT_MESSAGE等规则的语义谓词
     *
     * @param pos 相对当前位置的偏移量
     * @return 如果指定位置是换行符则返回true
     */
    public boolean IsNewlineAtPos(int pos) {
        int index = _input.index() + pos;
        if (index < 0 || index >= _input.size()) {
            return false;
        }
        // 使用LA方法获取指定位置的字符
        int ch = _input.LA(index - _input.index() + _input.index());
        return ch == '\n' || ch == '\r';
    }

    /**
     * 获取词法分析器实例
     * 用于grammar中的语义谓词
     *
     * @return Lexer实例
     */
    public Lexer getLexer() {
        return this;
    }

    /**
     * 获取ATN（增强转换器-神经网络）自动机
     * 子类会覆盖此方法返回实际的ATN
     */
    @Override
    public ATN getATN() {
        throw new UnsupportedOperationException("ATN should be provided by generated lexer");
    }

    /**
     * 获取Token名称数组
     * 子类会覆盖此方法返回实际的Token名称
     */
    @Override
    public String[] getTokenNames() {
        throw new UnsupportedOperationException("Token names should be provided by generated lexer");
    }

    /**
     * 获取规则名称数组
     * 子类会覆盖此方法返回实际的规则名称
     */
    @Override
    public String[] getRuleNames() {
        throw new UnsupportedOperationException("Rule names should be provided by generated lexer");
    }

    /**
     * 获取语法文件名
     * 子类会覆盖此方法返回实际的语法文件名
     */
    @Override
    public String getGrammarFileName() {
        throw new UnsupportedOperationException("Grammar file name should be provided by generated lexer");
    }
}