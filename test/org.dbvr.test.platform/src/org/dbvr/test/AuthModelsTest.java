/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dbvr.test;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class AuthModelsTest extends DBVRTest {

    public static final String AUTH_MODELS_ARG = "auth-models";

    @Test
    public void testListAllAuthModels() throws Exception {
        var args = new String[] {
            AUTH_MODELS_ARG
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertNotNull(result.getOutput());
        Assert.assertFalse(result.getOutput().isEmpty());
    }

    @Test
    public void testFilterByDriver() throws Exception {
        String driverId = "h2_embedded_v2";
        var args = new String[] {
            AUTH_MODELS_ARG,
            "--driver=" + driverId
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertNotNull(result.getOutput());
        Assert.assertFalse(result.getOutput().isEmpty());
        
        String output = result.getOutput().getFirst();
        Assert.assertTrue(output.contains("Applicable Drivers: H2 Embedded"));
        Assert.assertTrue(output.contains("Auth Model ID: native"));
    }

    @Test
    public void testFilterByProvider() throws Exception {
        String providerId = "generic";
        var args = new String[] {
            AUTH_MODELS_ARG,
            "--provider=" + providerId
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertNotNull(result.getOutput());
        Assert.assertFalse(result.getOutput().isEmpty());
        
        String output = result.getOutput().getFirst();
        Assert.assertTrue(output.contains("Auth Model ID:"));
        Assert.assertTrue(output.contains("Applicable Drivers:"));
    }

    @Test
    public void testFilterByConnection() throws Exception {
        String uniqueName = "datasource_" + UUID.randomUUID();
        DBPDataSourceContainer ds = createFakeDataSource(uniqueName);
        try {
            var args = new String[] {
                AUTH_MODELS_ARG,
                "--datasource=" + uniqueName
            };

            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

            Assert.assertNotNull(result.getOutput());
            Assert.assertFalse(result.getOutput().isEmpty());
            
            String output = result.getOutput().getFirst();
            Assert.assertTrue(output.contains("Applicable Drivers: H2 Embedded"));
            Assert.assertTrue(output.contains("Auth Model ID: native"));
        } finally {
            ds.getRegistry().removeDataSource(ds);
        }
    }

    @NotNull
    private static DBPDataSourceContainer createFakeDataSource(@NotNull String uniqName) throws DBException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry()
            .findDriver("h2_embedded_v2");
        var connectionConfiguration = new DBPConnectionConfiguration();
        connectionConfiguration.setUrl("jdbc:h2:mem:" + UUID.randomUUID());
        var dataSourceRegistry = DBWorkbench.getPlatform().getWorkspace().getActiveProject()
            .getDataSourceRegistry();

        var dbpDataSourceContainer = dataSourceRegistry.createDataSource(driver, connectionConfiguration);
        dbpDataSourceContainer.setName(uniqName);
        dataSourceRegistry.addDataSource(dbpDataSourceContainer);
        return dbpDataSourceContainer;
    }
}
