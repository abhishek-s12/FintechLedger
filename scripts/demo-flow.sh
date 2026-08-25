#!/usr/bin/env bash
# ==============================================================================
# FintechLedger - End-to-End Enterprise Flow Demonstration
# ==============================================================================

set -euo pipefail

BASE_URL="http://localhost:8080/api/v1"
TIMESTAMP=$(date +%s)

echo "======================================================================="
echo "💳 FINTECHLEDGER LIVE DEMONSTRATION"
echo "======================================================================="

# 1. Register Alice
echo -e "\n1️⃣  Registering User 'Alice'..."
ALICE_REG_RES=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"alice_${TIMESTAMP}@fintech.com\",
    \"password\": \"Password123!\",
    \"firstName\": \"Alice\",
    \"lastName\": \"Smith\"
  }")
echo "$ALICE_REG_RES" | grep -o '"accessToken":"[^"]*' || echo "$ALICE_REG_RES"

# 2. Register Bob
echo -e "\n2️⃣  Registering User 'Bob'..."
BOB_REG_RES=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"bob_${TIMESTAMP}@fintech.com\",
    \"password\": \"Password123!\",
    \"firstName\": \"Bob\",
    \"lastName\": \"Jones\"
  }")
echo "$BOB_REG_RES" | grep -o '"accessToken":"[^"]*' || echo "$BOB_REG_RES"

# Extract JWTs
ALICE_TOKEN=$(echo "$ALICE_REG_RES" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
BOB_TOKEN=$(echo "$BOB_REG_RES" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 3. Create Wallets
echo -e "\n3️⃣  Creating INR Wallet for Alice..."
ALICE_WALLET_RES=$(curl -s -X POST "$BASE_URL/wallets" \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currency":"INR"}')
echo "$ALICE_WALLET_RES"
ALICE_WALLET_ID=$(echo "$ALICE_WALLET_RES" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

echo -e "\n4️⃣  Creating INR Wallet for Bob..."
BOB_WALLET_RES=$(curl -s -X POST "$BASE_URL/wallets" \
  -H "Authorization: Bearer $BOB_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currency":"INR"}')
echo "$BOB_WALLET_RES"
BOB_WALLET_ID=$(echo "$BOB_WALLET_RES" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

# 4. Deposit into Alice's wallet
echo -e "\n5️⃣  Depositing ₹1,000 into Alice's Wallet..."
DEPOSIT_RES=$(curl -s -X POST "$BASE_URL/wallets/$ALICE_WALLET_ID/deposit" \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000.00, "currency": "INR"}')
echo "$DEPOSIT_RES"

# 5. Execute ₹300 Transfer with Idempotency Key
IDEMPOTENCY_KEY="idemp-demo-$TIMESTAMP-001"
echo -e "\n6️⃣  Initiating ₹300 Transfer from Alice to Bob (Idempotency-Key: $IDEMPOTENCY_KEY)..."
TRANSFER_RES_1=$(curl -s -X POST "$BASE_URL/payments/transfer" \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"senderWalletId\": \"$ALICE_WALLET_ID\",
    \"receiverWalletId\": \"$BOB_WALLET_ID\",
    \"amount\": 300.00,
    \"currency\": \"INR\"
  }")
echo "$TRANSFER_RES_1"

# 6. Retry exact same request with identical Idempotency-Key
echo -e "\n7️⃣  Re-submitting Identical Request (Testing Idempotency Key Deduping)..."
TRANSFER_RES_2=$(curl -s -X POST "$BASE_URL/payments/transfer" \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"senderWalletId\": \"$ALICE_WALLET_ID\",
    \"receiverWalletId\": \"$BOB_WALLET_ID\",
    \"amount\": 300.00,
    \"currency\": \"INR\"
  }")
echo "$TRANSFER_RES_2"

# 7. Check balances
echo -e "\n8️⃣  Alice Balance Check (Expected: ₹700.00):"
curl -s -X GET "$BASE_URL/wallets/$ALICE_WALLET_ID/balance" -H "Authorization: Bearer $ALICE_TOKEN"
echo ""

echo -e "\n9️⃣  Bob Balance Check (Expected: ₹300.00):"
curl -s -X GET "$BASE_URL/wallets/$BOB_WALLET_ID/balance" -H "Authorization: Bearer $BOB_TOKEN"
echo ""

# 8. Test Insufficient Funds Exception (RFC 7807 Problem Details)
echo -e "\n🔟 Testing Insufficient Funds Exception (Alice attempts to send ₹50,000)..."
curl -s -X POST "$BASE_URL/payments/transfer" \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H "Idempotency-Key: idemp-overdraft-$TIMESTAMP" \
  -H "Content-Type: application/json" \
  -d "{
    \"senderWalletId\": \"$ALICE_WALLET_ID\",
    \"receiverWalletId\": \"$BOB_WALLET_ID\",
    \"amount\": 50000.00,
    \"currency\": \"INR\"
  }"
echo ""

echo -e "\n======================================================================="
echo "🎉 DEMO EXECUTION COMPLETE"
echo "======================================================================="
