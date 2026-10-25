package com.navan.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TransactionApiTest extends ApiTestSupport {

    @Test
    void createsEveryTypeAndRoundTripsTypedMetadata() {
        JsonNode flight = get("/transactions/" + createTransaction(seed("flight"))).getBody();
        assertThat(flight.get("type").asText()).isEqualTo("flight");
        assertThat(flight.at("/metadata/origin").asText()).isEqualTo("TXL");
        assertThat(flight.at("/metadata/coupons/0/flight_number").asText()).isEqualTo("BA982");
        assertThat(flight.at("/tax_lines/0/name").asText()).isEqualTo("Airport tax");
        assertThat(flight.at("/fee_lines/0/name").asText()).isEqualTo("YQ");

        JsonNode hotel = get("/transactions/" + createTransaction(seed("hotel"))).getBody();
        assertThat(hotel.at("/metadata/name").asText()).isEqualTo("Hotel Mitte London");
        assertThat(hotel.at("/metadata/address/street").asText()).isEqualTo("12 Kingsway");
        assertThat(hotel.at("/metadata/address/city").asText()).isEqualTo("London");
        assertThat(hotel.at("/metadata/address/country").asText()).isEqualTo("GB");
        assertThat(money(hotel.at("/tax_lines/0/rate"))).isEqualByComparingTo("0.07");

        JsonNode rail = get("/transactions/" + createTransaction(seed("rail"))).getBody();
        assertThat(rail.at("/metadata/legs")).hasSize(2);
        assertThat(rail.at("/metadata/legs/1/origin").asText()).isEqualTo("Lille Europe");
        assertThat(rail.at("/metadata/legs/1/class").asText()).isEqualTo("standard");

        JsonNode tripFee = get("/transactions/" + createTransaction(seed("trip_fee"))).getBody();
        assertThat(tripFee.at("/metadata/fee_kind").asText()).isEqualTo("trip_fee");

        JsonNode callFee = get("/transactions/" + createTransaction(seed("agent_call_fee"))).getBody();
        assertThat(callFee.at("/metadata/fee_kind").asText()).isEqualTo("agent_call_fee");
    }

    @Test
    void duplicateExternalIdReturns409WithExistingIdAndInsertsNothing() {
        ObjectNode flight = seed("flight");
        long id = createTransaction(flight);

        ObjectNode duplicate = seed("duplicate_flight");
        duplicate.put("external_id", flight.get("external_id").asText());
        ResponseEntity<JsonNode> second = post("/transactions", duplicate);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().get("existing_id").asLong()).isEqualTo(id);
        assertThat(countByExternalId(flight.get("external_id").asText())).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateCreateLeavesExactlyOneRow() throws Exception {
        ObjectNode body = seed("flight");
        int clients = 8;
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch start = new CountDownLatch(1);
        List<ResponseEntity<JsonNode>> responses = new ArrayList<>();
        try {
            List<Future<ResponseEntity<JsonNode>>> futures = new ArrayList<>();
            for (int i = 0; i < clients; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return post("/transactions", body);
                }));
            }
            start.countDown();
            for (Future<ResponseEntity<JsonNode>> f : futures) {
                responses.add(f.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        List<ResponseEntity<JsonNode>> created = responses.stream()
                .filter(r -> r.getStatusCode() == HttpStatus.CREATED).toList();
        assertThat(created).hasSize(1);
        long id = created.getFirst().getBody().get("id").asLong();
        assertThat(responses).filteredOn(r -> r.getStatusCode() != HttpStatus.CREATED)
                .allSatisfy(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(r.getBody().get("existing_id").asLong()).isEqualTo(id);
                });
        assertThat(countByExternalId(body.get("external_id").asText())).isEqualTo(1);
    }

    @Test
    void unknownTypeOrFeeKindIsRejected() {
        ObjectNode unknownType = seed("flight");
        unknownType.put("type", "boat");
        assertThat(post("/transactions", unknownType).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ObjectNode unknownKind = seed("trip_fee");
        ((ObjectNode) unknownKind.get("metadata")).put("fee_kind", "lounge_fee");
        assertThat(post("/transactions", unknownKind).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ObjectNode emptyMetadata = seed("hotel");
        emptyMetadata.putObject("metadata");
        assertThat(post("/transactions", emptyMetadata).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void negativeAmountsAreRejected() {
        ObjectNode negativeTotal = seed("flight");
        negativeTotal.put("total", -500);
        assertThat(post("/transactions", negativeTotal).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ObjectNode negativeTax = seed("flight");
        ((ObjectNode) negativeTax.at("/tax_lines/0")).put("amount", -1);
        assertThat(post("/transactions", negativeTax).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ObjectNode negativeFare = seed("flight");
        ((ObjectNode) negativeFare.get("metadata")).put("fare", -1);
        assertThat(post("/transactions", negativeFare).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void metadataMoneyIsReturnedWithTwoDecimals() {
        long flightId = createTransaction(seed("flight"));
        JsonNode flight = get("/transactions/" + flightId).getBody();
        JsonNode hotel = get("/transactions/" + createTransaction(seed("hotel"))).getBody();

        assertThat(flight.at("/metadata/fare").asText()).isEqualTo("180.00");
        assertThat(hotel.at("/metadata/room_rate_per_night").asText()).isEqualTo("140.00");
        assertThat(hotel.at("/metadata/extras/0/amount").asText()).isEqualTo("30.00");

        JsonNode invoice = get("/invoices/" + createInvoice(flightId)).getBody();
        assertThat(invoice.at("/lines/0/metadata/fare").asText()).isEqualTo("180.00");
    }

    @Test
    void getMissingTransactionReturns404() {
        assertThat(get("/transactions/999999999").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listIsPaginatedAndBounded() {
        for (int i = 0; i < 51; i++) {
            createTransaction(seed("trip_fee"));
        }

        ResponseEntity<JsonNode> firstPage = get("/transactions");
        assertThat(firstPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstPage.getBody().get("items")).hasSize(50);
        assertThat(firstPage.getBody().get("next_cursor").isNull()).isFalse();

        long cursor = firstPage.getBody().get("next_cursor").asLong();
        JsonNode secondPage = get("/transactions?limit=1&after=" + cursor).getBody();
        assertThat(secondPage.get("items")).hasSize(1);
        assertThat(secondPage.at("/items/0/id").asLong()).isGreaterThan(cursor);

        assertThat(get("/transactions?limit=1000").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void patchAndDeleteWorkWhileNotInvoiced() {
        long id = createTransaction(seed("rail"));

        ResponseEntity<JsonNode> patched = patch("/transactions/" + id, MAPPER.createObjectNode().put("total", "95.00"));
        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(money(patched.getBody().get("total"))).isEqualByComparingTo("95.00");
        assertThat(patched.getBody().at("/metadata/legs")).hasSize(2);

        assertThat(delete("/transactions/" + id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get("/transactions/" + id).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void trailingZeroMoneyIsAcceptedEverywhereButRealThirdDecimalIsNot() {
        ObjectNode body = seed("flight");
        body.put("total", new BigDecimal("220.000"));
        ((ObjectNode) body.at("/tax_lines/0")).put("amount", new BigDecimal("15.000"));
        ((ObjectNode) body.at("/fee_lines/0")).put("amount", new BigDecimal("25.000"));
        ((ObjectNode) body.get("metadata")).put("fare", new BigDecimal("180.000"));

        ResponseEntity<JsonNode> created = post("/transactions", body);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("total").asText()).isEqualTo("220.00");
        assertThat(created.getBody().at("/tax_lines/0/amount").asText()).isEqualTo("15.00");
        assertThat(created.getBody().at("/fee_lines/0/amount").asText()).isEqualTo("25.00");

        ObjectNode thirdDecimal = seed("flight");
        thirdDecimal.put("total", new BigDecimal("220.001"));
        assertThat(post("/transactions", thirdDecimal).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void patchRejectsImmutableFields() {
        long id = createTransaction(seed("flight"));

        for (String field : new String[] {"type", "external_id", "currency"}) {
            ResponseEntity<JsonNode> response = patch("/transactions/" + id, MAPPER.createObjectNode().put(field, "x"));
            assertThat(response.getStatusCode()).as(field).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        ResponseEntity<JsonNode> explicitNull = patch("/transactions/" + id, MAPPER.createObjectNode().putNull("currency"));
        assertThat(explicitNull.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(get("/transactions/" + id).getBody().get("currency").asText()).isEqualTo("EUR");
    }
}
