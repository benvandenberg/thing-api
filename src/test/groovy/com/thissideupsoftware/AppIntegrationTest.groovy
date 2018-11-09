package com.thissideupsoftware

import io.dropwizard.jackson.Jackson
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.junit.DropwizardAppRule
import org.glassfish.jersey.client.JerseyClientBuilder
import org.testcontainers.containers.localstack.LocalStackContainer
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class AppIntegrationTest extends Specification {

    @Shared
    LocalStackContainer localStackContainer

    @Shared
    DropwizardAppRule<AppConfiguration> dropwizardAppRule

    def setup() {
        localStackContainer = new LocalStackContainer().withServices(LocalStackContainer.Service.DYNAMODB)
        localStackContainer.start()

        String dynamoDbServiceEndpoint = localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.DYNAMODB).serviceEndpoint
        URL url = new URL(dynamoDbServiceEndpoint)

        dropwizardAppRule =
                new DropwizardAppRule<>(App.class,
                        'src/main/resources/application.yml',
                        // By setting the ports to 0, a random available port will be selected
                        ConfigOverride.config('server.applicationConnectors[0].port', '0'),
                        ConfigOverride.config('server.adminConnectors[0].port', '0'),
                        // Override the DynamoDb endpoint URL
                        ConfigOverride.config('localstack.serviceHost', url.getHost()),
                        ConfigOverride.config('localstack.serviceScheme', url.getProtocol()),
                        ConfigOverride.config('localstack.dynamoDbPort', url.getPort().toString())
                )
        dropwizardAppRule.before()
    }

    def cleanup() {
        dropwizardAppRule.after()
        localStackContainer.stop()
    }

    def 'test application startup'() {
        setup:
        Client client = new JerseyClientBuilder().build();

        when:
        Response response = client.target(
                String.format("http://localhost:%d/ping", dropwizardAppRule.getAdminPort()))
                .request()
                .get()
        String responseBody
        if (response != null) {
            responseBody = response.readEntity(String.class)
        }

        then:
        response != null
        response.status == 200
        responseBody != null
        responseBody.contains('pong')
    }

    def 'test Thing create and get'() {
        setup:
        Client client = new JerseyClientBuilder().build();
        Thing thing = Thing.builder()
                .data('{"foo":"bar"}')
                .build()

        when: 'create a Thing'
        Response response = client.target(
                String.format("http://localhost:%d/things", dropwizardAppRule.getLocalPort()))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(thing, MediaType.APPLICATION_JSON))
        String responseBody
        if (response != null) {
            responseBody = response.readEntity(String.class)
        }

        then:
        response != null
        response.status == 201
        responseBody != null

        when:
        Thing created = Jackson.newObjectMapper().readValue(responseBody, Thing.class)

        then:
        created.id != null
        println("Created ${responseBody}")

        when: 'retrieve a Thing'
        response = client.target(
                String.format("http://localhost:%d/things/%s", dropwizardAppRule.getLocalPort(), created.id))
                .request(MediaType.APPLICATION_JSON)
                .get()
        responseBody = null
        if (response != null) {
            responseBody = response.readEntity(String.class)
        }

        then:
        response != null
        response.status == 200
        responseBody != null

        when:
        Thing retrieved = Jackson.newObjectMapper().readValue(responseBody, Thing.class)

        then:
        retrieved == created
        println("Retrieved ${responseBody}")

        when: 'retrive a non-existent Thing'
        response = client.target(
                String.format("http://localhost:%d/things/%s", dropwizardAppRule.getLocalPort(), UUID.randomUUID().toString()))
                .request(MediaType.APPLICATION_JSON)
                .get()
        responseBody = null
        if (response != null) {
            responseBody = response.readEntity(String.class)
        }

        then:
        response != null
        response.status == 404
        if (responseBody != null) {
            responseBody.trim().empty
        }
    }
}
