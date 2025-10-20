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
package org.apache.fineract.useradministration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.notification.domain.NotificationViaSmtp;
import org.apache.fineract.notification.domain.NotificationViaSmtpRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateUserPermissionJobServiceImpl {

    private final NotificationViaSmtpRepository notificationViaSmtpRepository;

    @CronTarget(jobName = JobName.SEND_NOTIFICATION_ALERT_TO_CHECKER_USER)
    public void updateUserPermissions() {

        List<NotificationViaSmtp> notificationViaSmtps = notificationViaSmtpRepository.findAll();
        for (NotificationViaSmtp notification : notificationViaSmtps) {
            //Send email logic to be implemented here
            notification.setSent(Boolean.TRUE);
            notificationViaSmtpRepository.saveAndFlush(notification);
        }
    }
}