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
package org.apache.fineract.infrastructure.bulkimport.importhandler.savings;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.fineract.commands.domain.CommandSourceRepository;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TransactionConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.SavingsAccountTransactionEnumValueSerialiser;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionEnumData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SavingsTransactionImportHandler implements ImportHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SavingsTransactionImportHandler.class);
    private Workbook workbook;
    private List<SavingsAccountTransactionData> savingsTransactions;
    private List<String> transactionReferences;

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper;
    private final CommandSourceRepository commandSourceRepository;

    @Autowired
    public SavingsTransactionImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final SavingsAccountTransactionRepository savingsAccountTransactionRepository,
            final SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper, final CommandSourceRepository commandSourceRepository) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.savingsAccountRepositoryWrapper = savingsAccountRepositoryWrapper;
        this.commandSourceRepository = commandSourceRepository;
    }

    @Override
    public Count process(Workbook workbook, String locale, String dateFormat) {
        this.workbook = workbook;
        this.savingsTransactions = new ArrayList<>();
        this.transactionReferences = new ArrayList<>();

        try {
            readExcelFile(locale, dateFormat);
            Count result = importEntity(dateFormat);

            return result;
        } catch (Exception ex) {

            ex.printStackTrace();
            LOG.error("Critical error during bulk import processing: {}", ex.getMessage(), ex);
            // Write error to the first row of the sheet so user can see it
            try {
                Sheet sheet = workbook.getSheet(TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME);
                if (sheet == null) {
                    sheet = workbook.createSheet(TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME);
                }
                Row errorRow = sheet.getRow(1);
                if (errorRow == null) {
                    errorRow = sheet.createRow(1);
                }
                Cell errorCell = errorRow.createCell(TransactionConstants.STATUS_COL);
                errorCell.setCellValue("CRITICAL ERROR: " + ex.getMessage());
                errorCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.RED));
            } catch (Exception writeEx) {

                LOG.error("Failed to write error to workbook: {}", writeEx.getMessage());
            }
            return Count.instance(0, 1);
        }
    }

    public void readExcelFile(String locale, String dateFormat) {

        Sheet savingsTransactionSheet = workbook.getSheet(TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME);

        if (savingsTransactionSheet == null) {

            LOG.error("Sheet '{}' not found in the uploaded file", TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME);
            throw new RuntimeException("Sheet '" + TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME
                    + "' not found in the uploaded file. Please download and use the correct template.");
        }

        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(savingsTransactionSheet, TransactionConstants.AMOUNT_COL);

        LOG.info("Processing {} rows from savings transaction sheet", noOfEntries);

        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row = savingsTransactionSheet.getRow(rowIndex);
            if (row == null) {

                LOG.warn("Row {} is null, skipping", rowIndex);
                continue;
            }
            if (ImportHandlerUtils.isNotImported(row, TransactionConstants.STATUS_COL)) {
                try {

                    SavingsAccountTransactionData transactionData = readSavingsTransaction(row, locale, dateFormat);
                    savingsTransactions.add(transactionData);
                    // Read and store transaction reference for duplicate checking
                    String transactionReference = ImportHandlerUtils.readAsString(TransactionConstants.TRANSACTION_REFERENCE_COL, row);
                    transactionReferences.add(transactionReference);

                } catch (RuntimeException ex) {

                    ex.printStackTrace();
                    LOG.error("Error reading row {}: {}", rowIndex, ex.getMessage(), ex);
                    // Add null to keep indices aligned, will be handled in importEntity
                    savingsTransactions.add(null);
                    transactionReferences.add(null);
                    // Write error to the row - ensure row exists
                    Row errorRow = savingsTransactionSheet.getRow(rowIndex);
                    if (errorRow == null) {
                        errorRow = savingsTransactionSheet.createRow(rowIndex);
                    }
                    Cell statusCell = errorRow.createCell(TransactionConstants.STATUS_COL);
                    statusCell.setCellValue("Error: " + ex.getMessage());
                    statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.RED));
                }
            }
        }
        long validCount = savingsTransactions.stream().filter(t -> t != null).count();

        LOG.info("Finished reading. Total transactions: {}, Valid: {}", savingsTransactions.size(), validCount);
    }

    private SavingsAccountTransactionData readSavingsTransaction(Row row, String locale, String dateFormat) {
        // Read account number from the Excel
        String accountNo = ImportHandlerUtils.readAsString(TransactionConstants.SAVINGS_ACCOUNT_NO_COL, row);
        if (accountNo == null) {
            // Try reading as Long if it's numeric
            Long accountNoLong = ImportHandlerUtils.readAsLong(TransactionConstants.SAVINGS_ACCOUNT_NO_COL, row);
            if (accountNoLong != null) {
                accountNo = accountNoLong.toString();
            }
        }

        // Look up the internal savings account ID from the database
        Long savingsId = lookupSavingsIdByAccountNo(accountNo);
        if (savingsId == null || savingsId == 0L) {
            throw new RuntimeException("Savings account with Account No '" + accountNo + "' not found in the system");
        }

        String transactionType = ImportHandlerUtils.readAsString(TransactionConstants.TRANSACTION_TYPE_COL, row);
        SavingsAccountTransactionEnumData savingsAccountTransactionEnumData = new SavingsAccountTransactionEnumData(null, null,
                transactionType);

        BigDecimal amount = null;
        Cell amountCell = row.getCell(TransactionConstants.AMOUNT_COL);

        Double amountDouble = ImportHandlerUtils.readAsDouble(TransactionConstants.AMOUNT_COL, row);

        if (amountDouble != null && amountDouble > 0) {
            amount = BigDecimal.valueOf(amountDouble);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount is required and must be greater than zero. Got: " + amount);
        }

        LocalDate transactionDate = ImportHandlerUtils.readAsDate(TransactionConstants.TRANSACTION_DATE_COL, row);
        String paymentType = ImportHandlerUtils.readAsString(TransactionConstants.PAYMENT_TYPE_COL, row);
        Long paymentTypeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.EXTRAS_SHEET_NAME),
                paymentType);
        String accountNumber = ImportHandlerUtils.readAsString(TransactionConstants.ACCOUNT_NO_COL, row);
        String checkNumber = ImportHandlerUtils.readAsString(TransactionConstants.CHECK_NO_COL, row);
        String routingCode = ImportHandlerUtils.readAsString(TransactionConstants.ROUTING_CODE_COL, row);
        String receiptNumber = ImportHandlerUtils.readAsString(TransactionConstants.RECEIPT_NO_COL, row);
        String bankNumber = ImportHandlerUtils.readAsString(TransactionConstants.BANK_NO_COL, row);
        return SavingsAccountTransactionData.importInstance(amount, transactionDate, paymentTypeId, accountNumber, checkNumber, routingCode,
                receiptNumber, bankNumber, savingsId, savingsAccountTransactionEnumData, row.getRowNum(), locale, dateFormat);
    }

    private Long lookupSavingsIdByAccountNo(String accountNo) {
        if (accountNo == null || accountNo.trim().isEmpty()) {
            return null;
        }
        SavingsAccount savingsAccount = savingsAccountRepositoryWrapper.findByAccountNumber(accountNo);
        if (savingsAccount == null) {
            return null;
        }
        return savingsAccount.getId();
    }

    public Count importEntity(String dateFormat) {

        Sheet savingsTransactionSheet = workbook.getSheet(TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME);
        if (savingsTransactionSheet == null) {
            throw new RuntimeException("Sheet '" + TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME
                    + "' not found in the uploaded file. Please use the correct template.");
        }

        int successCount = 0;
        int errorCount = 0;
        String errorMessage = "";
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        gsonBuilder.registerTypeAdapter(SavingsAccountTransactionEnumData.class, new SavingsAccountTransactionEnumValueSerialiser());

        // Track processed transaction references within this batch to detect duplicates
        Set<String> processedReferences = new HashSet<>();

        for (int i = 0; i < savingsTransactions.size(); i++) {
            SavingsAccountTransactionData transaction = savingsTransactions.get(i);
            String transactionReference = transactionReferences.get(i);

            // Skip transactions that had errors during reading (null entries)
            if (transaction == null) {
                errorCount++;
                continue;
            }

            try {
                // Validate transaction reference is provided
                if (transactionReference == null || transactionReference.trim().isEmpty()) {
                    throw new RuntimeException("Transaction Reference is required to prevent duplicate transactions");
                }

                // Check for duplicate within the current batch
                if (processedReferences.contains(transactionReference)) {
                    throw new RuntimeException("Duplicate Transaction Reference '" + transactionReference + "' found in upload file");
                }

                // Check for duplicate in completed transactions in database
                if (savingsAccountTransactionRepository.existsByRefNo(transactionReference)) {
                    throw new RuntimeException("Transaction with Reference '" + transactionReference + "' already exists in the system");
                }

                // Check for duplicate in pending commands (maker-checker awaiting approval)
                // If already pending, mark as pending (not an error) and continue to next row
                if (commandSourceRepository.existsPendingSavingsTransactionByRefNo(transactionReference)) {
                    throw new RuntimeException(
                            "Transaction with Reference '" + transactionReference + "' is already pending approval in the system");
                }

                JsonObject savingsTransactionJsonob = gsonBuilder.create().toJsonTree(transaction).getAsJsonObject();
                savingsTransactionJsonob.remove("transactionType");
                savingsTransactionJsonob.remove("reversed");
                savingsTransactionJsonob.remove("interestedPostedAsOn");
                savingsTransactionJsonob.remove("isManualTransaction");
                savingsTransactionJsonob.remove("lienTransaction");
                savingsTransactionJsonob.remove("chargesPaidByData");

                // Add transaction reference to payload
                savingsTransactionJsonob.addProperty("refNo", transactionReference);

                String payload = savingsTransactionJsonob.toString();

                CommandWrapper commandRequest = null;
                String transactionType = transaction.getTransactionType().getValue();

                if (TransactionConstants.TRANSACTION_TYPE_WITHDRAWAL.equals(transactionType)) {
                    commandRequest = new CommandWrapperBuilder() //
                            .savingsAccountWithdrawal(transaction.getSavingsAccountId()) //
                            .withJson(payload) //
                            .build(); //

                } else if (TransactionConstants.TRANSACTION_TYPE_DEPOSIT.equals(transactionType)) {
                    // Money Transfer Inward is processed as a deposit with the appropriate payment type

                    commandRequest = new CommandWrapperBuilder() //
                            .savingsAccountDeposit(transaction.getSavingsAccountId()) //
                            .withJson(payload) //
                            .build();
                } else {
                    throw new RuntimeException("Unknown transaction type: " + transactionType);
                }

                final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

                // Log the result
                Long transactionId = result.resourceId();

                if (result.isRollbackTransaction()) {
                    throw new RuntimeException("Transaction was marked for rollback by the system");
                }

                LOG.info("Transaction created successfully: transactionId={}, savingsAccountId={}, ref={}", transactionId,
                        transaction.getSavingsAccountId(), transactionReference);

                // Add to processed references after successful processing
                processedReferences.add(transactionReference);

                successCount++;
                Row successRow = savingsTransactionSheet.getRow(transaction.getRowIndex());
                if (successRow == null) {
                    successRow = savingsTransactionSheet.createRow(transaction.getRowIndex());
                }
                Cell statusCell = successRow.createCell(TransactionConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Error processing row {}: {}", transaction.getRowIndex(), ex.getMessage(), ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(savingsTransactionSheet, transaction.getRowIndex(), errorMessage,
                        TransactionConstants.STATUS_COL);
            }
        }

        LOG.info("Import completed. Success: {}, Errors: {}", successCount, errorCount);

        savingsTransactionSheet.setColumnWidth(TransactionConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        // Write status header to row 0 (header row), not to row index STATUS_COL
        Row headerRow = savingsTransactionSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        if (headerRow != null) {
            ImportHandlerUtils.writeString(TransactionConstants.STATUS_COL, headerRow,
                    TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        }
        return Count.instance(successCount, errorCount);
    }

}
