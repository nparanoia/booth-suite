package com.booth.agent.knowledge;

import com.booth.agent.config.BoothAgentProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 政策文档按标题切块，向量写入 pgvector，再用短语命中补充。
 * 低于阈值就拒答。检索片段只当资料，不能当成退货指令。
 */
@Slf4j
@Service
public class KnowledgeService {

    private final BoothAgentProperties properties;
    private final KnowledgeStore store;
    private final TextEmbedder embedder;

    public KnowledgeService(BoothAgentProperties properties, DataSource dataSource, TextEmbedder embedder) {
        this.properties = properties;
        this.store = new KnowledgeStore(dataSource);
        this.embedder = embedder;
    }

    @PostConstruct
    public void load() throws IOException {
        store.ensureSchema();
        int chunks = 0;
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:knowledge/*.md");
        // 只刷新 classpath 种子源，不 wipe 全表，保留 Ops 手工补库
        for (Resource resource : resources) {
            String name = resource.getFilename() == null ? "unknown.md" : resource.getFilename();
            try (InputStream input = resource.getInputStream()) {
                String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                List<MarkdownChunker.Chunk> parts = MarkdownChunker.split(name, content);
                store.deleteSource(name);
                for (KnowledgeStore.Row row : toRows(parts)) {
                    store.insert(row);
                }
                chunks += parts.size();
            }
        }
        log.info("knowledge seed refreshed chunks={} embedder={}", chunks, embedder.name());
    }

    public RetrievalResult retrieve(String question) {
        String vector = NgramEmbedder.literal(embedder.embed(question));
        Map<String, KnowledgeRank.Candidate> merged = new LinkedHashMap<>();
        for (KnowledgeStore.Row row : store.nearest(vector, 8)) {
            merged.put(key(row), candidate(question, row, row.cosine()));
        }
        for (String phrase : KnowledgeRank.phrases(question)) {
            for (KnowledgeStore.Row row : store.containing(phrase)) {
                merged.merge(key(row), candidate(question, row, cosineOf(question, row)),
                        (left, right) -> left.cosine() >= right.cosine() ? left : right);
            }
        }
        KnowledgeRank.Pick pick = KnowledgeRank.pick(
                List.copyOf(merged.values()),
                properties.agent().knowledgeMinScore(),
                properties.agent().vectorMinScore());
        if (!pick.hit()) {
            return RetrievalResult.empty();
        }
        KnowledgeRank.Candidate chosen = pick.candidate();
        return new RetrievalResult(true, chosen.source(), chosen.heading(), chosen.content(), chosen.cosine());
    }

    public void add(String source, String heading, String content) {
        add(source, heading, content, null, null);
    }

    public void add(String source, String heading, String content, Instant validFrom, Instant validTo) {
        MarkdownChunker.Chunk chunk = new MarkdownChunker.Chunk(source, heading, content.trim());
        float[] vector = embedder.embed(chunk.heading() + "\n" + chunk.content());
        store.insert(new KnowledgeStore.Row(
                chunk.source(), chunk.heading(), chunk.content(), 0, NgramEmbedder.literal(vector),
                validFrom, validTo));
    }

    public void removeSource(String source) {
        store.deleteSource(source);
    }

    public List<Map<String, Object>> list(int limit) {
        return store.listAll(limit);
    }

    public String embedderName() {
        return embedder.name();
    }

    private List<KnowledgeStore.Row> toRows(List<MarkdownChunker.Chunk> chunks) {
        List<KnowledgeStore.Row> rows = new ArrayList<>();
        for (MarkdownChunker.Chunk chunk : chunks) {
            float[] vector = embedder.embed(chunk.heading() + "\n" + chunk.content());
            rows.add(new KnowledgeStore.Row(
                    chunk.source(), chunk.heading(), chunk.content(), 0, NgramEmbedder.literal(vector)));
        }
        return rows;
    }

    private double cosineOf(String question, KnowledgeStore.Row row) {
        return cosine(embedder.embed(question), embedder.embed(row.heading() + "\n" + row.content()));
    }

    private static double cosine(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        int n = Math.min(left.length, right.length);
        for (int i = 0; i < n; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / Math.sqrt(leftNorm * rightNorm);
    }

    private static KnowledgeRank.Candidate candidate(String question, KnowledgeStore.Row row, double cosine) {
        return new KnowledgeRank.Candidate(
                row.source(),
                row.heading(),
                row.content(),
                cosine,
                KnowledgeRank.phraseHits(question, row.heading(), row.content()));
    }

    private static String key(KnowledgeStore.Row row) {
        return row.source() + "\n" + row.heading();
    }

    public record RetrievalResult(boolean hit, String source, String heading, String content, double score) {
        public static RetrievalResult empty() {
            return new RetrievalResult(false, null, null, null, 0);
        }
    }
}
