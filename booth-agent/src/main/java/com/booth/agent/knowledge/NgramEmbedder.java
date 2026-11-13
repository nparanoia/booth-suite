package com.booth.agent.knowledge;

import java.util.Locale;

/**
 * 字符 n-gram 向量。入库和查询用同一套，没有模型密钥也能做相似度。
 * 维度固定 256，和 pgvector 列一致。
 */
public final class NgramEmbedder implements TextEmbedder {

    @Override
    public String name() {
        return "ngram";
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSION];
        String compact = compact(text);
        for (int i = 0; i + 2 <= compact.length(); i++) {
            add(vector, compact.substring(i, i + 2), 1f);
        }
        for (int i = 0; i + 3 <= compact.length(); i++) {
            add(vector, compact.substring(i, i + 3), 2f);
        }
        normalize(vector);
        return vector;
    }

    public double cosine(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < DIMENSION; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / Math.sqrt(leftNorm * rightNorm);
    }

    public static String literal(float[] vector) {
        StringBuilder builder = new StringBuilder(vector.length * 8);
        builder.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.US, "%.6f", vector[i]));
        }
        return builder.append(']').toString();
    }

    private static String compact(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\p{P}\\s]+", "");
    }

    private static void add(float[] vector, String gram, float weight) {
        int hash = gram.hashCode() & 0x7fffffff;
        int bucket = hash % DIMENSION;
        float sign = ((hash >>> 8) & 1) == 0 ? weight : -weight;
        vector[bucket] += sign;
    }

    private static void normalize(float[] vector) {
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= (float) norm;
        }
    }
}
