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
package org.apache.fineract.infrastructure.bulkimport.populator;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientSheetPopulatorTest {

    @Mock
    private OfficeData officeData;
    
    @Test
    public void testPopulateClientsWithXssfAndTooManyClients() {
        // given
        List<OfficeData> offices = new ArrayList<>();
        when(officeData.name()).thenReturn("Office 1");
        offices.add(officeData);

        List<ClientData> clients = new ArrayList<>();
        for (int i = 0; i < 65537; i++) {
            clients.add(ClientData.clientIdentifier((long) i, "acc"+i, "Client", "", ""+i, "Client "+i, "Client "+i, 1L, "Office 1"));
        }

        ClientSheetPopulator populator = new ClientSheetPopulator(clients, offices);
        Workbook workbook = new XSSFWorkbook();

        // when
        populator.populate(workbook, "dd-MM-yyyy");

        // then
        // no exception should be thrown
    }
}