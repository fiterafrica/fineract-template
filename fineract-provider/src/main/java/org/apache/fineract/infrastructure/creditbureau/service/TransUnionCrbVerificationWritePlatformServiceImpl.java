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
package org.apache.fineract.infrastructure.creditbureau.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.loanaccount.data.AccountListData;
import org.apache.fineract.portfolio.loanaccount.data.CorporateProfileData;
import org.apache.fineract.portfolio.loanaccount.data.CrbAccountsSummaryData;
import org.apache.fineract.portfolio.loanaccount.data.HeaderData;
import org.apache.fineract.portfolio.loanaccount.data.PersonalProfileData;
import org.apache.fineract.portfolio.loanaccount.data.PhoneListData;
import org.apache.fineract.portfolio.loanaccount.data.PhysicalAddressListData;
import org.apache.fineract.portfolio.loanaccount.data.PostalAddressListData;
import org.apache.fineract.portfolio.loanaccount.data.RecentEnquiryListData;
import org.apache.fineract.portfolio.loanaccount.data.ScoreOutputData;
import org.apache.fineract.portfolio.loanaccount.data.SummaryData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaConsumerVerificationData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaConsumerVerificationResponseData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaCorporateVerificationData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaCorporateVerificationResponseData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbAccount;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbAccountRepository;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbCorporateProfile;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbCorporateProfileRepository;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbHeader;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbHeaderRepository;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbPersonalProfile;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbPersonalProfileRepository;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbScoreOutput;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbScoreOutputRepository;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbSummary;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbSummaryRepository;
import org.apache.fineract.portfolio.loanaccount.service.TransUnionCrbConsumerVerificationReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Service
@RequiredArgsConstructor
public class TransUnionCrbVerificationWritePlatformServiceImpl implements TransUnionCrbVerificationWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(TransUnionCrbVerificationWritePlatformServiceImpl.class);
    public static final String FORM_URL_CONTENT_TYPE = "Content-Type";
    private final TransUnionCrbConsumerVerificationReadPlatformService transUnionCrbClientVerificationReadPlatformService;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final TransunionCrbHeaderRepository transunionCrbHeaderRepository;
    private final TransunionCrbPersonalProfileRepository transunionCrbPersonalProfileRepository;
    private final TransunionCrbCorporateProfileRepository transunionCrbCorporateProfileRepository;
    private final TransunionCrbScoreOutputRepository transunionCrbScoreOutputRepository;
    private final TransunionCrbSummaryRepository transunionCrbSummaryRepository;
    private final TransunionCrbAccountRepository transunionCrbAccountRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;

    @Autowired
    private Environment env;

    @Override
    public CommandProcessingResult loanVerificationToTransUnionRwanda(Long loanId, JsonCommand command) {
        Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        Client clientObj = this.clientRepositoryWrapper.findOneWithNotFoundDetection(loan.getClientId());

        TransUnionRwandaConsumerVerificationData getProduct123 = null;
        TransUnionRwandaCorporateVerificationData getProduct168 = null;
        Boolean isClient = false;
        String requestToCrb = null;
        TransunionCrbHeader transunionCrbHeader = null;

        if (clientObj.getLegalForm().equals(LegalForm.PERSON.getValue())) {
            isClient = true;
            getProduct123 = this.transUnionCrbClientVerificationReadPlatformService.retrieveConsumer(clientObj.getId());
            requestToCrb = convertConsumerDataToJAXBRequest(getProduct123);
            LOG.info("Verifying clients to TransUnion Rwanda Request :: >> " + getProduct123.toString());
        } else {
            isClient = false;
            getProduct168 = this.transUnionCrbClientVerificationReadPlatformService.retrieveCorporate(clientObj.getId());
            requestToCrb = convertCorporateDataToJAXBRequest(getProduct168);
            LOG.info("Verifying clients to TransUnion Rwanda Request :: >> " + getProduct168.toString());
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("fineract.integrations.transUnion.crb.soap.verifyClient"))
                .newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;
        String resObject = null;

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), requestToCrb);

        Request request = new Request.Builder().url(url).header("Authorization",
                "Basic " + base64EncodeCredentials(getConfigProperty("fineract.integrations.transUnion.crb.soap.transportLevel.username"),
                        getConfigProperty("fineract.integrations.transUnion.crb.soap.transportLevel.password")))
                .header("Content-Type", "application/xml ").header("Content-Type", "text/xml ").post(formBody).build();
        try {
            response = client.newCall(request).execute();
            resObject = response.body().string();

            if (response.isSuccessful()) {
                LOG.info("Response from TransUnion Rwanda :: >> " + resObject);
                if (isClient) {
                    transunionCrbHeader = saveConsumerVerificationReport(clientObj, loan, resObject);
                } else {
                    transunionCrbHeader = saveCorporateVerificationReport(clientObj, loan, resObject);
                }

            }

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(clientObj.getId())
                    .withResourceIdAsString(transunionCrbHeader != null ? transunionCrbHeader.getId().toString() : null).build();
        } catch (Exception e) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.verification.failed",
                    "Failed to Verify consumer credit . Response code From TransUnion :- " + e.getMessage());
        }
    }

    private TransunionCrbHeader saveConsumerVerificationReport(Client clientObj, Loan loan, String resObject)
            throws ParserConfigurationException, IOException, SAXException {
        TransUnionRwandaConsumerVerificationResponseData clientVerificationResponseData = convertConsumerReponse(resObject);
        TransunionCrbHeader transunionCrbHeader = null;
        if (clientVerificationResponseData != null) {
            HeaderData headerData = clientVerificationResponseData.getHeader();
            PersonalProfileData personalProfileData = clientVerificationResponseData.getPersonalProfile();
            ScoreOutputData scoreOutputData = clientVerificationResponseData.getScoreOutput();
            SummaryData summaryData = clientVerificationResponseData.getSummaryData();
            List<AccountListData> accountListData = clientVerificationResponseData.getAccountListData();
            if (headerData != null) {
                transunionCrbHeader = new TransunionCrbHeader(clientObj, loan, headerData);
                transunionCrbHeaderRepository.saveAndFlush(transunionCrbHeader);
                if (personalProfileData != null) {
                    TransunionCrbPersonalProfile transunionCrbPersonalProfile = new TransunionCrbPersonalProfile(transunionCrbHeader,
                            personalProfileData);
                    transunionCrbPersonalProfileRepository.saveAndFlush(transunionCrbPersonalProfile);
                }
                if (scoreOutputData != null) {
                    TransunionCrbScoreOutput scoreOutput = new TransunionCrbScoreOutput(transunionCrbHeader, scoreOutputData);
                    transunionCrbScoreOutputRepository.saveAndFlush(scoreOutput);
                }

                if (summaryData != null) {
                    TransunionCrbSummary scoreOutput = new TransunionCrbSummary(transunionCrbHeader, summaryData);
                    transunionCrbSummaryRepository.saveAndFlush(scoreOutput);
                }
                if (!CollectionUtils.isEmpty(accountListData)) {
                    for (AccountListData account : accountListData) {
                        TransunionCrbAccount transunionCrbAccount = new TransunionCrbAccount(transunionCrbHeader, account);
                        transunionCrbAccountRepository.saveAndFlush(transunionCrbAccount);
                    }
                }
            }

        }
        return transunionCrbHeader;
    }

    private TransunionCrbHeader saveCorporateVerificationReport(Client clientObj, Loan loan, String resObject) {
        TransUnionRwandaCorporateVerificationResponseData corporateVerificationResponseData = convertCorporateResponse(resObject);
        TransunionCrbHeader transunionCrbHeader = null;
        if (corporateVerificationResponseData != null) {
            HeaderData headerData = corporateVerificationResponseData.getHeader();
            CorporateProfileData corporateProfileData = corporateVerificationResponseData.getCorporateProfile();
            ScoreOutputData scoreOutputData = corporateVerificationResponseData.getScoreOutput();
            SummaryData summaryData = corporateVerificationResponseData.getSummaryData();
            List<AccountListData> accountListData = corporateVerificationResponseData.getAccountListData();
            if (headerData != null) {
                transunionCrbHeader = new TransunionCrbHeader(clientObj, loan, headerData);
                transunionCrbHeaderRepository.saveAndFlush(transunionCrbHeader);
                if (corporateProfileData != null) {
                    TransunionCrbCorporateProfile transunionCrbCorporateProfile = new TransunionCrbCorporateProfile(transunionCrbHeader,
                            corporateProfileData);
                    transunionCrbCorporateProfileRepository.saveAndFlush(transunionCrbCorporateProfile);
                }
                if (scoreOutputData != null) {
                    TransunionCrbScoreOutput scoreOutput = new TransunionCrbScoreOutput(transunionCrbHeader, scoreOutputData);
                    transunionCrbScoreOutputRepository.saveAndFlush(scoreOutput);
                }

                if (summaryData != null) {
                    TransunionCrbSummary scoreOutput = new TransunionCrbSummary(transunionCrbHeader, summaryData);
                    transunionCrbSummaryRepository.saveAndFlush(scoreOutput);
                }
                if (!CollectionUtils.isEmpty(accountListData)) {
                    for (AccountListData account : accountListData) {
                        TransunionCrbAccount transunionCrbAccount = new TransunionCrbAccount(transunionCrbHeader, account);
                        transunionCrbAccountRepository.saveAndFlush(transunionCrbAccount);
                    }
                }
            }

        }
        return transunionCrbHeader;
    }

    private String getConfigProperty(String propertyName) {
        return this.env.getProperty(propertyName);
    }

    public String base64EncodeCredentials(String username, String password) {
        String credentials = username + ":" + password;
        byte[] credentialsBytes = credentials.getBytes(UTF_8);

        return Base64.getEncoder().encodeToString(credentialsBytes);
    }

    public String convertConsumerDataToJAXBRequest(TransUnionRwandaConsumerVerificationData getProduct123) {

        addExtraDetailsToProduct(getProduct123);

        try {
            JAXBContext context = JAXBContext.newInstance(TransUnionRwandaConsumerVerificationData.class);

            Marshaller marshaller = context.createMarshaller();

            StringWriter stringWriter = new StringWriter();

            stringWriter.write(
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws=\"http://ws.rw.crbws.transunion.ke.co/\">");
            stringWriter.write("<soapenv:Header/>");
            stringWriter.write("<soapenv:Body>");

            marshaller.marshal(getProduct123, stringWriter);

            stringWriter.write("</soapenv:Body>");
            stringWriter.write("</soapenv:Envelope>");

            String request = stringWriter.toString();
            request = request.replaceFirst("<\\?xml [^>]*\\?>", "");

            LOG.info(" unmarshal Consumer verification Response to Pojo :: >> " + request);
            return request;
        } catch (JAXBException e) {
            e.printStackTrace();
            LOG.error("Failure to unmarshal consumer credit :: >> " + e.getMessage());
        }
        return null;
    }

    public String convertCorporateDataToJAXBRequest(TransUnionRwandaCorporateVerificationData corporateVerificationData) {

        addExtraDetailsToProduct168(corporateVerificationData);

        try {
            JAXBContext context = JAXBContext.newInstance(TransUnionRwandaCorporateVerificationData.class);

            Marshaller marshaller = context.createMarshaller();

            StringWriter stringWriter = new StringWriter();

            stringWriter.write(
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws=\"http://ws.rw.crbws.transunion.ke.co/\">");
            stringWriter.write("<soapenv:Header/>");
            stringWriter.write("<soapenv:Body>");

            marshaller.marshal(corporateVerificationData, stringWriter);

            stringWriter.write("</soapenv:Body>");
            stringWriter.write("</soapenv:Envelope>");

            String request = stringWriter.toString();
            request = request.replaceFirst("<\\?xml [^>]*\\?>", "");

            LOG.info(" unmarshal Consumer verification Response to Pojo :: >> " + request);
            return request;
        } catch (JAXBException e) {
            e.printStackTrace();
            LOG.error("Failure to unmarshal consumer credit :: >> " + e.getMessage());
        }
        return null;
    }

    private void addExtraDetailsToProduct(TransUnionRwandaConsumerVerificationData getProduct123) {
        getProduct123.setUsername(getConfigProperty("fineract.integrations.transUnion.crb.soap.messageLevel.username"));
        getProduct123.setPassword(getConfigProperty("fineract.integrations.transUnion.crb.soap.messageLevel.password"));
        getProduct123.setCode("1570");
        getProduct123.setReportSector(1);
        getProduct123.setReportReason(2);
        getProduct123.setInfinityCode(getConfigProperty("fineract.integrations.transUnion.crb.rest.infinityCode"));
    }

    private void addExtraDetailsToProduct168(TransUnionRwandaCorporateVerificationData getProduct123) {
        getProduct123.setUsername(getConfigProperty("fineract.integrations.transUnion.crb.soap.messageLevel.username"));
        getProduct123.setPassword(getConfigProperty("fineract.integrations.transUnion.crb.soap.messageLevel.password"));
        getProduct123.setCode("1570");
        getProduct123.setReportSector(1);
        getProduct123.setReportReason(2);
        getProduct123.setInfinityCode(getConfigProperty("fineract.integrations.transUnion.crb.rest.infinityCode"));
    }

    public TransUnionRwandaConsumerVerificationResponseData convertConsumerReponse(String xml)
            throws ParserConfigurationException, IOException, SAXException {
        TransUnionRwandaConsumerVerificationResponseData product123Response = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));

        NodeList product123ResponseNodes = document.getElementsByTagName("ns2:getProduct123Response");
        if (product123ResponseNodes.getLength() > 0) {
            Element getProduct123ResponseElement = (Element) product123ResponseNodes.item(0);

            product123Response = new TransUnionRwandaConsumerVerificationResponseData();
            Element returnElement = (Element) getProduct123ResponseElement.getElementsByTagName("return").item(0);
            if (returnElement != null) {

                Element responseCodeElement = (Element) returnElement.getElementsByTagName("responseCode").item(0);
                if (responseCodeElement != null) {
                    Integer Value = Integer.parseInt(responseCodeElement.getTextContent());
                    if (Value != null) {
                        product123Response.setResponseCode(Value);
                    }
                } else {
                    throw new GeneralPlatformDomainRuleException("error.msg.crb.consumer.verification.failed",
                            "Failed to get 'responseCode' element from 'return' element.");
                }
                if (product123Response.getResponseCode() == 200) {
                    product123Response.setHeader(extractHeader(getProduct123ResponseElement));
                    product123Response.setPersonalProfile(extractPersonalProfile(getProduct123ResponseElement));
                    product123Response.setScoreOutput(extractScoreOutputData(getProduct123ResponseElement));
                    product123Response.setSummaryData(extractSummaryData(getProduct123ResponseElement));
                    product123Response.setAccountListData(extractAccountList(getProduct123ResponseElement));
                    product123Response.setPhoneListData(extractPhoneList(getProduct123ResponseElement));
                    product123Response.setRecentEnquiryListData(extractRecentEnquiryList(getProduct123ResponseElement));
                    product123Response.setPhysicalAddressListDataList(extractPhysicalAddressList(getProduct123ResponseElement));
                    product123Response.setPostalAddressListDataList(extractPostalAddressList(getProduct123ResponseElement));
                    LOG.info("Response from TransUnion Rwanda  product123Response:: >> " + product123Response);
                    return product123Response;
                } else {
                    throw new GeneralPlatformDomainRuleException("error.msg.crb.consumer.verification.failed",
                            "Failed to Verify consumer credit . Response code From TransUnion :- " + product123Response.getResponseCode());
                }
            } else {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consumer.verification.failed",
                        "No 'return' element found in the XML.");
            }
        }

        return product123Response;
    }

    public TransUnionRwandaCorporateVerificationResponseData convertCorporateResponse(String xml) {
        TransUnionRwandaCorporateVerificationResponseData product168Response = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            NodeList product168ResponseNodes = document.getElementsByTagName("ns2:getProduct168Response");
            if (product168ResponseNodes.getLength() > 0) {
                Element getProduct168ResponseElement = (Element) product168ResponseNodes.item(0);

                product168Response = new TransUnionRwandaCorporateVerificationResponseData();

                Element responseCodeElement = (Element) getProduct168ResponseElement.getElementsByTagName("responseCode").item(0);
                if (responseCodeElement != null) {
                    Integer Value = Integer.parseInt(responseCodeElement.getTextContent());
                    if (Value != null) {
                        product168Response.setResponseCode(Value);
                    }
                }
                if (product168Response.getResponseCode() == 200) {

                    product168Response.setHeader(extractHeader(getProduct168ResponseElement));
                    product168Response.setCorporateProfile(extractCorporateProfile(getProduct168ResponseElement));
                    product168Response.setSummaryData(extractSummaryData(getProduct168ResponseElement));
                    product168Response.setAccountListData(extractAccountList(getProduct168ResponseElement));
                    LOG.info("Response from TransUnion Rwanda  product168Response:: >> " + product168Response.toString());
                    return product168Response;
                } else {
                    throw new GeneralPlatformDomainRuleException("error.msg.crb.corporate.verification.failed",
                            "Failed to Verify Corporate credit . Response code From TransUnion :- " + product168Response.getResponseCode());
                }
            }
        } catch (Exception e) {
            LOG.info("Response from TransUnion Rwanda  Extraction:: >> " + e.getMessage());
        }

        return product168Response;
    }

    private HeaderData extractHeader(Element getProduct123ResponseElement) {
        HeaderData header = new HeaderData();
        Element headerElement = (Element) getProduct123ResponseElement.getElementsByTagName("header").item(0);
        if (headerElement != null) {
            header.setCrbName(getElementTextContent(headerElement, "crbName"));
            header.setPdfId(getElementTextContent(headerElement, "pdfId"));
            header.setProductDisplayName(getElementTextContent(headerElement, "productDisplayName"));
            header.setReportDate(getElementTextContent(headerElement, "reportDate"));
            header.setReportType(getElementTextContent(headerElement, "reportType"));
            header.setRequestNo(getElementTextContent(headerElement, "requestNo"));
            header.setRequester(getElementTextContent(headerElement, "requester"));
        }
        return header;
    }

    private PersonalProfileData extractPersonalProfile(Element getProduct123ResponseElement) {
        PersonalProfileData personalProfileData = new PersonalProfileData();
        Element personalProfile = (Element) getProduct123ResponseElement.getElementsByTagName("personalProfile").item(0);
        if (personalProfile != null) {
            personalProfileData.setCrn(getElementTextContent(personalProfile, "crn"));
            personalProfileData.setDateOfBirth(getElementTextContent(personalProfile, "dateOfBirth"));
            personalProfileData.setFullName(getElementTextContent(personalProfile, "fullName"));
            personalProfileData.setGender(getElementTextContent(personalProfile, "gender"));
            personalProfileData.setHealthInsuranceNo(getElementTextContent(personalProfile, "healthInsuranceNo"));
            personalProfileData.setMaritalStatus(getElementTextContent(personalProfile, "maritalStatus"));
            personalProfileData.setNationalID(getElementTextContent(personalProfile, "nationalID"));
            personalProfileData.setOtherNames(getElementTextContent(personalProfile, "otherNames"));
            personalProfileData.setSalutation(getElementTextContent(personalProfile, "salutation"));
            personalProfileData.setSurname(getElementTextContent(personalProfile, "surname"));
        }
        return personalProfileData;
    }

    private String getElementTextContent(Element parentElement, String tagName) {
        Element element = (Element) parentElement.getElementsByTagName(tagName).item(0);
        return (element != null) ? element.getTextContent() : null;
    }

    private BigDecimal getElementBigDecimalContent(Element parentElement, String tagName) {
        Element element = (Element) parentElement.getElementsByTagName(tagName).item(0);

        if (element != null) {
            String textContent = element.getTextContent();

            if (textContent != null && !textContent.isEmpty()) {
                try {
                    return new BigDecimal(textContent);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private Integer getElementIntegerContent(Element parentElement, String tagName) {
        Element element = (Element) parentElement.getElementsByTagName(tagName).item(0);

        if (element != null) {
            String textContent = element.getTextContent();

            if (textContent != null && !textContent.isEmpty()) {
                try {
                    return Integer.parseInt(textContent);
                } catch (NumberFormatException e) {
                    // Handle the case where text content cannot be converted to Integer
                    // You can log a warning or take other appropriate action
                    return null;
                }
            }
        }

        return null;
    }

    private Boolean getElementBooleanContent(Element parentElement, String tagName) {
        Element element = (Element) parentElement.getElementsByTagName(tagName).item(0);

        if (element != null) {
            String textContent = element.getTextContent();

            if (textContent != null && !textContent.isEmpty()) {
                if ("true".equalsIgnoreCase(textContent.trim())) {
                    return true;
                } else if ("false".equalsIgnoreCase(textContent.trim())) {
                    return false;
                }
            }
        }

        return null;
    }

    private CorporateProfileData extractCorporateProfile(Element corporateProfile) {
        CorporateProfileData corporateProfileData = new CorporateProfileData();
        Element corporateProfile1 = (Element) corporateProfile.getElementsByTagName("corporateProfile").item(0);
        if (corporateProfile1 != null) {
            corporateProfileData.setCrn(corporateProfile1.getElementsByTagName("crn").item(0).getTextContent());
            corporateProfileData.setCompanyName(corporateProfile1.getElementsByTagName("companyName").item(0).getTextContent());
            corporateProfileData.setCompanyRegNo(corporateProfile1.getElementsByTagName("companyRegNo").item(0).getTextContent());
        }
        return corporateProfileData;
    }

    private ScoreOutputData extractScoreOutputData(Element getProduct123ResponseElement) {
        ScoreOutputData scoreOutputData = new ScoreOutputData();
        Element scoreOutput = (Element) getProduct123ResponseElement.getElementsByTagName("scoreOutput").item(0);
        if (scoreOutput != null) {
            scoreOutputData.setGrade(getElementTextContent(scoreOutput, "grade"));
            scoreOutputData.setPositiveScore(getElementTextContent(scoreOutput, "positiveScore"));
            scoreOutputData.setProbability(getElementTextContent(scoreOutput, "probability"));
            scoreOutputData.setReasonCodeAARC1(getElementTextContent(scoreOutput, "reasonCodeAARC1"));
            scoreOutputData.setReasonCodeAARC2(getElementTextContent(scoreOutput, "reasonCodeAARC2"));
            scoreOutputData.setReasonCodeAARC3(getElementTextContent(scoreOutput, "reasonCodeAARC3"));
            scoreOutputData.setReasonCodeAARC4(getElementTextContent(scoreOutput, "reasonCodeAARC4"));
        }
        return scoreOutputData;
    }

    private SummaryData extractSummaryData(Element summary) {
        SummaryData summaryData = new SummaryData();
        Element summaryElement = (Element) summary.getElementsByTagName("summary").item(0);
        if (summaryElement != null) {
            summaryData.setBouncedCheques(createCrbSummaryDetails(summaryElement, "bouncedCheques"));
            summaryData.setBouncedCheques90Days(createCrbSummaryDetails(summaryElement, "bouncedCheques90Days"));
            summaryData.setBouncedCheques180Days(createCrbSummaryDetails(summaryElement, "bouncedCheques180Days"));
            summaryData.setBouncedCheques365Days(createCrbSummaryDetails(summaryElement, "bouncedCheques365Days"));
            summaryData.setClosedAccounts(createCrbSummaryDetails(summaryElement, "closedAccounts"));
            summaryData.setCreditApplications(createCrbSummaryDetails(summaryElement, "creditApplications"));
            summaryData.setCreditHistory(createCrbSummaryDetails(summaryElement, "creditHistory"));
            summaryData.setEnquiries31to60Days(createCrbSummaryDetails(summaryElement, "enquiries31to60Days"));
            summaryData.setEnquiries61to90Days(createCrbSummaryDetails(summaryElement, "enquiries61to90Days"));
            summaryData.setEnquiries91Days(createCrbSummaryDetails(summaryElement, "enquiries91Days"));
            summaryData.setEnquiriesLast30Days(createCrbSummaryDetails(summaryElement, "enquiriesLast30Days"));
            summaryData.setFraudulentCases(createCrbSummaryDetails(summaryElement, "fraudulentCases"));
            summaryData.setNpaAccounts(createCrbSummaryDetails(summaryElement, "npaAccounts"));
            summaryData.setOpenAccounts(createCrbSummaryDetails(summaryElement, "openAccounts"));
            summaryData.setPaAccounts(createCrbSummaryDetails(summaryElement, "paAccounts"));
            summaryData.setPaAccountsWithDh(createCrbSummaryDetails(summaryElement, "paAccountsWithDh"));
            summaryData.setPaClosedAccounts(createCrbSummaryDetails(summaryElement, "paClosedAccounts"));
            summaryData.setPaClosedAccountsWithDh(createCrbSummaryDetails(summaryElement, "paClosedAccountsWithDh"));
            summaryData.setPaAccounts(createCrbSummaryDetails(summaryElement, "paOpenAccounts"));
            summaryData.setInsurancePolicies(createCrbSummaryDetails(summaryElement, "insurancePolicies"));

            Element lastBouncedChequeDatelement = (Element) summaryElement.getElementsByTagName("lastBouncedChequeDate").item(0);
            if (lastBouncedChequeDatelement != null) {
                String Value = lastBouncedChequeDatelement.getTextContent();
                if (Value != null && !Value.isEmpty()) {
                    summaryData.setLastBouncedChequeDate(Value);
                }
            }

            Element lastCreditApplicationDateElement = (Element) summaryElement.getElementsByTagName("lastCreditApplicationDate").item(0);
            if (lastCreditApplicationDateElement != null) {
                String Value = lastCreditApplicationDateElement.getTextContent();
                if (Value != null && !Value.isEmpty()) {
                    summaryData.setLastCreditApplicationDate(Value);
                }
            }

            Element lastFraudDateElement = (Element) summaryElement.getElementsByTagName("lastFraudDate").item(0);
            if (lastFraudDateElement != null) {
                String Value = lastFraudDateElement.getTextContent();
                if (Value != null && !Value.isEmpty()) {
                    summaryData.setLastFraudDate(Value);
                }
            }

            Element lastNPAListingDateDateElement = (Element) summaryElement.getElementsByTagName("lastNPAListingDate").item(0);
            if (lastNPAListingDateDateElement != null) {
                String Value = lastNPAListingDateDateElement.getTextContent();
                if (Value != null && !Value.isEmpty()) {
                    summaryData.setLastNPAListingDate(Value);
                }
            }

            Element lastPAListingDateElement = (Element) summaryElement.getElementsByTagName("lastPAListingDate").item(0);
            if (lastPAListingDateElement != null) {
                String Value = lastPAListingDateElement.getTextContent();
                if (Value != null && !Value.isEmpty()) {
                    summaryData.setLastPAListingDate(Value);
                }
            }

            Element lastInsurancePolicyDateElement = (Element) summaryElement.getElementsByTagName("lastInsurancePolicyDate").item(0);
            if (lastInsurancePolicyDateElement != null) {
                String Value = lastInsurancePolicyDateElement.getTextContent();
                if (Value != null && !Value.isEmpty()) {
                    summaryData.setLastInsurancePolicyDate(Value);
                }
            }
        }
        return summaryData;
    }

    private CrbAccountsSummaryData createCrbSummaryDetails(Element summaryElement, String tag) {
        CrbAccountsSummaryData crbAccountsSummaryData = new CrbAccountsSummaryData();
        Element summaryElements = (Element) summaryElement.getElementsByTagName(tag).item(0);

        if (summaryElements != null) {

            Element allSectorsElement = (Element) summaryElements.getElementsByTagName("allSectors").item(0);
            if (allSectorsElement != null) {
                String allSectorsValue = allSectorsElement.getTextContent();
                if (allSectorsValue != null && !allSectorsValue.isEmpty()) {
                    crbAccountsSummaryData.setAllSectors(Integer.parseInt(allSectorsValue));
                }
            }

            Element mySectorElement = (Element) summaryElements.getElementsByTagName("mySector").item(0);
            if (mySectorElement != null) {
                String mySectorValue = mySectorElement.getTextContent();
                if (mySectorValue != null && !mySectorValue.isEmpty()) {
                    crbAccountsSummaryData.setMySector(Integer.parseInt(mySectorValue));
                }
            }

            Element otherSectorsElement = (Element) summaryElements.getElementsByTagName("otherSectors").item(0);
            if (otherSectorsElement != null) {
                String otherSectorsValue = otherSectorsElement.getTextContent();
                if (otherSectorsValue != null && !otherSectorsValue.isEmpty()) {
                    crbAccountsSummaryData.setOtherSectors(Integer.parseInt(otherSectorsValue));
                }
            }
        }

        return crbAccountsSummaryData;
    }

    private List<AccountListData> extractAccountList(Element returnElement) {
        List<AccountListData> accountList = new ArrayList<>();

        NodeList accountNodes = returnElement.getElementsByTagName("accountList");

        for (int i = 0; i < accountNodes.getLength(); i++) {
            Element accountElement = (Element) accountNodes.item(i);
            AccountListData accountData = extractAccountListData(accountElement);

            if (accountData != null) {
                accountList.add(accountData);
            }
        }

        return accountList;
    }

    private AccountListData extractAccountListData(Element account) {
        AccountListData accountListData = new AccountListData();

        accountListData.setAccountNo(getElementTextContent(account, "accountNo"));
        accountListData.setAccountOpeningDate(getElementTextContent(account, "accountOpeningDate"));
        accountListData.setAccountOwner(getElementTextContent(account, "accountOwner"));
        accountListData.setAccountStatus(getElementTextContent(account, "accountStatus"));
        accountListData.setAccountType(getElementTextContent(account, "accountType"));
        accountListData.setArrearAmount(getElementBigDecimalContent(account, "arrearAmount"));
        accountListData.setArrearDays(getElementIntegerContent(account, "arrearDays"));
        accountListData.setBalanceAmount(getElementBigDecimalContent(account, "balanceAmount"));
        accountListData.setCurrency(getElementTextContent(account, "currency"));
        accountListData.setDisputed(getElementBooleanContent(account, "disputed"));
        accountListData.setMyAccount(getElementBooleanContent(account, "isMyAccount"));
        accountListData.setLastPaymentDate(getElementTextContent(account, "lastPaymentDate"));
        accountListData.setListingDate(getElementTextContent(account, "listingDate"));
        accountListData.setPrincipalAmount(getElementBigDecimalContent(account, "principalAmount"));
        accountListData.setRepaymentDuration(getElementIntegerContent(account, "repaymentDuration"));
        accountListData.setRepaymentTerm(getElementTextContent(account, "repaymentTerm"));
        accountListData.setScheduledPaymentAmount(getElementBigDecimalContent(account, "scheduledPaymentAmount"));
        accountListData.setTradeSector(getElementTextContent(account, "tradeSector"));
        accountListData.setWorstArrear(getElementIntegerContent(account, "worstArrear"));

        return accountListData;
    }

    private List<PhoneListData> extractPhoneList(Element returnElement) {
        List<PhoneListData> phoneList = new ArrayList<>();

        NodeList accountNodes = returnElement.getElementsByTagName("phoneList");

        for (int i = 0; i < accountNodes.getLength(); i++) {
            Element accountElement = (Element) accountNodes.item(i);
            PhoneListData phoneData = extractPhoneListData(accountElement);

            if (phoneData != null) {
                phoneList.add(phoneData);
            }
        }

        return phoneList;
    }

    private PhoneListData extractPhoneListData(Element account) {
        PhoneListData phoneListData = new PhoneListData();

        phoneListData.setCreateDate(getElementTextContent(account, "createDate"));
        phoneListData.setPhoneExchange(getElementTextContent(account, "phoneExchange"));
        phoneListData.setPhoneNo(getElementTextContent(account, "phoneNo"));
        phoneListData.setPhoneType(getElementTextContent(account, "phoneType"));

        return phoneListData;
    }

    private List<RecentEnquiryListData> extractRecentEnquiryList(Element returnElement) {
        List<RecentEnquiryListData> recentEnquiryList = new ArrayList<>();

        NodeList accountNodes = returnElement.getElementsByTagName("recentEnquiryList");

        for (int i = 0; i < accountNodes.getLength(); i++) {
            Element accountElement = (Element) accountNodes.item(i);
            RecentEnquiryListData recentEnquiryData = extractRecentEnquiryData(accountElement);

            if (recentEnquiryData != null) {
                recentEnquiryList.add(recentEnquiryData);
            }
        }

        return recentEnquiryList;
    }

    private RecentEnquiryListData extractRecentEnquiryData(Element account) {
        RecentEnquiryListData recentEnquiryListData = new RecentEnquiryListData();

        recentEnquiryListData.setEnquiryDate(getElementTextContent(account, "enquiryDate"));
        recentEnquiryListData.setEnquiryReason(getElementTextContent(account, "enquiryReason"));
        recentEnquiryListData.setTradeSector(getElementTextContent(account, "tradeSector"));

        return recentEnquiryListData;
    }

    private List<PhysicalAddressListData> extractPhysicalAddressList(Element returnElement) {
        List<PhysicalAddressListData> physicalAddressListDataList = new ArrayList<>();

        NodeList accountNodes = returnElement.getElementsByTagName("physicalAddressList");

        for (int i = 0; i < accountNodes.getLength(); i++) {
            Element accountElement = (Element) accountNodes.item(i);
            PhysicalAddressListData physicalAddressData = extractPhysicalAddressData(accountElement);

            if (physicalAddressData != null) {
                physicalAddressListDataList.add(physicalAddressData);
            }
        }

        return physicalAddressListDataList;
    }

    private PhysicalAddressListData extractPhysicalAddressData(Element account) {
        PhysicalAddressListData physicalAddressListData = new PhysicalAddressListData();

        physicalAddressListData.setAddress(getElementTextContent(account, "address"));
        physicalAddressListData.setCreateDate(getElementTextContent(account, "createDate"));
        physicalAddressListData.setDurationInMonths(getElementTextContent(account, "durationInMonths"));
        physicalAddressListData.setDurationInYears(getElementTextContent(account, "durationInYears"));
        physicalAddressListData.setTown(getElementTextContent(account, "town"));

        return physicalAddressListData;
    }

    private List<PostalAddressListData> extractPostalAddressList(Element returnElement) {
        List<PostalAddressListData> postalAddressListDataList = new ArrayList<>();

        NodeList accountNodes = returnElement.getElementsByTagName("postalAddressList");

        for (int i = 0; i < accountNodes.getLength(); i++) {
            Element accountElement = (Element) accountNodes.item(i);
            PostalAddressListData postalAddressListData = extractPostalAddressListData(accountElement);

            if (postalAddressListData != null) {
                postalAddressListDataList.add(postalAddressListData);
            }
        }

        return postalAddressListDataList;
    }

    private PostalAddressListData extractPostalAddressListData(Element account) {
        PostalAddressListData postalAddressListData = new PostalAddressListData();

        postalAddressListData.setCountry(getElementTextContent(account, "country"));
        postalAddressListData.setCreateDate(getElementTextContent(account, "createDate"));
        postalAddressListData.setPostalCode(getElementTextContent(account, "postalCode"));
        postalAddressListData.setPostalNo(getElementTextContent(account, "postalNo"));
        postalAddressListData.setPostalType(getElementTextContent(account, "postalType"));
        postalAddressListData.setTown(getElementTextContent(account, "town"));

        return postalAddressListData;
    }
}
