# Digital Asset Platform — Architecture & Flow Documentation

> **Platform**: Enterprise-grade tokenized gold trading system built on Java 17 + Spring Boot 3.2  
> **Model**: Microservices with event-driven communication via Apache Kafka  
> **Blockchain**: Hyperledger Besu (Clique PoA, chain ID 1337)  
> **Database**: PostgreSQL 16 (one dedicated database per service)

---

## Table of Contents

1. [High-Level Architecture](#1-high-level-architecture)
2. [Services Overview](#2-services-overview)
3. [Infrastructure Components](#3-infrastructure-components)
4. [Inter-Service Communication](#4-inter-service-communication)
5. [Kafka Event Bus](#5-kafka-event-bus)
6. [Security Model](#6-security-model)
7. [Complete Application Flows](#7-complete-application-flows)
   - [Flow 1 — User Registration & KYC](#flow-1--user-registration--kyc)
   - [Flow 2 — Wallet Creation](#flow-2--wallet-creation)
   - [Flow 3 — Gold Tokenization](#flow-3--gold-tokenization)
   - [Flow 4 — Token Minting](#flow-4--token-minting)
   - [Flow 5 — Token Transfer](#flow-5--token-transfer)
   - [Flow 6 — DvP Settlement](#flow-6--dvp-settlement-delivery-vs-payment)
   - [Flow 7 — Audit Query](#flow-7--audit-query)
8. [Database Schemas](#8-database-schemas)
9. [API Reference](#9-api-reference)
10. [Port Map](#10-port-map)

---

## 1. High-Level Architecture

```
                          ┌─────────────────────────────────────────────────────┐
                          │                  CLIENT (Postman / App)              │
                          └────────────────────────┬────────────────────────────┘
                                                   │ HTTP  :8080
                          ┌────────────────────────▼────────────────────────────┐
                          │                   API GATEWAY                        │
                          │          Spring Cloud Gateway (WebFlux)              │
                          │  JWT validation → route → proxy to microservice      │
                          └──┬──────┬──────┬──────┬──────┬──────┬───────────────┘
                             │      │      │      │      │      │
                      :8081  │ :8082│ :8083│ :8084│ :8085│ :8086│
                    ┌────────▼─┐ ┌──▼───┐ ┌▼───┐ ┌▼────┐ ┌▼───┐ ┌▼─────────┐
                    │  USER    │ │WALLET│ │ASSET│ │BLOCK│ │LEDG│ │SETTLEMENT│
                    │ SERVICE  │ │ SVC  │ │ SVC │ │CHAIN│ │ ER │ │   SVC    │
                    └────┬─────┘ └──┬───┘ └──┬──┘ └──┬──┘ └─┬──┘ └────┬─────┘
                         │          │         │        │       │         │
                    ┌────▼──────────▼─────────▼────────▼───────▼─────────▼─────┐
                    │                  Apache Kafka (event bus)                  │
                    │   token-minted | token-transferred | token-burned          │
                    │   settlement-completed | kyc-verified                      │
                    └───────────────────────────────────────────────────────────┘
                         │          │         │                │
                    ┌────▼──┐  ┌────▼──┐  ┌──▼────┐  ┌───────▼──────────────┐
                    │user_db│  │wallet │  │asset_db│  │ledger_db settlement_db│
                    │       │  │  _db  │  │        │  │                       │
                    └───────┘  └───────┘  └────────┘  └───────────────────────┘
                                                  │
                                         ┌────────▼────────┐
                                         │  Hyperledger     │
                                         │  Besu :8545      │
                                         │  Clique PoA      │
                                         │  Chain ID 1337   │
                                         └─────────────────┘
```

---

## 2. Services Overview

| Service | Port | Database | Publishes to Kafka | Consumes from Kafka |
|---|---|---|---|---|
| **api-gateway** | 8080 | — | — | — |
| **user-service** | 8081 | `user_db` | `kyc-verified` | — |
| **wallet-service** | 8082 | `wallet_db` | `token-transferred` | — |
| **asset-service** | 8083 | `asset_db` | `token-minted`, `token-burned` | — |
| **blockchain-service** | 8084 | — | — | — |
| **transaction-ledger-service** | 8085 | `ledger_db` | — | `token-minted`, `token-transferred`, `token-burned`, `settlement-completed` |
| **settlement-service** | 8086 | `settlement_db` | `settlement-completed` | — |

### Responsibility Summary

| Service | What it owns |
|---|---|
| **user-service** | Identity — registration, login, JWT issuance, KYC status |
| **wallet-service** | Balances — create wallets, credit/debit, atomic transfers with pessimistic locking |
| **asset-service** | Supply — tokenize physical gold, mint new tokens, burn redeemed tokens |
| **blockchain-service** | Chain — interact with Besu via Web3j; ERC-20 calls, tx receipts, block queries |
| **transaction-ledger-service** | Audit — immutable append-only log of every on-chain event via Kafka |
| **settlement-service** | DvP — orchestrate Delivery-vs-Payment between buyer and seller |
| **api-gateway** | Edge — single entry point; JWT guard, reactive routing, error handling |

---

## 3. Infrastructure Components

### PostgreSQL 16
- Single container (`dap-postgres`), five logical databases, one per service.
- Initialized by `/docker/postgres/init.sql` on first start.
- Each service runs **Flyway** migrations at startup (`ddl-auto: validate` after Flyway applies).

```
postgres:5432
├── user_db       ← user-service
├── wallet_db     ← wallet-service
├── asset_db      ← asset-service
├── ledger_db     ← transaction-ledger-service
└── settlement_db ← settlement-service
```

### Apache Kafka + Zookeeper
- Confluent Platform 7.5.0.
- Auto topic creation enabled.
- Internal broker: `kafka:29092` (Docker network). External: `localhost:9092`.
- Kafka UI available at `http://localhost:9000`.

### Hyperledger Besu 23.10.3 (Clique PoA)
- Single-node proof-of-authority blockchain. Mines a block every **2 seconds**.
- Validator account: `0xfe3b557e8fb62b89f4916b721be55ceb828dbd73` (pre-funded 200 ETH).
- P2P disabled (`--p2p-enabled=false`) — dev-only single node.
- RPC APIs enabled: `ETH, NET, WEB3, CLIQUE, ADMIN`.
- JSON-RPC endpoint: `http://besu:8545` (internal), `http://localhost:8545` (host).

---

## 4. Inter-Service Communication

### Synchronous (HTTP)
The API Gateway is the **only** entry point for clients. Services do **not** call each other over HTTP — all cross-service side effects happen via Kafka events.

```
Client → Gateway (8080) → [route match] → Backend service (808x)
```

Gateway routing table (from `application.yml`):

| Path Pattern | Upstream Service |
|---|---|
| `/users/**` | `user-service:8081` |
| `/wallet/**` | `wallet-service:8082` |
| `/assets/**`, `/tokens/**` | `asset-service:8083` |
| `/blockchain/**` | `blockchain-service:8084` |
| `/ledger/**` | `transaction-ledger-service:8085` |
| `/settlements/**` | `settlement-service:8086` |

### Asynchronous (Kafka)
Events are published by producer services and consumed by the ledger service to build an immutable audit trail. Every event carries a UUID `eventId` — the ledger deduplicates on this field (`existsByEventId` check before insert).

```
asset-service     --[token-minted]-→        ledger-service
wallet-service    --[token-transferred]-→   ledger-service
asset-service     --[token-burned]-→        ledger-service
settlement-service--[settlement-completed]→ ledger-service
user-service      --[kyc-verified]-→        (future consumers)
```

---

## 5. Kafka Event Bus

### Topic: `token-minted`
Published by **asset-service** after a successful mint operation.

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "assetType": "GLD",
  "amount": 1000.00,
  "recipientWalletId": 1,
  "recipientWalletAddress": "0xabcd...",
  "blockchainTxHash": "0x1a2b3c...",
  "mintedBy": "admin1",
  "timestamp": "2026-05-16T10:00:00Z"
}
```

### Topic: `token-transferred`
Published by **wallet-service** after an atomic balance transfer.

```json
{
  "eventId": "660e8400-e29b-41d4-a716-446655440001",
  "assetType": "GLD",
  "amount": 100.00,
  "fromWalletAddress": "0xaaa...",
  "toWalletAddress": "0xbbb...",
  "fromUserId": 1,
  "toUserId": 2,
  "blockchainTxHash": "0x2b3c4d...",
  "memo": "Purchase of 100 GLD tokens",
  "timestamp": "2026-05-16T10:05:00Z"
}
```

### Topic: `token-burned`
Published by **asset-service** when tokens are destroyed (physical gold redemption).

```json
{
  "eventId": "770e8400-...",
  "assetType": "GLD",
  "amount": 50.00,
  "walletId": 1,
  "walletAddress": "0xaaa...",
  "blockchainTxHash": "0x3c4d5e...",
  "burnedBy": "admin1",
  "reason": "Physical gold withdrawal by owner"
}
```

### Topic: `settlement-completed`
Published by **settlement-service** after DvP settlement finalization.

```json
{
  "eventId": "880e8400-...",
  "settlementId": "uuid-of-settlement",
  "buyerUserId": 2,
  "sellerUserId": 1,
  "tokenAmount": 50.00,
  "settlementAmount": 2500.00,
  "currency": "USD",
  "blockchainTxHash": "0x4d5e6f...",
  "status": "COMPLETED"
}
```

### Topic: `kyc-verified`
Published by **user-service** when an admin updates KYC status.

```json
{
  "eventId": "990e8400-...",
  "userId": 2,
  "username": "investor1",
  "kycStatus": "APPROVED",
  "verifiedBy": "admin1",
  "notes": "Documents verified"
}
```

### Consumer: `transaction-ledger-service`
Listens to all four operational topics under `groupId = "ledger-group"`. For each event it builds an `AuditLog` row and persists it idempotently (skips if `eventId` already exists). This makes the ledger an append-only, eventually-consistent audit trail.

---

## 6. Security Model

### JWT Flow
```
POST /users/login
  → user-service validates credentials
  → issues JWT signed with HS256 (secret from JWT_SECRET env var)
  → token contains: subject=username, userId, role, iat, exp (24h)

Subsequent requests:
  Authorization: Bearer <token>
  → api-gateway's JwtAuthenticationFilter extracts and validates token
  → sets SecurityContext with username + role
  → proxies request to upstream service
  → upstream service's own JwtAuthenticationFilter re-validates
```

### Role-Based Access
| Role | Permissions |
|---|---|
| `ADMIN` | All endpoints; KYC approval; tokenize/mint/burn; view all settlements |
| `INVESTOR` | Own profile; own wallets; transfers from own wallet; view own settlements |

Spring Security method-level guards (`@PreAuthorize`) are applied at the controller level in each service. Example from `UserController`:
```java
@PutMapping("/{userId}/kyc")
@PreAuthorize("hasRole('ADMIN')")          // only ADMIN can approve KYC
public ResponseEntity<...> updateKycStatus(...)

@GetMapping("/profile")
@PreAuthorize("isAuthenticated()")         // any authenticated user
public ResponseEntity<...> getProfile(...)
```

### Public Endpoints (no token required)
- `POST /users/register`
- `POST /users/login`
- `GET /actuator/health`

---

## 7. Complete Application Flows

---

### Flow 1 — User Registration & KYC

**Actors**: New user (self-register), Admin (approve KYC)

#### Step 1.1 — Register

```
POST http://localhost:8080/users/register
Content-Type: application/json

{
  "username": "investor1",
  "email": "investor@example.com",
  "password": "Invest@1234",
  "fullName": "Alice Investor"
}
```

**Internal path:**

```
Gateway (8080)
  └─ routes to user-service (8081)
       └─ UserController.register()
            └─ UserService.register()
                 ├─ userRepository.existsByUsername("investor1") → false ✓
                 ├─ userRepository.existsByEmail("investor@example.com") → false ✓
                 ├─ BCryptPasswordEncoder(strength=12).encode("Invest@1234")
                 │    → "$2a$12$..."
                 ├─ User entity built:
                 │    role=INVESTOR (hardcoded, ignores any role in request body)
                 │    kycStatus=PENDING
                 │    active=true
                 ├─ userRepository.save(user) → INSERT INTO users
                 └─ returns UserDto
```

**Response:**
```json
HTTP 201 Created
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 2,
    "username": "investor1",
    "email": "investor@example.com",
    "fullName": "Alice Investor",
    "role": "INVESTOR",
    "kycStatus": "PENDING",
    "createdAt": "2026-05-16T10:00:00Z"
  }
}
```

#### Step 1.2 — Login (Admin approves KYC, so admin logs in first)

```
POST http://localhost:8080/users/login
{
  "username": "admin",
  "password": "Admin@1234"
}
```

`admin` is the **seeded user** from Flyway migration `V1__create_users_table.sql` with `role=ADMIN, kycStatus=VERIFIED`.

**Response:**
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": { "username": "admin", "role": "ADMIN" }
  }
}
```

**JWT payload (decoded):**
```json
{
  "sub": "admin",
  "userId": 1,
  "role": "ADMIN",
  "iat": 1747389600,
  "exp": 1747476000
}
```

#### Step 1.3 — Admin approves investor KYC

```
PUT http://localhost:8080/users/2/kyc
Authorization: Bearer <adminToken>
{
  "kycStatus": "APPROVED",
  "documentRef": "KYC-DOC-2026-001",
  "notes": "Passport verified"
}
```

**Internal path:**
```
UserService.updateKycStatus(userId=2, ...)
  ├─ Loads user from DB
  ├─ Sets kycStatus = APPROVED, kycDocumentRef = "KYC-DOC-2026-001"
  ├─ userRepository.save(user)
  └─ Publishes KycVerifiedEvent to topic "kyc-verified"
       {eventId, userId=2, kycStatus=APPROVED, verifiedBy="admin"}
```

---

### Flow 2 — Wallet Creation

**Prerequisite**: User must be authenticated.

```
POST http://localhost:8080/wallet/create?userId=2&assetType=GLD
Authorization: Bearer <investorToken>
```

**Internal path:**
```
WalletService.createWallet(userId=2, assetType=GLD)
  ├─ Check: walletRepository.findByUserIdAndAssetType(2, GLD) → empty ✓
  ├─ Generate wallet address:
  │    SecureRandom.nextBytes(20 bytes) → hex encode
  │    → "0x7f3a9b2c1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a"
  ├─ Wallet entity: userId=2, address=above, assetType=GLD, balance=0.00
  ├─ walletRepository.save(wallet) → INSERT INTO wallets
  └─ returns WalletDto
```

**Response:**
```json
{
  "data": {
    "id": 1,
    "userId": 2,
    "walletAddress": "0x7f3a9b2c1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a",
    "assetType": "GLD",
    "balance": 0.00
  }
}
```

> One user can have **one wallet per asset type**. Attempting to create a second GLD wallet for the same user returns `409 Conflict`.

---

### Flow 3 — Gold Tokenization

**Prerequisite**: Caller must have `ADMIN` role.

This represents the business act of taking a physical gold bar and representing it as digital tokens on the platform.

```
POST http://localhost:8080/assets/assets/tokenize
Authorization: Bearer <adminToken>
{
  "name": "Gold Bar #001",
  "physicalQuantity": 1.0,
  "tokenRatio": 1000,
  "assetType": "GLD",
  "vaultRef": "VAULT-DUBAI-001",
  "smartContractAddress": "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73"
}
```

**Internal path:**
```
AssetService.tokenize(request, performedBy="admin")
  ├─ Check: no active GLD asset already exists
  ├─ totalSupply = physicalQuantity × tokenRatio = 1.0 × 1000 = 1000 tokens
  ├─ Asset entity saved to asset_db
  └─ returns Asset (id=1, totalSupply=1000, active=true)
```

**Business meaning:**
- 1 kg of gold = 1000 GLD tokens
- `tokenRatio=1000` means 1 gram = 1 GLD token
- `vaultRef` links to the physical custodian record
- No tokens are distributed yet — only the total supply is defined

**Response:**
```json
{
  "data": {
    "id": 1,
    "name": "Gold Bar #001",
    "assetType": "GLD",
    "physicalQuantity": 1.0,
    "totalSupply": 1000.00,
    "tokenRatio": 1000,
    "vaultRef": "VAULT-DUBAI-001",
    "active": true
  }
}
```

---

### Flow 4 — Token Minting

Minting is the act of **issuing** GLD tokens from the asset supply into a wallet, typically done by the admin after tokenizing a gold bar.

```
POST http://localhost:8080/assets/assets/1/mint
Authorization: Bearer <adminToken>
{
  "assetType": "GLD",
  "recipientWalletId": 1,
  "amount": 1000,
  "reason": "Initial issuance of 1 KG gold bar VAULT-DUBAI-001"
}
```

**Internal path:**
```
AssetService.mint(request, performedBy="admin", blockchainTxHash="0xabc...")
  ├─ Load active GLD asset (id=1)
  ├─ asset.totalSupply += 1000  → persisted
  ├─ TokenOperation record saved:
  │    operationType=MINT, assetType=GLD, amount=1000,
  │    walletId=1, blockchainTxHash="0xabc...", performedBy="admin"
  └─ Publish to Kafka topic "token-minted":
       {eventId, assetType=GLD, amount=1000, recipientWalletId=1,
        blockchainTxHash, mintedBy="admin"}

                                    [async]
                                      │
                    ┌─────────────────▼──────────────────┐
                    │    transaction-ledger-service        │
                    │    onTokenMinted(event)              │
                    │    AuditLog saved to ledger_db:      │
                    │    type=MINT, amount=1000, GLD       │
                    └──────────────────────────────────────┘
```

---

### Flow 5 — Token Transfer

Transfers GLD tokens between two wallets. Uses **pessimistic locking** (`SELECT FOR UPDATE`) to prevent race conditions.

```
POST http://localhost:8080/wallet/transfer
Authorization: Bearer <adminToken>
{
  "fromWalletAddress": "0xaaa...",
  "toWalletAddress":   "0xbbb...",
  "amount": 100,
  "fromUserId": 1,
  "toUserId": 2,
  "memo": "Purchase of 100 GLD tokens",
  "blockchainTxHash": "0x1a2b..."
}
```

**Internal path:**
```
WalletService.transfer(...)
  ├─ walletRepository.findByWalletAddressForUpdate("0xaaa...")
  │    → SELECT * FROM wallets WHERE wallet_address=? FOR UPDATE
  ├─ walletRepository.findByWalletAddressForUpdate("0xbbb...")
  │    → SELECT * FROM wallets WHERE wallet_address=? FOR UPDATE
  ├─ Validate: both wallets same asset type (GLD=GLD ✓)
  ├─ Validate: from.balance(1000) >= amount(100) ✓
  ├─ from.balance = 1000 - 100 = 900  → saved
  ├─ to.balance   = 0   + 100 = 100  → saved
  │    (both saves in same @Transactional — atomic)
  └─ Publish to Kafka "token-transferred":
       {fromWalletAddress, toWalletAddress, amount=100, assetType=GLD,
        fromUserId=1, toUserId=2, memo, blockchainTxHash}

                                    [async]
                                      │
                    ┌─────────────────▼──────────────────┐
                    │    transaction-ledger-service        │
                    │    onTokenTransferred(event)         │
                    │    AuditLog: TRANSFER 100 GLD        │
                    │    from=0xaaa, to=0xbbb             │
                    └──────────────────────────────────────┘
```

**Failure cases:**
- `409` — different asset types between wallets
- `400` — insufficient balance (`from.balance < amount`)
- `404` — wallet address not found

---

### Flow 6 — DvP Settlement (Delivery vs. Payment)

**Delivery-vs-Payment** is the atomic exchange of tokens (delivery) for fiat payment (payment) simultaneously — the financial industry standard for reducing settlement risk.

#### Step 6.1 — Initiate Settlement

```
POST http://localhost:8080/settlements/settlements
Authorization: Bearer <adminToken>
{
  "buyerUserId":         2,
  "sellerUserId":        1,
  "buyerWalletAddress":  "0xbbb...",
  "sellerWalletAddress": "0xaaa...",
  "tokenAmount":         50,
  "settlementAmount":    2500.00,
  "currency":            "USD"
}
```

**Internal path:**
```
SettlementService.initiateSettlement(request)
  ├─ Validate: buyerUserId ≠ sellerUserId
  ├─ Settlement entity saved:
  │    settlementId = UUID (e.g. "a1b2c3d4-...")
  │    status = INITIATED
  │    tokenAmount = 50 GLD
  │    settlementAmount = 2500 USD
  └─ returns Settlement (status=INITIATED)
```

#### Step 6.2 — Complete Settlement (after blockchain confirmation)

```
PUT http://localhost:8080/settlements/settlements/a1b2c3d4-.../complete
Authorization: Bearer <adminToken>
{
  "blockchainTxHash": "0x5f6a7b..."
}
```

**Internal path:**
```
SettlementService.completeSettlement(settlementId, blockchainTxHash)
  ├─ Load settlement (must be INITIATED or PENDING_BLOCKCHAIN)
  ├─ settlement.status = COMPLETED
  ├─ settlement.blockchainTxHash = "0x5f6a7b..."
  ├─ settlement.settledAt = Instant.now()
  ├─ settlementRepository.save(settlement)
  └─ Publish to Kafka "settlement-completed":
       {settlementId, buyerUserId=2, sellerUserId=1,
        tokenAmount=50, settlementAmount=2500, currency=USD,
        blockchainTxHash, status=COMPLETED}

                                    [async]
                                      │
                    ┌─────────────────▼──────────────────┐
                    │    transaction-ledger-service        │
                    │    onSettlementCompleted(event)      │
                    │    AuditLog: SETTLEMENT 50 GLD       │
                    │    seller(1) → buyer(2)              │
                    │    metadata: settlementId, USD       │
                    └──────────────────────────────────────┘
```

**Settlement status machine:**
```
INITIATED → PENDING_BLOCKCHAIN → COMPLETED
                               → FAILED
```

---

### Flow 7 — Audit Query

The ledger service maintains a **complete, immutable audit trail** of all token events. Queries are read-only and require admin authentication.

```
GET http://localhost:8080/ledger/audit-logs
Authorization: Bearer <adminToken>
```

**Response example:**
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "eventId": "550e8400-...",
        "transactionType": "MINT",
        "assetType": "GLD",
        "toUserId": 1,
        "amount": 1000.00,
        "blockchainTxHash": "0xabc...",
        "kafkaTopic": "token-minted",
        "metadata": "mintedBy=admin",
        "recordedAt": "2026-05-16T10:01:00Z"
      },
      {
        "id": 2,
        "eventId": "660e8400-...",
        "transactionType": "TRANSFER",
        "assetType": "GLD",
        "fromWallet": "0xaaa...",
        "toWallet":   "0xbbb...",
        "fromUserId": 1,
        "toUserId":   2,
        "amount": 100.00,
        "kafkaTopic": "token-transferred",
        "metadata": "memo=Purchase of 100 GLD tokens",
        "recordedAt": "2026-05-16T10:05:00Z"
      }
    ],
    "totalElements": 2,
    "totalPages": 1
  }
}
```

**Filter by event type:**
```
GET /ledger/audit-logs?eventType=TOKEN_MINTED
GET /ledger/audit-logs?eventType=TOKEN_TRANSFERRED
GET /ledger/audit-logs?eventType=SETTLEMENT_COMPLETED
```

**Idempotency guarantee:** The ledger checks `existsByEventId(eventId)` before every insert. If Kafka delivers an event twice (at-least-once delivery), the duplicate is silently ignored — no double-counting.

---

### Flow 8 — Blockchain Queries

The blockchain service wraps Hyperledger Besu via **Web3j** and exposes chain data over REST.

#### Get latest block number
```
GET http://localhost:8080/blockchain/block/latest
Authorization: Bearer <token>

→ web3j.ethBlockNumber().send()
```

#### Query ERC-20 token balance on-chain
```
GET http://localhost:8080/blockchain/balance/0xfe3b557e8fb62b89f4916b721be55ceb828dbd73
Authorization: Bearer <token>

→ Encodes: balanceOf(address) = ABI selector 0x70a08231 + padded address
→ eth_call to contract on Besu
→ Decodes result (raw BigInteger ÷ 10^18)
```

#### Submit ERC-20 transfer on-chain
```
POST http://localhost:8080/blockchain/transfer
{
  "contractAddress": "0xfe3b...",
  "fromAddress":     "0xaaa...",
  "toAddress":       "0xbbb...",
  "amount":          100
}

→ Encodes: transfer(address,uint256) = ABI selector 0xa9059cbb + padded args
→ Signs with admin private key (Credentials.create(adminPrivateKey))
→ RawTransactionManager.sendTransaction(gasPrice, gasLimit, contract, data)
→ Returns txHash, status=PENDING
```

---

## 8. Database Schemas

### user_db — `users` table
```sql
id               BIGSERIAL PRIMARY KEY
username         VARCHAR(50)  NOT NULL UNIQUE
email            VARCHAR(100) NOT NULL UNIQUE
password         VARCHAR(255) NOT NULL          -- BCrypt hash, strength=12
full_name        VARCHAR(100) NOT NULL
role             VARCHAR(20)  NOT NULL DEFAULT 'INVESTOR'   -- ADMIN | INVESTOR
kyc_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING'    -- PENDING | APPROVED | REJECTED | VERIFIED
phone_number     VARCHAR(20)
country          VARCHAR(3)
kyc_document_ref VARCHAR(255)
is_active        BOOLEAN      NOT NULL DEFAULT TRUE
created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
```

**Seeded on startup:**
```
username=admin, role=ADMIN, kycStatus=VERIFIED
password (BCrypt $2a$12$): Admin@1234
```

### wallet_db — `wallets` table
```sql
id             BIGSERIAL PRIMARY KEY
user_id        BIGINT       NOT NULL
wallet_address VARCHAR(42)  NOT NULL UNIQUE   -- 0x + 20 hex bytes (SecureRandom)
asset_type     VARCHAR(20)  NOT NULL          -- GLD | ...
balance        DECIMAL(30,8) NOT NULL DEFAULT 0
is_active      BOOLEAN      NOT NULL DEFAULT TRUE
created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
UNIQUE(user_id, asset_type)                   -- one wallet per user per asset
```

### asset_db — `assets` + `token_operations` tables
```sql
-- assets
id                BIGSERIAL PRIMARY KEY
name              VARCHAR(100) NOT NULL
asset_type        VARCHAR(20)  NOT NULL      -- GLD
physical_quantity DECIMAL(20,8)              -- 1.0 (kg)
total_supply      DECIMAL(30,8)              -- 1000.0 (tokens)
token_ratio       DECIMAL(20,8)              -- 1000 (tokens per unit)
vault_ref         VARCHAR(100)               -- VAULT-DUBAI-001
smart_contract    VARCHAR(100)               -- ERC-20 address on Besu
active            BOOLEAN DEFAULT TRUE

-- token_operations
id                   BIGSERIAL PRIMARY KEY
asset_id             BIGINT NOT NULL
operation_type       VARCHAR(10) NOT NULL    -- MINT | BURN
asset_type           VARCHAR(20)
amount               DECIMAL(30,8)
wallet_id            BIGINT                  -- recipient/source wallet
wallet_address       VARCHAR(42)
blockchain_tx_hash   VARCHAR(100)
performed_by         VARCHAR(50)
reason               TEXT
created_at           TIMESTAMPTZ
```

### ledger_db — `audit_logs` table
```sql
id                 BIGSERIAL PRIMARY KEY
event_id           VARCHAR(50)  NOT NULL UNIQUE    -- UUID from Kafka event
transaction_type   VARCHAR(20)  NOT NULL           -- MINT | BURN | TRANSFER | SETTLEMENT
asset_type         VARCHAR(20)
from_wallet        VARCHAR(100)
to_wallet          VARCHAR(100)
from_user_id       BIGINT
to_user_id         BIGINT
amount             DECIMAL(30,8)
blockchain_tx_hash VARCHAR(100)
kafka_topic        VARCHAR(100)
metadata           VARCHAR(1000)                   -- key=value pairs
recorded_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
```

### settlement_db — `settlements` table
```sql
settlement_id          VARCHAR(36)  PRIMARY KEY    -- UUID
buyer_user_id          BIGINT
seller_user_id         BIGINT
buyer_wallet_address   VARCHAR(100)
seller_wallet_address  VARCHAR(100)
token_amount           DECIMAL(30,8)
settlement_amount      DECIMAL(20,4)               -- fiat amount
currency               VARCHAR(10)                 -- USD
status                 VARCHAR(30)                 -- INITIATED | PENDING_BLOCKCHAIN | COMPLETED | FAILED
blockchain_tx_hash     VARCHAR(100)
failure_reason         TEXT
settled_at             TIMESTAMPTZ
created_at             TIMESTAMPTZ DEFAULT NOW()
```

---

## 9. API Reference

All requests go through the gateway at `http://localhost:8080`.  
All responses follow the wrapper:
```json
{
  "success": true | false,
  "message": "...",
  "data":    { ... },
  "errorCode": "...",
  "timestamp": "2026-05-16T10:00:00Z"
}
```

### User Service (`/users`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/users/register` | None | Register a new user (always INVESTOR role) |
| POST | `/users/login` | None | Authenticate; returns JWT |
| GET | `/users/profile` | Any | Get own profile |
| GET | `/users/{userId}` | ADMIN or self | Get user by ID |
| PUT | `/users/{userId}/kyc` | ADMIN | Approve/reject KYC |
| GET | `/users/kyc/pending` | ADMIN | List users awaiting KYC |

### Wallet Service (`/wallet`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/wallet/create?userId=&assetType=` | Any | Create wallet |
| GET | `/wallet/{walletId}` | Any | Get wallet + balance |
| GET | `/wallet/user/{userId}` | Any | List user's wallets |
| GET | `/wallet/balance/{address}` | Any | Balance by wallet address |
| POST | `/wallet/transfer` | Any | Transfer tokens between wallets |
| POST | `/wallet/credit` | Any | Credit wallet (internal use) |
| POST | `/wallet/debit` | Any | Debit wallet (internal use) |

### Asset Service (`/assets`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/assets/assets/tokenize` | ADMIN | Tokenize physical gold bar |
| POST | `/assets/assets/{id}/mint` | ADMIN | Mint tokens to wallet |
| POST | `/assets/assets/{id}/burn` | ADMIN | Burn tokens (redemption) |
| GET | `/assets/assets` | Any | List all active assets |
| GET | `/assets/assets/{id}` | Any | Get asset details |
| GET | `/assets/assets/{id}/operations` | Any | Token operation history |

### Blockchain Service (`/blockchain`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/blockchain/block/latest` | Any | Latest block number from Besu |
| GET | `/blockchain/balance/{contractAddress}/{walletAddress}` | Any | On-chain ERC-20 balance |
| POST | `/blockchain/transfer` | Any | Submit ERC-20 transfer on-chain |
| GET | `/blockchain/transaction/{txHash}` | Any | Get transaction receipt |

### Transaction Ledger Service (`/ledger`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/ledger/audit-logs` | ADMIN | All audit logs (paginated) |
| GET | `/ledger/audit-logs?eventType=TOKEN_MINTED` | ADMIN | Filter by event type |
| GET | `/ledger/audit-logs/user/{userId}` | ADMIN | Logs for specific user |
| GET | `/ledger/audit-logs/tx/{txHash}` | ADMIN | Logs for blockchain tx |

### Settlement Service (`/settlements`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/settlements/settlements` | Any | Initiate DvP settlement |
| PUT | `/settlements/settlements/{id}/complete` | ADMIN | Mark settlement complete |
| PUT | `/settlements/settlements/{id}/fail` | ADMIN | Mark settlement failed |
| GET | `/settlements/settlements/{id}` | Any | Get settlement details |
| GET | `/settlements/settlements/user/{userId}` | Any | User's settlements |
| GET | `/settlements/settlements?status=INITIATED` | ADMIN | Filter by status |

---

## 10. Port Map

| Component | Host Port | Container Port | Protocol |
|---|---|---|---|
| API Gateway | 8080 | 8080 | HTTP |
| User Service | 8081 | 8081 | HTTP |
| Wallet Service | 8082 | 8082 | HTTP |
| Asset Service | 8083 | 8083 | HTTP |
| Blockchain Service | 8084 | 8084 | HTTP |
| Transaction Ledger Service | 8085 | 8085 | HTTP |
| Settlement Service | 8086 | 8086 | HTTP |
| PostgreSQL | 5432 | 5432 | TCP |
| Kafka | 9092 | 9092 | TCP |
| Kafka (internal Docker) | — | 29092 | TCP |
| Kafka UI | 9000 | 8080 | HTTP |
| Zookeeper | 2181 | 2181 | TCP |
| Besu JSON-RPC | 8545 | 8545 | HTTP |
| Besu WebSocket | 8546 | 8546 | WS |

---

## End-to-End Sequence: Full Gold Trading Scenario

```
1. Admin registers & logs in
   POST /users/login (admin / Admin@1234)
   → adminToken saved

2. Investor registers
   POST /users/register (investor1)
   → investorId saved

3. Investor logs in
   POST /users/login (investor1 / Invest@1234)
   → investorToken saved

4. Admin approves KYC
   PUT /users/{investorId}/kyc  [adminToken]
   → kycStatus = APPROVED
   → KycVerifiedEvent published to Kafka

5. Admin creates GLD wallet
   POST /wallet/create?userId={adminId}&assetType=GLD  [adminToken]
   → adminWalletAddress saved

6. Investor creates GLD wallet
   POST /wallet/create?userId={investorId}&assetType=GLD  [investorToken]
   → investorWalletAddress saved

7. Admin tokenizes 1 kg gold bar
   POST /assets/assets/tokenize  [adminToken]
   body: { physicalQuantity:1, tokenRatio:1000, vaultRef:"VAULT-001" }
   → 1000 GLD tokens defined (assetId saved)

8. Admin mints 1000 GLD → admin wallet
   POST /assets/assets/{assetId}/mint  [adminToken]
   body: { recipientWalletId:{adminWalletId}, amount:1000 }
   → TokenMintedEvent → ledger records MINT audit entry

9. Admin transfers 100 GLD to investor
   POST /wallet/transfer  [adminToken]
   body: { fromWalletAddress:adminAddr, toWalletAddress:investorAddr, amount:100 }
   → admin.balance:  1000 → 900
   → investor.balance: 0  → 100
   → TokenTransferredEvent → ledger records TRANSFER audit entry

10. Initiate DvP settlement (investor buys 50 GLD at $50/token)
    POST /settlements/settlements  [adminToken]
    body: { buyerUserId:{investorId}, sellerUserId:{adminId},
            tokenAmount:50, settlementAmount:2500, currency:USD }
    → status = INITIATED

11. Complete settlement after off-chain payment confirmation
    PUT /settlements/settlements/{settlementId}/complete  [adminToken]
    body: { blockchainTxHash:"0x..." }
    → status = COMPLETED
    → SettlementCompletedEvent → ledger records SETTLEMENT audit entry

12. Query full audit trail
    GET /ledger/audit-logs  [adminToken]
    → Returns: MINT(1000), TRANSFER(100), SETTLEMENT(50) with full metadata
```

---

*Generated from source code — `feature/digital-asset-platform` branch.*
