package com.accounts;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.BaseTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CreateOrUpdateCustomerAccountTest extends BaseTest {

    @BeforeAll
    static void setup() {
        accountsSetup();
    }

    @Test
    void usingValidUniqueValues_shouldCreateEnabledAccount() throws IOException {
        JsonNode newAccountJson = getUniqueCustomerAccountJson();
        String customerUuid = newAccountJson.get("customerAccountUuid").asText();

        JsonNode createResponseJson = 
            given()
                .body(newAccountJson)
            .expect() 
                .statusCode(CREATEACCOUNT_RESPONSECODE)
                .body("customerAccountUuid", equalTo(customerUuid))
                .body("customerId", equalTo(TEST_CUSTOMERID))
                .body("removed",is(false))
                .body(matchesJsonSchemaInClasspath("customerAccountSchema.json"))
            .when()
                .post("/accounts")
                .as(JsonNode.class)
        ;

        //Also verify directly, aside from response to request:\
        int id = createResponseJson.get("id").asInt();
        expect()
            .statusCode(200)
            .body("status", equalTo("ENABLED"))
            .body("customerAccountUuid", equalTo(customerUuid))
            .body("customerId", equalTo(TEST_CUSTOMERID))
        .when()
            .get("/accounts/{customerAccountId}", id)
        ;
    }

    /**
     * Requirements (api doc) state that any supplied customerId and Id values will be ignored.
     * And of course the account should be created only for the API user's customerId
     */
    @Test
    void usingAnothersCustomerId_AndUnlikelyId_shouldCreateAccountForCorrectCustomer() throws IOException {
        JsonNode newAccountJson = getUniqueCustomerAccountJson();
        String customerUuid = newAccountJson.get("customerAccountUuid").asText();
        ((ObjectNode) newAccountJson).put("customerId", VALID_OTHER_CUSTOMERID);
        int validButUnlikelyIdValue = -1;
        ((ObjectNode) newAccountJson).put("id", validButUnlikelyIdValue);
        
        JsonNode createResponseJson = 
            given()
                .body(newAccountJson)
            .expect() 
                .statusCode(CREATEACCOUNT_RESPONSECODE)
                .body("customerAccountUuid", equalTo(customerUuid))
                .body("customerId", equalTo(TEST_CUSTOMERID))
                .body("id", not(validButUnlikelyIdValue))
                .body(matchesJsonSchemaInClasspath("customerAccountSchema.json"))
            .when()
                .post("/accounts")
                .as(JsonNode.class)
        ;

        //Also verify directly, aside from response to request:
        int id = createResponseJson.get("id").asInt();
        expect()
            .statusCode(200)
            .body("status", equalTo("ENABLED"))
            .body("customerAccountUuid", equalTo(customerUuid))
            .body("customerId", equalTo(TEST_CUSTOMERID))
        .when()
            .get("/accounts/{customerAccountId}", id)
        ;
    }

    /*
     * For efficiency, this tests verifies both and UpdateCustomerAccount example, 
     * and the getCustomerAccountById endpoint's includeRemovedAccountPlans queryParam
     */
    @Test
    void updateCustomerAccount_shouldYieldRemovedAccount_NotRetrievableByDefaultGetAccount() throws IOException {
        JsonNode account = createUniqueCustomerAccount();
        int accountId = account.get("id").asInt();

        //modify json payload:
        ((ObjectNode) account).put("removed", true);

        given()
            .body(account)
        .expect()
            .statusCode(200)
            .body("removed", equalTo(true))
        .when()
            .patch("/accounts/{customerAccountId}", accountId)
            .as(JsonNode.class)
        ;

        //retrieval should fail when includeRemovedAccountPlans left false (default)
        expect().statusCode(404)
        .when().get("/accounts/{customerAccountId}", accountId);

        //retrieval by Id should succeed when includeRemovedAccountPlans=true
        //TODO: verify other values that may be required to change, e.g. status
        given()
            .queryParam("includeRemovedAccountPlans", "true")
        .expect()
            .statusCode(200)
            .body("removed", equalTo(true))
        .when()
            .get("/accounts/{customerAccountId}", accountId)
        ;
    }

    @Test
    void createAccount_withExistingValues_shouldReturn409() throws IOException {
        JsonNode existingAccountJson = createUniqueCustomerAccount();
        given().body(existingAccountJson)
        .expect().statusCode(DUPLICATE_RECORD_RESPONSECODE)
        .when().post("/accounts")
        ;
    }

    @Test
    void updating_BudgetToZero_withEnforcementTrue_shouldHaltSpend() throws IOException {
        JsonNode account = createUniqueCustomerAccount();
        int accountId = account.get("id").asInt();
        
        //modify json payload:
        ((ObjectNode) account).put("approvedAccountBudget", 0); 
        ((ObjectNode) account).put("overspendProtectionEnabled", true); 

        given()
            .body(account)
        .expect()
            .statusCode(200)
            .body("approvedAccountBudget", equalTo(0))
            .body("overspendProtectionEnabled", is(true))
        .when()
            .patch("/accounts/{customerAccountId}", accountId)
        ;

        assertTrue(isSpendPaused(accountId));
    }

}
