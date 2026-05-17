package com.github.ruediste.gdriveclient;

import java.net.ConnectException;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "google-drive-client", mixinStandardHelpOptions = true, version = "1.0", description = "Google Drive Client with support for lazy synchronization and offline operation.", subcommands = {
        App.MountCommand.class,
        App.StopCommand.class,
        App.StatusCommand.class,
        App.DaemonCommand.class
})
public class App implements Runnable {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Use 'help' to see available commands.");
    }

    @Command(name = "mount", description = "Mounts Google Drive to the specified local <path> and starts the background daemon.")
    static class MountCommand implements Runnable {
        @CommandLine.Parameters(index = "0", description = "The path to mount Google Drive.")
        private String path;

        @Override
        public void run() {
            try {
                sendToDaemon("status");
                System.out.println("Daemon is already running.");
                return;
            } catch (Exception e) {
                // Daemon not running
            }
            String java = System.getProperty("java.home") + "/bin/java";
            try {
                String jarPath = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                ProcessBuilder pb = new ProcessBuilder(java, "-jar", jarPath, "daemon", path);
                pb.inheritIO();
                pb.start();
                System.out.println("Daemon started in background for " + path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Command(name = "stop", description = "Unmounts the file system and gracefully stops the associated daemon.")
    static class StopCommand implements Runnable {
        @Override
        public void run() {
            try {
                sendToDaemon("stop");
                System.out.println("Stop command sent.");
            } catch (ConnectException e) {
                System.err.println("Daemon not running");
                System.exit(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Command(name = "status", description = "Queries the daemon for the current synchronization status and health of the mount.")
    static class StatusCommand implements java.util.concurrent.Callable<Integer> {
        @Override
        public Integer call() {
            try {
                System.out.println(sendToDaemon("status"));
                return 0;
            } catch (ConnectException e) {
                System.err.println("Daemon not running");
                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                return 2;
            }
        }
    }

    public static java.nio.file.Path dataPath() {
        return java.nio.file.Paths.get(System.getProperty("user.home"), ".config", "google-drive-client");
    }

    public static java.nio.file.Path getSocketPath() {
        return dataPath().resolve("daemon.sock");
    }

    private static String sendToDaemon(String command) throws java.io.IOException {
        try (java.nio.channels.SocketChannel channel = java.nio.channels.SocketChannel
                .open(java.net.UnixDomainSocketAddress.of(getSocketPath()))) {
            channel.write(java.nio.ByteBuffer.wrap(command.getBytes()));
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(1024);
            channel.read(buffer);
            buffer.flip();
            return new String(buffer.array(), 0, buffer.limit());
        }
    }

    @Command(name = "daemon", description = "Starts the background daemon.", hidden = true)
    static class DaemonCommand implements Runnable {
        @CommandLine.Parameters(index = "0", description = "The path being mounted.")
        private String mountPath;

        @Override
        public void run() {
            java.nio.file.Path lockPath = dataPath().resolve("daemon.lock");
            try {
                java.nio.file.Files.createDirectories(lockPath.getParent());
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(lockPath.toFile(), "rw");
                        java.nio.channels.FileLock lock = raf.getChannel().tryLock()) {
                    if (lock == null) {
                        System.out.println("Daemon already running.");
                        return;
                    }

                    System.out.println("Daemon started for " + mountPath);

                    new DaemonMain().start(mountPath);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
