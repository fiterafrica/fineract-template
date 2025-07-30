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

import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionState;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class MakerCheckerNotificationServiceImpl implements MakerCheckerNotificationService  {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PlatformEmailService emailService;
    private final ExecutorService executor;

    @Value("${mifos.system.base-url}")
    private String baseUrl;


    public MakerCheckerNotificationServiceImpl(AppUserRepository appUserRepository, RoleRepository roleRepository, PlatformEmailService emailService) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.executor = Executors.newSingleThreadExecutor();
    }


    @Override
    public void notifyCheckers(CommandSource commandSource) {

        FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant(); // capture current tenant

        executor.submit(() -> {
                ThreadLocalContextUtil.setTenant(tenant);
                sendEmailToCheckers(commandSource);
        });
    }

    private void sendEmailToCheckers(CommandSource commandSource) {
        AppUser maker = commandSource.getMaker();
        Long officeId = commandSource.getOfficeId();
        String permissionCode = commandSource.getPermissionCode() + "_CHECKER";

        List<Role> roles = roleRepository.findRolesByPermission(permissionCode);
        Set<AppUser> checkers = new HashSet<>();
        for (Role role : roles) {
            checkers.addAll(appUserRepository.findUsersByRoleAndOffice(role,officeId));
        }

        for (AppUser checker : checkers) {
            if (checker.getEmail() != null && !checker.equals(maker)) {
                String loanUrl = this.baseUrl + "/tasks";
                EmailDetail emailDetail = getChekerEmailDetail(commandSource, checker, loanUrl);
                emailService.sendDefinedEmail(emailDetail);
            }
        }
    }

    @NotNull
    private static EmailDetail getChekerEmailDetail(CommandSource commandSource, AppUser checker, String loanUrl) {
        String subject = "Action Pending Approval: " + commandSource.getPermissionCode();
        String article = getIndefiniteArticle(commandSource.getActionName());
        String body = String.format(
                """
                        Dear %s,<br><br>

                        %s %s request for %s ID: %s has been submitted and is awaiting your action.\s <br><br>
                        Please <a href="%s">log in </a> to the system to review and take the next action.<br><br>
                        
                        Kind Regards.
                """,
                checker.getDisplayName(),
                article,
                commandSource.getActionName(),
                commandSource.getEntityName(),
                commandSource.getEntityId(),
                loanUrl
        );
        String address = checker.getEmail();
        String contactName = checker.getDisplayName();

        return new EmailDetail(subject, body, address, contactName);
    }


    public static String getIndefiniteArticle(String word) {
        if (word == null || word.isEmpty()) {
            return "a"; // fallback
        }
        char firstChar = Character.toLowerCase(word.charAt(0));
        // Check for vowel sounds
        if ("aeiou".indexOf(firstChar) != -1) {
            return "an";
        }
        return "a";
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}