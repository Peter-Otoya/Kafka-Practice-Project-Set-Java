/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pedro.kafka.streams.twitter.filter;

import com.google.gson.JsonParser;
import com.pedro.kafka.my.utils.MyKafkaConstants;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author PC
 */
public class StreamsFilterTweets {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            StreamsFilterTweets.class.getName());
    public static final JsonParser JSON_PARSER = new JsonParser();

    public static void main(String[] args) {
        //create properties
        Properties props = new Properties();
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                MyKafkaConstants.KAFKA_LOCATION);
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG,
                "demo-kafka-streams");
        props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.StringSerde.class.getName());
        props.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.StringSerde.class.getName());

        //topology
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        String sourceTopic = "twitter_tweets";
        String sinkTopic = "important_tweets";

        //input topic -> transform ops -> output topic (similar to RDD)
        KStream<String, String> inputStream
                = streamsBuilder.stream(sourceTopic);
        KStream<String, String> filteredStream = inputStream.filter(
                (k, newTweet) -> {
                    // filter only tweets from users with more than 10,000
                    // followers (influencers)
                    return extractUserFollowersInTweet(newTweet) > 10000;
                });
        filteredStream.to(sinkTopic);

        //build the topology
        KafkaStreams kafkaStreams = new KafkaStreams(
                streamsBuilder.build(), props);

        //start our application
        kafkaStreams.start();
    }

    /**
     * @param newTweet
     * @return
     */
    private static int extractUserFollowersInTweet(String newTweet) {
        try {
            return JSON_PARSER.parse(newTweet)
                    .getAsJsonObject()
                    .get("user")
                    .getAsJsonObject()
                    .get("followers_count")
                    .getAsInt();
        } catch (Exception exception) {
            return 0;
        }
    }
}
