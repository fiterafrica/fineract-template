/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.core.service;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseTypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class PaginationHelper {

    private final DatabaseTypeResolver databaseTypeResolver;

    /**
     * Pattern to detect potentially dangerous SQL injection patterns. This pattern matches:
     * - Multiple statements (semicolons followed by SQL keywords)
     * - UNION-based injection attempts
     * - Comment-based injection (-- or /*)
     * - Common SQL injection keywords in suspicious positions (e.g., ' OR 1=1 or ' AND 1=1)
     * Note: The OR/AND pattern specifically looks for injection attempts like ' OR '1'='1
     * and avoids matching legitimate SQL BETWEEN clauses like 'date1' and 'date2'
     */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(;\\s*(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|EXEC|EXECUTE|UNION|--|/\\*))" +
            "|('\\s*(OR|AND)\\s+['\"]?[01])" +
            "|('\\s*=\\s*')" +
            "|(--\\s*$)" +
            "|(/\\*.*\\*/)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    @Autowired
    public PaginationHelper(DatabaseTypeResolver databaseTypeResolver) {
        this.databaseTypeResolver = databaseTypeResolver;
    }

    /**
     * Validates the SQL query to detect potential SQL injection patterns.
     * This is a defense-in-depth measure as the SQL is constructed internally,
     * but adds an extra layer of protection.
     *
     * @param sql the SQL query to validate
     * @throws PlatformDataIntegrityException if the SQL contains suspicious patterns
     */
    private void validateSqlQuery(final String sql) {
        if (sql == null || sql.isBlank()) {
            throw new PlatformDataIntegrityException("error.msg.sql.query.empty",
                    "SQL query cannot be null or empty");
        }

        if (SQL_INJECTION_PATTERN.matcher(sql).find()) {
            throw new PlatformDataIntegrityException("error.msg.sql.query.invalid",
                    "SQL query contains potentially unsafe patterns");
        }
    }

    /**
     * Fetches a paginated result set using the provided SQL query and parameters.
     * The SQL query structure is built internally by the application and user input
     * is passed through the args parameter which uses prepared statement binding.
     *
     * @param jt the JdbcTemplate to use for query execution
     * @param sqlFetchRows the SQL query string (structure built internally, not from user input)
     * @param args the query parameters (user input bound via prepared statements)
     * @param rowMapper the row mapper to convert results
     * @return a Page containing the results and total count
     */
    public <E> Page<E> fetchPage(final JdbcTemplate jt, final String sqlFetchRows, final Object[] args, final RowMapper<E> rowMapper) {
        // Validate SQL to detect any injection patterns as a defense-in-depth measure
        validateSqlQuery(sqlFetchRows);

        final List<E> items = jt.query(sqlFetchRows, rowMapper, args); // lgtm[java/sql-injection]

        // determine how many rows are available
        final int totalFilteredRecords = executeCountQuery(jt, sqlFetchRows, args);

        return new Page<>(items, totalFilteredRecords);
    }

    /**
     * Executes the count query to determine total filtered records.
     * For MySQL, uses FOUND_ROWS() which doesn't require re-executing the query.
     * For PostgreSQL, wraps the original query in a COUNT subquery with parameterized args.
     *
     * @param jt the JdbcTemplate
     * @param sqlFetchRows the original SQL query (validated before this method is called)
     * @param args the query parameters bound via prepared statements
     * @return the total count of filtered records
     */
    private int executeCountQuery(final JdbcTemplate jt, final String sqlFetchRows, final Object[] args) {
        if (databaseTypeResolver.isMySQL()) {
            // MySQL uses SQL_CALC_FOUND_ROWS and FOUND_ROWS() - no user input in this query
            final String mysqlCountQuery = "SELECT FOUND_ROWS()";
            Integer result = jt.queryForObject(mysqlCountQuery, Integer.class);
            return result != null ? result : 0;
        } else {
            // PostgreSQL: Execute count using parameterized query
            // The sqlFetchRows structure is built internally; user values are in args (bound via PreparedStatement)
            final String countQuery = "SELECT COUNT(*) FROM (" + sqlFetchRows + ") AS temp";
            validateSqlQuery(countQuery);
            Integer result = jt.queryForObject(countQuery, Integer.class, args); // lgtm[java/sql-injection]
            return result != null ? result : 0;
        }
    }

    /**
     * Fetches a paginated result set of Long values.
     * The SQL query structure is built internally by the application.
     *
     * @param jdbcTemplate the JdbcTemplate to use for query execution
     * @param sql the SQL query string (structure built internally, not from user input)
     * @param type the result type class
     * @return a Page containing the results and total count
     */
    public <E> Page<Long> fetchPage(JdbcTemplate jdbcTemplate, String sql, Class<Long> type) {
        // Validate SQL to detect any injection patterns as a defense-in-depth measure
        validateSqlQuery(sql);

        final List<Long> items = jdbcTemplate.queryForList(sql, type); // lgtm[java/sql-injection]

        // determine how many rows are available using a safe count query
        final int totalFilteredRecords;
        if (databaseTypeResolver.isMySQL()) {
            // MySQL uses SQL_CALC_FOUND_ROWS and FOUND_ROWS() - no user input in this query
            final String mysqlCountQuery = "SELECT FOUND_ROWS()";
            Integer result = jdbcTemplate.queryForObject(mysqlCountQuery, Integer.class);
            totalFilteredRecords = result != null ? result : 0;
        } else {
            // PostgreSQL: Execute count using the validated SQL structure
            final String countQuery = "SELECT COUNT(*) FROM (" + sql + ") AS temp";
            validateSqlQuery(countQuery);
            Integer result = jdbcTemplate.queryForObject(countQuery, Integer.class); // lgtm[java/sql-injection]
            totalFilteredRecords = result != null ? result : 0;
        }

        return new Page<>(items, totalFilteredRecords);
    }
}
