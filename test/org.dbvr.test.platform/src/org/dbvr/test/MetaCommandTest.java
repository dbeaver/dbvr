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
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class MetaCommandTest extends DBVRTest {

    private String dataSourceName;
    private DBPDataSourceContainer dataSourceContainer;

    @Before
    public void setUp() throws Exception {
        dataSourceName = "meta_test_ds_" + UUID.randomUUID();
        dataSourceContainer = createFakeDataSource(dataSourceName);
    }

    @Test
    public void testDatabaseList() throws Exception {
        var args = new String[] {
            "meta", "database", "list",
            "--datasource=" + dataSourceName
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertNotNull(result.getOutput());
        // H2 does not support catalogs (databases in CLI terms)
        // If we want this test to be "valid" (showing results), we should use a datasource that supports catalogs.
        // But since we use H2 in tests, we check for the expected error/message.
        Assert.assertTrue("Output should contain 'No databases found' or 'not support' for H2: " + result.getOutput(),
            result.getOutput().stream().anyMatch(line -> line.contains("No databases found") || line.contains("does not support metadata")));
    }

    @Test
    public void testSchemaList() throws Exception {
        var args = new String[] {
            "meta", "schema", "list",
            "--datasource=" + dataSourceName
        };
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

        Assert.assertNotNull(result.getOutput());
        Assert.assertFalse("Output should not be empty", result.getOutput().isEmpty());
        boolean foundPublic = result.getOutput().stream().anyMatch(line -> line.contains("PUBLIC"));
        Assert.assertTrue("Schema PUBLIC should be present: " + String.join("\n", result.getOutput()), foundPublic);
    }

    @Test
    public void testTableListAndDDL() throws Exception {
        var monitor = new VoidProgressMonitor();
        dataSourceContainer.connect(monitor, true, false);
        var dataSource = dataSourceContainer.getDataSource();
        Assert.assertNotNull(dataSource);
        var context = dataSource.getDefaultInstance().getDefaultContext(monitor, false);
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.USER, "Create test table")) {
            try (var stmt = session.prepareStatement(DBCStatementType.QUERY, "CREATE TABLE TEST_META_TABLE (ID INT PRIMARY KEY, NAME VARCHAR(100))", false, false, false)) {
                stmt.executeStatement();
            }
        }

        var listArgs = new String[] {
            "meta", "table", "list",
            "--datasource=" + dataSourceName,
            "--schema=PUBLIC"
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult listResult = cmd.executeCommandLineCommands(null, false, false, listArgs);

        Assert.assertNotNull(listResult.getOutput());
        boolean foundTable = listResult.getOutput().stream().anyMatch(line -> line.contains("TEST_META_TABLE"));
        Assert.assertTrue(foundTable);

        var ddlArgs = new String[] {
            "meta", "table", "ddl",
            "--datasource=" + dataSourceName,
            "--schema=PUBLIC",
            "--object=TEST_META_TABLE"
        };

        CLIProcessResult ddlResult = cmd.executeCommandLineCommands(null, false, false, ddlArgs);
        Assert.assertNotNull(ddlResult.getOutput());
        Assert.assertFalse(ddlResult.getOutput().isEmpty());
        
        String ddl = String.join("\n", ddlResult.getOutput());
        Assert.assertTrue("DDL should contain CREATE TABLE", ddl.contains("CREATE TABLE"));
        Assert.assertTrue("DDL should contain TEST_META_TABLE", ddl.contains("TEST_META_TABLE"));
    }

    @NotNull
    private static DBPDataSourceContainer createFakeDataSource(@NotNull String uniqName) throws DBException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry()
            .findDriver("h2_embedded_v2");
        if (driver == null) {
            driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("h2_embedded");
        }
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
