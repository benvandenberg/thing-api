package com.thissideupsoftware;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.waiters.FixedDelayStrategy;
import com.amazonaws.waiters.MaxAttemptsRetryStrategy;
import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.WaiterParameters;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class ThingDbService {

    public static final ThingDbService INSTANCE = new ThingDbService();

    private static final String THING_TABLE = "thing";
    private static final String THING_ID = "id";

    private AmazonDynamoDB amazonDynamoDB;
    private DynamoDBMapper dynamoDBMapper;
    private LocalstackConfiguration localstackConfiguration;

    public void init(LocalstackConfiguration localstackConfiguration) {
        this.localstackConfiguration = localstackConfiguration;
        createClient();
        createTable();
        createMapper();
    }

    private void createClient() {
        try {
            String serviceScheme = localstackConfiguration.getServiceScheme();
            String serviceHost = localstackConfiguration.getServiceHost();
            Integer dynamoDbPort = localstackConfiguration.getDynamoDbPort();
            String dynamoDbEndpointUrl = serviceScheme + "://" + serviceHost + ":" + dynamoDbPort;
            log.info("dynamoDbEndpointUrl={}, awsRegion={}", dynamoDbEndpointUrl, localstackConfiguration.getAwsRegion());

            AmazonDynamoDBClientBuilder clientBuilder = AmazonDynamoDBClientBuilder.standard();
            clientBuilder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                            dynamoDbEndpointUrl,
                            localstackConfiguration.getAwsRegion())
            ).withClientConfiguration(new ClientConfiguration().withConnectionTimeout(30));
            amazonDynamoDB = clientBuilder.build();
            log.info("DynamoDB client created");
        } catch (Exception e) {
            log.warn("Failed to create DynamoDB client", e);
        }
    }

    private void createTable() {
        try {
            log.info("Checking for DynamoDB table {}", THING_TABLE);

            // Use a "waiter", which waits on the condition.
            // This will throw an exception if connectivity to the DB is not possible.
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(THING_TABLE);
            WaiterParameters waiterParameters = new WaiterParameters(describeTableRequest).withPollingStrategy(
                    new PollingStrategy(new MaxAttemptsRetryStrategy(30), new FixedDelayStrategy(1)));
            amazonDynamoDB.waiters().tableNotExists().run(waiterParameters);

            log.info("Creating {} DynamoDB table", THING_TABLE);
            AttributeDefinition attributeDefinition = new AttributeDefinition()
                    .withAttributeName(THING_ID)
                    .withAttributeType(ScalarAttributeType.S);

            KeySchemaElement keySchemaElement = new KeySchemaElement().withAttributeName(THING_ID).withKeyType(KeyType.HASH);

            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(THING_TABLE)
                    .withAttributeDefinitions(attributeDefinition)
                    .withKeySchema(keySchemaElement)
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(10L)
                            .withWriteCapacityUnits(10L));

            amazonDynamoDB.createTable(request);
            log.info("DynamoDB table {} was created", THING_TABLE);
        } catch (Exception e) {
            log.warn("Failed to create DynamoDB table", e);
        }
    }

    private void createMapper() {
        try {
            log.info("Creating DynamoDB mapper");
            DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig.Builder()
                    .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                    .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                    .withPaginationLoadingStrategy(DynamoDBMapperConfig.PaginationLoadingStrategy.LAZY_LOADING)
                    .build();

            dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, mapperConfig);
            log.info("DynamoDB mapper created");
        } catch (Exception e) {
            log.warn("Failed to create DynamoDB mapper", e);
        }
    }

    public Thing save(Thing thing) {
        dynamoDBMapper.save(thing);
        log.info("Saved thing with id " + thing.id);
        return thing;
    }

    public Thing get(UUID id) {
        return dynamoDBMapper.load(Thing.class, id);
    }
}
