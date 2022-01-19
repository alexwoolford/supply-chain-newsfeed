package io.woolford.newsfeed;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;

@Component
public class ArticleUrlCache {

    private final Logger LOG = LoggerFactory.getLogger(ArticleUrlCache.class);

    @Autowired
    public void cacheArticleUrls(StreamsBuilder builder) {

        KStream<String, String> scArticleStream = builder.stream("sc-article", Consumed.with(Serdes.String(), Serdes.String()));
        scArticleStream.groupByKey().count(Materialized.as("url-cache-store"));

    }

    @Bean
    NewTopic scArticle() {
        return TopicBuilder.name("sc-article")
                .partitions(1)
                .replicas(3)
                .compact()
                .build();
    }

    @Bean
    NewTopic scArticleEntity() {
        return TopicBuilder.name("sc-article-entity")
                .partitions(1)
                .replicas(3)
                .build();
    }

}
