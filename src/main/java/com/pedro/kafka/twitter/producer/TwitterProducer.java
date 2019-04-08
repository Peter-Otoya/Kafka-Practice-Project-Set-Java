/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pedro.kafka.twitter.producer;

import com.google.common.collect.Lists;
import com.pedro.kafka.my.utils.MyKafkaConstants;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author PC
 */
public class TwitterProducer {

    public static final Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());
    
    public static final String TWITTER_CONSUMER_KEY = "vnuOD1LUHDholS0OLjdQXCzFK";
    public static final String TWITTER_CONSUMER_SECRET = "4psHwYzkLqHPffKX9BeUmM7oAluwjnqLY3KBL2RhG5XaSJocoZ";
    public static final String TWITTER_TOKEN = "1082383482653945856-JzmLliREisSYoQI35uQUzuOcV6KX54";
    public static final String TWITTER_SECRET = "OgX9qK0T0TenTShhd6IISAm1r4bzBcEQPQQmWo7es8uZ7";
    
    public static void main(String[] args) {
        new TwitterProducer().run();
    }

    public void run() {
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(1000);

        // Create twitter client
        Client client = createTwitterClient(msgQueue);
        client.connect();

        // Create kafka producer
        KafkaProducer<String,String> producer = creatKafkaProducer();
        
        // loop to send tweets to kafka
        // on a different thread, or multiple different threads....
        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                logger.error("m",ex);
                client.stop();
            }
            if (msg != null) {
                logger.info(msg);
                producer.send(
                        new ProducerRecord<>("twitter_tweets", null, msg),
                        (rm, excptn) -> {
                            if(excptn!=null){
                                logger.error("Something bad happened", excptn);
                            }
                        });
            }
        }
    }

    public Client createTwitterClient(BlockingQueue<String> msgQueue) {//Set up your blocking queues
        /**
         * Set up your blocking queues: Be sure to size these properly based on
         * expected TPS of your stream
         */
        BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(1000);

        /**
         * Declare the host you want to connect to, the endpoint, and
         * authentication (basic auth or oauth)
         */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        // Optional: set up some followings and track terms
        List<Long> followings = Lists.newArrayList(1234L, 566788L);
        List<String> terms = Lists.newArrayList(
                "bitcoin", "usa", "politics", "sport");
        hosebirdEndpoint.followings(followings);
        hosebirdEndpoint.trackTerms(terms);

        Authentication hosebirdAuth = new OAuth1(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET, TWITTER_TOKEN, TWITTER_SECRET);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01") // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue))
                .eventMessageQueue(eventQueue); // optional: use this if you want to process client events

        Client hosebirdClient = builder.build();

        return hosebirdClient;
    }

    private KafkaProducer<String, String> creatKafkaProducer() {
                // create properties
        Properties properties = new Properties();
        properties.setProperty(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                MyKafkaConstants.KAFKA_LOCATION);
        properties.setProperty(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                 StringSerializer.class.getName());
        properties.setProperty(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                StringSerializer.class.getName());
        
        // Make Safer Producer
        //(Best config for balance between performance and consistency)
        properties.setProperty(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(
                ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
        
        // High throughput producer(at the expense of a bit latency and CPU)
        properties.setProperty(
                ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(
                ProducerConfig.LINGER_MS_CONFIG, "20");
        properties.setProperty(
                ProducerConfig.BATCH_SIZE_CONFIG, String.valueOf(32*1024)); //32 KB        
        
        // create producer 
        KafkaProducer<String, String> producer=new KafkaProducer<>(properties); 
        return producer;
    }

}
