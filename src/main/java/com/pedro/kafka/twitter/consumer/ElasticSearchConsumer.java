/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pedro.kafka.twitter.consumer;

import com.google.gson.JsonParser;
import com.pedro.kafka.my.utils.MyKafkaConstants;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author PC
 */
public class ElasticSearchConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            ElasticSearchConsumer.class.getName());
    public static final JsonParser JSON_PARSER = new JsonParser();

    /**
     * Main method.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Create kafka consumer
        final String groupId = "kafka-demo-elasticsearch";
        final String topic = "twitter_tweets";
        KafkaConsumer<String, String> kafkaConsumer
                = createKafkaConsumer(groupId, topic);

        // Create Elastic Search Client
        RestHighLevelClient client = createClient();

        //poll for new data in topic and send to elastic search
        while (true) {
            pollAndWrite(kafkaConsumer, client); // Batch by batch (reads N - Writes N)
        }

        //client.close();
    }

    /**
     * Polls data from a kafka consumer topic and pushes dumps to Elastic.
     *
     * @param kafkaConsumer
     * @param client
     * @throws IOException
     */
    private static void pollAndWrite(KafkaConsumer kafkaConsumer,
            RestHighLevelClient client) throws IOException {
        ConsumerRecords<String, String> records
                = kafkaConsumer.poll(Duration.ofMillis(100));

        int recordCount = records.count();
        LOGGER.info("Recieved " + recordCount + " records");

        BulkRequest bulkRequest = new BulkRequest();
        for (ConsumerRecord<String, String> record : records) {
            // Insert data to elastic search
            try {
                String newTweet = record.value();

                String kid = getIdToMakeIdempotent(newTweet);

                // Insert data into elastic search
                IndexRequest indexRequest
                        = new IndexRequest("twitter", "tweets", kid)
                                .source(newTweet, XContentType.JSON);

                bulkRequest.add(indexRequest);
            } catch (NullPointerException e) {
                LOGGER.warn("Skipping bad data: " + record.value());
            }

        }

        if (recordCount > 0) {
            BulkResponse bulkItemResponses = client.bulk(bulkRequest,
                    RequestOptions.DEFAULT);

            LOGGER.info("Commiting offsets..");
            kafkaConsumer.commitSync();
            LOGGER.info("Offsets have been commited...");
        }

    }

    /**
     * Creates elastic search rest client
     *
     * @return
     */
    public static RestHighLevelClient createClient() {

        String hostname = "pedro-elastic-search-2759418908.eu-west-1.bonsaisearch.net";
        String username = "66amo6188g";
        String password = "tnclrzlfzv";

        final CredentialsProvider credentialsProvider
                = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback((hacb) -> {
                    return hacb.setDefaultCredentialsProvider(
                            credentialsProvider);
                });
        return new RestHighLevelClient(builder);
    }

    /**
     * Creates a new kafka consumer.
     *
     * @param groupId
     * @param topic
     * @return
     */
    public static KafkaConsumer<String, String>
            createKafkaConsumer(String groupId, String topic) {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                MyKafkaConstants.KAFKA_LOCATION);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                "false");
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                "100");

        //Create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topic));
        return consumer;
    }

    /**
     * Assists with tweet id extraction
     *
     * This kid is used to save tweet in Elastic Search. IDEMPOTENT is
     * guarranteed; if same tweet gets processed twice, then elastic search will
     * recognize is the same tweetid and reject the insert. (resopnse status
     * 400) Twitter feed specific id
     *
     * @param newTweet
     * @return
     */
    private static String getIdToMakeIdempotent(String newTweet) {
        return JSON_PARSER.parse(newTweet)
                .getAsJsonObject()
                .get("id_str")
                .getAsString();
    }

}
