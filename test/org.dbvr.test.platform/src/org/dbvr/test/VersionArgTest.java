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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionArgTest extends DBVRTest {

    @Test
    public void testLowercaseVersionOptionPrintsVersion() throws Exception {
        String[] args = {"-v"};
        CLIProcessResult result = DBVRTestSuite.getApplication().executeCommandLine(args);

        String[] canonicalArgs = {"--version"};
        CLIProcessResult canonicalResult = DBVRTestSuite.getApplication().executeCommandLine(canonicalArgs);

        Assertions.assertEquals(CLIProcessResult.PostAction.SHUTDOWN, result.getPostAction());
        Assertions.assertEquals(canonicalResult.getOutput(), result.getOutput());
    }

    @Test
    public void testUnknownShortOptionIsRejected() throws Exception {
        String[] args = {"-x"};
        CLIProcessResult result = DBVRTestSuite.getApplication().executeCommandLine(args);

        Assertions.assertEquals(CLIProcessResult.PostAction.ERROR, result.getPostAction());
    }
}
