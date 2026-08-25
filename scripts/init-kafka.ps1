# ==============================================================================
# Kafka Topic Initializer for FintechLedger (PowerShell)
# ==============================================================================

param (
    [string]$KafkaContainer = "fintech-kafka",
    [string]$BootstrapServer = "localhost:9092"
)

Write-Host "🚀 Initializing FintechLedger Kafka Topics..." -ForegroundColor Cyan

$topics = @(
    @{ Name = "fintech.payment.completed"; Partitions = 3; Replication = 1 },
    @{ Name = "fintech.payment.failed"; Partitions = 3; Replication = 1 },
    @{ Name = "fintech.wallet.created"; Partitions = 3; Replication = 1 },
    @{ Name = "fintech.fraud.alert"; Partitions = 3; Replication = 1 }
)

foreach ($t in $topics) {
    Write-Host "📦 Creating topic '$($t.Name)' ($($t.Partitions) partitions)..." -ForegroundColor Yellow
    docker exec $KafkaContainer /opt/kafka/bin/kafka-topics.sh `
        --bootstrap-server $BootstrapServer `
        --create `
        --if-not-exists `
        --topic $t.Name `
        --partitions $t.Partitions `
        --replication-factor $t.Replication
}

Write-Host "✅ Active Kafka Topics in cluster:" -ForegroundColor Green
docker exec $KafkaContainer /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server $BootstrapServer `
    --list
