package org.eclipse.jdt.ls.standalone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Minimal text document service providing empty implementations.
 */
public class SimpleTextDocumentService implements TextDocumentService, LanguageClientAware {

    private final Map<String, String> documents = new ConcurrentHashMap<>();
    private LanguageClient client;

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        documents.put(params.getTextDocument().getUri(), params.getTextDocument().getText());
        publishDiagnostics(params.getTextDocument().getUri());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        if (!params.getContentChanges().isEmpty()) {
            String text = params.getContentChanges().get(params.getContentChanges().size() - 1).getText();
            documents.put(params.getTextDocument().getUri(), text);
            publishDiagnostics(params.getTextDocument().getUri());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        documents.remove(params.getTextDocument().getUri());
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), Collections.emptyList()));
        }
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        publishDiagnostics(params.getTextDocument().getUri());
    }

    @Override
    public CompletableFuture<org.eclipse.lsp4j.jsonrpc.messages.Either<java.util.List<CompletionItem>, CompletionList>> completion(org.eclipse.lsp4j.CompletionParams params) {
        String text = documents.get(params.getTextDocument().getUri());
        if (text == null) {
            return CompletableFuture.completedFuture(org.eclipse.lsp4j.jsonrpc.messages.Either.forRight(new CompletionList()));
        }

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(text.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<CompletionItem> items = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                CompletionItem item = new CompletionItem(node.getName().getIdentifier());
                item.setKind(CompletionItemKind.Method);
                items.add(item);
                return super.visit(node);
            }

            @Override
            public boolean visit(TypeDeclaration node) {
                CompletionItem item = new CompletionItem(node.getName().getIdentifier());
                item.setKind(CompletionItemKind.Class);
                items.add(item);
                return super.visit(node);
            }

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                CompletionItem item = new CompletionItem(node.getName().getIdentifier());
                item.setKind(CompletionItemKind.Variable);
                items.add(item);
                return super.visit(node);
            }
        });

        return CompletableFuture.completedFuture(org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(items));
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return CompletableFuture.completedFuture(unresolved);
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    private void publishDiagnostics(String uri) {
        if (client == null) {
            return;
        }
        String text = documents.get(uri);
        if (text == null) {
            return;
        }

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(text.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<Diagnostic> diagnostics = new ArrayList<>();
        for (IProblem problem : cu.getProblems()) {
            Diagnostic d = new Diagnostic();
            d.setMessage(problem.getMessage());
            d.setSeverity(problem.isError() ? DiagnosticSeverity.Error : DiagnosticSeverity.Warning);
            int startLine = cu.getLineNumber(problem.getSourceStart()) - 1;
            int startCol = cu.getColumnNumber(problem.getSourceStart()) - 1;
            int endLine = cu.getLineNumber(problem.getSourceEnd()) - 1;
            int endCol = cu.getColumnNumber(problem.getSourceEnd()) - 1;
            d.setRange(new Range(new Position(startLine, startCol), new Position(endLine, endCol)));
            diagnostics.add(d);
        }

        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
    }
}
