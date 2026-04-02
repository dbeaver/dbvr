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
package org.dbvr.cli.command;

import org.jkiss.dbeaver.model.cli.CLIAbstractSubcommand;
import org.jkiss.dbeaver.model.cli.CLIException;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "test11", description = "Command for test options help")

public class TestCommand extends CLIAbstractSubcommand {
    @CommandLine.Parameters(index = "0", description = "test", arity = "0..1")
    private int test;
    @CommandLine.Parameters(index = "1", description = "test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11test11", arity = "0..1")
    private Integer test11;

    @CommandLine.Parameters(index = "3", description = "test2211test2211test2211test2211test2211", arity = "1..2")
    private List<Integer> test2211;
    @CommandLine.Parameters(index = "2", description = "test2211test2211test2211", arity = "0..*")
    private Integer[] test222211;

    @Override
    public void run() throws CLIException {
            System.out.println("Test command executed");
    }
}
