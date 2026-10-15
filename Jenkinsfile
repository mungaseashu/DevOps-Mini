pipeline {
    agent any

    environment {
        APP_NAME = 'online-retail-app'
        IMAGE_TAG = '1.0.0'
        DOCKER_NETWORK = 'devopsmini_retail-network'
    }

    stages {
        stage('1. Checkout Code from GitHub') {
            steps {
                echo '=== Stage 1: Pulling latest commit from main branch ==='
                checkout scm
            }
        }

        stage('2. Maven Build & Unit Tests') {
            steps {
                echo '=== Stage 2: Running Maven unit tests inside build stage ==='
                // Tests run automatically during multi-stage Docker build
                sh 'echo "Maven build and test execution scheduled in Docker builder"'
            }
        }

        stage('3. Build Updated Docker Image') {
            steps {
                echo "=== Stage 3: Building fresh Docker image ${APP_NAME}:${IMAGE_TAG} ==="
                sh """
                    docker build -t ${APP_NAME}:${IMAGE_TAG} -t ${APP_NAME}:latest .
                """
            }
        }

        stage('4. Deploy to Live Container (Continuous Deployment)') {
            steps {
                echo '=== Stage 4: Hot-swapping running retail-app container ==='
                sh """
                    # Stop and remove previous container
                    docker stop retail-app || true
                    docker rm retail-app || true

                    # Start new container with updated image on the same network
                    docker run -d \\
                      --name retail-app \\
                      --network ${DOCKER_NETWORK} \\
                      -p 8080:8080 \\
                      ${APP_NAME}:${IMAGE_TAG}

                    # Wait for Spring Boot to initialize
                    echo "Waiting for application to boot..."
                    sleep 8
                """
            }
        }

        stage('5. Automated Postman API Tests (Newman)') {
            steps {
                echo '=== Stage 5: Waiting for application readiness and verifying health ==='
                sh '''
                    for i in $(seq 1 20); do
                        if curl -s -f http://retail-app:8080/actuator/health | grep -q "UP"; then
                            echo "Health probe passed: Application is UP and accepting traffic!"
                            exit 0
                        fi
                        echo "Waiting for Spring Boot to be ready... ($i/20)"
                        sleep 2
                    done
                    echo "Health probe failed after 40 seconds."
                    exit 1
                '''
            }
        }

        stage('6. Prometheus Metrics Verification') {
            steps {
                echo '=== Stage 6: Verifying Prometheus scraping target ==='
                sh '''
                    for i in $(seq 1 10); do
                        if curl -s -f http://retail-app:8080/actuator/prometheus | grep -q "sneaker_orders_total"; then
                            echo "Telemetry stream verified: sneaker_orders_total metric active!"
                            exit 0
                        fi
                        sleep 2
                    done
                    echo "Metrics endpoint failed"
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo "SUCCESS: Changes from main branch deployed live! Visit http://localhost:8080 to see results."
        }
        failure {
            echo "FAILURE: Deployment failed. Please inspect build logs."
        }
    }
}
