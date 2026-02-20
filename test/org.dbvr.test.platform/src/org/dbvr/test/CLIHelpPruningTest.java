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

import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.CLIProcessResult.PostAction;
import org.junit.Assert;
import org.junit.Test;

public class CLIHelpPruningTest extends DBVRTest {

    @Test
    public void testHelpDoesNotContainRedundantCommands() throws Exception {
        String[] args = {"--help"};
        var cmd = DBVRTestSuite.getApplication().createCommandLine();

        CLIProcessResult result = cmd.executeCommandLineCommands(null, false, false, args);
        Assert.assertNotNull(result.getOutput());
        String helpOutput = String.join("\n", result.getOutput());

        Assert.assertEquals("Execution should be successful (PostAction.SHUTDOWN)",
            PostAction.SHUTDOWN, result.getPostAction());

        Assert.assertFalse("Help should not contain 'license' command", helpOutput.contains("license"));
        Assert.assertFalse("Help should not contain 'token' command", helpOutput.contains("token"));
        Assert.assertFalse("Help should not contain '-dump' option", helpOutput.contains("-dump"));
    }
}
