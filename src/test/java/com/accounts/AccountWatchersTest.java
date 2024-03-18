package com.accounts;

import com.BaseTest;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class AccountWatchersTest extends BaseTest {

    @Test
    void addingTwoAccountWatchers_shouldSucceed() throws IOException {
        JsonNode account1 = createUniqueCustomerAccount();
        int account1Id = account1.get("id").asInt();
        int uuid = account1.get("customerAccountUuid").asInt();

        String watcher1 = "qaAccount1_" + uuid + "@fluency.inc";
        String watcher2 = "qaAccount2_" + uuid + "@fluency.inc";

        ObjectNode watchers = new ObjectNode(null);
        watchers.putArray("customerAccountIds")
                .add(account1Id);
        watchers.putArray("watcherUsernames")
                .add(watcher1).add(watcher1);

        given()
            .body(watchers)
        .expect()
            .statusCode(200)
            .body("customerAccountIds", hasItem(account1Id))
            .body("watcherUsernames", hasItems(watcher1, watcher2))
        .when()
            .patch("/accounts/{customerAccountId}/watchers", account1Id)
        ;
    }

    @Test
    void deleteAllWatchers_fromAccount_shouldSucceed() throws IOException {
        JsonNode account = createUniqueCustomerAccount();
        int accountId = account.get("id").asInt();

        ObjectNode zeroWatchers = new ObjectNode(null);
        zeroWatchers.putArray("customerAccountIds");
        zeroWatchers.putArray("watcherUsernames");

        given().body(zeroWatchers)
        .expect().statusCode(200)
        .when().delete("/accounts/{customerAccountId}/watchers", accountId)
        ;

        //TODO: determine a way to verify Delete. 
        // Could do an updateWatchers and check exact content -- but could be that way accidentally

    }


    @AfterAll
    public static void cleanup() {
        /* possibly remove all created Watchers if necessary
        (but avoid possible collision with other test runs) 
        */
    }



}
