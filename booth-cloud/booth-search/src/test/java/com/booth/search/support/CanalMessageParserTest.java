package com.booth.search.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanalMessageParserTest {

    @Test
    void readsProductSkuBrandAndSkipsDdl() {
        String body = """
                [
                  {"table":"product","isDdl":false,"data":[{"id":"15"},{"id":"16"}]},
                  {"table":"sku","isDdl":false,"data":[{"product_id":"15"}]},
                  {"table":"brand","isDdl":false,"data":[{"id":"3"}]},
                  {"table":"category","isDdl":true,"data":[{"id":"9"}]}
                ]
                """;

        List<CanalMessageParser.CanalChange> changes = CanalMessageParser.parse(body);

        assertThat(changes).hasSize(3);
        assertThat(changes.get(0).productIds()).containsExactly(15L, 16L);
        assertThat(changes.get(1).productIds()).containsExactly(15L);
        assertThat(changes.get(2).brandIds()).containsExactly(3L);
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> CanalMessageParser.parse("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
