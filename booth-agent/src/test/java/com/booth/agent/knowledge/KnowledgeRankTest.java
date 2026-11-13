package com.booth.agent.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeRankTest {

    private static final String POLICY = """
            # 退换货政策

            ## 七天无理由退货

            已签收的商品，自签收之日起 7 天内，可申请七天无理由退货。商品需未使用、附件齐全、不影响二次销售。

            ## 质量问题退货

            收到商品存在质量问题，可在签收后 15 天内申请退货或换货。
            """;

    @Test
    void sevenDayQuestionHitsByPhraseWhileUnknownPolicyDoesNot() {
        List<MarkdownChunker.Chunk> chunks = MarkdownChunker.split("return-policy.md", POLICY);
        MarkdownChunker.Chunk seven = chunks.stream()
                .filter(chunk -> chunk.heading().contains("七天无理由"))
                .findFirst()
                .orElseThrow();

        assertTrue(KnowledgeRank.phraseHits("七天无理由怎么退", seven.heading(), seven.content()) >= 1);
        assertTrue(KnowledgeRank.phraseHits("7 天无理由怎么退", seven.heading(), seven.content()) >= 1);
        assertEquals(0, KnowledgeRank.phraseHits("怎么退", seven.heading(), seven.content()));
        assertEquals(0, KnowledgeRank.phraseHits("退货政策里的海外仓怎么退", seven.heading(), seven.content()));
        assertTrue(KnowledgeRank.phrases("退货政策 内部备忘").contains("内部备忘"));

        NgramEmbedder embedder = new NgramEmbedder();
        float[] sevenVector = embedder.embed(seven.heading() + "\n" + seven.content());
        double asked = embedder.cosine(embedder.embed("七天无理由怎么退"), sevenVector);
        double overseas = embedder.cosine(embedder.embed("退货政策里的海外仓怎么退"), sevenVector);
        double shortQuestion = embedder.cosine(embedder.embed("怎么退"), sevenVector);
        assertTrue(asked > overseas, "七天 " + asked + " 海外仓 " + overseas);
        assertTrue(overseas < 0.72, "海外仓余弦 " + overseas + " 不能达到向量阈值");
        assertTrue(shortQuestion < 0.72, "怎么退余弦 " + shortQuestion + " 不能达到向量阈值");
    }
}
