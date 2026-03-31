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
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class MetaLocalSQLiteCommandTest extends DBVRTest {
    private static final String SQLITE_CHINOOK_URL = "jdbc:sqlite:/Users/labanovichttps/Library/DBeaverData/workspace6/.metadata/sample-database-sqlite-1/Chinook.db";
    private String dsName;
    private DBPDataSourceContainer sqliteDs;

    @org.junit.Before
    public void setup() throws Exception {
        dsName = "sqlite_chinook_" + UUID.randomUUID();
        sqliteDs = createSQLiteDataSource(dsName, SQLITE_CHINOOK_URL);
    }

    @org.junit.After
    public void tearDown() {
        if (sqliteDs != null) {
            sqliteDs.getRegistry().removeDataSource(sqliteDs);
        }
    }

    @Test
    public void testSQLiteTableList() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var tableArgs = new String[]{
            "meta", "table", "list",
            "--datasource=" + dsName
        };
        CLIProcessResult tableResult = cmd.executeCommandLineCommands(null, false, false, tableArgs);
        Assert.assertNotNull(tableResult.getOutput());
        List<String> tables = tableResult.getOutput();
        System.out.println("[DEBUG_LOG] SQLite tables: " + tables);

        // Chinook database typically has tables like 'Album', 'Artist', 'Track'
        boolean foundAlbum = tables.stream().anyMatch(line -> line.contains("Album"));
        Assert.assertTrue("Table 'Album' should be present in Chinook SQLite", foundAlbum);
    }

    @Test
    public void testSQLiteTableDdl() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var ddlArgs = new String[]{
            "meta", "table", "ddl",
            "--datasource=" + dsName,
            "--table-name=Album"
        };
        CLIProcessResult ddlResult = cmd.executeCommandLineCommands(null, false, false, ddlArgs);
        Assert.assertNotNull(ddlResult.getOutput());
        String ddlOutput = String.join("\n", ddlResult.getOutput());
        System.out.println("[DEBUG_LOG] DDL for Album:\n" + ddlOutput);
        Assert.assertTrue("DDL should contain 'CREATE TABLE'", ddlOutput.toUpperCase().contains("CREATE TABLE"));
        Assert.assertTrue("DDL should contain 'Album'", ddlOutput.contains("Album"));
    }

    @Test
    public void testSQLiteDatabaseList() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var dbArgs = new String[]{
            "meta", "database", "list",
            "--datasource=" + dsName
        };
        CLIProcessResult dbResult = cmd.executeCommandLineCommands(null, false, false, dbArgs);
        Assert.assertNotNull(dbResult.getOutput());
        String dbOutput = String.join("\n", dbResult.getOutput());
        System.out.println("[DEBUG_LOG] Database list output: " + dbOutput);
        // It might return "Database doesn't support databases/catalogs" like H2
        Assert.assertTrue("Should mention lack of database support or be empty",
            dbOutput.contains("doesn't support") || dbOutput.isEmpty());
    }

    @Test
    public void testSQLiteTableNotFound() throws Exception {
        var cmd = DBVRTestSuite.getApplication().createCommandLine();
        var tableNotFoundArgs = new String[]{
            "meta", "table", "ddl",
            "--datasource=" + dsName,
            "--table-name=non_existent_table"
        };
        CLIProcessResult tableNotFoundResult = cmd.executeCommandLineCommands(null, false, false, tableNotFoundArgs);
        Assert.assertEquals(CLIConstants.EXIT_CODE_ERROR, tableNotFoundResult.getExitCode());
        Assert.assertTrue(String.join("\n", tableNotFoundResult.getOutput()).contains("not found"));
    }

    @NotNull
    private DBPDataSourceContainer createSQLiteDataSource(String name, String url) throws DBException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("sqlite_jdbc");
        Assert.assertNotNull("SQLite driver not found", driver);

        var connectionConfiguration = new DBPConnectionConfiguration();
        connectionConfiguration.setUrl(url);

        var dataSourceRegistry = DBWorkbench.getPlatform().getWorkspace().getActiveProject().getDataSourceRegistry();
        var dbpDataSourceContainer = dataSourceRegistry.createDataSource(driver, connectionConfiguration);
        dbpDataSourceContainer.setName(name);
        dataSourceRegistry.addDataSource(dbpDataSourceContainer);
        return dbpDataSourceContainer;
    }
}
