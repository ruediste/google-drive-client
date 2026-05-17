# google-drive-client

Google Drive Client with support for lazy synchronization and offline operation

## Overview

Unfortunately, there is no official Google Drive client for Linux. There are a few options, most notably

- [Google Drive OcamlFuse](https://github.com/astrada/google-drive-ocamlfuse): does not support offline operation
- [Rclone](https://rclone.org/): Supports bidirectional synchronization, but always replicates the whole directory

The goal of this project is to create a client using FUSE to support lazy synchronization, with caching and offline capabilities.

The client maintains an Observed Remote State (ORS), capturing the state last seen on the server and downloaded to the client. Then there is the Local State (LS), which is what is mounted via the FUSE file system. As long as there are no local changes, the LS matches the ORS.

When changes are detected on the server, the changes are downloaded and the ORS is updated. If there are already local changes, a conflict occurred. It is resolved by for example renaming the local file.

Local changes are uploaded to the server as soon as possible.

## Data Structure

![Data Model](doc/data-model.lofi.png)

The data model is centered around a hierarchical structure of `Directory` and `File` entities, which mirror the Google Drive structure.

- **Root**: Represents the starting point, allowing for multiple roots.
- **Directory**: Can contain subdirectories or files. It tracks metadata like `locallyDeleted`, `shallow` (indicating partial loading), `lastAccessed`, and `fetchEagerly`.
- **File**: Represents individual files, tracking a `locallyDeleted` flag.

Both `Directory` and `File` maintain two states:

- **ORS (Observed Remote State)**: Represents the state as seen on the server.
- **LS (Local State)**: Represents the state as it exists locally (0..1 relationship, as it might not be fully synced or loaded).

The `DirectoryState` and `FileState` capture specific attributes for their respective entities, such as `name`, `modifiedAt` for files, and others.

## Cli and Daemon Process

The application is distributed as a single executable JAR file that serves as both the Command Line Interface (CLI) tool and the background daemon process.

### Architecture

When you mount a Google Drive folder, the CLI tool forks a new background process (the daemon) using the same JAR. This daemon is responsible for:

- Maintaining the FUSE file system mount.
- Synchronizing the Observed Remote State (ORS) and Local State (LS).
- Handling background uploads and downloads.

### Inter-Process Communication (IPC)

The CLI tool and the background daemon communicate using **Unix Domain Sockets (UDS)**.
When the daemon starts, it creates a local socket file (e.g., in `~/.config/google-drive-client/daemon.sock`). The CLI tool connects to this socket to send commands (like querying status or stopping the daemon) and receive responses. UDS is chosen because it provides secure, fast, and reliable local communication without the overhead or security concerns of exposing a local TCP port.

### CLI Commands

The CLI provides several commands to manage the daemon and the FUSE mount. Below is an overview of the available commands:

```text
Usage: java -jar google-drive-client.jar [COMMAND]

Commands:
  mount <path>    Mounts Google Drive to the specified local <path> and starts the background daemon.
                  Example: java -jar google-drive-client.jar mount ~/GoogleDrive

  stop            Unmounts the file system and gracefully stops the associated daemon.
                  Example: java -jar google-drive-client.jar stop

  status          Queries the daemon for the current synchronization status and health of the mount at.
                  Example: java -jar google-drive-client.jar status

  help            Displays this help message.
```

## Technologies

- Main implementation language: Java
- FUSE via https://github.com/serceman/jnr-fuse
- Persistence: https://www.h2database.com/html/mvstore.html
- Google Drive API Client: https://developers.google.com/workspace/drive/api/quickstart/java
- Thunar Extensions: https://developer.xfce.org/thunar/index.html
- Tray Notification: https://github.com/dorkbox/SystemTray
