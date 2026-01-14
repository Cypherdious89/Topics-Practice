import subprocess
import time
import re
import os
import signal
import sys

def setup_topic():
    print("🛠️ Setting up Topic 'safe-topic' with 3 partitions...")
    # Delete if exists (ignore error)
    subprocess.run(["docker", "exec", "kafka-1", "kafka-topics", "--delete", "--topic", "safe-topic", "--bootstrap-server", "kafka-1:19092"], stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL)
    time.sleep(2)
    # Create with 3 partitions, 3 replicas
    subprocess.run(["docker", "exec", "kafka-1", "kafka-topics", "--create", "--topic", "safe-topic", "--partitions", "3", "--replication-factor", "3", "--bootstrap-server", "kafka-1:19092"])
    time.sleep(2)

def run_test():
    setup_topic()

    print("🧹 Cleaning up old logs...")
    for f in ['c1.log', 'c2.log', 'c3.log', 'producer.log']:
        if os.path.exists(f):
            os.remove(f)

    # 1. Start 3 Consumers (Unbuffered output)
    print("👥 Starting 3 Consumers...")
    c1 = subprocess.Popen(["python3", "-u", "consumer.py"], stdout=open("c1.log", "w"), stderr=subprocess.STDOUT)
    c2 = subprocess.Popen(["python3", "-u", "consumer.py"], stdout=open("c2.log", "w"), stderr=subprocess.STDOUT)
    c3 = subprocess.Popen(["python3", "-u", "consumer.py"], stdout=open("c3.log", "w"), stderr=subprocess.STDOUT)
    
    # Give them time to join the group
    print("⏳ Waiting 10s for consumers to join and rebalance...")
    time.sleep(10)
    
    # 2. Start Producer
    # Note: Producer sends 1,000,000 messages. This will take time.
    print("📣 Starting Producer (sending 1,000,000 messages)...")
    producer = subprocess.Popen(["python3", "-u", "producer.py"], stdout=open("producer.log", "w"), stderr=subprocess.STDOUT)
    
    # 3. Wait a bit, then kill kafka-1
    # Increase wait before kill to ensure good flow of data first?
    time.sleep(20)
    print("🔥 CHAOS: Killing kafka-1 container...")
    subprocess.run(["docker", "stop", "kafka-1"])
    
    # 4. Wait for producer to finish
    print("⏳ Waiting for producer to finish (this may take a while)...")
    producer.wait()
    print("✅ Producer finished.")
    
    # 5. Bring successful verification? Restart kafka-1 to restore state
    print("🚑 Restarting kafka-1...")
    subprocess.run(["docker", "start", "kafka-1"])
    
    # 6. Wait a bit for consumers to pick up anything leftover (if any)
    print("🕒 Waiting 60s for consumers to drain...")
    time.sleep(60)
    
    # 7. Stop consumers
    print("🛑 Stopping consumers...")
    c1.terminate()
    c2.terminate()
    c3.terminate()
    
    # 8. Analyze Logs
    analyze_logs()

def analyze_logs():
    print("\n📊 ANALYZING RESULTS")
    received_ids = set()
    
    consumers_received = {}
    
    for log_file in ['c1.log', 'c2.log', 'c3.log']:
        if not os.path.exists(log_file):
            print(f"⚠️ {log_file} not found!")
            continue
            
        count = 0
        with open(log_file, 'r') as f:
            for line in f:
                # Regex to match: {"id": 123, ...}
                match = re.search(r'"id": (\d+)', line)
                if match:
                    received_ids.add(int(match.group(1)))
                    count += 1
        print(f"  - {log_file} received {count} messages")
        consumers_received[log_file] = count
    
    expected_ids = set(range(1000000))
    missing_ids = expected_ids - received_ids
    
    total_received = len(received_ids)
    print(f"\n🔢 Total Unique Messages Received: {total_received}/1000000")
    
    # Check Distribution
    active_consumers = sum(1 for c in consumers_received.values() if c > 0)
    if active_consumers < 3:
        print(f"⚠️ WARNING: Only {active_consumers} consumers received messages! Check partition assignment.")
    else:
        print(f"✅ Distribution confirmed: All 3 consumers processed messages.")

    if not missing_ids:
        print("✅ SUCCESS! All 1,000,000 messages were received without loss.")
    else:
        print(f"❌ FAILURE! Missing {len(missing_ids)} messages.")
        # print(f"   Missing IDs: {sorted(list(missing_ids))[:20]}...")

if __name__ == "__main__":
    try:
        run_test()
    except KeyboardInterrupt:
        print("\nTest interrupted. Cleaning up...")
        # Add cleanup logic if needed
