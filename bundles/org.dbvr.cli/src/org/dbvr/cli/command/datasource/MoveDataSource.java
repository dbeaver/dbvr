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

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.*;
import picocli.CommandLine;

@CommandLine.Command(name = "move", description = "Move datasource to another project")
public class MoveDataSource extends CLIAbstractSubcommand {

    @CommandLine.Parameters(index = "0", description = "Datasource id or name", arity = "1")
    private String datasourceIdOrName;

    @CommandLine.Option(
        names = {"-from", "--from-project"},
        description = "Source project name or ID, if not specified, use current project"
    )
    private String fromProjectIdOrName;

    @CommandLine.Option(names = {"-to", "--to-project"}, required = true, description = "Target project name or ID")
    private String targetProjectIdOrName;

    @Override
    public void run() throws CLIException {
        DBPProject sourceProject = CLIUtils.findProject(fromProjectIdOrName, context());
        DBPProject targetProject = CLIUtils.findProject(targetProjectIdOrName, context());

        if (sourceProject.getId().equals(targetProject.getId())) {
            throw new CLIException(
                "Source and target projects are the same: '" + sourceProject.getName() + "'",
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }

        DBPDataSourceContainer sourceContainer = CLIUtils.findDataSource(sourceProject, datasourceIdOrName);
        DBPDataSourceRegistry targetRegistry = targetProject.getDataSourceRegistry();

        DBPDataSourceContainer newContainer = sourceContainer.createCopy(targetRegistry);
        try {
            targetRegistry.addDataSource(newContainer);
            sourceProject.getDataSourceRegistry().removeDataSource(sourceContainer);
            targetRegistry.checkForErrors();
        } catch (Exception e) {
            throw new CLIException(
                "Error moving datasource from project '" + sourceProject.getName()
                    + "' to project '" + targetProject.getName() + "': " + e.getMessage(),
                e,
                CLIConstants.EXIT_CODE_ERROR
            );
        }

        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(
            "Datasource " + newContainer.getName() + " [k" + newContainer.getId() + "] moved to project '" + targetProject.getName() + "'"
        );
    }
}
