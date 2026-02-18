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
package org.dbvr.cli.command.datasource;

import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import picocli.CommandLine;

@CommandLine.Command(name = "view", description = "View datasource details")
public class ViewDataSource extends AbstractDataSourceCommand {

    @CommandLine.Parameters(index = "0", description = "Datasource ID or name",  arity = "1")
    private String id;

    @Override
    public void run() throws CLIException {
        super.run();
        var project = getProject();
        var dataSourceContainer = CLIUtils.findDataSource(project, id);
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        String data = serializeDataSourceToJson(project, dataSourceContainer.getId());
        context().addResult(data.trim());
    }
}
