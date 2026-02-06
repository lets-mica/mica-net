# Agent Guidelines for mica-net

This document provides essential information for AI agents working on the mica-net repository. It covers build commands, testing procedures, and code style guidelines to ensure consistency and quality.

## 1. Project Overview

**mica-net** is a non-blocking network framework based on Java AIO (Asynchronous I/O).
*   **Java Version:** 1.8
*   **Build System:** Maven
*   **Key Dependencies:** fastjson/fastjson2, hutool, slf4j, tinylog (testing).

### Module Structure
*   `mica-net-core`: Core networking implementation (AIO, TCP, UDP).
*   `mica-net-http`: HTTP protocol support.
*   `mica-net-utils`: Common utility classes.

## 2. Build and Test Commands

### Build
To build the entire project, run:
```bash
mvn clean install
```

To build without running tests (faster):
```bash
mvn clean install -Dmaven.test.skip=true
```

To build a specific module (e.g., core):
```bash
mvn clean install -pl mica-net-core -am
```

### Testing
This project uses **JUnit 5** for testing.

**Run all tests:**
```bash
mvn test
```

**Run a single test class:**
```bash
mvn -Dtest=TestClassName test
# Example:
mvn -Dtest=TcpClientTest test
```

**Run a specific test method:**
```bash
mvn -Dtest=TestClassName#methodName test
# Example:
mvn -Dtest=TcpClientTest#testConnection test
```

**Note on Logs:** Test logs typically use `tinylog`. Check console output for details.

## 3. Code Style Guidelines

Adhere strictly to the existing code style. When in doubt, mimic the surrounding code.

### Formatting
*   **Indentation:** Use **Tabs** or 4 spaces (detect from file). Most files appear to use **Tabs**.
*   **Braces:** K&R style (opening brace on the same line).
*   **Encoding:** UTF-8.
*   **Line Endings:** LF (Unix) or CRLF (Windows) - respect the file's existing setting.

### Imports
*   **No Wildcard Imports:** Do not use `import java.util.*;`. Import each class explicitly.
*   **Ordering:**
    1.  Standard Java imports (`java.*`, `javax.*`)
    2.  Third-party imports (`org.slf4j.*`, `com.alibaba.*`)
    3.  Project imports (`org.tio.*`, `net.dreamlu.*`)
*   **Cleanup:** Remove unused imports.

### Naming Conventions
*   **Classes/Interfaces:** `PascalCase` (e.g., `ChannelContext`, `TioServer`).
*   **Methods/Variables:** `camelCase` (e.g., `sendPacket`, `clientNode`).
*   **Constants:** `UPPER_SNAKE_CASE` (e.g., `MAX_DATA_LENGTH`, `UNKNOWN_ADDRESS_IP`).
*   **Packages:** `lowercase` (e.g., `org.tio.core`).

### Logging
*   Use **SLF4J** for logging.
*   **Logger Declaration:**
    ```java
    private static final Logger log = LoggerFactory.getLogger(CurrentClass.class);
    ```
*   **Usage:** Use placeholders `{}` instead of string concatenation.
    ```java
    // Good
    log.error("Connection failed for user: {}", userId, e);
    
    // Avoid
    log.error("Connection failed for user: " + userId, e);
    ```

### Error Handling
*   Use `try-catch` blocks where appropriate.
*   Log exceptions with stack traces using `log.error(msg, e)`.
*   Avoid swallowing exceptions without logging or handling.
*   When throwing custom exceptions, use existing exception classes in `org.tio.core.exception` if applicable.

### Comments and Javadoc
*   **Language:** Existing comments contain Chinese. New comments should ideally be in English for broader accessibility, but Chinese is acceptable if modifying existing Chinese-commented sections to maintain consistency.
*   **Javadoc:** Required for public classes and interfaces.
*   **Implementation Comments:** Add comments for complex logic explaining *why*, not just *what*.

### License Header
New files must include the Apache License 2.0 header. Copy it from an existing file (e.g., `mica-net-core/src/main/java/org/tio/core/ChannelContext.java`).

```java
/*
    Apache License
    Version 2.0, January 2004
    http://www.apache.org/licenses/
    ... (See full header in existing files)
*/
```

## 4. Specific Patterns

### ChannelContext
*   `ChannelContext` is the core state object for a connection.
*   It extends `MapPropSupport` for attaching custom properties.
*   Use `tioConfig` to access global configuration.

### ByteBuffer Handling
*   The framework relies heavily on `java.nio.ByteBuffer`.
*   Ensure buffers are flipped (`buffer.flip()`) before reading after writing to them.
*   Watch out for buffer underflows/overflows.

### JSON Serialization
*   The project supports multiple JSON libraries (`fastjson`, `gson`, `jackson`).
*   Check `mica-net-utils` or local configuration to see which one is active or preferred in a specific context.

## 5. Development Workflow for Agents

1.  **Explore:** Before changing code, read related files to understand the context.
    *   Use `grep` to find usages of methods/classes.
    *   Use `read` to inspect file content.
2.  **Verify:**
    *   Check for existing tests in `src/test/java`.
    *   Create new tests if adding new functionality.
    *   Run `mvn test` to ensure no regressions.
3.  **Implement:**
    *   Apply changes respecting the code style.
    *   Keep changes atomic and focused.

## 6. Miscellaneous

*   **Null Checks:** Use `java.util.Objects.requireNonNull` or explicitly check for nulls where necessary.
*   **Concurrency:** The framework is multi-threaded. Be careful with shared state. Use `AtomicInteger`, `ConcurrentHashMap`, etc., where appropriate.
*   **Git:** Do not commit `.idea`, `.vscode`, `target/` directories.
