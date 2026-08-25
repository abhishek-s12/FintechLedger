#!/usr/bin/env bash
# ==============================================================================
# Kafka Topic Initializer for FintechLedger
# ==============================================================================

set -euo pipefail

KAFKA_CONTAINER=${1:-"fintech-kafka"}
BOOTSTRAP_SERVER="localhost:9092"

echo "🚀 Initializing FintechLedger Kafka Topics..."

TOPICS=(
  "fintech.payment.completed:3:1"
  "fintech.payment.failed:3:1"
  "fintech.wallet.created:3:1"
  "fintech.fraud.alert:3:1"
)

for topic_config in "${TOPICS[@]}"; do
  IFS=":" read -r topic partitions rf <<< "$topic_config"
  echo "📦 Creating topic '$topic' with $partitions partitions and replication-factor $rf..."
  docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP_SERVER" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$rf"
done

echo "✅ All Kafka topics successfully verified / created:"
docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --list
