package com.github.kafkacourse.tutorial4;

import com.google.gson.JsonParser;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class StreamFilterTweets {

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "demo-kafka-streams");
        properties.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        properties.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        KStream<String, String> inputTopics = streamsBuilder.stream("twitter_topics");

        KStream<String, String> filteredStream = inputTopics.filter(
                (k, jsonTweet) -> getUserFollowersNumber(jsonTweet) > 10000
        );

        filteredStream.to("important_tweets");

        KafkaStreams kafkaStreams = new KafkaStreams(
                streamsBuilder.build(),
                properties
        );

        kafkaStreams.start();
    }

    private static JsonParser jsonParser = new JsonParser();
    private static Integer getUserFollowersNumber(String value) {
        try {
            return jsonParser.parse(value)
                    .getAsJsonObject()
                    .get("user")
                    .getAsJsonObject()
                    .get("followers_count")
                    .getAsInt();
        }
        catch(NullPointerException e){
            return 0;
        }
    }
}
