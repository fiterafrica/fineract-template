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
package org.apache.fineract.commands.service;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CamelCommandWebsocketSessionService {

    private static final Logger LOG = LoggerFactory.getLogger(CamelCommandWebsocketSessionService.class);

    private final Map<String, String> websocketUserMapping = new HashMap<>();

    public void addUser(String connectionKey, String username) {
        websocketUserMapping.put(username, connectionKey);
        LOG.debug(">>>>>>>>>> WEBSOCKET ADD: {} {}", connectionKey, username);
    }

    public String getUserConnectionKey(String username) {
        return websocketUserMapping.get(username);
    }

    public void removeUserByConnectionKey(String connectionKey) {
        String username = websocketUserMapping.entrySet().stream().filter(m -> m.getValue().equalsIgnoreCase(connectionKey))
                .map(Map.Entry::getKey).findFirst().orElse(null);

        if (!StringUtils.isEmpty(username)) {
            websocketUserMapping.remove(username);
            LOG.debug(">>>>>>>>>> WEBSOCKET REMOVE: {} {}", connectionKey, username);
        }
    }

    public void removeUserByUsername(String username) {
        websocketUserMapping.remove(username);
    }
}
