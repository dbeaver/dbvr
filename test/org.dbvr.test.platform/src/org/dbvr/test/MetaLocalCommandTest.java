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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class MetaLocalCommandTest extends DBVRTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testDatabaseList() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-db-list");
        try {
            var args = new String[] {
                "meta", "database", "list",
                "--datasource=" + h2Ds.getName()
            };
            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);

            List<String> output = result.getOutput();
            Assert.assertNotNull(output);
            Assert.assertTrue(
                output.contains("Database doesn't support databases/catalogs")
            );
            Assert.assertEquals("Should be OK exit code", CLIConstants.EXIT_CODE_OK, result.getExitCode());
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
    }

    @Test
    public void testSchemaList() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-schema-list");
        try {
            var args = new String[] {
                "meta", "schema", "list",
                "--datasource=" + h2Ds.getName()
            };
            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
            Assert.assertNotNull("Result should not be null", result);
            List<String> output = result.getOutput();
            Assert.assertNotNull(output);
            String fullOutput = String.join("\n", output);
            boolean hasPublic = fullOutput.lines().anyMatch(s -> s.trim().equalsIgnoreCase("PUBLIC"));
            Assert.assertTrue("H2 should have PUBLIC schema, but output was: " + fullOutput, hasPublic);
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
    }

    @Test
    public void testTableList() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-table-list");
        try {
            createTestTable(h2Ds);

            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            var listArgs = new String[] {
                "meta", "table", "list",
                "--datasource=" + h2Ds.getName(),
                "--schema-name=PUBLIC"
            };
            CLIProcessResult listResult = cmd.executeCommandLineCommands(null, false, false, listArgs);
            Assert.assertNotNull(listResult);
            String listOutput = listResult.getOutput() != null ? String.join("\n", listResult.getOutput()) : "";
            Assert.assertTrue("Output should contain TEST_TABLE", listOutput.contains("TEST_TABLE"));
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
    }

    @Test
    public void testTableDDL() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-table-ddl");
        try {
            createTestTable(h2Ds);

            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            var ddlArgs = new String[] {
                "meta", "table", "ddl",
                "--datasource=" + h2Ds.getName(),
                "--schema-name=PUBLIC",
                "--table-name=TEST_TABLE"
            };
            CLIProcessResult ddlResult = cmd.executeCommandLineCommands(null, false, false, ddlArgs);
            Assert.assertNotNull(ddlResult);
            String ddlOutput = ddlResult.getOutput() != null ? String.join("\n", ddlResult.getOutput()) : "";
            Assert.assertTrue(ddlOutput.toUpperCase().contains("CREATE TABLE"));
            Assert.assertTrue(ddlOutput.toUpperCase().contains("TEST_TABLE"));
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
    }

    private void createTestTable(DBPDataSourceContainer h2Ds) throws DBException {
        DBPDataSource dataSource = h2Ds.getDataSource();
        if (dataSource == null) {
            h2Ds.connect(new VoidProgressMonitor(), true, false);
            dataSource = h2Ds.getDataSource();
        }
        Assert.assertNotNull(dataSource);

        try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), dataSource, "Create test table")) {
            try (
                DBCStatement statement = session.prepareStatement(
                    DBCStatementType.QUERY,
                    "CREATE TABLE PUBLIC.TEST_TABLE (ID INT PRIMARY KEY, NAME VARCHAR(255))",
                    false,
                    false,
                    false
                )
            ) {
                statement.executeStatement();
            }
        }
    }

    @Test
    public void testSchemaDDL() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-schema-ddl");
        try {
            var cmd = DBVRTestSuite.getApplication().createCommandLine();

            var ddlArgs = new String[] {
                "meta", "schema", "ddl",
                "--datasource=" + h2Ds.getName(),
                "--schema-name=PUBLIC"
            };
            CLIProcessResult ddlResult = cmd.executeCommandLineCommands(null, false, false, ddlArgs);
            Assert.assertNotNull(ddlResult);
            String ddlOutput = ddlResult.getOutput() != null ? String.join("\n", ddlResult.getOutput()) : "";
            Assert.assertTrue(ddlOutput.contains("does not support DDL"));
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
    }

    @Test
    public void testSchemaListNegative() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-schema-neg");
        try {
            var args = new String[] {
                "meta", "schema", "list",
                "--datasource=" + h2Ds.getName(),
                "--database-name=lalala"
            };
            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
            Assert.assertNotNull(result);
            Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, result.getExitCode());
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
    }

    @Test
    public void testTableListNegative() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-table-neg");
        try {
            var args = new String[] {
                "meta", "table", "list",
                "--datasource=" + h2Ds.getName(),
                "--schema-name=lalala"
            };
            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
            Assert.assertNotNull(result);
            Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, result.getExitCode());
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
    }

    @Test
    public void testDatabaseDDL() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-db-ddl");
        try {
            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            var ddlArgs = new String[] {
                "meta", "database", "ddl",
                "--datasource=" + h2Ds.getName(),
                "--database-name=PUBLIC"
            };
            CLIProcessResult ddlResult = cmd.executeCommandLineCommands(null, false, false, ddlArgs);
            Assert.assertNotNull(ddlResult);
            String ddlOutput = ddlResult.getOutput() != null ? String.join("\n", ddlResult.getOutput()) : "";
            Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, ddlResult.getExitCode());
            Assert.assertTrue(
                ddlOutput.contains("Database DDL is not supported for this database type")
            );
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
    }

    @Test
    public void testDatabaseDDLNoDatabaseName() throws Exception {
        DBPDataSourceContainer h2Ds = createFakeDataSource("h2-test-db-ddl-noname");
        try {
            var cmd = DBVRTestSuite.getApplication().createCommandLine();
            var ddlArgs = new String[] {
                "meta", "database", "ddl",
                "--datasource=" + h2Ds.getName()
            };
            CLIProcessResult ddlResult = cmd.executeCommandLineCommands(null, false, false, ddlArgs);
            Assert.assertNotNull(ddlResult);
            String ddlOutput = ddlResult.getOutput() != null ? String.join("\n", ddlResult.getOutput()) : "";
            Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, ddlResult.getExitCode());
            Assert.assertTrue(
                "Output should explain that database DDL is unsupported, but was: " + ddlOutput,
                ddlOutput.contains("Database DDL is not supported for this database type")
            );
        } finally {
            h2Ds.getRegistry().removeDataSource(h2Ds);
        }
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
