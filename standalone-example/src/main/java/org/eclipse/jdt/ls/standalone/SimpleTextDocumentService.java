package org.eclipse.jdt.ls.standalone;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.services.TextDocumentService;

/**
 * Minimal text document service providing empty implementations.
 */
public class SimpleTextDocumentService implements TextDocumentService {

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<org.eclipse.lsp4j.jsonrpc.messages.Either<java.util.List<CompletionItem>, CompletionList>> completion(org.eclipse.lsp4j.CompletionParams params) {
        return CompletableFuture.completedFuture(org.eclipse.lsp4j.jsonrpc.messages.Either.forRight(new CompletionList()));
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return CompletableFuture.completedFuture(unresolved);
    }
}
