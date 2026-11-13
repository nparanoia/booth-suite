package com.booth.trade;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 需要本地 docker compose 环境。每个 Mapper 按实体的全部字段查一次，实体和表结构对不上时立即报错。
 * 运行：mvn -pl booth-trade -am test -Dtest=MapperSchemaIT -Dsurefire.failIfNoSpecifiedTests=false
 */
@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.register-enabled=false",
        "spring.task.scheduling.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class MapperSchemaIT {

    @Autowired
    private ApplicationContext context;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void everyMapperMatchesItsTable() {
        Map<String, BaseMapper> mappers = context.getBeansOfType(BaseMapper.class);
        assertThat(mappers).hasSize(14);
        mappers.forEach((name, mapper) -> assertThatCode(() -> mapper.selectList(Wrappers.query().last("LIMIT 1")))
                .as(name)
                .doesNotThrowAnyException());
    }
}