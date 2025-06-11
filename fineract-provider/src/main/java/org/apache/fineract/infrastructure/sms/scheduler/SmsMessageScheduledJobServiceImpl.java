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
package org.apache.fineract.infrastructure.sms.scheduler;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;

import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.helper.SmsConfigUtils;
import org.apache.fineract.infrastructure.campaigns.sms.constants.SmsCampaignConstants;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsCampaign;
import org.apache.fineract.infrastructure.campaigns.sms.exception.ConnectionFailureException;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.gcm.service.NotificationSenderService;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.sms.data.SmsMessageApiQueueResourceData;
import org.apache.fineract.infrastructure.sms.data.SmsMessageDeliveryReportData;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageStatusType;
import org.apache.fineract.infrastructure.sms.service.SmsReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Scheduled job services that send SMS messages and get delivery reports for the sent SMS messages
 **/
@Service
@Slf4j
public class SmsMessageScheduledJobServiceImpl implements SmsMessageScheduledJobService {

    private final SmsMessageRepository smsMessageRepository;
    private final SmsReadPlatformService smsReadPlatformService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final SmsConfigUtils smsConfigUtils;
    private final NotificationSenderService notificationSenderService;
    private ExecutorService genericExecutorService;
    private ExecutorService triggeredExecutorService;

    private final String FINERACT_SERVICE_NAME = "FINERACT";

    /**
     * SmsMessageScheduledJobServiceImpl constructor
     **/
    @Autowired
    public SmsMessageScheduledJobServiceImpl(SmsMessageRepository smsMessageRepository, SmsReadPlatformService smsReadPlatformService,
                                             final SmsConfigUtils smsConfigUtils, final NotificationSenderService notificationSenderService) {
        this.smsMessageRepository = smsMessageRepository;
        this.smsReadPlatformService = smsReadPlatformService;
        this.smsConfigUtils = smsConfigUtils;
        this.notificationSenderService = notificationSenderService;
    }

    @PostConstruct
    public void initializeExecutorService() {
        genericExecutorService = Executors.newSingleThreadExecutor();
        triggeredExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Send batches of SMS messages to the SMS gateway (or intermediate gateway)
     **/
    @Override
    @Transactional
    @CronTarget(jobName = JobName.SEND_MESSAGES_TO_SMS_GATEWAY)
    public void sendMessagesToGateway() {
        int pageLimit = 200;
        int page = 0;
        int totalRecords;
        do {
            PageRequest pageRequest = PageRequest.of(0, pageLimit);
            org.springframework.data.domain.Page<SmsMessage> pendingMessages = this.smsMessageRepository
                    .findByStatusType(SmsMessageStatusType.PENDING.getValue(), pageRequest);
            List<SmsMessage> toSaveMessages = new ArrayList<>();
            List<SmsMessage> toSendNotificationMessages = new ArrayList<>();
            try {

                if (!pendingMessages.getContent().isEmpty()) {
                    Iterator<SmsMessage> pendingMessageIterator = pendingMessages.iterator();
                    Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas = new ArrayList<>();
                    while (pendingMessageIterator.hasNext()) {
                        SmsMessage smsData = pendingMessageIterator.next();
                        if (smsData.isNotification()) {
                            smsData.setStatusType(SmsMessageStatusType.WAITING_FOR_DELIVERY_REPORT.getValue());
                            toSendNotificationMessages.add(smsData);
                        } else {
                            SmsMessageApiQueueResourceData apiQueueResourceData = SmsMessageApiQueueResourceData.instance(smsData.getId()
                                    , null, smsData.getMobileNo(), smsData.getMessage(),
                                    FINERACT_SERVICE_NAME);
                            apiQueueResourceDatas.add(apiQueueResourceData);
                            smsData.setStatusType(SmsMessageStatusType.WAITING_FOR_DELIVERY_REPORT.getValue());
                            toSaveMessages.add(smsData);
                        }
                    }
                    if (!toSaveMessages.isEmpty()) {
                        this.smsMessageRepository.saveAll(toSaveMessages);
                        this.smsMessageRepository.flush();
                        this.genericExecutorService.execute(new SmsTask(apiQueueResourceDatas, ThreadLocalContextUtil.getContext()));
                    }
                    if (!toSendNotificationMessages.isEmpty()) {
                        this.notificationSenderService.sendNotification(toSendNotificationMessages);
                    }
                }
            } catch (Exception e) {
                throw new ConnectionFailureException(SmsCampaignConstants.SMS, e);
            }
            page++;
            totalRecords = pendingMessages.getTotalPages();
        } while (page < totalRecords);
    }

    private void connectAndSendToIntermediateServer(Collection<SmsMessageApiQueueResourceData> apiQueueResourceData) {
        try {
            Map<String, Object> hostConfig = this.smsConfigUtils.getMessageGateWayRequestURI("send",
                    SmsMessageApiQueueResourceData.toJsonString(apiQueueResourceData));
            URI uri = (URI) hostConfig.get("uri");
            HttpEntity<?> entity = (HttpEntity<?>) hostConfig.get("entity");

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().equals(HttpStatus.MULTI_STATUS)) {
                log.error("Unexpected response status: {}", response.getStatusCode());
                throw new ConnectionFailureException(SmsCampaignConstants.SMS);
            }

            // Push response to processor thread
            genericExecutorService.submit(() -> processSmsGatewayResponse(response.getBody()));

        } catch (Exception e) {
            log.error("Error sending SMS batch to intermediate server", e);
            throw new ConnectionFailureException(SmsCampaignConstants.SMS, e);
        }
    }

