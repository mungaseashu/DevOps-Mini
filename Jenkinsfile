pipeline {
    agent any

    environment {
        APP_NAME = 'online-retail-app'
        IMAGE_TAG = "1.0.${BUILD_NUMBER}"
        K8S_NAMESPACE = 'default'
        BASE_URL = 'http://localhost:8080'
    }

    stages {
        stage('1. Checkout Source') {
            steps {
                echo '=== Stage 1: Pulling latest changes from GitHub ==='
                checkout scm
            }
        }

        stage('2. Maven Compile & Unit Tests') {
            steps {
                echo '=== Stage 2: Compiling Java with Maven & Running Unit Tests ==='
                // Runs Maven build inside container or host runner
                sh '''
                    if command -v mvn &> /dev/null; then
                        mvn clean package
                    elif [ -f "./mvnw" ]; then
                        ./mvnw clean package
                    else
                        docker run --rm -v "$(pwd)":/app -w /app maven:3.9.6-eclipse-temurin-17 mvn clean package
                    fi
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('3. Build & Tag Docker Container') {
            steps {
                echo "=== Stage 3: Building Docker Image ${APP_NAME}:${IMAGE_TAG} ==="
                sh """
                    docker build -t ${APP_NAME}:${IMAGE_TAG} -t ${APP_NAME}:latest .
                """
            }
        }

        stage('4. Deploy to Kubernetes Cluster') {
            steps {
                echo '=== Stage 4: Applying Kubernetes Manifests with Rolling Update ==='
                sh """
                    # Apply deployment and service
                    kubectl apply -f k8s/deployment.yaml -n ${K8S_NAMESPACE}
                    kubectl apply -f k8s/service.yaml -n ${K8S_NAMESPACE}
                    
                    # Wait for rollout completion (Zero-Downtime verification)
                    kubectl rollout status deployment/${APP_NAME} -n ${K8S_NAMESPACE} --timeout=90s
                """
            }
        }

        stage('5. Postman Automated API Tests (Newman)') {
            steps {
                echo '=== Stage 5: Executing Newman Postman Test Suite ==='
                sh """
                    if command -v newman &> /dev/null; then
                        newman run postman/retail-api-collection.json -e postman/retail-environment.json --reporters cli,json --reporter-json-export newman-report.json
                    else
                        # Run via official Postman Newman Docker image
                        docker run --rm --network="host" -v "\$(pwd)/postman":/etc/newman postman/newman run /etc/newman/retail-api-collection.json -e /etc/newman/retail-environment.json
                    fi
                """
            }
        }

        stage('6. Prometheus Metrics Scrape Check') {
            steps {
                echo '=== Stage 6: Verifying Prometheus Metrics Endpoint ==='
                sh """
                    curl -s -f http://localhost:8080/actuator/prometheus | grep -E "retail_orders_total|jvm_memory_used_bytes" || exit 1
                    echo 'SUCCESS: Application metrics successfully registered with Prometheus!'
                """
            }
        }
    }

    post {
        success {
            echo "SUCCESS: Pipeline completed! Retail App deployed and verified on Kubernetes with Prometheus observability."
        }
        failure {
            echo "FAILURE: Pipeline failed at one of the stages. Inspect logs and rollback if necessary."
        }
    }
}
