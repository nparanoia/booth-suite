package com.booth.agent.knowledge;

/** 入库和查询用同一套向量。维度必须和 pgvector 列一致。 */
public interface TextEmbedder {

    int DIMENSION = 256;

    float[] embed(String text);

    String name();
}
