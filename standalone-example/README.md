# Standalone Example

This module demonstrates a minimal setup of the JDT language server without the Eclipse platform. It uses only `lsp4j` and the `org.eclipse.jdt.core` library.

Run the server with:

```bash
mvn package
java -cp target/standalone-example-0.1.0-SNAPSHOT.jar org.eclipse.jdt.ls.standalone.MinimalLanguageServer
```
