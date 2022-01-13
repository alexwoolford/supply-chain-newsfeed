package io.woolford.newsfeed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import io.woolford.entity.ArticleRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Component
public class Republisher {

    private final Logger LOG = LoggerFactory.getLogger(Republisher.class);

    @Autowired
    private RssFeedUrlListProperties rssFeedUrlListProperties;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    // poll feeds every 5 minutes
    @Scheduled(cron="0 */5 * * * *")
    private void consumeRssFeed() throws JsonProcessingException {

        LOG.info("Polling RSS feeds.");

        for (String url: rssFeedUrlListProperties.getUrls()) {
            List<SyndEntry> rssEntries = getRssEntries(url);
            for (SyndEntry rssEntry: rssEntries){

                ArticleRecord article = new ArticleRecord();
                article.setLink(rssEntry.getLink());
                article.setTitle(rssEntry.getTitle());
                article.setDescription(html2text(rssEntry.getDescription().getValue()));
                article.setTimestamp(new Date());

                ObjectMapper mapper = new ObjectMapper();
                String articleJson = mapper.writeValueAsString(article);

                // don't publish to Kafka if it's already been put in the topic
                if (Boolean.FALSE.equals(redisTemplate.hasKey(article.getLink()))){
                    ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>("sc-article", article.getLink(), articleJson);
                    kafkaTemplate.send(producerRecord);
                    redisTemplate.opsForValue().set(article.getLink(), articleJson);
                    LOG.info(article.toString());
                }

            }
        }

    }

    private List<SyndEntry> getRssEntries(String url) {

        List<SyndEntry> syndEntries = new ArrayList<>();
        try (XmlReader reader = new XmlReader(new URL(url))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            syndEntries = feed.getEntries();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return syndEntries;

    }

    private static String html2text(String html) {
        return Jsoup.parse(html).text();
    }

}
