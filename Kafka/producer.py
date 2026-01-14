from confluent_kafka import Producer
import json
import time

# 1. Update Bootstrap Servers to include all nodes
conf = {
    'bootstrap.servers': 'localhost:29092,localhost:29094,localhost:29096',
    'client.id': 'python-producer',
    'acks': 'all',                # "Advanced" Reliability: wait for all replicas
    'enable.idempotence': True    # "Advanced" Reliability: no duplicates
}

producer = Producer(conf)


def delivery_report(err, msg):
    if err is not None:
        print(f"❌ Failed: {err}")
    else:
        # Check the partition - with 3 nodes, you'll see this balanced
        print(
            f"✅ Success! Topic: {msg.topic()} | Partition: [{msg.partition()}] | Offset: {msg.offset()} | Key: {msg.key()}")


# 2. Update the topic name in producer.produce
topic_name = 'safe-topic'

print("🚀 Starting High Volume Production (1,000,000 messages)...")
for i in range(1000000):
    data = {"id": i, "message": "High Availability Test"}

    # Update happens here!
    while True:
        try:
            producer.produce(
                topic=topic_name,
                key=str(i),
                value=json.dumps(data).encode('utf-8'),
                # Optimization: Only print errors, or print success periodically
                callback=delivery_report if i % 10000 == 0 else None
            )
            break
        except BufferError:
            # Queue is full, give it some time to drain
            producer.poll(0.1)

    producer.poll(0)
    # time.sleep(0.1)  <-- REMOVED per request

producer.flush()
