# Extraction Plan for Minimal JDT Language Server

This document outlines a strategy for reducing the `eclipse.jdt.ls`
repository to a lightweight Java 21 library that exposes only the core
Language Server Protocol (LSP) functionality. The aim is to remove OSGi
and Eclipse platform dependencies while keeping the parts of the Eclipse
JDT that are required for code completion and refactoring.

## 1. Identify Essential Modules

The repository currently contains several Maven modules:

- `org.eclipse.jdt.ls.target` – target platform definition (p2)
- `org.eclipse.jdt.ls.core` – language server implementation
- `org.eclipse.jdt.ls.filesystem` – custom file system integration
- `org.eclipse.jdt.ls.logback.appender` – logback appender
- `org.eclipse.jdt.ls.product` – Eclipse product packaging
- `org.eclipse.jdt.ls.tests` – unit/integration tests
- `org.eclipse.jdt.ls.tests.syntaxserver` – tests for syntax server
- `org.eclipse.jdt.ls.repository` – p2 update site

For a minimal standalone library we should keep only:

1. The language server logic from `org.eclipse.jdt.ls.core`
2. Any required file system helpers from `org.eclipse.jdt.ls.filesystem`
3. Logging support (either keep the appender module or switch to a
   simpler logging setup)

All product packaging, p2 repository modules, and OSGi configuration can
be removed.

## 2. Convert to Standard JAR Packaging

- Change the packaging of remaining modules from `eclipse-plugin` to
  `jar` in their `pom.xml` files.
- Delete `plugin.xml`, `build.properties` and other OSGi descriptors.
- Replace Tycho plugins with `maven-compiler-plugin` and
  `maven-jar-plugin` configured for Java 21.
- Ensure dependencies such as `lsp4j` and `org.eclipse.jdt.core` are
  pulled from Maven Central.

## 3. Remove OSGi and Eclipse Runtime Code

- Eliminate any uses of `org.osgi.*` classes, service trackers or
  extension points.
- Refactor initialization logic so the server can be created via a
  regular Java `main` method or factory, without an Equinox launcher.
- Drop the target platform and product modules since they only serve
  Eclipse packaging.

## 4. Minimal Dependency Set

After cleanup, the library should depend only on:

- **lsp4j** – for the Language Server Protocol APIs
- **org.eclipse.jdt.core** – compiler and tooling APIs needed for code
  completion and refactoring
- **slf4j/logback** – for logging (optional)

Other dependencies related to the Eclipse IDE, Buildship, M2E or the
OSGi framework can be deleted.

## 5. Restructure Source Layout

- Move Java sources into the Maven standard layout
  `src/main/java` (and `src/test/java` for tests).
- Adjust package names if needed to remove references to internal Eclipse
  packages.
- Convert existing tests to JUnit 5 where possible and drop those that
  require the OSGi runtime.

## 6. Provide a Simple Build and Usage Example

- Create a trimmed parent `pom.xml` that only includes the kept modules
  and sets the compiler level to Java 21.
- Document a basic example that creates and starts the server using
  lsp4j, suitable for embedding in other projects.
- Describe how to publish the resulting JAR to a repository for use as a
  dependency elsewhere.

## 7. Recommended Extraction Steps

1. **Isolate** the core packages from `org.eclipse.jdt.ls.core` by
   removing OSGi-specific code and verifying they compile independently.
2. **Refactor** service initialization and dependency management to use
   plain Java constructs instead of OSGi services.
3. **Build** the modules with standard Maven to ensure they work as
   regular JARs and start the server from a small `main` method.
4. **Remove** the obsolete modules and clean up the parent POM.
5. **Validate** LSP features (completion, diagnostics, refactoring) using
   the remaining JUnit tests.

Following this plan will result in a minimal Java 21 library that offers
language server functionality using only lsp4j and the essential pieces
of the Eclipse JDT core.
