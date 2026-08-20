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

import org.eclipse.core.runtime.IStatus;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.runtime.ui.console.ConsoleUserInterface;

/**
 * Console UI used by dbvr so errors are not mixed with command output.
 */
public class CLIUserInterface extends ConsoleUserInterface {

    @Override
    protected void initialize() {
        super.initialize();
    }

    @Override
    @NotNull
    public UserResponse showError(@NotNull String title, @Nullable String message, @NotNull IStatus status) {
        System.err.println(title + (message == null ? "" : ": " + message));
        if (status.getMessage() != null) {
            System.err.println(status.getMessage());
        }
        if (status.getException() != null) {
            status.getException().printStackTrace(System.err);
        }
        return UserResponse.OK;
    }

    @Override
    @NotNull
    public UserResponse showError(@NotNull String title, @Nullable String message, @NotNull Throwable e) {
        System.err.println(title + (message == null ? "" : ": " + message));
        e.printStackTrace(System.err);
        return UserResponse.OK;
    }

    @Override
    @NotNull
    public UserResponse showError(@NotNull String title, @Nullable String message) {
        System.err.println(title + (message == null ? "" : ": " + message));
        return UserResponse.OK;
    }

    @Override
    public void showMessageBox(@NotNull String title, String message, boolean error) {
        if (error) {
            System.err.println(title + (message == null ? "" : ": " + message));
        } else {
            super.showMessageBox(title, message, false);
        }
    }
}
