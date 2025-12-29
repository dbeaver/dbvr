/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class ConnectionManagementTest extends DBVRTest {

    @Test
    public void testCreate() throws Exception {
        String uniqName = "test_create" + UUID.randomUUID();
        var args = new String[] {
            "connections", "--create",
            "--connection-spec", "driver=postgres-jdbc|database=cloudbeaver|host=localhost|name=" + uniqName,
            "-u", "postgres",
            "-p", "postgres"
        };

        var cmd = DBVRTestSuite.getApplication().createCommandLine();

        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        Assert.assertTrue(result.getOutput().get(0).contains(uniqName));

        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        Assert.assertNotNull(project);
        DBPDataSourceContainer ds = project.getDataSourceRegistry().findDataSourceByName(uniqName);
        Assert.assertNotNull(ds);
        project.getDataSourceRegistry().removeDataSource(ds);
    }

}
