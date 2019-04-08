/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pedro.kafka.my.utils;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 *
 * @author PC
 */
public class DefaultKafkaProducer {
    
    public KafkaProducer<String,String> create(){
                // create properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                                                MyKafkaConstants.KAFKA_LOCATION);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                                            StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                StringSerializer.class.getName());
        
        // create producer 
        KafkaProducer<String, String> producer=new KafkaProducer<>(properties); 
        return producer;
    }
}
