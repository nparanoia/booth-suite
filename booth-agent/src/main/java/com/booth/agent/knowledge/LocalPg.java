package com.booth.agent.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 给本机开发和测试用的 pgvector。生产环境改连已有的 PostgreSQL，不走这里。
 */
public final class LocalPg {

    public static final String JDBC_URL =
            "jdbc:postgresql://127.0.0.1:5433/booth_agent?connectTimeout=5&socketTimeout=15";
    public static final String USER = "booth";
    public static final String PASSWORD = "boothagent";

    private static final Logger log = LoggerFactory.getLogger(LocalPg.class);
    private static final int PORT = 5433;
    private static final Object LOCK = new Object();
    private static volatile boolean ready;

    private LocalPg() {
    }

    public static void main(String[] args) {
        ensureStarted();
        System.out.println("knowledge database is ready at " + JDBC_URL);
    }

    public static void ensureStarted() {
        if (ready && canConnect(JDBC_URL)) {
            return;
        }
        synchronized (LOCK) {
            if (ready && canConnect(JDBC_URL)) {
                return;
            }
            Path prefix = prefix();
            Path bin = binaryDir(prefix);
            Path data = prefix.getParent().resolve(".pgdata");
            try {
                Files.createDirectories(data.getParent());
                if (!Files.exists(data.resolve("PG_VERSION"))) {
                    initDatabase(bin, data);
                }
                if (!canConnect("jdbc:postgresql://127.0.0.1:" + PORT + "/postgres?connectTimeout=2&socketTimeout=5")) {
                    startServer(bin, data);
                }
                createDatabase();
                ready = canConnect(JDBC_URL);
                if (!ready) {
                    throw new IllegalStateException("数据库已启动，但连不上 " + JDBC_URL);
                }
                log.info("pgvector ready at {}", JDBC_URL);
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("启动本地 pgvector 失败: " + exception.getMessage(), exception);
            }
        }
    }

    private static Path prefix() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path direct = cwd.resolve(".pg");
        if (Files.exists(direct.resolve("Library/bin/initdb.exe"))) {
            return direct;
        }
        Path nested = cwd.resolve("booth-agent/.pg");
        if (Files.exists(nested.resolve("Library/bin/initdb.exe"))) {
            return nested;
        }
        throw new IllegalStateException("没找到本地 PostgreSQL。在 booth-agent 目录执行："
                + " conda create -y -p .pg -c conda-forge postgresql pgvector");
    }

    private static Path binaryDir(Path prefix) {
        return prefix.resolve("Library/bin");
    }

    private static void initDatabase(Path bin, Path data) throws IOException, InterruptedException {
        if (Files.exists(data.resolve("PG_VERSION"))) {
            return;
        }
        if (Files.exists(data)) {
            try (var entries = Files.list(data)) {
                if (entries.findAny().isPresent()) {
                    throw new IllegalStateException("数据目录非空且未初始化: " + data);
                }
            }
        } else {
            Files.createDirectories(data);
        }
        Path password = data.getParent().resolve(".pg-password.txt");
        Files.writeString(password, PASSWORD + System.lineSeparator(), StandardCharsets.UTF_8);
        List<String> command = new ArrayList<>(List.of(
                bin.resolve("initdb.exe").toString(),
                "-D", data.toString(),
                "-U", USER,
                "--pwfile=" + password.toString(),
                "-E", "UTF8",
                "-A", "scram-sha-256",
                "--locale=C"
        ));
        try {
            run(bin, command);
        } catch (IOException exception) {
            if (exception.getMessage() == null || !exception.getMessage().toLowerCase().contains("locale")) {
                throw exception;
            }
            wipe(data);
            Files.createDirectories(data);
            command.remove("--locale=C");
            run(bin, command);
        }
    }

    private static void wipe(Path data) throws IOException {
        if (!Files.exists(data) || Files.exists(data.resolve("PG_VERSION"))) {
            return;
        }
        try (var walk = Files.walk(data)) {
            List<Path> paths = walk.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void startServer(Path bin, Path data) throws IOException, InterruptedException {
        Path logFile = data.resolve("server.log");
        run(bin, List.of(
                bin.resolve("pg_ctl.exe").toString(),
                "-D", data.toString(),
                "-l", logFile.toString(),
                "-w",
                "-t", "40",
                "start",
                "-o", "-p " + PORT + " -h 127.0.0.1"
        ));
    }

    private static void createDatabase() throws IOException {
        String admin = "jdbc:postgresql://127.0.0.1:" + PORT + "/postgres?connectTimeout=5&socketTimeout=15";
        try (Connection connection = DriverManager.getConnection(admin, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            ResultSet existing = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = 'booth_agent'");
            if (!existing.next()) {
                existing.close();
                statement.execute("CREATE DATABASE booth_agent");
            }
        } catch (Exception exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }

    private static boolean canConnect(String url) {
        try (Connection connection = DriverManager.getConnection(url, USER, PASSWORD)) {
            return connection.isValid(2);
        } catch (Exception exception) {
            return false;
        }
    }

    private static void run(Path bin, List<String> command) throws IOException, InterruptedException {
        Path logFile = bin.getParent().getParent().resolve(".pg-cmd.log");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(bin.toFile());
        String path = bin + ";" + System.getenv("PATH");
        builder.environment().put("PATH", path);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        Process process = builder.start();
        boolean finished = process.waitFor(90, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("命令超时: " + command.get(0));
        }
        if (process.exitValue() != 0) {
            String text = Files.exists(logFile) ? Files.readString(logFile) : "";
            if (text.length() > 2000) {
                text = text.substring(text.length() - 2000);
            }
            throw new IOException(command.get(0) + " 退出码 " + process.exitValue() + "\n" + text);
        }
    }
}
