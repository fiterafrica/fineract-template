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
package org.apache.fineract.notification.config;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.fineract.infrastructure.core.config.EnableFineractEventsCondition;
import org.apache.fineract.notification.eventandlistener.ActiveMQNotificationEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Configuration
@Profile("activeMqEnabled")
@Conditional(EnableFineractEventsCondition.class)
public class MessagingConfiguration {

    @Autowired
    private Environment env;

    @Autowired
    private ActiveMQNotificationEventListener notificationEventListener;

    @Bean
    public Logger loggerBean() {
        return LoggerFactory.getLogger(MessagingConfiguration.class);
    }

    @Bean
    public ActiveMQConnectionFactory amqConnectionFactory() {
        ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory(); // NOSONAR
        amqConnectionFactory.setTrustAllPackages(true);
        try {
            amqConnectionFactory.setBrokerURL(this.env.getProperty("fineract.activemq.brokerUrl"));
            amqConnectionFactory.setUserName(this.env.getProperty("fineract.activemq.brokerUsername"));
            amqConnectionFactory.setPassword(this.env.getProperty("fineract.activemq.brokerPassword"));
        } catch (Exception e) {
            amqConnectionFactory.setBrokerURL(this.env.getProperty("fineract.activemq.brokerUrl"));
            amqConnectionFactory.setUserName(this.env.getProperty("fineract.activemq.brokerUsername"));
            amqConnectionFactory.setPassword(this.env.getProperty("fineract.activemq.brokerPassword"));
        }
        return amqConnectionFactory;
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(amqConnectionFactory());
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate;
        jmsTemplate = new JmsTemplate(connectionFactory());
        jmsTemplate.setConnectionFactory(connectionFactory());
        return jmsTemplate;
    }

    @Bean
    public DefaultMessageListenerContainer messageListenerContainer() {

        DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();
        messageListenerContainer.setConnectionFactory(connectionFactory());
        messageListenerContainer.setDestinationName("NotificationQueue");
        messageListenerContainer.setMessageListener(notificationEventListener);
        messageListenerContainer.setExceptionListener(new ExceptionListener() {

            @Override
            public void onException(JMSException jmse) {
                loggerBean().error("Network Error: ActiveMQ Broker Unavailable.");
                messageListenerContainer.shutdown();
            }
        });
        return messageListenerContainer;
    }

}
