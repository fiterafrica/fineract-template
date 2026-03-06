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
package org.apache.fineract.infrastructure.bulkimport.populator.loanrepayment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.fineract.infrastructure.bulkimport.constants.LoanRepaymentConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.ExtrasSheetPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.OfficeSheetPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.comparator.LoanComparatorByStatusActive;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoanRepaymentWorkbookPopulator extends AbstractWorkbookPopulator {

    private static final Logger LOG = LoggerFactory.getLogger(LoanRepaymentWorkbookPopulator.class);
    private final OfficeSheetPopulator officeSheetPopulator;
    private final ExtrasSheetPopulator extrasSheetPopulator;
    private final List<LoanAccountData> allloans;

    public LoanRepaymentWorkbookPopulator(List<LoanAccountData> loans, OfficeSheetPopulator officeSheetPopulator,
            ExtrasSheetPopulator extrasSheetPopulator) {
        this.allloans = loans;
        this.officeSheetPopulator = officeSheetPopulator;
        this.extrasSheetPopulator = extrasSheetPopulator;
    }

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet loanRepaymentSheet = workbook.createSheet(TemplatePopulateImportConstants.LOAN_REPAYMENT_SHEET_NAME);
        setLayout(loanRepaymentSheet);
        officeSheetPopulator.populate(workbook, dateFormat);
        extrasSheetPopulator.populate(workbook, dateFormat);
        populateLoansTable(loanRepaymentSheet, dateFormat);
        setRules(loanRepaymentSheet, dateFormat);
        setDefaults(loanRepaymentSheet, dateFormat);
    }


    private void setDefaults(Sheet worksheet, String dateFormat) {
        // No defaults needed as product, principal, total outstanding and disbursement date columns are removed
    }

    private void setRules(Sheet worksheet, String dateFormat) {
        CellRangeAddressList officeNameRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanRepaymentConstants.OFFICE_NAME_COL, LoanRepaymentConstants.OFFICE_NAME_COL);
        CellRangeAddressList repaymentTypeRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanRepaymentConstants.REPAYMENT_TYPE_COL, LoanRepaymentConstants.REPAYMENT_TYPE_COL);
        CellRangeAddressList loanAccountNoRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL, LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL);

        DataValidationHelper validationHelper = new XSSFDataValidationHelper((XSSFSheet) worksheet);

        setNames(worksheet);

        DataValidationConstraint officeNameConstraint = validationHelper.createFormulaListConstraint("Office");
        DataValidationConstraint paymentTypeConstraint = validationHelper.createFormulaListConstraint("PaymentTypes");
        DataValidationConstraint loanAccountNoConstraint = validationHelper.createFormulaListConstraint("LoanAccounts");

        DataValidation officeValidation = validationHelper.createValidation(officeNameConstraint, officeNameRange);
        DataValidation repaymentTypeValidation = validationHelper.createValidation(paymentTypeConstraint, repaymentTypeRange);
        DataValidation loanAccountNoValidation = validationHelper.createValidation(loanAccountNoConstraint, loanAccountNoRange);

        worksheet.addValidationData(officeValidation);
        worksheet.addValidationData(repaymentTypeValidation);
        worksheet.addValidationData(loanAccountNoValidation);

    }

    private void setNames(Sheet worksheet) {
        ArrayList<String> officeNames = new ArrayList<>(officeSheetPopulator.getOfficeNames());
        Workbook loanRepaymentWorkbook = worksheet.getWorkbook();
        // Office Names
        Name officeGroup = loanRepaymentWorkbook.createName();
        officeGroup.setNameName("Office");
        officeGroup.setRefersToFormula(TemplatePopulateImportConstants.OFFICE_SHEET_NAME + "!$B$2:$B$" + (officeNames.size() + 1));

        // Payment Type Name
        Name paymentTypeGroup = loanRepaymentWorkbook.createName();
        paymentTypeGroup.setNameName("PaymentTypes");
        paymentTypeGroup.setRefersToFormula(
                TemplatePopulateImportConstants.EXTRAS_SHEET_NAME + "!$D$2:$D$" + (extrasSheetPopulator.getPaymentTypesSize() + 1));

        // Loan Account Numbers (from Lookup Account column)
        if (allloans != null && !allloans.isEmpty()) {
            Name loanAccountGroup = loanRepaymentWorkbook.createName();
            loanAccountGroup.setNameName("LoanAccounts");
            loanAccountGroup.setRefersToFormula(TemplatePopulateImportConstants.LOAN_REPAYMENT_SHEET_NAME + "!$L$2:$L$" + (allloans.size() + 1));
        }
    }

    private void populateLoansTable(Sheet loanRepaymentSheet, String dateFormat) {
        // Only populate loans table if loans list is not empty
        if (allloans == null || allloans.isEmpty()) {
            return;
        }
        int rowIndex = 1;
        Row row;
        Collections.sort(allloans, new LoanComparatorByStatusActive());
        for (LoanAccountData loan : allloans) {
            row = loanRepaymentSheet.createRow(rowIndex++);
            writeString(LoanRepaymentConstants.LOOKUP_ACCOUNT_NO_COL, row, loan.getAccountNo());
        }
    }

    private void setLayout(Sheet worksheet) {
        Workbook workbook = worksheet.getWorkbook();
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);

        // Create text format style for Loan Account No column to preserve leading zeros
        CellStyle textStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        textStyle.setDataFormat(format.getFormat("@")); // "@" means text format
        worksheet.setDefaultColumnStyle(LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL, textStyle);

        // Pre-create cells with text format for the first 1000 rows to ensure leading zeros are preserved
        for (int i = 1; i <= 1000; i++) {
            Row row = worksheet.getRow(i);
            if (row == null) {
                row = worksheet.createRow(i);
            }
            row.createCell(LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL).setCellStyle(textStyle);
        }

        worksheet.setColumnWidth(LoanRepaymentConstants.OFFICE_NAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.AMOUNT_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.REPAID_ON_DATE_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.REPAYMENT_TYPE_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.ACCOUNT_NO_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.CHECK_NO_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.RECEIPT_NO_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.ROUTING_CODE_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.BANK_NO_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanRepaymentConstants.LOOKUP_ACCOUNT_NO_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        writeString(LoanRepaymentConstants.OFFICE_NAME_COL, rowHeader, "Office Name*");
        writeString(LoanRepaymentConstants.LOAN_ACCOUNT_NO_COL, rowHeader, "Loan Account No.*");
        writeString(LoanRepaymentConstants.AMOUNT_COL, rowHeader, "Amount Repaid*");
        writeString(LoanRepaymentConstants.REPAID_ON_DATE_COL, rowHeader, "Date*");
        writeString(LoanRepaymentConstants.REPAYMENT_TYPE_COL, rowHeader, "Type*");
        writeString(LoanRepaymentConstants.ACCOUNT_NO_COL, rowHeader, "Account No");
        writeString(LoanRepaymentConstants.CHECK_NO_COL, rowHeader, "Check No");
        writeString(LoanRepaymentConstants.RECEIPT_NO_COL, rowHeader, "Receipt No");
        writeString(LoanRepaymentConstants.ROUTING_CODE_COL, rowHeader, "Routing Code");
        writeString(LoanRepaymentConstants.BANK_NO_COL, rowHeader, "Bank No");
        writeString(LoanRepaymentConstants.LOOKUP_ACCOUNT_NO_COL, rowHeader, "Lookup Account");
    }
}
