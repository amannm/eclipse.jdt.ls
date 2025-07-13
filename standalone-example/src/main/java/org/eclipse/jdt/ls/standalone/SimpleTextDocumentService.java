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
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.NodeFinder;

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
    public CompletableFuture<Hover> hover(HoverParams params) {
        String text = documents.get(params.getTextDocument().getUri());
        if (text == null) {
            return CompletableFuture.completedFuture(null);
        }
        int offset = offsetAt(text, params.getPosition());
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(text.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        ASTNode node = NodeFinder.perform(cu, offset, 0);
        String msg = null;
        if (node instanceof MethodDeclaration) {
            msg = "method " + ((MethodDeclaration) node).getName().getIdentifier();
        } else if (node instanceof TypeDeclaration) {
            msg = "class " + ((TypeDeclaration) node).getName().getIdentifier();
        } else if (node instanceof VariableDeclarationFragment) {
            msg = "variable " + ((VariableDeclarationFragment) node).getName().getIdentifier();
        } else if (node instanceof SimpleName) {
            ASTNode parent = node.getParent();
            if (parent instanceof MethodDeclaration && ((MethodDeclaration) parent).getName().equals(node)) {
                msg = "method " + ((MethodDeclaration) parent).getName().getIdentifier();
            } else if (parent instanceof TypeDeclaration && ((TypeDeclaration) parent).getName().equals(node)) {
                msg = "class " + ((TypeDeclaration) parent).getName().getIdentifier();
            } else if (parent instanceof VariableDeclarationFragment && ((VariableDeclarationFragment) parent).getName().equals(node)) {
                msg = "variable " + ((VariableDeclarationFragment) parent).getName().getIdentifier();
            }
        }
        if (msg == null) {
            return CompletableFuture.completedFuture(null);
        }
        Hover hover = new Hover(new MarkupContent("plaintext", msg));
        return CompletableFuture.completedFuture(hover);
    }

    @Override
    public CompletableFuture<org.eclipse.lsp4j.jsonrpc.messages.Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        String text = documents.get(params.getTextDocument().getUri());
        if (text == null) {
            return CompletableFuture.completedFuture(org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(Collections.emptyList()));
        }

        int offset = offsetAt(text, params.getPosition());
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(text.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        ASTNode node = NodeFinder.perform(cu, offset, 0);
        String name = null;
        if (node instanceof SimpleName) {
            name = ((SimpleName) node).getIdentifier();
        } else if (node instanceof MethodDeclaration) {
            name = ((MethodDeclaration) node).getName().getIdentifier();
        } else if (node instanceof TypeDeclaration) {
            name = ((TypeDeclaration) node).getName().getIdentifier();
        } else if (node instanceof VariableDeclarationFragment) {
            name = ((VariableDeclarationFragment) node).getName().getIdentifier();
        }

        final ASTNode[] result = new ASTNode[1];
        if (name != null) {
            final String searchName = name;
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration n) {
                    if (n.getName().getIdentifier().equals(searchName)) {
                        result[0] = n.getName();
                        return false;
                    }
                    return true;
                }
                @Override
                public boolean visit(TypeDeclaration n) {
                    if (n.getName().getIdentifier().equals(searchName)) {
                        result[0] = n.getName();
                        return false;
                    }
                    return true;
                }
                @Override
                public boolean visit(VariableDeclarationFragment n) {
                    if (n.getName().getIdentifier().equals(searchName)) {
                        result[0] = n.getName();
                        return false;
                    }
                    return true;
                }
            });
        }

        if (result[0] == null) {
            return CompletableFuture.completedFuture(org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(Collections.emptyList()));
        }
        int start = result[0].getStartPosition();
        int length = result[0].getLength();
        int startLine = cu.getLineNumber(start) - 1;
        int startCol = cu.getColumnNumber(start) - 1;
        int endLine = cu.getLineNumber(start + length) - 1;
        int endCol = cu.getColumnNumber(start + length) - 1;
        Range range = new Range(new Position(startLine, startCol), new Position(endLine, endCol));
        Location loc = new Location(params.getTextDocument().getUri(), range);
        return CompletableFuture.completedFuture(org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(Collections.singletonList(loc)));
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

    private int offsetAt(String text, Position pos) {
        int offset = 0;
        int line = pos.getLine();
        for (int i = 0; i < line; i++) {
            int nl = text.indexOf('\n', offset);
            if (nl == -1) {
                return text.length();
            }
            offset = nl + 1;
        }
        return Math.min(offset + pos.getCharacter(), text.length());
    }
}
