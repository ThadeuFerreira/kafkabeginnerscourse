package com.github.simplesteph.kafka.tutorial3;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ElasticSearchConsumer {

    private final static String BOOTSTRAP_SERVERS ="localhost:9092";

    private final static String groupId = "kafka-demo-elastic";



    private static KafkaConsumer<String, String> createConsumer(String topic){
        final Properties props = new Properties();

        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,
                groupId);
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10");

        // Create the consumer using props.
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // Subscribe to the topic.

        consumer.subscribe(Arrays.asList(topic));

        return consumer;
    }

    private static RestHighLevelClient createClient(){

        String hostname = "thadeu-kafka-6787423158.us-east-1.bonsaisearch.net";
        String username = "fb0x74taq6";
        String password = "80nhbd5b7u";

        final CredentialsProvider  credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username,password));

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });

        return  new RestHighLevelClient(builder);


    }

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class);
        RestHighLevelClient client = createClient();


        KafkaConsumer<String, String> consumer = createConsumer("twitter_tweets");

        while(true){
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            logger.info("Received " + records.count() + " records");
            for(ConsumerRecord<String, String> record : records){
                String id = getTweetId(record.value());
                IndexRequest indexRequest = new IndexRequest(
                        "twitter", "tweets", id
                ).source(record.value(), XContentType.JSON);
                IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);

                logger.info(indexResponse.getId());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logger.info("Committing offsets...");
            consumer.commitSync();
            logger.info("Offsets have been committed");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    private static JsonParser jsonParser = new JsonParser();
    private static String getTweetId(String value) {
        return jsonParser.parse(value)
                .getAsJsonObject()
                .get("id_str")
                .getAsString();
    }
}
