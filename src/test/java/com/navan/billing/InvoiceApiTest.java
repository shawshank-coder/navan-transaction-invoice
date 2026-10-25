package com.navan.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.LongStream;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class InvoiceApiTest extends ApiTestSupport {

    @Value
    private static class Trip {
        long flight;
        long hotel;
        long rail;
        long tripFee;
        long agentCallFee;

        long[] ids() {
            return new long[] {flight, hotel, rail, tripFee, agentCallFee};
        }
    }

    private Trip createTrip() {
        return new Trip(
                createTransaction(seed("flight")),
                createTransaction(seed("hotel")),
                createTransaction(seed("rail")),
                createTransaction(seed("trip_fee")),
                createTransaction(seed("agent_call_fee")));
    }

    @Test
    void tripInvoiceMatchesExpectedFixture() {
        Trip trip = createTrip();
        ResponseEntity<JsonNode> response = post("/invoices", invoiceBody(trip.ids()));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode invoice = get("/invoices/" + response.getBody().get("id").asLong()).getBody();
        JsonNode expected = EXPECTED_INVOICE.get("totals");

        assertThat(invoice.get("currency").asText()).isEqualTo(EXPECTED_INVOICE.get("currency").asText());
        assertThat(money(invoice.get("total"))).isEqualByComparingTo(expected.get("grand_total").decimalValue());
        assertThat(invoice.get("lines")).hasSizeGreaterThanOrEqualTo(EXPECTED_INVOICE.get("line_count_minimum").asInt());

        Map<String, JsonNode> lineByKey = new HashMap<>();
        for (JsonNode line : invoice.get("lines")) {
            String type = line.get("type").asText();
            String key = type.equals("navan_fee") ? line.at("/metadata/fee_kind").asText() : type;
            lineByKey.put(key, line);
        }
        for (String key : new String[] {"flight", "hotel", "rail", "trip_fee", "agent_call_fee"}) {
            assertThat(money(lineByKey.get(key).get("total")))
                    .as(key).isEqualByComparingTo(expected.get(key).decimalValue());
        }
        assertThat(lineByKey.get("flight").at("/metadata/coupons/0/flight_number").asText()).isEqualTo("BA982");
        assertThat(lineByKey.get("hotel").at("/metadata/address/city").asText()).isEqualTo("London");
        assertThat(lineByKey.get("rail").at("/metadata/legs")).hasSize(2);

        JsonNode taxRollup = invoice.get("tax_rollup");
        assertThat(taxRollup).hasSize(2);
        assertThat(taxRollup.at("/0/name").asText()).isEqualTo("Airport tax");
        assertThat(taxRollup.at("/0/rate").isMissingNode()).isTrue();
        assertThat(money(taxRollup.at("/0/amount"))).isEqualByComparingTo("15.00");
        assertThat(taxRollup.at("/1/name").asText()).isEqualTo("VAT");
        assertThat(money(taxRollup.at("/1/rate"))).isEqualByComparingTo("0.07");
        assertThat(money(taxRollup.at("/1/amount"))).isEqualByComparingTo("21.70");

        JsonNode feeRollup = invoice.get("fee_rollup");
        assertThat(feeRollup).hasSize(2);
        assertThat(feeRollup.at("/0/name").asText()).isEqualTo("YQ");
        assertThat(money(feeRollup.at("/0/amount"))).isEqualByComparingTo("25.00");
        assertThat(feeRollup.at("/1/name").asText()).isEqualTo("Booking fee");
        assertThat(money(feeRollup.at("/1/amount"))).isEqualByComparingTo("4.00");
    }

    @Test
    void singleTransactionIdShapeIsSupported() {
        long flight = createTransaction(seed("flight"));
        ResponseEntity<JsonNode> response = post("/invoices", Map.of("transaction_id", flight));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(money(response.getBody().get("total"))).isEqualByComparingTo("220.00");
    }

    @Test
    void invoiceIsAFrozenSnapshot() {
        long flight = createTransaction(seed("flight"));
        long invoiceId = createInvoice(flight);

        // PATCH is blocked once invoiced, so mutate the stored row directly to prove the invoice does not join back.
        jdbc.sql("UPDATE transactions SET total = 999.99 WHERE id = :id").param("id", flight).update();
        jdbc.sql("UPDATE transaction_tax_lines SET amount = 1.00 WHERE transaction_id = :id").param("id", flight).update();
        assertThat(money(get("/transactions/" + flight).getBody().get("total"))).isEqualByComparingTo("999.99");

        JsonNode invoice = get("/invoices/" + invoiceId).getBody();
        assertThat(money(invoice.at("/lines/0/total"))).isEqualByComparingTo("220.00");
        assertThat(money(invoice.at("/lines/0/tax_lines/0/amount"))).isEqualByComparingTo("15.00");
        assertThat(money(invoice.get("total"))).isEqualByComparingTo("220.00");
    }

    @Test
    void mixedCurrencyReturns409() {
        long eur = createTransaction(seed("flight"));
        ObjectNode usdBody = seed("hotel");
        usdBody.put("currency", "USD");
        long usd = createTransaction(usdBody);

        ResponseEntity<JsonNode> response = post("/invoices", invoiceBody(eur, usd));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("error").asText()).isEqualTo("mixed_currency");
    }

    @Test
    void sameIdTwiceOrMissingIdIsRejected() {
        long flight = createTransaction(seed("flight"));
        assertThat(post("/invoices", invoiceBody(flight, flight)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/invoices", invoiceBody(flight, 999999999L)).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void moreThanOneHundredTransactionIdsIsRejected() {
        long[] tooMany = LongStream.rangeClosed(1, 101).toArray();

        ResponseEntity<JsonNode> response = post("/invoices", invoiceBody(tooMany));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void largestAllowedTotalsStillFitOnOneInvoice() {
        BigDecimal max = new BigDecimal("99999999999999999.99");
        ObjectNode first = seed("trip_fee");
        first.put("total", max);
        ObjectNode second = seed("trip_fee");
        second.put("total", max);

        ResponseEntity<JsonNode> response =
                post("/invoices", invoiceBody(createTransaction(first), createTransaction(second)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("total").asText()).isEqualTo("199999999999999999.98");
    }

    @Test
    void transactionCannotBeOnTwoInvoices() {
        long flight = createTransaction(seed("flight"));
        long hotel = createTransaction(seed("hotel"));
        long firstInvoice = createInvoice(flight);

        ResponseEntity<JsonNode> onNewInvoice = post("/invoices", invoiceBody(flight, hotel));
        ResponseEntity<JsonNode> onOtherInvoice = patch("/invoices/" + createInvoice(hotel), invoiceBody(hotel, flight));

        assertThat(onNewInvoice.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(onNewInvoice.getBody().get("error").asText()).isEqualTo("transaction_already_invoiced");
        assertThat(onOtherInvoice.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(money(get("/invoices/" + firstInvoice).getBody().get("total"))).isEqualByComparingTo("220.00");
    }

    @Test
    void patchInvoicedTransactionReturns409AndSnapshotIsUnchanged() {
        long flight = createTransaction(seed("flight"));
        long invoiceId = createInvoice(flight);

        ResponseEntity<JsonNode> response = patch("/transactions/" + flight, MAPPER.createObjectNode().put("total", "1.00"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        assertThat(money(get("/transactions/" + flight).getBody().get("total"))).isEqualByComparingTo("220.00");
        assertThat(money(get("/invoices/" + invoiceId).getBody().get("total"))).isEqualByComparingTo("220.00");
    }

    @Test
    void deleteInvoicedTransactionReturns409AndRowStays() {
        long flight = createTransaction(seed("flight"));
        createInvoice(flight);

        assertThat(delete("/transactions/" + flight).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(get("/transactions/" + flight).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteInvoiceKeepsTransactionsAndUnlocksThem() {
        Trip trip = createTrip();
        long invoiceId = createInvoice(trip.ids());

        assertThat(delete("/invoices/" + invoiceId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get("/invoices/" + invoiceId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        for (long id : trip.ids()) {
            assertThat(get("/transactions/" + id).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(patch("/transactions/" + trip.getFlight(), MAPPER.createObjectNode().put("total", "221.00"))
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void patchInvoiceRebuildsSnapshotAtomically() {
        long flight = createTransaction(seed("flight"));
        long hotel = createTransaction(seed("hotel"));
        long invoiceId = createInvoice(flight);

        ResponseEntity<JsonNode> rebuilt = patch("/invoices/" + invoiceId, invoiceBody(flight, hotel));
        assertThat(rebuilt.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rebuilt.getBody().get("lines")).hasSize(2);
        assertThat(money(rebuilt.getBody().get("total"))).isEqualByComparingTo("551.70");

        ObjectNode usdBody = seed("rail");
        usdBody.put("currency", "USD");
        long usd = createTransaction(usdBody);
        assertThat(patch("/invoices/" + invoiceId, invoiceBody(flight, usd)).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(money(get("/invoices/" + invoiceId).getBody().get("total"))).isEqualByComparingTo("551.70");
    }

    @Test
    void invoiceListIsBounded() {
        createInvoice(createTransaction(seed("flight")));
        ResponseEntity<JsonNode> page = get("/invoices?limit=1");
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody().get("items")).hasSize(1);
        assertThat(get("/invoices?limit=101").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
