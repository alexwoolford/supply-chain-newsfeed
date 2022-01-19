package io.woolford.comprehend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.woolford.entity.ArticleRecord;
import io.woolford.entity.EntityRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.ComprehendException;
import software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest;
import software.amazon.awssdk.services.comprehend.model.DetectEntitiesResponse;
import software.amazon.awssdk.services.comprehend.model.Entity;

import java.util.*;

@Component
public class ComprehendArticles {

    private final Logger LOG = LoggerFactory.getLogger(ComprehendArticles.class);

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "sc-article", groupId = "comprehend-articles")
    private void enrichArticles(String message) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        ArticleRecord article = mapper.readValue(message, ArticleRecord.class);

        Region region = Region.US_EAST_1;
        ComprehendClient comClient = ComprehendClient.builder()
                .region(region)
                .build();

        Map<String, List<String>> entities = detectAllEntities(comClient, article.getDescription());

        entities.forEach((entityType, entityList) -> {
            for (String entity: entityList) {

                EntityRecord entityRecord = new EntityRecord();
                entityRecord.setLink(article.getLink());
                entityRecord.setEntityType(capitalize(entityType));
                entityRecord.setEntityValue(entity);
                entityRecord.setTimestamp(article.getTimestamp());

                try {
                    String entityRecordJson = mapper.writeValueAsString(entityRecord);
                    kafkaTemplate.send("sc-article-entity", entityRecordJson);
                } catch (JsonProcessingException e) {
                    LOG.error(e.getMessage());
                }

                LOG.info("entityType: " + entityType + "; entity: " + entity);

            }
        });

    }

    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    private Map<String, List<String>> detectAllEntities(ComprehendClient comClient, String text ) throws JsonProcessingException {

        Map<String, List<String>> entities = new HashMap<>();

        try {
            DetectEntitiesRequest detectEntitiesRequest = DetectEntitiesRequest.builder()
                    .text(text)
                    .languageCode("en")
                    .build();

            DetectEntitiesResponse detectEntitiesResult = comClient.detectEntities(detectEntitiesRequest);
            List<Entity> entList = detectEntitiesResult.entities();

            for (Entity entity : entList) {

                // don't capture low-quality entities
                if (entity.score() > 0.8) {

                    String type = entity.type().toString().toLowerCase();
                    String entityValue = entity.text();

                    // exclude quantity, date
                    List<String> excludedEntityTypes = new ArrayList<>();
                    excludedEntityTypes.add("quantity");
                    excludedEntityTypes.add("date");

                    if (!excludedEntityTypes.contains(type)) {
                        entities.computeIfAbsent(type, k -> new ArrayList<>()).add(entityValue);
                    }

                    LOG.debug("Entity text is " + entity.text() + "; entity type is " + entity.type());

                }

            }

        } catch (ComprehendException e) {
            LOG.error(e.awsErrorDetails().errorMessage());
        }

        return entities;

    }

}
