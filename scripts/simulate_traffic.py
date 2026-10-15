#!/usr/bin/env python3
"""
Reliability Traffic Simulator for Online Retail Application
Simulates baseline traffic, flash sale surges, latency degradation, and error spikes.
"""

import sys
import time
import random
import json
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor

BASE_URL = "http://localhost:8080"
PRODUCTS = ["PROD-001", "PROD-002", "PROD-003", "PROD-004", "PROD-005", "PROD-006"]

def send_request(url, method="GET", data=None):
    start = time.time()
    try:
        headers = {"Content-Type": "application/json"} if data else {}
        req_data = json.dumps(data).encode("utf-8") if data else None
        req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=5) as response:
            latency_ms = (time.time() - start) * 1000
            return response.status, latency_ms
    except urllib.error.HTTPError as e:
        latency_ms = (time.time() - start) * 1000
        return e.code, latency_ms
    except Exception as e:
        latency_ms = (time.time() - start) * 1000
        return 0, latency_ms

def simulate_order():
    pid = random.choice(PRODUCTS)
    payload = {
        "customerName": f"Shopper_{random.randint(1000, 9999)}",
        "customerEmail": f"user{random.randint(1000, 9999)}@traffic-surge.com",
        "items": [{"productId": pid, "quantity": random.randint(1, 3)}]
    }
    return send_request(f"{BASE_URL}/api/orders", method="POST", data=payload)

def simulate_browse():
    return send_request(f"{BASE_URL}/api/products", method="GET")

def run_traffic_surge(total_requests=100, concurrency=10):
    print("\n=======================================================")
    print(f"  LAUNCHING FLASH SALE SURGE: {total_requests} Requests (Concurrency: {concurrency})")
    print("=======================================================")
    start_total = time.time()
    latencies = []
    statuses = {}

    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = []
        for _ in range(total_requests):
            if random.random() < 0.65:
                futures.append(executor.submit(simulate_order))
            else:
                futures.append(executor.submit(simulate_browse))

        for f in futures:
            status, latency = f.result()
            latencies.append(latency)
            statuses[status] = statuses.get(status, 0) + 1
            print(f"-> Status: {status} | Latency: {latency:.1f}ms")

    duration = time.time() - start_total
    rps = total_requests / duration if duration > 0 else 0
    avg_lat = sum(latencies) / len(latencies) if latencies else 0
    p95_lat = sorted(latencies)[int(len(latencies) * 0.95)] if latencies else 0

    print("\n------------------ SURGE ANALYSIS REPORT ------------------")
    print(f"Total Requests Processed : {total_requests}")
    print(f"Duration                : {duration:.2f} seconds")
    print(f"Throughput Rate         : {rps:.2f} Req/Sec")
    print(f"Average Response Time   : {avg_lat:.2f} ms")
    print(f"95th Percentile Latency : {p95_lat:.2f} ms")
    print(f"Status Code Breakdown   : {statuses}")
    print("-----------------------------------------------------------\n")

def trigger_latency_alert():
    print("\n[!] Injecting 650ms artificial latency...")
    status, _ = send_request(f"{BASE_URL}/api/simulate/latency?delayMs=650", method="POST")
    print(f"Status: {status}. Prometheus histogram will record >500ms SLA breach. Check Grafana Alerts!")

def reset_simulation():
    print("\n[*] Normalizing application performance...")
    status, _ = send_request(f"{BASE_URL}/api/simulate/reset", method="POST")
    print(f"Status: {status}. Delays cleared.")

def main():
    print("\n=== CLOUD RETAIL RELIABILITY & STRESS TESTING SUITE ===")
    print("1. Run Baseline Traffic (20 requests)")
    print("2. Run Flash Sale Surge (100 concurrent requests)")
    print("3. Inject Latency Spike (>500ms) to trigger Grafana Alert")
    print("4. Reset System Performance to Normal")
    print("5. Continuous Stress Loop (Simulate Black Friday)")
    print("0. Exit")
    
    choice = input("\nSelect an option [1-5]: ").strip()
    if choice == "1":
        run_traffic_surge(total_requests=20, concurrency=3)
    elif choice == "2":
        run_traffic_surge(total_requests=100, concurrency=15)
    elif choice == "3":
        trigger_latency_alert()
    elif choice == "4":
        reset_simulation()
    elif choice == "5":
        print("\nStarting continuous shopping load (Press Ctrl+C to stop)...")
        try:
            while True:
                run_traffic_surge(total_requests=30, concurrency=8)
                time.sleep(2)
        except KeyboardInterrupt:
            print("\nStress loop stopped.")
    else:
        print("Exiting.")

if __name__ == "__main__":
    main()
