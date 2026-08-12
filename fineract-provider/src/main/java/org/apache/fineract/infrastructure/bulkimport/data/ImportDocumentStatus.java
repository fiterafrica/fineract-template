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
package org.apache.fineract.infrastructure.bulkimport.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Lifecycle status of a single bulk-import job tracked by {@code m_import_document}. Introduced so that an
 * upload still being processed (PENDING) can be reliably distinguished from one that finished normally
 * (COMPLETED, regardless of per-row success/failure counts) or one whose background processing thread died
 * before it could record a result (FAILED).
 */
public enum ImportDocumentStatus {

    PENDING(100, "pending"), COMPLETED(300, "completed"), FAILED(600, "failed");

    private final Integer value;
    private final String code;

    private static final Map<Integer, ImportDocumentStatus> intToEnumMap = new HashMap<>();

    static {
        for (final ImportDocumentStatus status : ImportDocumentStatus.values()) {
            intToEnumMap.put(status.value, status);
        }
    }

    ImportDocumentStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static ImportDocumentStatus fromInt(final Integer value) {
        return intToEnumMap.get(value);
    }
}
