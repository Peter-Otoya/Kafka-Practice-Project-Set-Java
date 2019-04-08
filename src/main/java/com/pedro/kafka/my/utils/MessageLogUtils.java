/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pedro.kafka.my.utils;

import java.util.StringJoiner;
import org.apache.kafka.clients.consumer.ConsumerRecord;





/**
 *
 * @author PC
 */
public class MessageLogUtils {

    public static String buildLogMessage(ConsumerRecord record) {
        return new StringJoiner("\n").add("\nRecieved Record")
                .add("Key:" + record.key())
                .add("Value:" + record.value())
                .add("Partition:" + record.partition())
                .add("Offset:" + record.offset())
                .add("Timestamp" + record.timestamp())
                .toString();
    }
}
