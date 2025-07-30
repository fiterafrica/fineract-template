/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.commands.service;

import org.apache.fineract.commands.domain.CommandProcessingResultType;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class MakerCheckerNotificationServiceImpl implements MakerCheckerNotificationService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PlatformEmailService emailService;
    private final ExecutorService executor;
    private final ConfigurationDomainService configurationDomainService;


    @Value("${mifos.system.base-url}")
    private String baseUrl;


    public MakerCheckerNotificationServiceImpl(AppUserRepository appUserRepository, RoleRepository roleRepository, PlatformEmailService emailService, ConfigurationDomainService configurationDomainService) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.configurationDomainService = configurationDomainService;
        this.executor = Executors.newSingleThreadExecutor();
    }


    @Override
    public void notifyCheckers(CommandSource commandSource) {
        if (this.configurationDomainService.isMakerCheckerNotificationEnabled()) {

            FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant(); // capture current tenant

            executor.submit(() -> {
                ThreadLocalContextUtil.setTenant(tenant);
                sendEmailToCheckers(commandSource);
            });
        }
    }

    @Override
    public void notifyMaker(CommandSource commandSource, CommandProcessingResultType processingResult) {
        if (this.configurationDomainService.isMakerCheckerNotificationEnabled()) {

            FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant(); // capture current tenant

            executor.submit(() -> {
                ThreadLocalContextUtil.setTenant(tenant);
                sendEmailToMaker(commandSource,processingResult);
            });
        }
    }

    private void sendEmailToCheckers(CommandSource commandSource) {
        AppUser maker = commandSource.getMaker();
        Long officeId = commandSource.getOfficeId();
        String permissionCode = commandSource.getPermissionCode() + "_CHECKER";

        List<Role> roles = roleRepository.findRolesByPermission(permissionCode);
        Set<AppUser> checkers = new HashSet<>();
        for (Role role : roles) {
            checkers.addAll(appUserRepository.findUsersByRoleAndOffice(role, officeId));
        }

        for (AppUser checker : checkers) {
            if (checker.getEmail() != null && !checker.equals(maker)) {
                String loanUrl = this.baseUrl + "/tasks";
                EmailDetail emailDetail = getChekerEmailDetail(commandSource, checker, loanUrl);
                emailService.sendDefinedEmail(emailDetail);
            }
        }
    }

    private void sendEmailToMaker(CommandSource commandSource, CommandProcessingResultType processingResult) {
        AppUser maker = commandSource.getMaker();

        EmailDetail emailDetail = getMakerEmailDetail(commandSource, maker, baseUrl,processingResult);
        emailService.sendDefinedEmail(emailDetail);
    }


    @NotNull
    private static EmailDetail getChekerEmailDetail(CommandSource commandSource, AppUser checker, String loanUrl) {
        String subject = "CBS Action Pending Approval: " + commandSource.getPermissionCode();
        String article = getIndefiniteArticle(commandSource.getActionName());
        String body = String.format("""
                        Dear %s,<br><br>

                        %s %s request for %s ID: %s has been submitted and is awaiting your action.  <br><br>
                        Please <a href="%s">log in </a> to the system to review and take the next action.<br><br>
                        
                        Kind Regards.
                """,
                checker.getDisplayName(), article, commandSource.getActionName(), commandSource.getEntityName(),
                resolveEntityIdFromCommandSource(commandSource), loanUrl);
        String address = checker.getEmail();
        String contactName = checker.getDisplayName();

        return new EmailDetail(subject, body, address, contactName);
    }


    private static EmailDetail getMakerEmailDetail(CommandSource commandSource, AppUser maker, String url, CommandProcessingResultType processingResult) {

        String subject = String.format("CBS Task %s: %s",
                processingResult.toString(),
                commandSource.getPermissionCode());
        String article = getIndefiniteArticle(commandSource.getActionName());
        String body = String.format("""
                        Dear %s,<br><br>

                        %s %s request for %s ID: %s has been %s and is awaiting your action.  <br><br>
                        Please <a href="%s">log in </a> to the system to review and take the next action.<br><br>
                        
                        Kind Regards.
                """,
                maker.getDisplayName(), article, commandSource.getActionName(), commandSource.getEntityName(),
                resolveEntityIdFromCommandSource(commandSource), processingResult, url);
        String address = maker.getEmail();
        String contactName = maker.getDisplayName();

        return new EmailDetail(subject, body, address, contactName);
    }


    public static String getIndefiniteArticle(String word) {
        if (word == null || word.isEmpty()) {
            return "A"; // fallback
        }
        char firstChar = Character.toLowerCase(word.charAt(0));
        // Check for vowel sounds
        if ("aeiou".indexOf(firstChar) != -1) {
            return "An";
        }
        return "A";
    }


    private static Long resolveEntityIdFromCommandSource(CommandSource command) {
        if (command == null || command.getEntityName() == null) {
            return null;
        }

        String entity = command.getEntityName().toUpperCase();

        return switch (entity) {
            case "LOAN" -> command.getLoanId();
            case "CLIENT" -> command.getClientId();
            case "PRODUCT" -> command.getProductId();
            case "OFFICE" -> command.getOfficeId();
            case "CREDITBUREAU" -> command.getCreditBureauId();
            case "ORGANISATION_CREDITBUREAU" -> command.getOrganisationCreditBureauId();
            // Add more entity types as needed
            default -> command.getResourceId(); // fallback
        };
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