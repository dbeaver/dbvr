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


import org.dbvr.cli.app.ce.command.TestCommand;
import org.dbvr.cli.app.ce.command.TestTransformer;
import org.jkiss.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;


public class InjectionTest extends DBVRTest {
    @Test
    public void testThatParamInjected() throws Exception {
        var args = new String[] {
            TestCommand.TEST_COMMAND_NAME, "--help"
        };

        var result = DBVRTestSuite.getApplication().executeCommandLine(args);
        Assert.assertFalse(CommonUtils.isEmpty(result.getOutput()));
        Assert.assertTrue(result.getOutput().getFirst().contains(TestTransformer.RANDOM_PARAM_NAME));
    }
}