    private void processSmsGatewayResponse(String responseBody) {
        try {
            Type listType = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
            List<Map<String, Object>> responseList = new Gson().fromJson(responseBody, listType);

            for (Map<String, Object> item : responseList) {
                Long internalId = Long.valueOf((String) item.get("id"));
                String status = (String) item.get("status");

                Optional<SmsMessage> optionalSms = smsMessageRepository.findById(internalId);
                if (optionalSms.isPresent()) {
                    SmsMessage sms = optionalSms.get();
                    sms.setStatusType(mapDeliveryStatusToEnum(status, SmsMessageStatusType.PENDING.getValue()));
                    smsMessageRepository.saveAndFlush(sms);
                } else {
                    log.warn("SMS with internal ID {} not found", internalId);
                }
            }
            log.info("Processed {} SMS responses", responseList.size());
        } catch (Exception e) {
            log.error("Error processing SMS response", e);
        }
    }

    @Override
    public void sendTriggeredMessages(Map<SmsCampaign, Collection<SmsMessage>> smsDataMap) {
        try {
            if (!smsDataMap.isEmpty()) {
                List<SmsMessage> toSaveMessages = new ArrayList<>();
                List<SmsMessage> toSendNotificationMessages = new ArrayList<>();
                for (Map.Entry<SmsCampaign, Collection<SmsMessage>> entry : smsDataMap.entrySet()) {
                    Iterator<SmsMessage> smsMessageIterator = entry.getValue().iterator();
                    Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas = new ArrayList<>();
                    while (smsMessageIterator.hasNext()) {
                        SmsMessage smsMessage = smsMessageIterator.next();
                        smsMessage.setStatusType(SmsMessageStatusType.WAITING_FOR_DELIVERY_REPORT.getValue());
                        if (smsMessage.isNotification()) {
                            toSendNotificationMessages.add(smsMessage);
                        } else {
                            SmsMessageApiQueueResourceData apiQueueResourceData = SmsMessageApiQueueResourceData.instance(
                                    smsMessage.getId(), null, smsMessage.getMobileNo(), smsMessage.getMessage(),
                                    FINERACT_SERVICE_NAME);
                            apiQueueResourceDatas.add(apiQueueResourceData);
                            toSaveMessages.add(smsMessage);
                        }
                    }
                    if (!toSaveMessages.isEmpty()) {
                        this.smsMessageRepository.saveAll(toSaveMessages);
                        this.smsMessageRepository.flush();
                        this.triggeredExecutorService.execute(new SmsTask(apiQueueResourceDatas, ThreadLocalContextUtil.getContext()));
                    }
                    if (!toSendNotificationMessages.isEmpty()) {
                        this.notificationSenderService.sendNotification(toSendNotificationMessages);
                    }

                }
            }
        } catch (Exception e) {
            log.error("Error occured.", e);
        }
    }

