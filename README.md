# Travel billing API

A small Spring Boot service that stores Navan travel transactions and builds invoices from them. There are four transaction types: `flight`, `hotel`, `rail`, and `navan_fee`. Each invoice is a frozen snapshot of the transactions it includes.

- Java 21, Spring Boot 3.5 (Spring MVC), Spring Data JPA (Hibernate), Flyway, Lombok (`@Value` for immutable models)
- Database: **file-based H2**, stored at `./data/travel-billing.mv.db`. Nothing is kept only in memory.
- Design notes and assumptions: [ARCHITECTURE.md](ARCHITECTURE.md)

## How to run

There are two ways to run the project; pick whichever your machine allows. Both run the same code, the same tests, and the same API on port 8080.

| | **Option A: Docker** | **Option B: Local (no Docker)** |
|---|---|---|
| You need | Docker only | JDK 21 + Maven 3.9+ |
| Start the API | `docker compose up --build api` | `mvn spring-boot:run` |
| Stop the API | `Ctrl+C` in that terminal (then `docker compose down` to remove the container) | `Ctrl+C` in that terminal |
| Run the tests | `docker compose run --rm --build tests` | `mvn test` |
| Data lives in | Docker volume `h2-data` | `./data/travel-billing.mv.db` |
| Reset data | `docker compose down -v` | stop the app, then `rm -rf data/` |

