package org.eclipse.jdt.ls.standalone;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Minimal workspace service with no functionality.
 */
public class SimpleWorkspaceService implements WorkspaceService {

    @Override
    public CompletableFuture<Object> executeCommand(org.eclipse.lsp4j.ExecuteCommandParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void didChangeConfiguration(org.eclipse.lsp4j.DidChangeConfigurationParams params) {
    }

    @Override
    public void didChangeWatchedFiles(org.eclipse.lsp4j.DidChangeWatchedFilesParams params) {
    }

    @Override
    public void didChangeWorkspaceFolders(org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams params) {
    }
}
