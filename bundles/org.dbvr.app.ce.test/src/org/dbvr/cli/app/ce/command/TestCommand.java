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
package org.dbvr.cli.app.ce.command;

import org.jkiss.dbeaver.model.cli.CLIAbstractSubcommand;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.help.CLIExample;
import picocli.CommandLine;

import java.util.List;

import static org.dbvr.cli.app.ce.command.TestCommand.TEST_COMMAND_NAME;

@CommandLine.Command(
    name = TEST_COMMAND_NAME,
    description = TestCommand.DESCRIPTION
)
@CLIExample(examples = {TestCommand.EXAMPLE_COMMAND1, TestCommand.EXAMPLE_COMMAND2})
public class TestCommand extends CLIAbstractSubcommand {
    public static final String DESCRIPTION = "A test command for testing purposes";
    public static final String TEST_COMMAND_NAME = "test-command";
    public static final String TEST_PARAM_NAME_NOT_REQ = "--test-parameter-value";
    public static final String TEST_INT_ARRAY = "--test-int-array";
    public static final String TEST_INT_LIST = "--test-int-list";
    public static final String TEST_STRING_LIST = "--test-string-list";
    public static final String TEST_DOUBLE = "--test-double";
    public static final String TEST_REQ_FIRST = "--test-req-first";
    public static final String TEST_REQ_IN_MIDDLE = "--test-req-middle";
    public static final String TEST_ONLY_SHORT_NAME = "-w";


    public static final String EXAMPLE_COMMAND1 = TEST_COMMAND_NAME + " " + TEST_DOUBLE + "12.00";
    public static final String EXAMPLE_COMMAND2 = TEST_COMMAND_NAME + " " + TEST_PARAM_NAME_NOT_REQ + "ASDASD";


    @CommandLine.Option(names = {TEST_REQ_FIRST}, required = true, description = "Req opt first position")
    private String reqString;

    @CommandLine.Option(names = {TEST_PARAM_NAME_NOT_REQ}, description = "A test parameter")
    private String param;

    @CommandLine.Option(names = {TEST_INT_ARRAY}, description = "An int array")
    private int[] intArray;

    @CommandLine.Option(names = {TEST_STRING_LIST}, description = "A string list")
    private List<String> stringList;

    @CommandLine.Option(names = {TEST_INT_LIST}, description = "An int array")
    private List<Integer> integerList;


    @CommandLine.Option(names = {TEST_DOUBLE}, description = "A double")
    private Double doubleValue;

    @CommandLine.Option(names = {TEST_ONLY_SHORT_NAME}, description = "A short name command")
    private boolean commandWithoutLongName;


    //must be sorted to the top in help
    @CommandLine.Option(names = {TEST_REQ_IN_MIDDLE}, required = true, description = "Req param in the middle")
    private String reqStringMiddle;

    @CommandLine.Parameters(index = "0", description = "Positional parameter 1", arity = "0..1")
    private String pos1;
    // arity > 0 means req parameter, but it is not sorted because tied to position
    @CommandLine.Parameters(index = "1", description = "Positional parameter 2", arity = "1")
    private String pos2;
    @CommandLine.Parameters(index = "2", description = "Positional parameter 3", arity = "0..1")
    private String pos3;


    @Override
    public void run() {
        context().addResult("Test command executed with parameter: " + param);
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
    }
}
