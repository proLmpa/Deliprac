pipeline {
    agent any

    environment {
        DOCKER_HUB_USER   = 'prolmpa'
        DOCKER_HUB_CRED   = credentials('docker-hub-cred')
        SSH_CRED          = 'deploy-ssh-key'
    }

    stages {

        // ── 1. Bring in sources ────────────────────────────────────────────
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/proLmpa/Deliprac.git',
                    credentialsId: 'github-cred'
            }
        }

        // ── 2. Build container images ──────────────────────────────────────
        stage('Build Images') {
            steps {
                script {
                    env.IMAGE_TAG = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                }
                sh './gradlew bootJar -x test'

                script {
                    def springServices = [
                        'bff-service', 'user-service', 'store-service',
                        'order-service', 'notification-service'
                    ]
                    for (svc in springServices) {
                        // Rename to app.jar so each Dockerfile has a fixed COPY target
                        sh "find ${svc}/build/libs -name '*.jar' ! -name '*plain*' ! -name 'app.jar' | head -1 | xargs -I{} cp {} ${svc}/build/libs/app.jar"
                        sh "docker build -t ${DOCKER_HUB_USER}/${svc}:${IMAGE_TAG} -t ${DOCKER_HUB_USER}/${svc}:latest ./${svc}"
                    }
                    // front-service: multi-stage Dockerfile handles npm build internally
                    sh "docker build -t ${DOCKER_HUB_USER}/front-service:${IMAGE_TAG} -t ${DOCKER_HUB_USER}/front-service:latest ./front-service"
                }
            }
        }

        // ── 3. Push to Docker Hub ──────────────────────────────────────────
        stage('Push Images') {
            steps {
                sh 'echo $DOCKER_HUB_CRED_PSW | docker login -u $DOCKER_HUB_CRED_USR --password-stdin'
                script {
                    def allServices = [
                        'bff-service', 'user-service', 'store-service',
                        'order-service', 'notification-service', 'front-service'
                    ]
                    for (svc in allServices) {
                        sh "docker push ${DOCKER_HUB_USER}/${svc}:${IMAGE_TAG}"
                        sh "docker push ${DOCKER_HUB_USER}/${svc}:latest"
                        // Remove local image immediately after push — Jenkins doesn't need to keep it
                        sh "docker rmi ${DOCKER_HUB_USER}/${svc}:${IMAGE_TAG} ${DOCKER_HUB_USER}/${svc}:latest || true"
                    }
                }
            }
        }

        // ── 4. Provision environment variables on every VM ────────────────
        stage('Provision') {
            parallel {
                stage('vm-front') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh "ssh -o StrictHostKeyChecking=no ${BFF_HOST} 'grep -qxF SPRING_PROFILES_ACTIVE=prod /etc/environment || echo SPRING_PROFILES_ACTIVE=prod | sudo tee -a /etc/environment'"
                        }
                    }
                }
                stage('vm-user') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh "ssh -o StrictHostKeyChecking=no ${USER_HOST} 'grep -qxF SPRING_PROFILES_ACTIVE=prod /etc/environment || echo SPRING_PROFILES_ACTIVE=prod | sudo tee -a /etc/environment'"
                        }
                    }
                }
                stage('vm-store') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh "ssh -o StrictHostKeyChecking=no ${STORE_HOST} 'grep -qxF SPRING_PROFILES_ACTIVE=prod /etc/environment || echo SPRING_PROFILES_ACTIVE=prod | sudo tee -a /etc/environment'"
                        }
                    }
                }
                stage('vm-order') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh "ssh -o StrictHostKeyChecking=no ${ORDER_HOST} 'grep -qxF SPRING_PROFILES_ACTIVE=prod /etc/environment || echo SPRING_PROFILES_ACTIVE=prod | sudo tee -a /etc/environment'"
                        }
                    }
                }
                stage('vm-notification') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh "ssh -o StrictHostKeyChecking=no ${NOTIFICATION_HOST} 'grep -qxF SPRING_PROFILES_ACTIVE=prod /etc/environment || echo SPRING_PROFILES_ACTIVE=prod | sudo tee -a /etc/environment'"
                        }
                    }
                }
            }
        }

        // ── 5. Deploy each service to its own VM in parallel ──────────────
        stage('Deploy') {
            parallel {

                // vm-front: bff-service + front-service (Nginx)
                stage('vm-front') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${BFF_HOST} '
                                    docker pull ${DOCKER_HUB_USER}/bff-service:${IMAGE_TAG}
                                    docker stop bff-service || true
                                    docker rm   bff-service || true
                                    docker run -d --name bff-service --network host -e SPRING_PROFILES_ACTIVE=prod --restart unless-stopped ${DOCKER_HUB_USER}/bff-service:${IMAGE_TAG}

                                    docker pull ${DOCKER_HUB_USER}/front-service:${IMAGE_TAG}
                                    docker stop front-service || true
                                    docker rm   front-service || true
                                    docker run -d --name front-service --network host --restart unless-stopped ${DOCKER_HUB_USER}/front-service:${IMAGE_TAG}
                                '
                            """
                        }
                    }
                }

                // vm-user: user-service (DB is a separate container already running on this VM)
                stage('vm-user') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${USER_HOST} '
                                    docker pull ${DOCKER_HUB_USER}/user-service:${IMAGE_TAG}
                                    docker stop user-service || true
                                    docker rm   user-service || true
                                    docker run -d --name user-service --network host -e SPRING_PROFILES_ACTIVE=prod --restart unless-stopped ${DOCKER_HUB_USER}/user-service:${IMAGE_TAG}
                                '
                            """
                        }
                    }
                }

                // vm-store: store-service
                stage('vm-store') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${STORE_HOST} '
                                    docker pull ${DOCKER_HUB_USER}/store-service:${IMAGE_TAG}
                                    docker stop store-service || true
                                    docker rm   store-service || true
                                    docker run -d --name store-service --network host -e SPRING_PROFILES_ACTIVE=prod --restart unless-stopped ${DOCKER_HUB_USER}/store-service:${IMAGE_TAG}
                                '
                            """
                        }
                    }
                }

                // vm-order: order-service
                stage('vm-order') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${ORDER_HOST} '
                                    docker pull ${DOCKER_HUB_USER}/order-service:${IMAGE_TAG}
                                    docker stop order-service || true
                                    docker rm   order-service || true
                                    docker run -d --name order-service --network host -e SPRING_PROFILES_ACTIVE=prod --restart unless-stopped ${DOCKER_HUB_USER}/order-service:${IMAGE_TAG}
                                '
                            """
                        }
                    }
                }

                // vm-notification: notification-service
                stage('vm-notification') {
                    steps {
                        sshagent(credentials: [SSH_CRED]) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${NOTIFICATION_HOST} '
                                    docker pull ${DOCKER_HUB_USER}/notification-service:${IMAGE_TAG}
                                    docker stop notification-service || true
                                    docker rm   notification-service || true
                                    docker run -d --name notification-service --network host -e SPRING_PROFILES_ACTIVE=prod --restart unless-stopped ${DOCKER_HUB_USER}/notification-service:${IMAGE_TAG}
                                '
                            """
                        }
                    }
                }

                // vm-monitoring: prometheus + alertmanager + grafana
                stage('vm-monitoring') {
                    steps {
                        withCredentials([
                            string(credentialsId: 'telegram-bot-token', variable: 'TELEGRAM_BOT_TOKEN'),
                            string(credentialsId: 'telegram-chat-id',   variable: 'TELEGRAM_CHAT_ID')
                        ]) {
                            sshagent(credentials: [SSH_CRED]) {
                                // Substitute VM hostnames + Telegram credentials into config templates
                                sh """
                                    envsubst < monitoring/prometheus.yml   > /tmp/baemin-prometheus.yml
                                    envsubst < monitoring/alertmanager.yml > /tmp/baemin-alertmanager.yml
                                """

                                // Copy all config files to the monitoring VM
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${MONITORING_HOST} 'mkdir -p /opt/monitoring/grafana/provisioning/datasources /opt/monitoring/grafana/provisioning/dashboards /opt/monitoring/grafana/dashboards'
                                    scp -o StrictHostKeyChecking=no /tmp/baemin-prometheus.yml                                           ${MONITORING_HOST}:/opt/monitoring/prometheus.yml
                                    scp -o StrictHostKeyChecking=no /tmp/baemin-alertmanager.yml                                         ${MONITORING_HOST}:/opt/monitoring/alertmanager.yml
                                    scp -o StrictHostKeyChecking=no monitoring/alerting-rules.yml                                        ${MONITORING_HOST}:/opt/monitoring/alerting-rules.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/provisioning/datasources/prometheus.yml            ${MONITORING_HOST}:/opt/monitoring/grafana/provisioning/datasources/prometheus.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/provisioning/dashboards/dashboard.yml              ${MONITORING_HOST}:/opt/monitoring/grafana/provisioning/dashboards/dashboard.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/dashboards/baemin.json                            ${MONITORING_HOST}:/opt/monitoring/grafana/dashboards/baemin.json
                                """

                                // Recreate containers
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${MONITORING_HOST} '
                                        docker network create monitoring 2>/dev/null || true

                                        docker stop prometheus alertmanager grafana || true
                                        docker rm   prometheus alertmanager grafana || true

                                        docker run -d --name prometheus --network monitoring --restart unless-stopped \\
                                            -p 9090:9090 \\
                                            -v /opt/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro \\
                                            -v /opt/monitoring/alerting-rules.yml:/etc/prometheus/alerting-rules.yml:ro \\
                                            prom/prometheus:v2.53.4

                                        docker run -d --name alertmanager --network monitoring --restart unless-stopped \\
                                            -p 9093:9093 \\
                                            -v /opt/monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro \\
                                            prom/alertmanager:v0.27.0

                                        docker run -d --name grafana --network monitoring --restart unless-stopped \\
                                            -p 3000:3000 \\
                                            -v /opt/monitoring/grafana/provisioning:/etc/grafana/provisioning:ro \\
                                            -v /opt/monitoring/grafana/dashboards:/var/lib/grafana/dashboards:ro \\
                                            grafana/grafana:11.4.0
                                    '
                                """
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            sh 'docker image prune -f || true'
        }
        success { echo 'All services deployed successfully.' }
        failure { echo 'Deployment failed — check the stage logs above.' }
    }
}
