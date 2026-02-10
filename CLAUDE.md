# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

mica-net is a high-performance Java network communication framework simplified from t-io, using Java NIO's AsynchronousSocketChannel for async non-blocking network communication. It supports both TCP and UDP protocols.

## Build Commands

```bash
# Build all modules (tests are skipped by default via maven.test.skip=true)
mvn clean install

# Build with specific profile
mvn clean install -P develop  # Default profile with Aliyun repository
mvn clean install -P release  # Release profile with javadoc and GPG signing
mvn clean install -P snapshot # Snapshot deployment to central repository

# Compile only
mvn compile

# Run tests (need to enable tests first)
mvn test -Dmaven.test.skip=false

# Package
mvn package
```

## Module Structure

- **mica-net-utils**: Utility classes (HexUtils, DigestUtils, ExceptionUtils, etc.)
- **mica-net-core**: Core networking framework with TCP/UDP support
- **mica-net-http**: HTTP protocol implementation

## Core Architecture

### Three-Layer Task Queue Architecture

The framework uses a pipeline of asynchronous task queues:

```
[Network I/O] → [Decode Queue] → [Handler Queue] → [Send Queue] → [Network I/O]
       ↓              ↓                ↓                ↓
ReadCompletionHandler  DecodeRunnable  HandlerRunnable  SendRunnable
```

### Core Components

**Tio (org.tio.core.Tio)**: Main API entry point for all operations (send, close, bind, unbind)

**ChannelContext**: Connection context that exists per TCP/UDP connection. Contains:
- Connection state, statistics, and binding relationships
- Three core Runnables: DecodeRunnable, HandlerRunnable, SendRunnable
- Uses binary bit flags for state management to reduce memory footprint

**TioConfig**: Global configuration (thread pools, statistics, heartbeat, SSL)

**Channel Contexts**:
- TcpChannelContext (mica-net-core/src/main/java/org/tio/core/tcp/TcpChannelContext.java) for TCP
- UdpChannelContext (mica-net-core/src/main/java/org/tio/core/udp/UdpChannelContext.java) for UDP

### Receive Data Flow

1. AsynchronousSocketChannel.read() triggers ReadCompletionHandler.completed()
2. ReadCompletionHandler updates statistics, handles SSL decryption, adds to decode queue
3. DecodeRunnable processes lastByteBuffer, calls TioHandler.decode() in loop, detects slow packet attacks
4. HandlerRunnable processes business logic via TioHandler.handler() with sync message support

### Send Data Flow

1. Tio.send() checks connection state, applies PacketConverter if configured
2. Adds to queue or sends directly based on useQueueSend flag
3. SendRunnable batches packets adaptively, encodes via TioHandler.encode(), applies SSL encryption
4. Writes via AsynchronousSocketChannel.write() with WritePendingException prevention
5. WriteCompletionHandler tracks completion and triggers next batch

## Important Development Notes

### Connection Lifecycle

- **Client-initiated disconnect**: Use `Tio.close()` - allows reconnection logic
- **Server-initiated disconnect**: Use `Tio.remove()` - prevents reconnection attempts

### State Management

ChannelContext uses binary bit flags for state (see ChannelContext.java:86-96):
- Bits 0-2: Configuration state (isVirtual, isReconnect, logWhenDecodeError)
- Bits 3-5: Connection state (isWaitingClose, isClosed, isRemoved)
- Bits 6-7: Extension state (isAccepted, isBizStatus) - reserved for business use

### Performance Optimizations

- Uses LongAdder instead of AtomicInteger for statistics
- Concurrent collections replace locks where possible
- Field alignment optimization in ChannelContext to reduce memory padding
- Batch packet sending with adaptive batch size based on queue depth

### SSL Support

- Supports both client and server mutual authentication
- Client can skip domain validation
- Uses SslFacadeContext for SSL state management

### Protocol Support

- **TCP Proxy Protocol v1**: Supports nginx/ELB forwarding with original IP preservation
- **TCP and UDP**: Unified API with protocol-specific ChannelContext implementations

## Testing

Test examples are in `mica-net-core/src/test/java/org/tio/core/`:
- agnss: AGNSS protocol client example
- clickhouse: ClickHouse protocol implementation example

Each test demonstrates a complete client-server implementation with codec.

## Logging

Framework uses SLF4J. For testing, tinylog is used (lower memory footprint, better for edge devices).

## Version Information

- Minimum Java version: 1.8
- Current version: 1.2.8 (see pom.xml revision property)
- License: Apache License v2.0
