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
package org.dbvr.cli.app;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.registry.BasePlatformImpl;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Base CLI platform
 */
public class CLIPlatform extends BasePlatformImpl {

    public static final String PLUGIN_ID = "org.dbvr.cli"; //$NON-NLS-1$

    private static final Log log = Log.getLog(CLIPlatform.class);

    static CLIPlatform instance;

    private static volatile boolean isClosing = false;

    private CLIWorkspace workspace;

    private DefaultCertificateStorage defaultCertificateStorage;

    CLIPlatform() {
    }

    protected void initialize() throws DBException {
        instance = this;
        long startTime = System.currentTimeMillis();
        log.trace("Initialize CLI Platform...");
        try {
            Path installPath = RuntimeUtils.getLocalPathFromURL(Platform.getInstallLocation().getURL());

            this.defaultCertificateStorage = new DefaultCertificateStorage(
                this,
                installPath.resolve(DBConstants.CERTIFICATE_STORAGE_FOLDER));
        } catch (IOException e) {
            log.debug(e);
        }
        // Register properties adapter
        try {
            getApplication().beforeWorkspaceInitialization();
            this.workspace = getApplication().createWorkspace(this);
            this.workspace.initializeProjects();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize CLI workspace", e);
        }

        QMUtils.initPlatform(false);

        super.initialize();
        log.trace("CLI Platform initialized (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public synchronized void dispose() {
        log.trace("Shutdown CLI...");
        isClosing = true;
        super.dispose();
        workspace.dispose();
        QMUtils.disposePlatform();

        CLIPlatform.instance = null;
    }

    @NotNull
    @Override
    public CLIWorkspace getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public CLIApplicationBase getApplication() {
        return (CLIApplicationBase) BaseApplicationImpl.getInstance();
    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return getApplication().getPreferenceStore();
    }

    @NotNull
    @Override
    public DBACertificateStorage getCertificateStorage() {
        return defaultCertificateStorage;
    }

    @Override
    protected Plugin getProductPlugin() {
        return CLIActivator.getInstance();
    }

    @Override
    public boolean isShuttingDown() {
        return isClosing;
    }

}
