from confluent_kafka import Consumer, KafkaError

conf = {
    # 1. Point to all three brokers
    'bootstrap.servers': 'localhost:29092,localhost:29094,localhost:29096',

    # 2. Group ID: Kafka tracks offsets for this specific ID
    'group.id': 'ha-consumer-group',

    # 3. Handling offsets: Start from the beginning if no offset is found
    'auto.offset.reset': 'earliest',

    # 4. Advanced: Reliability settings
    'enable.auto.commit': True,     # Let Kafka handle position saving
    'auto.commit.interval.ms': 1000  # Save position every 1 second
}

consumer = Consumer(conf)

# Subscribe to the SAFE topic
consumer.subscribe(['safe-topic'])

print("🚀 Resilient Consumer Started... (Waiting for messages)")

try:
    while True:
        msg = consumer.poll(1.0)  # Wait 1 sec for data

        if msg is None:
            continue
        if msg.error():
            if msg.error().code() == KafkaError._PARTITION_EOF:
                # End of partition event (not an actual error)
                continue
            else:
                print(f"⚠️ Error (retrying): {msg.error()}")
                # Don't break! Just retry code usually handles reconnects.
                continue

        # Successfully received a message
        print(f"📩 Received: {msg.value().decode('utf-8')} "
              f"| Partition: {msg.partition()} "
              f"| Offset: {msg.offset()} "
              f"| Key: {msg.key().decode('utf-8') if msg.key() else 'None'}")

except KeyboardInterrupt:
    pass
finally:
    # Crucial for large scale: Cleanly unregister the consumer
    # This triggers an immediate rebalance for other consumers in the group
    consumer.close()
