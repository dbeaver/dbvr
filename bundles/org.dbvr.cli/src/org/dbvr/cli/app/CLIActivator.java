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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.command.AbstractTopLevelCommand;
import org.jkiss.utils.ArrayUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.hooks.bundle.EventHook;

/**
 * The activator class controls the plug-in life cycle
 */
public class CLIActivator extends Plugin {

    private static final Log log = Log.getLog(CLIActivator.class);

    private static CLIActivator instance;

    public static CLIActivator getInstance() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        instance = this;
        checkTraceLogging(context);

        super.start(context);
    }

    private static void checkTraceLogging(BundleContext context) {
        if (ArrayUtils.contains(Platform.getApplicationArgs(), AbstractTopLevelCommand.TRACE_LOGS_OPTION) && !Log.isQuietMode()) {
            Log.enableTraceLogs(true);
            context.registerService(
                EventHook.class,
                (event, contexts) -> {
                    String message = null;
                    Bundle bundle = event.getBundle();
                    if (event.getType() == BundleEvent.STARTED) {
                        if (bundle.getState() == Bundle.ACTIVE) {
                            message = "Start bundle " + bundle.getSymbolicName();
                        }
                    }
                    if (message != null) {
                        log.trace(message);
                    }
                },
                null);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (CLIPlatform.instance != null) {
            CLIPlatform.instance.dispose();
        }
        super.stop(context);
        instance = null;
    }
}
