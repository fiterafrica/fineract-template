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
package org.apache.fineract.notification.listener;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.notification.service.NotificationService;
import org.apache.fineract.portfolio.businessevent.domain.client.ClientCreateBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanCreatedBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.savings.SavingsCreateBusinessEvent;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.service.UserPermissionService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountCreationEventListener {

    private final UserPermissionService userPermissionService;
    private final NotificationService notificationService;

    @EventListener
    public void handleClientCreateEvent(ClientCreateBusinessEvent event) {
        List<AppUser> users = userPermissionService.findUsersWithCheckerPermission("CREATE_CLIENT");
        notificationService.sendNotification(users, "New Client Created", "A new client has been created and is pending approval.");
    }

    @EventListener
    public void handleLoanCreateEvent(LoanCreatedBusinessEvent event) {
        List<AppUser> users = userPermissionService.findUsersWithCheckerPermission("CREATE_LOAN");
        notificationService.sendNotification(users, "New Loan Created", "A new loan has been created and is pending approval.");
    }

    @EventListener
    public void handleSavingsCreateEvent(SavingsCreateBusinessEvent event) {
        List<AppUser> users = userPermissionService.findUsersWithCheckerPermission("CREATE_SAVINGSACCOUNT");
        notificationService.sendNotification(users, "New Savings Account Created", "A new savings account has been created and is pending approval.");
    }
}
