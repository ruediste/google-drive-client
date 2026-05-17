package com.github.ruediste.gdriveclient;

import com.github.ruediste.gdriveclient.model.Data;
import com.github.ruediste.gdriveclient.model.Directory;
import com.github.ruediste.gdriveclient.model.DirectoryState;

public class DaemonMain {

    private final long startTime = System.currentTimeMillis();

    public void start(String mountPath) throws Exception {
        var dataStore = new DataStore();
        dataStore.open();

        // temporary scaffolding
        var dir = new Directory();
        dir.ors = new DirectoryState();
        dir.ors.name = "TestDir";
        long dirId = dataStore.nextId();
        dataStore.directories.put(dirId, dir);

        var root = new Data.RootDirectory();
        root.name = "test";
        root.directoryId = dirId;
        dataStore.data.roots.put(dataStore.nextId(), root);
        dataStore.saveData();

        new Thread(this::socketListener, "socket-listener").start();
    }

    private void socketListener() {
        try {
            java.nio.file.Path socketPath = App.getSocketPath();

            java.nio.file.Files.deleteIfExists(socketPath);
            try (java.nio.channels.ServerSocketChannel server = java.nio.channels.ServerSocketChannel
                    .open(java.net.StandardProtocolFamily.UNIX)) {
                server.bind(java.net.UnixDomainSocketAddress.of(socketPath));

                while (true) {
                    try (java.nio.channels.SocketChannel client = server.accept()) {
                        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(1024);
                        client.read(buffer);
                        buffer.flip();
                        String command = new String(buffer.array(), 0, buffer.limit());
                        String response = "unknown command";
                        if ("status".equals(command.trim())) {
                            response = "Uptime: " + ((System.currentTimeMillis() - startTime) / 1000.) + "s";
                        } else if ("stop".equals(command.trim())) {
                            System.out.println("Stopping daemon");
                            System.exit(0);
                        }
                        client.write(java.nio.ByteBuffer.wrap(response.getBytes()));
                    }
                }
            } finally {
                java.nio.file.Files.deleteIfExists(socketPath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
