package org.eclipse.jdt.ls.standalone;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

/**
 * Entry point that starts the minimal language server using stdio.
 */
public class MinimalLanguageServer {
    public static void main(String[] args) throws Exception {
        SimpleLanguageServer server = new SimpleLanguageServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
        launcher.startListening();
    }
}
