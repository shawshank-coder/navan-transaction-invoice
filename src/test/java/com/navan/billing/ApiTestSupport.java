package com.navan.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:file:./target/test-db/billing-${random.uuid};DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000")
abstract class ApiTestSupport {

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode SEEDS = readJson("fixtures/task-b/seed-payloads.json");
    protected static final JsonNode EXPECTED_INVOICE = readJson("fixtures/task-b/expected-invoice.json");

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcClient jdbc;

    /** Seed payload with a unique external_id so tests sharing one database never collide. */
    protected ObjectNode seed(String name) {
        ObjectNode body = SEEDS.get(name).deepCopy();
        body.remove("comment");
        body.put("external_id", body.get("external_id").asText() + "-" + UUID.randomUUID());
        return body;
    }

    protected long createTransaction(JsonNode body) {
        ResponseEntity<JsonNode> response = post("/transactions", body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").asLong();
    }

    protected long createInvoice(long... transactionIds) {
        ResponseEntity<JsonNode> response = post("/invoices", invoiceBody(transactionIds));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").asLong();
    }

    protected static Map<String, Object> invoiceBody(long... transactionIds) {
        return Map.of("transaction_ids", Arrays.stream(transactionIds).boxed().toList());
    }

    protected long countByExternalId(String externalId) {
        return jdbc.sql("SELECT COUNT(*) FROM transactions WHERE external_id = :externalId")
                .param("externalId", externalId)
                .query(Long.class)
                .single();
    }

    protected ResponseEntity<JsonNode> get(String path) {
        return send(HttpMethod.GET, path, null);
    }

    protected ResponseEntity<JsonNode> post(String path, Object body) {
        return send(HttpMethod.POST, path, body);
    }

    protected ResponseEntity<JsonNode> patch(String path, Object body) {
        return send(HttpMethod.PATCH, path, body);
    }

    protected ResponseEntity<JsonNode> delete(String path) {
        return send(HttpMethod.DELETE, path, null);
    }

    protected static BigDecimal money(JsonNode node) {
        return new BigDecimal(node.asText());
    }

    private ResponseEntity<JsonNode> send(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private static JsonNode readJson(String path) {
        try {
            return MAPPER.readTree(Path.of(path).toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