No API keys or external services are needed, and the service does not call an LLM. `jq` is only needed for the [example curls](#example-curls).

### Option A: Docker

Only Docker is required. The Java 21 and Maven images are pulled automatically on the first build.

```bash
# start the API on http://localhost:8080 (stop with Ctrl+C)
docker compose up --build api

# run the test suite inside a container
docker compose run --rm --build tests
```

If your Docker has no Compose plugin, use plain Docker:

```bash
docker build --target runtime -t travel-billing . && docker run --rm -p 8080:8080 travel-billing
docker build --target test -t travel-billing-test . && docker run --rm travel-billing-test
```

The first build downloads the Maven dependencies once. Later builds reuse them until `pom.xml` changes.

### Option B: Local, without Docker

Use this if Docker isn't available, for example when it's blocked on a company laptop.

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 | Check with `java -version`. If Maven picks up a different JDK, point it at 21 first: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` (macOS). |
| Maven | 3.9+ | Check with `mvn -version`. |

```bash
# start the API on http://localhost:8080 (stop with Ctrl+C)
mvn spring-boot:run

# run the test suite
mvn test
```

Only one process can open the H2 file at a time, so run a single instance. Stop it with `Ctrl+C` before starting another one or deleting `data/`.

### Once the API is running (either option)

- API: `http://localhost:8080`
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI spec: http://localhost:8080/v3/api-docs
- Health check: `GET /health`, which returns `{"status":"ok"}`

Flyway creates the schema automatically on first start.

## Tests

The tests start the app on a random port, call the real HTTP API, and use a throwaway H2 file under `target/test-db/`. They never touch your API data.

| # | Required case | Test |
|---|---|---|
| 1 | Create flight, hotel, rail, `trip_fee`, `agent_call_fee`; GET returns coupons, address, legs, `fee_kind` | `TransactionApiTest.createsEveryTypeAndRoundTripsTypedMetadata` |
| 2 | Duplicate `external_id` gives 409 with existing `id`, and only one row exists | `TransactionApiTest.duplicateExternalIdReturns409WithExistingIdAndInsertsNothing` |
| 3 | Trip invoice totals match `expected-invoice.json` (685.70 EUR) | `InvoiceApiTest.tripInvoiceMatchesExpectedFixture` |
| 4 | Invoice snapshot stays frozen after the stored flight changes | `InvoiceApiTest.invoiceIsAFrozenSnapshot` |
| 5 | Mixed currency gives 409 | `InvoiceApiTest.mixedCurrencyReturns409` |
| 6 | Concurrent duplicate create leaves one row | `TransactionApiTest.concurrentDuplicateCreateLeavesExactlyOneRow` (8 parallel POSTs) |
| 7 | PATCH an invoiced transaction gives 409; snapshot unchanged | `InvoiceApiTest.patchInvoicedTransactionReturns409AndSnapshotUnchanged` |
| 8 | DELETE an invoiced transaction gives 409; row still exists | `InvoiceApiTest.deleteInvoicedTransactionReturns409AndRowStays` |
| 9 | DELETE invoice keeps its transactions | `InvoiceApiTest.deleteInvoiceKeepsTransactionsAndUnlocksThem` |
| 10 | List defaults to 50 items; `limit=1000` gives 400 | `TransactionApiTest.listIsPaginatedAndBounded` |

Extra tests cover: unknown `type` / `fee_kind` / empty metadata (400), a missing id (404), the same id listed twice on an invoice (409), the single `transaction_id` request body, PATCH invoice rebuild and rollback, the invoice list limits, trailing-zero money (`220.000`), PATCH of immutable fields (400), and an invoice of two maximum-size totals.

## API summary

| Method | Path | Success | Errors |
|---|---|---|---|
| `POST` | `/transactions` | 201, stored transaction | 400 invalid input, 409 duplicate `external_id` (`existing_id` in body) |
| `GET` | `/transactions/{id}` | 200 | 404 |
| `GET` | `/transactions?limit=&after=` | 200 `{items, next_cursor}` | 400 if `limit` is outside 1–100 |
| `PATCH` | `/transactions/{id}` | 200 | 404, 409 if on any invoice, 400 (including `type`, `external_id` or `currency` in the body: they can't change) |
| `DELETE` | `/transactions/{id}` | 204 | 404, 409 if on any invoice |
| `POST` | `/invoices` | 201, invoice snapshot | 400 (including more than 100 ids), 404 missing id, 409 mixed currency, id listed twice, or transaction already on another invoice |
| `GET` | `/invoices/{id}` | 200 | 404 |
| `GET` | `/invoices?limit=&after=` | 200 `{items, next_cursor}` | 400 |
| `PATCH` | `/invoices/{id}` | 200, rebuilt snapshot | 404, 409 (same rules as create; its own transactions don't count as "already invoiced"); the invoice is left unchanged on any error |
| `DELETE` | `/invoices/{id}` | 204 | 404 |

All errors have this shape: `{"error": "<code>", "message": "...", "details": [...]?, "existing_id": 123?}`.

Pagination uses a cursor, not an offset. `limit` defaults to 50 and can be at most 100. To get the next page, pass the `next_cursor` from the previous response as `after`. `next_cursor` is `null` on the last page.

Money values are returned as JSON strings (`"220.00"`) so clients never parse them into floating point.

## Example curls

These work with either run option. They assume an empty database (see "Reset data" in [How to run](#how-to-run)) and are run from the project root.

```bash
BASE=http://localhost:8080
F=fixtures/task-b/seed-payloads.json
H='Content-Type: application/json'
```

### Create one transaction of each type

```bash
FLIGHT=$(jq '.flight'         $F | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
HOTEL=$(jq  '.hotel'          $F | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
RAIL=$(jq   '.rail'           $F | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
TRIP=$(jq   '.trip_fee'       $F | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
CALL=$(jq   '.agent_call_fee' $F | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
echo "flight=$FLIGHT hotel=$HOTEL rail=$RAIL trip_fee=$TRIP agent_call_fee=$CALL"
```

### Duplicate create: same `external_id` twice, only one row stored

```bash
jq '.duplicate_flight' $F | curl -s -w '\nHTTP %{http_code}\n' -X POST $BASE/transactions -H "$H" -d @-
# {"error":"duplicate_external_id","message":"...","existing_id":1}   HTTP 409

curl -s "$BASE/transactions?limit=100" \
  | jq '[.items[] | select(.external_id == "flight-TXL-LHR-2026-03-11")] | length'
# 1
```

### Get and list transactions

```bash
curl -s $BASE/transactions/$FLIGHT | jq
curl -s "$BASE/transactions" | jq                       # default limit 50
curl -s "$BASE/transactions?limit=2" | jq               # includes next_cursor
curl -s "$BASE/transactions?limit=2&after=2" | jq       # next page
curl -s -w '\nHTTP %{http_code}\n' "$BASE/transactions?limit=1000"   # 400
```

### PATCH and DELETE a transaction that is not on an invoice

```bash
curl -s -X PATCH $BASE/transactions/$RAIL -H "$H" -d '{"total": 95.00}' | jq .total     # "95.00"
curl -s -X PATCH $BASE/transactions/$RAIL -H "$H" -d '{"total": 94.00}' | jq .total     # restore

TMP=$(jq '.trip_fee | .external_id = "temp-fee"' $F | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
curl -s -w 'HTTP %{http_code}\n' -X DELETE $BASE/transactions/$TMP                      # 204
```

### Create an invoice

```bash
# one transaction (a separate fee, because a transaction can be on only one invoice)
SOLO=$(jq '.trip_fee | .external_id = "solo-fee"' $F | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
curl -s -X POST $BASE/invoices -H "$H" -d "{\"transaction_id\": $SOLO}" | jq '{id, total}'

# a whole trip: flight + hotel + rail + both Navan fees
INV=$(curl -s -X POST $BASE/invoices -H "$H" \
  -d "{\"transaction_ids\": [$FLIGHT,$HOTEL,$RAIL,$TRIP,$CALL]}" | jq -r .id)
```

### Get and list invoices

```bash
curl -s $BASE/invoices/$INV | jq '{total, tax_rollup, fee_rollup, lines: [.lines[] | {type, external_id, total}]}'
# total "685.70"
# tax_rollup [Airport tax 15.00, VAT 0.07 21.70]
# fee_rollup [YQ 25.00, Booking fee 4.00]

curl -s "$BASE/invoices?limit=10" | jq
```

### PATCH or DELETE an invoiced transaction returns 409

```bash
curl -s -w '\nHTTP %{http_code}\n' -X PATCH  $BASE/transactions/$FLIGHT -H "$H" -d '{"total": 1.00}'  # 409
curl -s -w '\nHTTP %{http_code}\n' -X DELETE $BASE/transactions/$FLIGHT                                # 409
curl -s $BASE/invoices/$INV | jq .total                                                                 # still "685.70"
```

### Invoice error cases

```bash
EUR=$(jq '.rail | .external_id = "rail-eur-2"' $F | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
USD=$(jq '.hotel | .currency = "USD" | .external_id = "hotel-usd-1"' $F \
  | curl -s -X POST $BASE/transactions -H "$H" -d @- | jq -r .id)
curl -s -w '\nHTTP %{http_code}\n' -X POST $BASE/invoices -H "$H" -d "{\"transaction_ids\": [$EUR,$USD]}"     # 409 mixed_currency
curl -s -w '\nHTTP %{http_code}\n' -X POST $BASE/invoices -H "$H" -d "{\"transaction_ids\": [$EUR,$EUR]}"     # 409 duplicate_transaction_id
curl -s -w '\nHTTP %{http_code}\n' -X POST $BASE/invoices -H "$H" -d "{\"transaction_ids\": [$FLIGHT]}"       # 409 transaction_already_invoiced (it is on $INV)
curl -s -w '\nHTTP %{http_code}\n' -X POST $BASE/invoices -H "$H" -d '{"transaction_ids": [999999]}'        # 404
```

### PATCH an invoice (replace its transactions and rebuild the snapshot)

```bash
curl -s -X PATCH $BASE/invoices/$INV -H "$H" -d "{\"transaction_ids\": [$FLIGHT,$HOTEL]}" \
  | jq '{total, lines: (.lines | length)}'
# {"total":"551.70","lines":2}
```

### DELETE an invoice (its transactions are kept)

```bash
curl -s -w 'HTTP %{http_code}\n' -X DELETE $BASE/invoices/$INV     # 204
curl -s -w '\nHTTP %{http_code}\n' $BASE/invoices/$INV             # 404
curl -s $BASE/transactions/$FLIGHT | jq '{id, total}'              # still there, and editable again
```

## Project layout

```
src/main/java/com/navan/billing
├── controller/   HTTP only: TransactionController, InvoiceController, HealthController
├── service/      contracts: ITransactionService, IInvoiceService (interfaces),
│                 InvoiceSnapshotBuilder (pure snapshot and rollup logic)
│   └── impl/     business rules, DB transactions, locking: TransactionServiceImpl, InvoiceServiceImpl
├── repository/   Spring Data JPA interfaces: TransactionRepository, InvoiceRepository, InvoiceLineRepository
├── entity/       JPA entities mapped onto the Flyway schema (tax/fee lines as element collections),
│                 InvoiceLineSnapshot (stored JSON format of a frozen invoice line)
├── mapper/       entity ↔ model conversion: TransactionMapper, InvoiceMapper
├── model/        everything services and the API work with
│   ├── transaction/  Transaction, TaxLine, FeeLine, Create/PatchTransactionRequest, TransactionSummary
│   │   └── metadata/ sealed TransactionMetadata + Flight/Hotel/Rail/NavanFee classes
│   ├── invoice/      Invoice, InvoiceLine, InvoiceDraft, InvoiceRequest, InvoiceSummary, rollup lines
│   ├── enums/        TransactionType, FeeKind
│   └── (root)        shared: ApiError, PageQuery, PageResponse, MoneyDeserializer
├── exception/    exception types + GlobalExceptionHandler (exception → HTTP status)
└── config/       Jackson (money as strings), OpenAPI
src/main/resources/db/migration/V1__init.sql   schema, constraints, indexes
```
