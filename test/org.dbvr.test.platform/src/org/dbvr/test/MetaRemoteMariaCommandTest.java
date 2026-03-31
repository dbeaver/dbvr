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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class MetaRemoteMariaCommandTest extends DBVRTest {
    private String mariaDsName;
    private DBPDataSourceContainer mariaDs;

    @org.junit.Before
    public void setup() throws Exception {
        mariaDsName = "maria_real_ds_" + UUID.randomUUID();
        mariaDs = createRealMariaDBDataSource(mariaDsName);
    }

    @org.junit.After
    public void tearDown() {
        if (mariaDs != null) {
            mariaDs.getRegistry().removeDataSource(mariaDs);
        }
    }

    @Test
    public void testMariaDBDatabaseList() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var dbArgs = new String[]{
            "meta", "database", "list",
            "--datasource=" + mariaDsName
        };
        CLIProcessResult dbResult = cmd.executeCommandLineCommands(null, false, false, dbArgs);
        Assert.assertNotNull(dbResult.getOutput());
        boolean foundLmDb = dbResult.getOutput().stream().anyMatch(line -> line.contains("lm"));
        Assert.assertTrue("Database 'lm' should be present in MariaDB", foundLmDb);
    }

    @Test
    public void testMariaDBSchemaList() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var schemaArgs = new String[]{
            "meta", "schema", "list",
            "--datasource=" + mariaDsName
        };
        CLIProcessResult schemaResult = cmd.executeCommandLineCommands(null, false, false, schemaArgs);
        Assert.assertNotNull(schemaResult.getOutput());
        boolean foundLmSchema = schemaResult.getOutput().stream().anyMatch(line -> line.contains("lm"));
        Assert.assertTrue("Schema 'lm' should be present in MariaDB (as it's a catalog-based system)", foundLmSchema);
    }

    @Test
    public void testMariaDBTableList() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var tableArgs = new String[]{
            "meta", "table", "list",
            "--datasource=" + mariaDsName,
            "--database-name=lm"
        };
        CLIProcessResult tableResult = cmd.executeCommandLineCommands(null, false, false, tableArgs);
        Assert.assertNotNull(tableResult.getOutput());
        Assert.assertFalse("Tables in 'lm' should not be empty", tableResult.getOutput().isEmpty());
    }

    @Test
    public void testMariaDBTableDdl() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var tableArgs = new String[]{
            "meta", "table", "list",
            "--datasource=" + mariaDsName,
            "--database-name=lm"
        };
        CLIProcessResult tableResult = cmd.executeCommandLineCommands(null, false, false, tableArgs);
        Assert.assertNotNull(tableResult.getOutput());

        if (!tableResult.getOutput().isEmpty()) {
            String firstTable = tableResult.getOutput().get(0).trim();
            System.out.println("[DEBUG_LOG] Requesting DDL for table: " + firstTable);
            var ddlArgs = new String[]{
                "meta", "table", "ddl",
                "--datasource=" + mariaDsName,
                "--database-name=lm",
                "--table-name=" + firstTable
            };
            CLIProcessResult ddlResult = cmd.executeCommandLineCommands(null, false, false, ddlArgs);
            Assert.assertNotNull(ddlResult.getOutput());
            String ddlOutput = String.join("\n", ddlResult.getOutput());
            Assert.assertFalse("DDL for '" + firstTable + "' should not be empty", ddlOutput.isEmpty());
            Assert.assertTrue("DDL should contain '" + firstTable + "'", ddlOutput.contains(firstTable));
        }
    }

    @Test
    public void testMariaDBSchemaDdl() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var schemaDdlArgs = new String[]{
            "meta", "schema", "ddl",
            "--datasource=" + mariaDsName,
            "--database-name=lm"
        };
        CLIProcessResult schemaDdlResult = cmd.executeCommandLineCommands(null, false, false, schemaDdlArgs);
        Assert.assertNotNull(schemaDdlResult.getOutput());
        String schemaDdlOutput = String.join("\n", schemaDdlResult.getOutput());
        Assert.assertFalse("Schema DDL should not be null", schemaDdlOutput.isEmpty());
    }

    @Test
    public void testMariaDBDatabaseDdl() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var dbDdlArgs = new String[]{
            "meta", "database", "ddl",
            "--datasource=" + mariaDsName,
            "--database-name=lm"
        };
        CLIProcessResult dbDdlResult = cmd.executeCommandLineCommands(null, false, false, dbDdlArgs);
        Assert.assertNotNull(dbDdlResult.getOutput());
        String dbDdlOutput = String.join("\n", dbDdlResult.getOutput());
        Assert.assertFalse("Database DDL should not be null", dbDdlOutput.isEmpty());
    }

    @Test
    public void testMariaDBDatabaseNotFound() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var dbNotFoundArgs = new String[]{
            "meta", "database", "list",
            "--datasource=" + mariaDsName,
            "--database-name=non_existent_db"
        };
        CLIProcessResult dbNotFoundResult = cmd.executeCommandLineCommands(null, false, false, dbNotFoundArgs);
        Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, dbNotFoundResult.getExitCode());
        Assert.assertTrue(String.join("\n", dbNotFoundResult.getOutput()).contains("not found"));
    }

    @Test
    public void testMariaDBTableNotFound() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var tableNotFoundArgs = new String[]{
            "meta", "table", "ddl",
            "--datasource=" + mariaDsName,
            "--database-name=lm",
            "--table-name=non_existent_table"
        };
        CLIProcessResult tableNotFoundResult = cmd.executeCommandLineCommands(null, false, false, tableNotFoundArgs);
        Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, tableNotFoundResult.getExitCode());
        Assert.assertTrue(String.join("\n", tableNotFoundResult.getOutput()).contains("not found"));
    }

    private DBPDataSourceContainer createRealMariaDBDataSource(String name) throws DBException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("mariadb");
        if (driver == null) {
            driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("mysql8");
        }
        Assert.assertNotNull("MariaDB or MySQL driver not found", driver);

        var connectionConfiguration = new DBPConnectionConfiguration();
        connectionConfiguration.setUrl("jdbc:mariadb://stage.dbeaver.infra:3306/lm");
        connectionConfiguration.setUserName("wp");
        connectionConfiguration.setUserPassword("wp");

        var dataSourceRegistry = DBWorkbench.getPlatform().getWorkspace().getActiveProject().getDataSourceRegistry();
        var dbpDataSourceContainer = dataSourceRegistry.createDataSource(driver, connectionConfiguration);
        dbpDataSourceContainer.setName(name);
        dataSourceRegistry.addDataSource(dbpDataSourceContainer);
        return dbpDataSourceContainer;
    }
}
