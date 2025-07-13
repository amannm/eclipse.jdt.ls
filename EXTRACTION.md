# Codebase Extraction Plan

This document outlines a plan to refactor the repository into a minimal Java 21
library that contains only the core functionality required to run the language
server. The goal is to drop all OSGi/Eclipse platform dependencies and keep only
the essential pieces such as LSP4J and the minimal JDT libraries needed for
refactoring operations.

## Goals

- Produce a plain Java 21 library that implements the Language Server Protocol
  (LSP) for Java.
- Eliminate OSGi/Tycho specific build artifacts and Eclipse platform plug-ins.
- Retain only the pieces of the current code base that directly implement the
  LSP server logic and rely on the minimal JDT core libraries.

## Proposed Steps

1. **Identify core packages**
   - Inspect `org.eclipse.jdt.ls.core` and locate packages that implement the
     LSP server (handlers, preferences, diagnostics, etc.).
   - Mark unrelated plug-in specific packages (e.g., OSGi activators,
     product definitions, p2 repository code) for removal.

2. **Remove Tycho/OSGi build**
   - Replace `eclipse-plugin` packaging with a simple `jar` packaging in the
     Maven modules that will remain.
   - Delete modules such as `org.eclipse.jdt.ls.product`,
     `org.eclipse.jdt.ls.repository`, and `org.eclipse.jdt.ls.target` that are
     used only for Eclipse product builds.

3. **Create a new Maven module**
   - Introduce a plain Java module (e.g., `jdt.ls.minimal`) that depends on
     `org.eclipse.lsp4j` and the minimal set of JDT libraries (typically
     `org.eclipse.jdt.core`).
   - Move the selected source packages from `org.eclipse.jdt.ls.core` into this
     module and adapt package names as desired.

4. **Adjust dependencies**
   - Keep `org.eclipse.lsp4j` and remove any dependency that pulls in Eclipse
     platform or OSGi bundles.
   - Include `org.eclipse.jdt.core` and other JDT libraries strictly required to
     compile and perform refactoring actions.
   - Drop integrations with M2Eclipse, Buildship, and other tooling.

5. **Rewrite entry point**
   - Provide a small `main` class that launches the server using LSP4J’s
     `Launcher` API.
   - Remove the existing OSGi `Activator` and associated plug-in metadata.

6. **Simplify configuration**
   - Remove launch configuration files under `launch/` and replace them with a
     simple command that runs the jar: `java -jar jdtls-minimal.jar`.

7. **Prune test suite**
   - Retain only tests that exercise core language server operations using JUnit.
   - Remove tests dependent on OSGi, the Eclipse IDE, or complex workspace setups.

8. **Update build instructions**
   - Document how to build the new module using Java 21 with standard Maven:
     `JAVA_HOME=/path/to/jdk21 mvn package`.
   - Provide instructions for integration into other code bases as a plain jar.

9. **Iterate and validate**
   - After the initial extraction, compile the minimal module and run basic LSP
     scenarios (e.g., open file, completion, refactor) to verify functionality.
   - Continue removing residual dependencies until only LSP4J and the required
     JDT libraries remain.

## Result

Following this plan will yield a lightweight Java 21 library that exposes the
core LSP server for Java without any OSGi or Eclipse platform overhead. This
stripped-down version can then be copied into another repository and built as a
standard Java dependency.
