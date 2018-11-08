package com.thissideupsoftware;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.*;
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
        String serviceScheme = localstackConfiguration.getServiceScheme();
        String serviceHost = localstackConfiguration.getServiceHost();
        Integer dynamoDbPort = localstackConfiguration.getDynamoDbPort();
        String dynamoDbEndpointUrl = serviceScheme + "://" + serviceHost + ":" + dynamoDbPort;
        log.info("dynamoDbEndpointUrl=" + dynamoDbEndpointUrl);

        AmazonDynamoDBClientBuilder clientBuilder = AmazonDynamoDBClientBuilder.standard();
        clientBuilder.withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(
                        dynamoDbEndpointUrl,
                        localstackConfiguration.getAwsRegion())
        );
        amazonDynamoDB = clientBuilder.build();
    }

    private void createTable() {
        ListTablesResult tablesResult = amazonDynamoDB.listTables();
        if (!tablesResult.getTableNames().contains(THING_TABLE)) {
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
            log.info("Table {} was created", THING_TABLE);
        }
    }

    private void createMapper() {
        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig.Builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .withPaginationLoadingStrategy(DynamoDBMapperConfig.PaginationLoadingStrategy.LAZY_LOADING)
                .build();

        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, mapperConfig);
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
