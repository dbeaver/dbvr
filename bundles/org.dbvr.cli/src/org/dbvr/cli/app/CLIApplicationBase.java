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
package org.dbvr.cli.app;


import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.command.AbstractTopLevelCommand;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.BasePlatformImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.runtime.ui.console.ConsoleUserInterface;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.net.URL;
import java.nio.file.Path;

/**
 * Base CLI application
 */
public class CLIApplicationBase extends BaseApplicationImpl {
    private static final Log log = Log.getLog(CLIApplicationBase.class);
    protected final Path WORKSPACE_DIR_CURRENT;
    private boolean started = false;
    private static final String[] DEFAULT_ARGS = new String[] {AbstractTopLevelCommand.HELP_OPTION};

    private final DBPPreferenceStore preferenceStore = new SimplePreferenceStore() {
        @Override
        public void save() {

        }
    };

    protected CLIApplicationBase() {

        // Explicitly set UTF-8 as default file encoding
        // In some places Eclipse reads this property directly.
        //System.setProperty(StandardConstants.ENV_FILE_ENCODING, GeneralUtils.UTF8_ENCODING);

        // Detect default workspace location
        // Since 6.1.3 it is different for different OSes
        // Windows: %AppData%/DBeaverData
        // MacOS: ~/Library/DBeaverData
        // Linux: $XDG_DATA_HOME/DBeaverData
        String workingDirectory = RuntimeUtils.getWorkingDirectory(BasePlatformImpl.DBEAVER_DATA_DIR);

        // Workspace dir
        WORKSPACE_DIR_CURRENT = Path.of(workingDirectory, DEFAULT_WORKSPACE_FOLDER);
        Log.setLogHandler(new VoidLogHandler());
    }

    @NotNull
    @Override
    public Object start(IApplicationContext context) throws Exception {
        // Register core components
        initializeApplicationServices();

        Location instanceLoc = Platform.getInstanceLocation();
        try {
            if (!instanceLoc.isSet()) { // always false?
                URL wsLocationURL = WORKSPACE_DIR_CURRENT.toUri().toURL();
                instanceLoc.set(wsLocationURL, false);
            }
        } catch (Exception e) {
            log.error("Error setting workspace location to " + WORKSPACE_DIR_CURRENT, e);
            throw e;
        }
        DBWorkbench.getPlatform();
        configureApplication();
        started = true;
        try {
            CLIProcessResult processResult = executeCommandLine(Platform.getApplicationArgs());
            var out = processResult.getPostAction() == CLIProcessResult.PostAction.ERROR
                ? System.err
                : System.out;
            if (!CommonUtils.isEmpty(processResult.getOutput())) {
                for (String res : processResult.getOutput()) {
                    out.println(res);
                }
            }
        } catch (DBException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return EXIT_OK;
    }

    public CLIProcessResult executeCommandLine(@NotNull String[] args) throws DBException {
        CLICommandLine commandLine = createCommandLine();
        String[] appArgs = commandLine.preprocessCommandLine(args);
        if (ArrayUtils.isEmpty(appArgs)) {
            appArgs = DEFAULT_ARGS;
        }
        try {
            return commandLine.executeCommandLineCommands(
                null,
                false,
                false,
                appArgs
            );
        } catch (Exception e) {
            throw new DBException("Error executing command line: " + e.getMessage(), e);
        }
    }

    @NotNull
    public CLICommandLine createCommandLine() {
        return new CLICommandLine();
    }

    protected void configureApplication() {

    }

    @Override
    public void stop() {
        super.stop();
    }

    @Nullable
    @Override
    public String getDefaultProjectName() {
        return DBConstants.DEFAULT_PROJECT_NAME;
    }

    @Nullable
    @Override
    public Path getDefaultWorkingFolder() {
        return WORKSPACE_DIR_CURRENT;
    }

    @NotNull
    @Override
    public Class<? extends DBPPlatform> getPlatformClass() {
        return CLIPlatform.class;
    }

    @Override
    public Class<? extends DBPPlatformUI> getPlatformUIClass() {
        return ConsoleUserInterface.class;
    }

    @Override
    public boolean isEnvironmentVariablesAccessible() {
        return true;
    }

    @Override
    public boolean isHeadlessMode() {
        return true;
    }

    @NotNull
    public DBPPreferenceStore getPreferenceStore() {
        return CLIActivator.getInstance().getPreferenceStore();
    }

    @NotNull
    public CLIWorkspace createWorkspace(@NotNull CLIPlatform cliPlatform) {
        return new CLIWorkspace(cliPlatform, WORKSPACE_DIR_CURRENT);
    }

    public synchronized boolean isStarted() {
        return started;
    }
}
