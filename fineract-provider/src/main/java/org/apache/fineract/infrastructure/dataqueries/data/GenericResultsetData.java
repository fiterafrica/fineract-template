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
package org.apache.fineract.infrastructure.dataqueries.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Immutable data object for generic resultset data.
 */
public final class GenericResultsetData {

    private final List<ResultsetColumnHeaderData> columnHeaders;
    private final List<ResultsetRowData> data;

    @Setter
    private Integer count;

    public GenericResultsetData(final List<ResultsetColumnHeaderData> columnHeaders, final List<ResultsetRowData> resultsetDataRows) {
        this.columnHeaders = columnHeaders;
        this.data = resultsetDataRows;
    }

    public List<ResultsetColumnHeaderData> getColumnHeaders() {
        return this.columnHeaders;
    }

    public List<ResultsetRowData> getData() {
        return this.data;
    }

    public Integer getCount() {
        return count;
    }

    public String getColTypeOfColumnNamed(final String columnName) {

        String colType = null;
        for (final ResultsetColumnHeaderData columnHeader : this.columnHeaders) {
            if (columnHeader.isNamed(columnName)) {
                colType = columnHeader.getColumnType();
            }
        }

        return colType;
    }

    public boolean hasNoEntries() {
        return this.data.isEmpty();
    }

    public boolean hasEntries() {
        return !hasNoEntries();
    }

    public boolean hasMoreThanOneEntry() {
        return this.data.size() > 1;
    }

    public void replaceWordInColumHeader(String wordToReplace, String wordToReplaceWith) {
        for (final ResultsetColumnHeaderData columnHeader : this.columnHeaders) {
            if (columnHeader.getColumnName().contains(wordToReplace)) {
                String columnName = columnHeader.getColumnName();
                columnName = StringUtils.replace(columnName, wordToReplace, wordToReplaceWith);
                columnHeader.setColumnName(columnName);
            }
        }
    }

    public List convertToJSON() {
        List<Map<String, Object>> jsonList = new ArrayList<>();

        // Extract column names
        List<ResultsetColumnHeaderData> columnHeaders = this.getColumnHeaders();
        List<ResultsetRowData> dataRows = this.getData();

        for (ResultsetRowData rowData : dataRows) {
            Map<String, Object> jsonObject = new LinkedHashMap<>();
            List<String> rowValues = rowData.getRow();

            for (int i = 0; i < columnHeaders.size(); i++) {
                ResultsetColumnHeaderData columnHeader = columnHeaders.get(i);
                String columnName = columnHeader.getColumnName();
                String columnType = columnHeader.getColumnType();
                Object value = (i < rowValues.size() && rowValues.get(i) != null && !rowValues.get(i).isEmpty())
                        ? parseValue(columnType, rowValues.get(i))
                        : null; // Ensure null entries stay null

                jsonObject.put(columnName, value);
            }
            jsonList.add(jsonObject);
        }
        return jsonList;
    }


    private static Object parseValue(String columnType, String value) {
        if (value == null || value.isEmpty()) {
            return null; // Ensure null values remain null
        }

        try {
            return switch (columnType.toUpperCase()) {
                case "INTEGER" -> Integer.parseInt(value);
                case "DECIMAL" -> new BigDecimal(value);
                case "BOOLEAN" -> Boolean.parseBoolean(value);
                default -> value;
            };
        } catch (Exception e) {
            return value; // Fallback to string if parsing fails
        }
    }

}
