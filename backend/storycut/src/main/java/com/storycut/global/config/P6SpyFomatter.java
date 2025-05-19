package com.storycut.global.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import java.util.Locale;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;

@Configuration
public class P6SpyFomatter implements MessageFormattingStrategy {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category,
        String prepared, String sql, String url) {

        // STATEMENT (실제 SQL 실행) 외에는 로그 무시
        if (!Category.STATEMENT.getName().equals(category)) {
            return "";  // 또는 return null;
        }

        // SQL이 없으면 무시
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        String formattedSql = formatSql(category, sql);
        return String.format("""
                        
            ___________________ 🛢️ SQL 실행 시작(⏳ 소모시간: %dms ) ______
            %s
            ___________________ 🛢️ SQL 실행 종료 ________________________
            """, elapsed, formattedSql);
    }

    private String formatSql(String category, String sql) {
        if (Category.STATEMENT.getName().equals(category)) {
            String trimmedSql = sql.trim().toLowerCase(Locale.ROOT);
            if (trimmedSql.startsWith("create") || trimmedSql.startsWith("alter")
                || trimmedSql.startsWith("comment")) {
                return FormatStyle.DDL.getFormatter().format(sql);
            } else {
                return FormatStyle.BASIC.getFormatter().format(sql);
            }
        }
        return sql;
    }
}