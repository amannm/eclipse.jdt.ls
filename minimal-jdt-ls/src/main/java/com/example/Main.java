package com.example;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;

public class Main {
    public static void main(String[] args) throws Exception {
        // Placeholder minimal server that does nothing yet
        LanguageServer server = new DummyJdtLanguageServer();
        LSPLauncher.createServerLauncher(server, System.in, System.out).startListening();
    }
}
