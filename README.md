# Online Retail Application - CI/CD & Reliability Monitoring Pipeline

A cloud-native Retail E-Commerce application with automated continuous delivery and site reliability engineering (SRE) observability built with **Spring Boot, GitHub, Jenkins, Maven, Postman (Newman), Docker, Kubernetes, Prometheus, and Grafana**.

---

## 🏗️ Architecture & Pipeline Flow

```
[ Developer ] 
      │ (git push)
      ▼
[ GitHub Repository ]
      │ (Webhook)
      ▼
[ Jenkins Automation Server ]
      ├── 1. Maven Compile & Unit Tests (mvn test)
      ├── 2. Build & Tag Docker Container
      ├── 3. Deploy to Kubernetes Cluster (Rolling Update)
      ├── 4. Automated API Testing with Postman / Newman
      └── 5. Prometheus Scrape & SLA Verification
      │
      ▼
[ Kubernetes Cluster ] ────────────► [ Prometheus (Scrapes every 5s) ]
  • online-retail-app Pods                     │
  • Liveness & Readiness Probes                ▼
  • Horizontal Pod Autoscaler (HPA)    [ Grafana Dashboards & Alerts ]
                                               │
                                         (Fires alerts on:
                                          Latency > 500ms,
                                          Error Rate > 5%,
                                          Service Down)
```

---

## ⚡ Quick Start (Instant 1-Command Run)

You can launch the entire stack (**Retail App, Prometheus, Grafana, Jenkins**) with Docker Compose:

```bash
docker compose up -d --build
```

### Access URLs:
- **Online Retail Web Store**: [http://localhost:8080](http://localhost:8080)
- **Actuator Health Probe**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Prometheus Metrics Stream**: [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)
- **Prometheus Dashboard**: [http://localhost:9090](http://localhost:9090)
- **Grafana Dashboard**: [http://localhost:3000](http://localhost:3000) *(User: `admin`, Password: `admin`)*
- **Jenkins CI Server**: [http://localhost:8085](http://localhost:8085)

---

## 🧪 Testing the Project

### 1. Automated Postman API Tests (Newman)
Run the automated test suite directly in PowerShell:
```powershell
.\scripts\run_newman_tests.ps1
```
Or via Newman CLI:
```bash
newman run postman/retail-api-collection.json -e postman/retail-environment.json
```

### 2. Traffic Surge & Reliability Simulation
Run the load simulator to test application behavior under stress:
```powershell
.\scripts\simulate_traffic.ps1 -Requests 50
```
Or with Python:
```bash
python scripts/simulate_traffic.py
```

### 3. Demonstrating Alerts in Grafana
1. Open the web interface at [http://localhost:8080](http://localhost:8080).
2. Scroll to the **Reliability Lab** section.
3. Click **"Inject Latency (>500ms)"**.
4. Open Grafana at [http://localhost:3000](http://localhost:3000) or Prometheus at [http://localhost:9090/alerts](http://localhost:9090/alerts).
5. Watch the `HighResponseTime` alert transition from `Pending` to `Firing`!
6. Click **"Reset System"** on the website to normalize the latency and resolve the alert.

---

## ☸️ Deploying to Kubernetes

To deploy the manifests to your local Kubernetes cluster (Docker Desktop Kubernetes or Minikube):

```bash
# 1. Build and tag the Docker image
docker build -t online-retail-app:1.0.0 .

# 2. Apply Kubernetes Deployment and Service
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

# 3. Verify Rollout Status
kubectl rollout status deployment/online-retail-app
kubectl get pods -l app=online-retail-app
kubectl get svc online-retail-service
```
Access the application on Kubernetes at: `http://localhost:30080`.