    @Override
    public void sendTriggeredMessage(Collection<SmsMessage> smsMessages, long providerId) {
        try {
            Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas = new ArrayList<>();
            StringBuilder request = new StringBuilder();
            for (SmsMessage smsMessage : smsMessages) {
                SmsMessageApiQueueResourceData apiQueueResourceData = SmsMessageApiQueueResourceData.instance(smsMessage.getId(), null,
                         smsMessage.getMobileNo(), smsMessage.getMessage(), FINERACT_SERVICE_NAME);
                apiQueueResourceDatas.add(apiQueueResourceData);
                smsMessage.setStatusType(SmsMessageStatusType.WAITING_FOR_DELIVERY_REPORT.getValue());
            }
            this.smsMessageRepository.saveAll(smsMessages);
            request.append(SmsMessageApiQueueResourceData.toJsonString(apiQueueResourceDatas));
            log.info("Sending triggered SMS to specific provider with request - {}", request);
            this.triggeredExecutorService.execute(new SmsTask(apiQueueResourceDatas, ThreadLocalContextUtil.getContext()));
        } catch (Exception e) {
            log.error("Error occured.", e);
        }
    }

    /**
     * get SMS message delivery reports from the SMS gateway (or intermediate gateway)
     **/
    @Override
    @Transactional
    @CronTarget(jobName = JobName.GET_DELIVERY_REPORTS_FROM_SMS_GATEWAY)
    public void getDeliveryReports() {
        int pageSize = 200;
        int offset = 0;

        while (true) {
            Page<Long> internalIdsPage = smsReadPlatformService.retrieveAllWaitingForDeliveryReport(pageSize, offset);
            List<Long> internalIds = internalIdsPage.getPageItems();

            if (internalIds.isEmpty()) break;

            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("service", "string"); // Replace with actual service name if needed
                payload.put("internal_ids", internalIds);

                String jsonPayload = new Gson().toJson(payload);
                Map<String, Object> config = smsConfigUtils.getMessageGateWayRequestURI("report", jsonPayload);

                URI uri = (URI) config.get("uri");
                HttpEntity<?> entity = (HttpEntity<?>) config.get("entity");

                ResponseEntity<Collection<SmsMessageDeliveryReportData>> response = restTemplate.exchange(
                        uri, HttpMethod.POST, entity,
                        new ParameterizedTypeReference<>() {
                        }
                );

                Collection<SmsMessageDeliveryReportData> reports = response.getBody();
                if (reports == null || reports.isEmpty()) continue;

                for (SmsMessageDeliveryReportData report : reports) {
                    String deliveryStatus = report.getDeliveryStatus();
                    String errorMessage = report.getErrorMessage();

                    if ("PENDING".equalsIgnoreCase(deliveryStatus)) continue;

                    Optional<SmsMessage> smsOpt = smsMessageRepository.findById(report.getId());
                    if (smsOpt.isEmpty()) continue;

                    SmsMessage sms = smsOpt.get();
                    int newStatus = mapDeliveryStatusToEnum(deliveryStatus, sms.getStatusType());

                    if (sms.getStatusType() != newStatus) {
                        sms.setStatusType(newStatus);
                        sms.setExternalId(report.getExternalId());
                        if (report.getHasError()) {
                            sms.setErrorMessage(errorMessage);
                        }
                        smsMessageRepository.saveAndFlush(sms);
                        log.info("SMS ID {} status updated to {}", sms.getId(), newStatus);
                    }
                }

                log.info("{} delivery reports processed from SMS gateway.", reports.size());

            } catch (Exception ex) {
                log.error("Failed to fetch delivery reports", ex);
            }

            offset += pageSize;
            if (offset >= internalIdsPage.getTotalFilteredRecords()) break;

        }
    }

    private int mapDeliveryStatusToEnum(String deliveryStatus, int fallback) {
        return switch (deliveryStatus.toUpperCase()) {
            case "INVALID" -> SmsMessageStatusType.INVALID.getValue();
            case "SENT" -> SmsMessageStatusType.WAITING_FOR_DELIVERY_REPORT.getValue();
            case "DELIVERED" -> SmsMessageStatusType.DELIVERED.getValue();
            case "FAILED", "EXPIRED" -> SmsMessageStatusType.FAILED.getValue();
            default -> fallback;
        };
    }


    class SmsTask implements Runnable, ApplicationListener<ContextClosedEvent> {

        private final FineractContext context;
        private final Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas;

        SmsTask(final Collection<SmsMessageApiQueueResourceData> apiQueueResourceDatas, final FineractContext context) {
            this.context = context;
            this.apiQueueResourceDatas = apiQueueResourceDatas;
        }

        @Override
        public void run() {
            ThreadLocalContextUtil.init(context);
            connectAndSendToIntermediateServer(apiQueueResourceDatas);
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            genericExecutorService.shutdown();
            log.info("Shutting down the ExecutorService");
        }
    }
}
