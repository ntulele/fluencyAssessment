package com;

import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static org.hamcrest.Matchers.lessThan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;

public class BaseTest {
    protected static final long MAX_RESPONSE_TIME = 5000L; //Intentionally long to allow for latency due to JVM warmup

    /* Test customers: Determine actual value for single test user; 
       BETTER: if able to dynamically create them using backend API or DB queries, do that 
              (and provide util method here for test classes to use)
    */ 
    protected static final int TEST_CUSTOMERID = 1;        
    protected static final int VALID_OTHER_CUSTOMERID = 2;

    //Expected http response codes: (subject to agreement with requirement owners)
    protected static final int CREATEACCOUNT_RESPONSECODE = 201;
    protected static final int DUPLICATE_RECORD_RESPONSECODE = 409;

    
    private static String getBearerToken() {
        return "fluency-is-so-cool-that-testing-is-made-easy-with-permanent-auth-token";
        // TODO: Actually fetch auth token for static or dynamically fetch new test customers
        //       If can't, probably move static value to config file
    }

    /**
     * Setup RestAssured config for use throughout test suite
     */
    @BeforeAll
    private static void setupRestAssured() {

        //TODO: accomodate different environments: QA, Staging, Canary?  i.e. choose domain based on env token on execution from CI, 
        RestAssured.baseURI = "https://backpack-staging.fluencyinc.co/"; 
        RestAssured.basePath = "/api/v2";

        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        RestAssured.config = RestAssured.config().
            jsonConfig(jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL)).
            sslConfig(new SSLConfig().allowAllHostnames());

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .addHeader("Content-Type", ContentType.JSON.toString())
            .addHeader("Accept", ContentType.JSON.toString())
            .addHeader("Authorization", "Bearer " + getBearerToken())
            .build();

        //This will be the default response spec; it can be overridden at test level by supplying alternate responseSpec
        RestAssured.responseSpecification = new ResponseSpecBuilder()
            .expectResponseTime(lessThan(MAX_RESPONSE_TIME))
            .build();
        //Note: intentionally *not* expecting 200 response code for all requests

        //TODO: Possibly dynamically fetch & set things like VALID_OTHER_CUSTOMERID

    }

    /**
     * 
     * @return JsonNode for use with CreateCustomerAccount, with unique customerAccountUuid
     * @throws IOException
     */
    protected JsonNode getUniqueCustomerAccountJson() throws IOException {
        String testAccount = "src\\test\\resources\\customerAccountTemplate.json";
        byte[] json = Files.readAllBytes(Paths.get(testAccount));
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode newAccountJson = objectMapper.readTree(json); 

        //ensure account is unique and thus acceptable as a new one 
        UUID uuid = UUID.randomUUID();
        ((ObjectNode) newAccountJson).put("customerAccountUuid", uuid.toString());
        //TODO: Determine if additional fields need to be unique (e.g. name) 

        return newAccountJson;
    }

    /*
     * Minimal validation here - that's covered in a separate test
     */
    protected JsonNode createUniqueCustomerAccount() throws IOException {
        JsonNode newAccountJson = getUniqueCustomerAccountJson();
        JsonNode createdAccountJson= 
            given().body(newAccountJson)
            .expect().statusCode(CREATEACCOUNT_RESPONSECODE)
            .when().post("/accounts").as(JsonNode.class)
        ;
        return createdAccountJson;
    }

    protected boolean isSpendPaused(int customerAccountId) {
        //TODO: implement, perhaps via an internally exposed API??
        return false;
    }

    @AfterAll
    protected static void accountsSetup() {
        /* TODO: Remove all created accounts in case of one static test customer.
         *  Main goal is to avoid Test Customer bloat (which increases latency unrealistically
         *  & complicates debugging)
         *  Perhaps using internal API?  Else DB queries   
         *  Hopefully do not also need to manually remove associated graph of related data
        */
    }

}