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
import picocli.CommandLine;

import static org.dbvr.cli.app.ce.command.TestCommand.TEST_COMMAND_NAME;

@CommandLine.Command(
    name = TEST_COMMAND_NAME,
    description = "A test command for testing purposes"
)
public class TestCommand extends CLIAbstractSubcommand {
    public static final String TEST_COMMAND_NAME = "test-command";
    public static final String TEST_PARAM_NAME = "--test-parameter-value";

    @CommandLine.Option(names = {TEST_PARAM_NAME}, description = "A test parameter")
    private String param;

    @Override
    public void run() {
        context().addResult("Test command executed with parameter: " + param);
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
    }
}
