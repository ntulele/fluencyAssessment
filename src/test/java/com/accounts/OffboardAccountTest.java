package com.accounts;

import static io.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.BaseTest;
import com.fasterxml.jackson.databind.JsonNode;

public class OffboardAccountTest extends BaseTest {

    @Test
    void offboarding_shouldYield_PausedAccount() throws IOException {
        JsonNode account = createUniqueCustomerAccount();
        int accountId = account.get("id").asInt();

        expect()
            .statusCode(200)
            .body("status", equalTo("PAUSED"))
        .when()
            .put("/accounts/{customerAccountId}/offboard", accountId)
        ;

        expect()
            .statusCode(200)
            .body("status", equalTo("PAUSED"))
            .body("removed", equalTo(false))
        .when()
            .get("/accounts/{customerAccountId}", accountId)
        ;
    }

    @Test
    void Offboarding_NonExistentAccount_shouldFailWith404() throws IOException {
        int unlikelyAccountId = -100;

        expect()
            .statusCode(404)
        .when()
            .put("/accounts/{customerAccountId}/offboard", unlikelyAccountId)
        ;
    }

}
