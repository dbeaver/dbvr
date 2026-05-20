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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.utils.CommonUtils;
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


    @Test
    public void testHelpFormatAndOptionsOrder() throws Exception {
        String[] args = {TestCommand.TEST_COMMAND_NAME, "--help"};
        CLIProcessResult result = DBVRTestSuite.getApplication().executeCommandLine(args);
        Assert.assertNotNull(result.getOutput());
        Assert.assertEquals(1, result.getOutput().size());
        String help = result.getOutput().getFirst();
        Assert.assertTrue(CommonUtils.isNotEmpty(help));

        String[] commandDescAndOptions = help.split(TestCommand.DESCRIPTION);
        Assert.assertEquals(2, commandDescAndOptions.length);
        String allArgsHelp = commandDescAndOptions[1].trim();

        Assert.assertTrue(CommonUtils.isNotEmpty(allArgsHelp));
        //because it global option and must be only in top level help
        Assert.assertFalse(allArgsHelp.contains("--debug-logs"));
        String[] allArgsByLine = allArgsHelp.split("\n");


        String position1 = allArgsByLine[0].trim();
        String position2 = allArgsByLine[1].trim();
        String position3 = allArgsByLine[2].trim();

        Assert.assertFalse(position1.contains("required"));
        //required position param not sorted
        Assert.assertTrue(position2.contains("required"));
        Assert.assertFalse(position3.contains("required"));


        String reqOption1 = allArgsByLine[4].trim();
        String reqOption2 = allArgsByLine[6].trim(); // cause line 6 - it help for command from line 4

        Assert.assertTrue(reqOption1.startsWith(TestCommand.TEST_REQ_FIRST));
        Assert.assertTrue(reqOption1.contains("required"));
        Assert.assertTrue(reqOption2.startsWith(TestCommand.TEST_REQ_IN_MIDDLE));
        Assert.assertTrue(reqOption2.contains("required"));
        Assert.assertFalse(findOptionLine(allArgsByLine, TestCommand.TEST_PARAM_NAME_NOT_REQ).contains("required"));


        Assert.assertTrue(findOptionLine(allArgsByLine, TestCommand.TEST_INT_ARRAY).contains("(integer[])"));
        Assert.assertTrue(findOptionLine(allArgsByLine, TestCommand.TEST_INT_LIST).contains("(integer[])"));
        Assert.assertTrue(findOptionLine(allArgsByLine, TestCommand.TEST_DOUBLE).contains("(double)"));
        Assert.assertTrue(findOptionLine(allArgsByLine, TestCommand.TEST_STRING_LIST).contains("(string[])"));
        Assert.assertTrue(findOptionLine(allArgsByLine, TestCommand.TEST_ONLY_SHORT_NAME).contains("(boolean)"));


        String example1 = allArgsByLine[allArgsByLine.length - 2].trim();
        String example2 = allArgsByLine[allArgsByLine.length - 1].trim();
        Assert.assertEquals("- dbvr " + TestCommand.EXAMPLE_COMMAND1, example1);
        Assert.assertEquals("- dbvr " + TestCommand.EXAMPLE_COMMAND2, example2);
    }


    @NotNull
    private String findOptionLine(@NotNull String[] args, @NotNull String optionName) {
        for (String arg : args) {
            if(arg.contains(optionName)) {
                return arg;
            }
        }

        throw new IllegalArgumentException("No such option: " + optionName);
    }
}
