ARG ES_VERSION=8.17.0
FROM elasticsearch:${ES_VERSION}

# IK 分词插件版本必须与 ES 版本完全一致。
# 网络不通时，把 elasticsearch-analysis-ik-${ES_VERSION}.zip 放到 plugins/ 目录即可离线安装
ARG ES_VERSION
COPY plugins/ /tmp/plugins/
RUN if [ -f "/tmp/plugins/elasticsearch-analysis-ik-${ES_VERSION}.zip" ]; then \
        bin/elasticsearch-plugin install --batch "file:///tmp/plugins/elasticsearch-analysis-ik-${ES_VERSION}.zip"; \
    else \
        bin/elasticsearch-plugin install --batch "https://get.infini.cloud/elasticsearch/analysis-ik/${ES_VERSION}"; \
    fi
