# CLI Commands — Room Server & Repeater

Reference for commands available when interacting with MeshCore room server and repeater nodes.
C++ firmware source: `examples/simple_repeater/MyMesh.cpp`, `examples/simple_room_server/MyMesh.cpp`, `src/helpers/CommonCLI.cpp`.

---

## 1. Binary Protocol Requests

Sent by the companion radio (on behalf of the Java client) to a room server or repeater over the mesh.

### Anonymous Requests (`CMD_SEND_ANON_REQ`, code 57)

No login required. First byte of payload is the `ANON_REQ_TYPE_*` code.

| Code | Name | Description |
|------|------|-------------|
| 0x01 | `ANON_REQ_TYPE_REGIONS` | Query regional flood-scope configuration |
| 0x02 | `ANON_REQ_TYPE_OWNER` | Get node owner info (name, location) |
| 0x03 | `ANON_REQ_TYPE_BASIC` | Get current clock + bridge feature status |

Response: `PUSH_BINARY_RESPONSE` (async push). Login response uses `RESP_SERVER_LOGIN_OK (0)` prefix.

### Authenticated Binary Requests (`CMD_SEND_BINARY_REQ`, code 50)

Require a successful login. First byte of the data payload is the `REQ_TYPE_*` code.
The reply is always prefixed with a reflected 4-byte sender timestamp.

| Code | Name | Who | Description |
|------|------|-----|-------------|
| 0x01 | `REQ_TYPE_GET_STATUS` | repeater + room server | Node statistics: battery, RSSI, SNR, packet counters, air time, uptime, error flags. Guests allowed. |
| 0x02 | `REQ_TYPE_KEEP_ALIVE` | repeater + room server | Keep-alive + sync check. Payload: `[0x02][syncSince u32 LE]` where `syncSince` is epoch-seconds of last received post. Resets push-failure tracking on room servers. |
| 0x03 | `REQ_TYPE_GET_TELEMETRY_DATA` | repeater + room server | Battery voltage and sensor readings. Second payload byte is an inverse permission mask (`~mask`) to filter sensor channels. Guests receive only base telemetry. |
| 0x05 | `REQ_TYPE_GET_ACCESS_LIST` | repeater + room server | Admin only. Returns list of ACL entries: 6-byte pubkey prefix + 1-byte permissions per entry. |
| 0x06 | `REQ_TYPE_GET_NEIGHBOURS` | repeater only | List neighboring repeaters heard over-the-air. Payload: `[count][offset u16 LE][order_by][pubkey_prefix_len][4-byte random tag]`. `order_by`: 0=newest, 1=oldest, 2=strongest SNR, 3=weakest SNR. Returns total count + paged results with `[pubkey prefix][heard_seconds_ago u32 LE][snr i8]` per entry. |
| 0x07 | `REQ_TYPE_GET_OWNER_INFO` | repeater only (v2+) | Firmware version, node name, and owner info as newline-separated text. |

**Java frame classes:** `CmdSendBinaryReq`, `CmdSendAnonReq`  
**Response push:** `PUSH_BINARY_RESPONSE` (0x8C)

### Status Request (`CMD_SEND_STATUS_REQ`, code 27)

Dedicated companion-protocol shorthand for `REQ_TYPE_GET_STATUS`.  
Payload: just the 32-byte target pubkey.  
**Java frame class:** `CmdSendStatusReq`  
**Response push:** `PUSH_STATUS_RESPONSE`

---

## 2. Login / Logout

### Login (`CMD_SEND_LOGIN`, code 26)

Authenticates the client with a room server or repeater.

```
Payload: [32-byte target pubkey][password string (null-terminated)]
```

- Immediate response: `RESP_SENT` (0x06)
- Async response: `PUSH_LOGIN_SUCCESS` (0x85) or `PUSH_LOGIN_FAIL` (0x86)

`PUSH_LOGIN_SUCCESS` fields:

| Field | Size | Notes |
|-------|------|-------|
| `permissions` | 1 byte | Bitmask; bit 0 = is_admin |
| `prefix6` | 6 bytes | First 6 bytes of server's public key (used as response key) |
| `timestamp` | 4 bytes (u32 LE) | Reflected from login request; only present for firmware v7+ |
| `acl` | 1 byte | ACL permission bits; only present for firmware v7+ |
| `fwVersionLevel` | 1 byte | Firmware version level; 6 when server is pre-v7 |

