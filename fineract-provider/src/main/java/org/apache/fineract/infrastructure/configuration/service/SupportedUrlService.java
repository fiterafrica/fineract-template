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
package org.apache.fineract.infrastructure.configuration.service;

import java.util.Collection;
import java.util.List;

/**
 * Service for managing supported external service URLs at runtime. This allows adding/removing URLs programmatically in
 * addition to configuration.
 */
public interface SupportedUrlService {

    /**
     * Add a URL to the supported URLs list.
     *
     * @param url
     *            the URL to add
     */
    void addSupportedUrl(String url);

    /**
     * Add multiple URLs to the supported URLs list.
     *
     * @param urls
     *            the URLs to add
     */
    void addSupportedUrls(Collection<String> urls);

    /**
     * Remove a URL from the supported URLs list.
     *
     * @param url
     *            the URL to remove
     * @return true if the URL was removed, false if it wasn't in the list
     */
    boolean removeSupportedUrl(String url);

    /**
     * Check if a URL is in the supported URLs list.
     *
     * @param url
     *            the URL to check
     * @return true if the URL is supported
     */
    boolean isUrlSupported(String url);

    /**
     * Get all supported URLs.
     *
     * @return unmodifiable list of supported URLs
     */
    List<String> getSupportedUrls();

    /**
     * Clear all dynamically added URLs (keeps configuration-based URLs).
     */
    void clearDynamicUrls();
}
