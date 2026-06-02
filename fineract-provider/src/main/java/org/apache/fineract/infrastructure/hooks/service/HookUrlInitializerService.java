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
package org.apache.fineract.infrastructure.hooks.service;

import static org.apache.fineract.infrastructure.hooks.api.HookApiConstants.payloadURLName;

import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.configuration.service.SupportedUrlService;
import org.apache.fineract.infrastructure.hooks.domain.HookConfiguration;
import org.apache.fineract.infrastructure.hooks.domain.HookConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that loads existing webhook URLs from the database on application startup and registers them with the
 * SupportedUrlService for SSRF validation.
 */
@Service
@RequiredArgsConstructor
public class HookUrlInitializerService {

    private static final Logger LOG = LoggerFactory.getLogger(HookUrlInitializerService.class);

    private final HookConfigurationRepository hookConfigurationRepository;
    private final SupportedUrlService supportedUrlService;

    @PostConstruct
    public void initializeWebhookUrls() {
        try {
            List<HookConfiguration> payloadUrls = hookConfigurationRepository.findAllByFieldName(payloadURLName);

            int count = 0;
            for (HookConfiguration config : payloadUrls) {
                String url = config.getFieldValue();
                if (url != null && !url.isBlank()) {
                    supportedUrlService.addSupportedUrl(url);
                    count++;
                }
            }

            LOG.info("Initialized {} webhook URLs from database", count);
        } catch (Exception e) {
            LOG.warn("Failed to initialize webhook URLs from database: {}", e.getMessage());
            // Don't fail startup if database is not available yet
        }
    }
}