**Java frame class:** `CmdSendLogin`  
**Java response class:** `LoginSuccessPush`

### Logout (`CMD_LOGOUT`, code 29)

```
Payload: [32-byte target pubkey]
```

Response: `RESP_OK` (0x00)  
**Java frame class:** `CmdLogout`

---

## 3. Text CLI Commands

Sent as authenticated text messages (payload type `TXT_TYPE_CLI_DATA`) to the target node.
Admin credentials required. The node replies with the result as a text message.

Commands marked **serial-only** are accepted only when `sender_timestamp == 0` (i.e., typed on the device's own serial console, not received over the mesh).

### ACL Management (repeater + room server)

| Command | Description |
|---------|-------------|
| `setperm {pubkey-hex} {perm-int}` | Set ACL permissions for a client identified by hex pubkey prefix. `perm-int` is an integer (e.g. `1` = admin). |
| `get acl` | **Serial-only.** Dump all non-zero ACL entries to serial: `{perm_hex} {pubkey_hex}` per line. |

### Neighbor Discovery (repeater only)

| Command | Description |
|---------|-------------|
| `discover.neighbors` | Initiates a neighbor discovery broadcast. Repeaters within range respond. |

### Device Management (repeater + room server)

| Command | Description |
|---------|-------------|
| `ver` | Firmware version and build date. |
| `board` | Hardware manufacturer/board name. |
| `reboot` | Reboot the device. |
| `poweroff` / `shutdown` | Power off the device. |
| `clkreboot` | Reset RTC to 2024-05-15 and reboot. |
| `erase` | **Serial-only.** Format the filesystem. |

### Clock

| Command | Description |
|---------|-------------|
| `clock` | Display current RTC time (UTC). |
| `clock sync` | Sync RTC to sender's timestamp (only advances clock, never goes backwards). |
| `time {epoch-secs}` | **Serial-only.** Set RTC to given Unix epoch seconds (only advances). |

### Advertisement

| Command | Description |
|---------|-------------|
| `advert` | Send a flood advertisement immediately. |
| `advert.zerohop` | Send a zero-hop (local-only) advertisement. |

### Statistics

| Command | Description |
|---------|-------------|
| `neighbors` | List known neighboring nodes with SNR and last-heard time. |
| `neighbor.remove {pubkey-hex}` | Remove a neighbor from the known list. |
| `clear stats` | Reset all packet/radio statistics counters. |
| `stats-core` | **Serial-only.** Print core mesh stats. |
| `stats-radio` | **Serial-only.** Print radio stats. |
| `stats-packets` | **Serial-only.** Print packet stats. |

### Logging (serial-only)

| Command | Description |
|---------|-------------|
| `log` | Dump log file to serial. |
| `log start` | Enable logging. |
| `log stop` | Disable logging. |
| `log erase` | Erase the log file. |

### Configuration — `get` / `set`

Read with `get {key}`, write with `set {key} {value}`.

| Key | Type | Description |
|-----|------|-------------|
| `name` | string | Node advertised name (no `[]:\,?*` chars) |
| `lat` / `lon` | float | Node GPS coordinates (stored in prefs) |
| `radio` | `freq bw sf cr` | LoRa radio params (freq MHz, BW MHz, SF 5–12, CR 5–8). `set radio` requires reboot to apply. |
| `tx` | int (dBm) | TX power in dBm |
| `repeat` | `on` / `off` | Enable/disable packet forwarding |
| `rxdelay` | float | Base RX delay (seconds) |
| `txdelay` | float | TX delay factor |
| `direct.txdelay` | float | Direct-path TX delay factor |
| `dutycycle` | float (%) | Air-time duty cycle percentage (1–100) |
| `af` | float | Air-time factor (= 100/dutycycle – 1) |
| `flood.max` | int (0–64) | Maximum flood-hop count |
| `advert.interval` | int (minutes) | Local advertisement interval (60–240 min, or 0 to disable) |
| `flood.advert.interval` | int (hours) | Flood advertisement interval (3–168 h, or 0 to disable) |
| `guest.password` | string | Password for guest login |
| `password {new}` | — | Change admin password (`set` form not used; use `password {new}` directly) |
| `allow.read.only` | `on` / `off` | Allow read-only (guest) access |
| `multi.acks` | `0` / `1` | Enable multi-ACK mode |
| `path.hash.mode` | `0` / `1` / `2` | Path encoding mode |
| `loop.detect` | `off` / `minimal` / `moderate` / `strict` | Loop detection aggressiveness |
| `int.thresh` | int | Interference threshold |
| `agc.reset.interval` | int (seconds, rounded to 4 s) | AGC reset interval |
| `owner.info` | string | Owner info text; use `|` as line separator |
| `freq` | float (MHz) | **Serial-only.** Set frequency directly (requires reboot) |
| `radio.rxgain` | `on` / `off` | SX1262/SX1268 only. Boosted RX gain. |
| `public.key` | — | Read-only. Node's public key (hex). |
| `role` | — | Read-only. Node role string. |
| `adc.multiplier` | float | ADC voltage multiplier (0 = board default) |
| `prv.key` | hex | **Serial-only.** Read or set private key (requires reboot). |
| `bridge.type` | — | Read-only. Bridge type: `rs232`, `espnow`, or `none`. |
| `bridge.enabled` | `on` / `off` | Enable/disable RS232 or ESP-NOW bridge. |
| `bridge.delay` | int (ms) | Bridge packet delay (0–10000 ms). |
| `bridge.source` | `tx` / `rx` | Bridge packet source: `logTx` or `logRx`. |
| `bridge.baud` | int | RS232 bridge baud rate (9600–115200). |
| `bridge.channel` | int (1–14) | ESP-NOW bridge channel. |
| `bridge.secret` | string | ESP-NOW bridge XOR encryption secret. |

### Temporary Radio Override

```
tempradio {freq} {bw} {sf} {cr} {timeout_mins}
```

Applies radio parameters temporarily for `timeout_mins` minutes, then reverts. Does not persist.
Ranges: freq 150–2500 MHz, BW 7–500 MHz, SF 5–12, CR 5–8, timeout > 0.

### Power Saving

| Command | Description |
|---------|-------------|
| `powersaving` | Show current state. |
| `powersaving on` | Enable power saving mode. |
| `powersaving off` | Disable power saving mode. |

### GPS (if hardware present)

| Command | Description |
|---------|-------------|
| `gps` | Show GPS state (on/off, fix, satellite count). |
| `gps on` | Enable GPS. |
| `gps off` | Disable GPS. |
| `gps sync` | Sync RTC from GPS time. |
| `gps setloc` | Save current GPS fix to prefs as node location. |
| `gps advert` | Show location advertise policy. |
| `gps advert none` | Don't include location in advertisements. |
| `gps advert share` | Include live GPS location in advertisements. |
| `gps advert prefs` | Include saved prefs location in advertisements. |

### Sensor Variables

| Command | Description |
|---------|-------------|
| `sensor list [offset]` | List all custom sensor variables (paginated). |
| `sensor get {key}` | Get a sensor variable value. |
| `sensor set {key} {value}` | Set a sensor variable value. |

### Regions (Flood Scope)

| Command | Description |
|---------|-------------|
| `region` | List all configured regions. |
| `region load` | Begin bulk region load mode. Each subsequent text line is `{name}F` (allow flood) or `{name}` (deny flood). A blank line ends load and applies changes. |
| `region save` | Persist current region map to flash. |
| `region put {name} [{parent}]` | Add a region (defaults to wildcard parent). |
| `region remove {name}` | Remove a leaf region. |
| `region get {name}` | Show a region's flood flag and parent. |
| `region allowf {name}` | Allow flood for a region. |
| `region denyf {name}` | Deny flood for a region. |
| `region home [{name}]` | Get or set the home region. |
| `region default [{name}\|<null>]` | Get or set the default flood scope. |
| `region list allowed` | List regions where flood is allowed. |
| `region list denied` | List regions where flood is denied. |

### OTA Update

| Command | Description |
|---------|-------------|
| `start ota` | Initiate an OTA firmware update (board-specific support required). |
