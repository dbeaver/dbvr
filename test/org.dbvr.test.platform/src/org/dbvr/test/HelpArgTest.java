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

import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.junit.Assert;
import org.junit.Test;

public class HelpArgTest extends DBVRTest {

    @Test
    public void testHelpArg() throws Exception {
        String[] args = {"--help"};
        var cmd = DBVRTestSuite.getApplication().createCommandLine();

        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
    }

    @Test
    public void testEmptyArgs() throws Exception {
        String[] args = {"--help"};
        CLIProcessResult resultWithArg = DBVRTestSuite.getApplication().executeCommandLine(args);
        Assert.assertNotNull(resultWithArg.getOutput());

        String[] noArgs = {};
        CLIProcessResult resultNoArg = DBVRTestSuite.getApplication().executeCommandLine(noArgs);
        Assert.assertEquals(resultWithArg.getOutput(), resultNoArg.getOutput());
    }
}
