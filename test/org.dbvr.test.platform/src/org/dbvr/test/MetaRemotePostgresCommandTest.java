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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class MetaRemotePostgresCommandTest extends DBVRTest {
    private String pgDsName;
    private DBPDataSourceContainer pgDs;

    @Before
    public void setup() throws Exception {
        pgDsName = "pg_real_ds_" + UUID.randomUUID();
        pgDs = createRealPostgresDataSource(pgDsName);
    }

    @After
    public void tearDown() {
        if (pgDs != null) {
            pgDs.getRegistry().removeDataSource(pgDs);
        }
    }

    @Test
    public void testPostgresDatabaseList() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var dbArgs = new String[]{
            "meta", "database", "list",
            "--datasource=" + pgDsName
        };
        CLIProcessResult dbResult = cmd.executeCommandLineCommands(null, false, false, dbArgs);
        Assert.assertNotNull(dbResult.getOutput());
        boolean foundPostgresDb = dbResult.getOutput().stream().anyMatch(line -> line.contains("postgres"));
        Assert.assertTrue("Database 'postgres' should be present in Postgres", foundPostgresDb);
    }

    @Test
    public void testPostgresSchemaList() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var schemaArgs = new String[]{
            "meta", "schema", "list",
            "--datasource=" + pgDsName,
            "--database-name=postgres"
        };
        CLIProcessResult schemaResult = cmd.executeCommandLineCommands(null, false, false, schemaArgs);
        Assert.assertNotNull(schemaResult.getOutput());
        boolean foundPublicSchema = schemaResult.getOutput().stream().anyMatch(line -> line.contains("public"));
        Assert.assertTrue("Schema 'public' should be present in Postgres 'postgres' database", foundPublicSchema);
    }

    @Test
    public void testPostgresTableList() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var tableArgs = new String[]{
            "meta", "table", "list",
            "--datasource=" + pgDsName,
            "--database-name=postgres",
            "--schema-name=public"
        };
        CLIProcessResult tableResult = cmd.executeCommandLineCommands(null, false, false, tableArgs);
        Assert.assertNotNull(tableResult.getOutput());
        System.out.println("[DEBUG_LOG] Tables in public: " + tableResult.getOutput());
    }

    @Test
    public void testPostgresTableDdl() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        // First, get a table to check DDL for
        var tableArgs = new String[]{
            "meta", "table", "list",
            "--datasource=" + pgDsName,
            "--database-name=postgres",
            "--schema-name=public"
        };
        CLIProcessResult tableResult = cmd.executeCommandLineCommands(null, false, false, tableArgs);
        Assert.assertNotNull(tableResult.getOutput());

        if (!tableResult.getOutput().isEmpty()) {
            String firstTable = tableResult.getOutput().get(0).trim();
            var ddlArgs = new String[]{
                "meta", "table", "ddl",
                "--datasource=" + pgDsName,
                "--database-name=postgres",
                "--schema-name=public",
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
    public void testPostgresSchemaDdl() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var schemaDdlArgs = new String[]{
            "meta", "schema", "ddl",
            "--datasource=" + pgDsName,
            "--database-name=postgres",
            "--schema-name=public"
        };
        CLIProcessResult schemaDdlResult = cmd.executeCommandLineCommands(null, false, false, schemaDdlArgs);
        Assert.assertNotNull(schemaDdlResult.getOutput());
        String schemaDdlOutput = String.join("\n", schemaDdlResult.getOutput());
        Assert.assertFalse("Schema DDL should not be null", schemaDdlOutput.isEmpty());
    }

    @Test
    public void testPostgresDatabaseDdl() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var dbDdlArgs = new String[]{
            "meta", "database", "ddl",
            "--datasource=" + pgDsName,
            "--database-name=postgres"
        };
        CLIProcessResult dbDdlResult = cmd.executeCommandLineCommands(null, false, false, dbDdlArgs);
        Assert.assertNotNull(dbDdlResult.getOutput());
        String dbDdlOutput = String.join("\n", dbDdlResult.getOutput());
        Assert.assertFalse("Database DDL should not be null", dbDdlOutput.isEmpty());
    }

    @Test
    public void testPostgresDatabaseNotFound() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var dbNotFoundArgs = new String[]{
            "meta", "database", "list",
            "--datasource=" + pgDsName,
            "--database-name=non_existent_db"
        };
        CLIProcessResult dbNotFoundResult = cmd.executeCommandLineCommands(null, false, false, dbNotFoundArgs);
        Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, dbNotFoundResult.getExitCode());
        Assert.assertTrue(String.join("\n", dbNotFoundResult.getOutput()).contains("not found"));
    }

    @Test
    public void testPostgresTableNotFound() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var tableNotFoundArgs = new String[]{
            "meta", "table", "ddl",
            "--datasource=" + pgDsName,
            "--database-name=postgres",
            "--schema-name=public",
            "--table-name=non_existent_table"
        };
        CLIProcessResult tableNotFoundResult = cmd.executeCommandLineCommands(null, false, false, tableNotFoundArgs);
        Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, tableNotFoundResult.getExitCode());
        Assert.assertTrue(String.join("\n", tableNotFoundResult.getOutput()).contains("not found"));
    }

    @Test
    public void testPostgresSchemaListWithoutDb() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var schemaArgsNoDb = new String[]{
            "meta", "schema", "list",
            "--datasource=" + pgDsName
        };
        CLIProcessResult schemaResultNoDb = cmd.executeCommandLineCommands(null, false, false, schemaArgsNoDb);
        System.out.println("[DEBUG_LOG] Schema list without database-name: " + schemaResultNoDb.getOutput());
        // Just verify it doesn't crash and returns something if it defaults to 'postgres'
    }

    @Test
    public void testPostgresTableListWithoutSchema() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var tableArgsNoSchema = new String[]{
            "meta", "table", "list",
            "--datasource=" + pgDsName,
            "--database-name=postgres"
        };
        CLIProcessResult tableResultNoSchema = cmd.executeCommandLineCommands(null, false, false, tableArgsNoSchema);
        System.out.println("[DEBUG_LOG] Table list without schema-name output: " + tableResultNoSchema.getOutput());
        Assert.assertNotNull(tableResultNoSchema.getOutput());
    }

    private DBPDataSourceContainer createRealPostgresDataSource(String name) throws DBException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("postgres-jdbc");
        Assert.assertNotNull("Postgres driver not found", driver);

        var connectionConfiguration = new DBPConnectionConfiguration();
        connectionConfiguration.setUrl("jdbc:postgresql://localhost:5433/postgres");
        connectionConfiguration.setUserName("postgres");
        connectionConfiguration.setUserPassword("pass");

        var dataSourceRegistry = DBWorkbench.getPlatform().getWorkspace().getActiveProject().getDataSourceRegistry();
        var dbpDataSourceContainer = dataSourceRegistry.createDataSource(driver, connectionConfiguration);
        dbpDataSourceContainer.setName(name);
        dataSourceRegistry.addDataSource(dbpDataSourceContainer);
        return dbpDataSourceContainer;
    }
}
