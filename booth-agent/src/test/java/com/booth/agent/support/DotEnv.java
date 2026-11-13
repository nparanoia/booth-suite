package com.booth.agent.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** 读取 booth-agent/.env，不打印密钥。 */
public final class DotEnv {

    private DotEnv() {
    }

    public static Map<String, String> load() throws IOException {
        return load(Path.of(".env"));
    }

    public static Map<String, String> load(Path path) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        Path resolved = path.isAbsolute() ? path : Path.of("").toAbsolutePath().resolve(path).normalize();
        if (!Files.exists(resolved)) {
            // surefire 有时 cwd 在模块根或仓库根
            Path alt = Path.of("booth-agent", ".env");
            if (Files.exists(alt)) {
                resolved = alt;
            } else {
                return map;
            }
        }
        for (String raw : Files.readAllLines(resolved, StandardCharsets.UTF_8)) {
            String line = raw.replace("\uFEFF", "").trim();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            int i = line.indexOf('=');
            String key = line.substring(0, i).trim();
            String value = line.substring(i + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            map.put(key, value);
        }
        return map;
    }
}
