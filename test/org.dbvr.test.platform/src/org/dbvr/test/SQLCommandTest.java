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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class SQLCommandTest extends DBVRTest {
    private static final String TEST_SCHEMA = "TEST_SCHEMA";

    private DBPDataSourceContainer dataSource;

    @BeforeEach
    public void setUp() throws DBException {
        dataSource = createDataSource();
    }

    @AfterEach
    public void tearDown() throws DBException {
        if (dataSource == null) {
            return;
        }
        if (dataSource.isConnected()) {
            dataSource.disconnect(new VoidProgressMonitor());
        }
        dataSource.getRegistry().removeDataSource(dataSource);
    }

    @Test
    public void testDefaultSchema() throws Exception {
        assertDefaultSchema("--datasource=" + dataSource.getName());
    }

    @Test
    public void testDefaultSchemaWithDataSourceSpecification() throws Exception {
        String specification = "driver=" + dataSource.getDriver().getId()
            + "|url=" + dataSource.getConnectionConfiguration().getUrl();
        assertDefaultSchema("--datasource-specification=" + specification);
    }

    private void assertDefaultSchema(@NotNull String dataSourceArgument) throws Exception {
        String[] args = {
            "sql",
            "select SCHEMA()",
            dataSourceArgument,
            "--default-schema=" + TEST_SCHEMA
        };
        var commandLine = DBVRTestSuite.getApplication().createCommandLine();
        CLIProcessResult result = commandLine.executeCommandLineCommands(null, false, false, args);

        Assertions.assertEquals(
            CLIConstants.EXIT_CODE_OK,
            result.getExitCode(),
            "Command output: " + result.getOutput()
        );
        Assertions.assertNotNull(result.getOutput());
        Assertions.assertTrue(result.getOutput().stream().anyMatch(line -> line.contains(TEST_SCHEMA)));
    }

    @NotNull
    private DBPDataSourceContainer createDataSource() throws DBException {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("h2_embedded_v2");
        if (driver == null) {
            driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("h2_embedded");
        }
        Assertions.assertNotNull(driver);

        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";INIT=CREATE SCHEMA IF NOT EXISTS " + TEST_SCHEMA
        );
        var registry = DBWorkbench.getPlatform().getWorkspace().getActiveProject().getDataSourceRegistry();
        DBPDataSourceContainer dataSource = registry.createDataSource(driver, configuration);
        dataSource.setName("h2-test-default-schema-" + UUID.randomUUID());
        registry.addDataSource(dataSource);
        return dataSource;
    }
}
