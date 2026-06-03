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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link SupportedUrlService} that manages supported external service URLs. Combines URLs from
 * configuration with dynamically added URLs at runtime.
 */
@Service
@RequiredArgsConstructor
public class SupportedUrlServiceImpl implements SupportedUrlService {

    private static final Logger LOG = LoggerFactory.getLogger(SupportedUrlServiceImpl.class);

    private final FineractProperties fineractProperties;

    // Thread-safe set for dynamically added URLs
    private final Set<String> dynamicUrls = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        LOG.info("SupportedUrlService initialized. Configuration URLs: {}", getConfiguredUrls().size());
    }

    @Override
    public void addSupportedUrl(String url) {
        if (url != null && !url.isBlank()) {
            dynamicUrls.add(url.trim());
            LOG.info("Added supported URL: {}", url);
        }
    }

    @Override
    public void addSupportedUrls(Collection<String> urls) {
        if (urls != null) {
            urls.stream().filter(url -> url != null && !url.isBlank()).map(String::trim).forEach(dynamicUrls::add);
            LOG.info("Added {} supported URLs", urls.size());
        }
    }

    @Override
    public boolean removeSupportedUrl(String url) {
        if (url != null) {
            boolean removed = dynamicUrls.remove(url.trim());
            if (removed) {
                LOG.info("Removed supported URL: {}", url);
            }
            return removed;
        }
        return false;
    }

    @Override
    public boolean isUrlSupported(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmedUrl = url.trim();

        // Check dynamic URLs first
        if (dynamicUrls.contains(trimmedUrl)) {
            return true;
        }

        // Check configured URLs
        return getConfiguredUrls().contains(trimmedUrl);
    }

    @Override
    public List<String> getSupportedUrls() {
        List<String> allUrls = new ArrayList<>();
        allUrls.addAll(getConfiguredUrls());
        allUrls.addAll(dynamicUrls);
        return Collections.unmodifiableList(allUrls);
    }

    @Override
    public void clearDynamicUrls() {
        int count = dynamicUrls.size();
        dynamicUrls.clear();
        LOG.info("Cleared {} dynamic URLs", count);
    }

    /**
     * Get URLs from configuration.
     */
    private List<String> getConfiguredUrls() {
        if (fineractProperties.getSupported() != null && fineractProperties.getSupported().getUrls() != null) {
            return fineractProperties.getSupported().getUrls();
        }
        return Collections.emptyList();
    }
}
