# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```bash
mvn package                        # build main library
cd examples/MeshBot && mvn package # build the MeshBot example
```

Target Java version is JDK 8 (`maven.compiler.release=8`). Do not use language features above Java 8.

There are no automated tests. Correctness is verified by cross-checking against the reference C++ firmware source at https://github.com/meshcore-dev/MeshCore/blob/main/examples/companion_radio/MyMesh.cpp. Companion protocol version is `FIRMWARE_VER_CODE=13`, unchanged from firmware v1.16.0 through v1.17.1 (verified against the v1.17.0→v1.17.1 diff: only a FEM radio gain prefs bugfix landed, no companion protocol changes).

## Architecture

### Class hierarchy

```
MeshcoreCompanionBase   – threading, send/receive loop, requestLock
  └─ MeshcoreCompanion  – protocol logic: contacts, channels, listeners, high-level send methods
       └─ SerialMeshcoreCompanion – USB/serial transport via jSerialComm
```

To add a BLE transport, subclass `MeshcoreCompanion` and implement the three abstract methods (`sendBinaryFrame`, `getBinaryFrame`, `checkConnection/isConnected`).

### Frame hierarchy

All wire frames are under `frames/`:

- `frames/cmd/`  – outgoing command frames (one class per command, serialized with `ByteBuilder`)
- `frames/resp/` – incoming response frames parsed by the reader thread (deserialized with `ByteReader`)
- `frames/push/` – unsolicited push frames from firmware, also parsed by reader thread
- `frames/group/` – abstract superclasses used for multi-frame listener registration (e.g. `MessageFrameGroup`, `ContactFrameGroup`)

`FrameConstants.java` is the single source of truth for all command/response/push byte codes and related enums (`CommandFrameType`, `ResponseFrameType`, `PushFrameType`, `MessageTextType`, etc.).

### Binary serialization

`ByteBuilder` – builds outgoing byte arrays (little-endian). `ByteReader` – reads incoming byte arrays. All frame parsing must stay consistent with the C++ firmware's exact byte layout. When adding or fixing a frame, always verify field widths and order against the C++ source.

### Threading model

- One **reader thread** owns all frame parsing.
- Listener callbacks are dispatched to a single-thread **eventExecutor**, so they must not block.
- Blocking commands (`sendFrameWithResult`, `sendFrameWithResultAndResponse`) are serialized by `requestLock` because the protocol has no request ID.

### OTA frames

`otaframe/` contains parsers for over-the-air (radio) frame formats (`OtaGroupFrame`, `OtaUnicastFrame`, `OtaAdvertFrame`, etc.). These are decoded from raw `LogRXDataPush` air traffic, not from the serial companion protocol.

### Listeners

Register listeners by frame class or superclass:

```java
companion.registerFrameListener(MessageFrameGroup.class, frame -> { ... });
```

`FrameListenerRegistry` dispatches each incoming frame to all matching listeners by walking the class hierarchy.

### Logging

All serial frame traffic is logged to `cz.bliksoft.meshcore.companion.MeshcoreCompanion.DEV` at FINE level. Detailed air-frame decoding is controlled at runtime via `LogRXDataPush.isDecodeRaw(true)` / `isDecodePayload(true)` / `setTranslatePath(true)`.

### Room server & repeater commands

See `CLI.md` for a full reference of binary protocol requests and text CLI commands available when interacting with room server and repeater nodes.

