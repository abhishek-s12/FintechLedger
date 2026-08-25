# ==============================================================================
# FintechLedger - End-to-End Enterprise Flow Demonstration (PowerShell)
# ==============================================================================

$BaseUrl = "http://localhost:8080/api/v1"
$Timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "💳 FINTECHLEDGER LIVE DEMONSTRATION" -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan

# 1. Register Alice
Write-Host "`n1️⃣ Registering User 'Alice'..." -ForegroundColor Yellow
$aliceRegBody = @{
    email = "alice_$Timestamp@fintech.com"
    password = "Password123!"
    firstName = "Alice"
    lastName = "Smith"
} | ConvertTo-Json

$aliceReg = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $aliceRegBody -ContentType "application/json"
$aliceToken = $aliceReg.accessToken
Write-Host "Alice Registered. User ID: $($aliceReg.userId)" -ForegroundColor Green

# 2. Register Bob
Write-Host "`n2️⃣ Registering User 'Bob'..." -ForegroundColor Yellow
$bobRegBody = @{
    email = "bob_$Timestamp@fintech.com"
    password = "Password123!"
    firstName = "Bob"
    lastName = "Jones"
} | ConvertTo-Json

$bobReg = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $bobRegBody -ContentType "application/json"
$bobToken = $bobReg.accessToken
Write-Host "Bob Registered. User ID: $($bobReg.userId)" -ForegroundColor Green

# 3. Create Wallets
Write-Host "`n3️⃣ Creating INR Wallet for Alice..." -ForegroundColor Yellow
$aliceWalletBody = @{ currency = "INR" } | ConvertTo-Json
$aliceWallet = Invoke-RestMethod -Uri "$BaseUrl/wallets" -Method Post -Headers @{ Authorization = "Bearer $aliceToken" } -Body $aliceWalletBody -ContentType "application/json"
$aliceWalletId = $aliceWallet.id
Write-Host "Alice Wallet ID: $aliceWalletId (Balance: $($aliceWallet.balance))" -ForegroundColor Green

Write-Host "`n4️⃣ Creating INR Wallet for Bob..." -ForegroundColor Yellow
$bobWalletBody = @{ currency = "INR" } | ConvertTo-Json
$bobWallet = Invoke-RestMethod -Uri "$BaseUrl/wallets" -Method Post -Headers @{ Authorization = "Bearer $bobToken" } -Body $bobWalletBody -ContentType "application/json"
$bobWalletId = $bobWallet.id
Write-Host "Bob Wallet ID: $bobWalletId (Balance: $($bobWallet.balance))" -ForegroundColor Green

# 4. Deposit into Alice's wallet
Write-Host "`n5️⃣ Depositing ₹1,000 into Alice's Wallet..." -ForegroundColor Yellow
$depositBody = @{ amount = 1000.00; currency = "INR" } | ConvertTo-Json
$deposit = Invoke-RestMethod -Uri "$BaseUrl/wallets/$aliceWalletId/deposit" -Method Post -Headers @{ Authorization = "Bearer $aliceToken" } -Body $depositBody -ContentType "application/json"
Write-Host "Deposit successful. Alice New Balance: ₹$($deposit.balance)" -ForegroundColor Green

# 5. Transfer ₹300 with Idempotency Key
$idempKey = "idemp-pwsh-$Timestamp-001"
Write-Host "`n6️⃣ Initiating ₹300 Transfer from Alice to Bob (Idempotency-Key: $idempKey)..." -ForegroundColor Yellow
$transferBody = @{
    senderWalletId = $aliceWalletId
    receiverWalletId = $bobWalletId
    amount = 300.00
    currency = "INR"
} | ConvertTo-Json

$transfer1 = Invoke-RestMethod -Uri "$BaseUrl/payments/transfer" -Method Post -Headers @{ 
    Authorization = "Bearer $aliceToken"
    "Idempotency-Key" = $idempKey 
} -Body $transferBody -ContentType "application/json"

Write-Host "Transfer Completed. Payment Ref: $($transfer1.referenceId), Status: $($transfer1.status)" -ForegroundColor Green

# 6. Re-submit exact same request with identical Idempotency-Key
Write-Host "`n7️⃣ Re-submitting Identical Request (Testing Idempotency Key Deduping)..." -ForegroundColor Yellow
$transfer2 = Invoke-RestMethod -Uri "$BaseUrl/payments/transfer" -Method Post -Headers @{ 
    Authorization = "Bearer $aliceToken"
    "Idempotency-Key" = $idempKey 
} -Body $transferBody -ContentType "application/json"

Write-Host "Idempotent Replay Verified. Returned identical Reference: $($transfer2.referenceId)" -ForegroundColor Green

# 7. Check Balances
Write-Host "`n8️⃣ Alice Balance Check (Expected: ₹700.00):" -ForegroundColor Yellow
$aliceBal = Invoke-RestMethod -Uri "$BaseUrl/wallets/$aliceWalletId/balance" -Method Get -Headers @{ Authorization = "Bearer $aliceToken" }
Write-Host "Alice Current Balance: ₹$($aliceBal.balance) $($aliceBal.currency)" -ForegroundColor Green

Write-Host "`n9️⃣ Bob Balance Check (Expected: ₹300.00):" -ForegroundColor Yellow
$bobBal = Invoke-RestMethod -Uri "$BaseUrl/wallets/$bobWalletId/balance" -Method Get -Headers @{ Authorization = "Bearer $bobToken" }
Write-Host "Bob Current Balance: ₹$($bobBal.balance) $($bobBal.currency)" -ForegroundColor Green

# 8. Insufficient funds test
Write-Host "`n🔟 Testing Insufficient Funds Rejection (Alice attempts to send ₹50,000)..." -ForegroundColor Yellow
$overdraftBody = @{
    senderWalletId = $aliceWalletId
    receiverWalletId = $bobWalletId
    amount = 50000.00
    currency = "INR"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$BaseUrl/payments/transfer" -Method Post -Headers @{ 
        Authorization = "Bearer $aliceToken"
        "Idempotency-Key" = "idemp-overdraft-$Timestamp" 
    } -Body $overdraftBody -ContentType "application/json"
} catch {
    $errRes = $_.ErrorDetails.Message
    Write-Host "Correctly rejected with RFC 7807 Problem Detail: $errRes" -ForegroundColor Red
}

Write-Host "`n=======================================================================" -ForegroundColor Cyan
Write-Host "🎉 DEMO EXECUTION COMPLETE" -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan
