// ── Helper: pull → stop → rm → run for a single-service VM ──────────────────
def deployService(String host, String svc, String extraEnv = '') {
    sshagent(credentials: [env.SSH_CRED]) {
        sh """
            ssh -o StrictHostKeyChecking=no ${host} '
                docker pull ${env.DOCKER_HUB_USER}/${svc}:${env.IMAGE_TAG}
                docker stop ${svc} || true
                docker rm   ${svc} || true
                docker run -d --name ${svc} --network host \
                    -e SPRING_PROFILES_ACTIVE=prod \
                    ${extraEnv} \
                    --restart unless-stopped ${env.DOCKER_HUB_USER}/${svc}:${env.IMAGE_TAG}
            '
        """
    }
}

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
            steps {
                script {
                    def vmHosts = [
                        'vm-front':        env.BFF_HOST,
                        'vm-user':         env.USER_HOST,
                        'vm-store':        env.STORE_HOST,
                        'vm-order':        env.ORDER_HOST,
                        'vm-notification': env.NOTIFICATION_HOST
                    ]
                    parallel vmHosts.collectEntries { name, host ->
                        [(name): {
                            sshagent(credentials: [env.SSH_CRED]) {
                                sh "ssh -o StrictHostKeyChecking=no ${host} 'grep -qxF SPRING_PROFILES_ACTIVE=prod /etc/environment || echo SPRING_PROFILES_ACTIVE=prod | sudo tee -a /etc/environment'"
                            }
                        }]
                    }
                }
            }
        }

        // ── 5. Deploy each service to its own VM in parallel ──────────────
        stage('Deploy') {
            steps {
                script {
                    def tasks = [:]

                    // vm-front: two services on the same host
                    tasks['vm-front'] = {
                        sshagent(credentials: [env.SSH_CRED]) {
                            sh """
                                ssh -o StrictHostKeyChecking=no ${env.BFF_HOST} '
                                    docker pull ${env.DOCKER_HUB_USER}/bff-service:${env.IMAGE_TAG}
                                    docker stop bff-service || true
                                    docker rm   bff-service || true
                                    docker run -d --name bff-service --network host \
                                        -e SPRING_PROFILES_ACTIVE=prod \
                                        -e USER_SERVICE_URL=${env.USER_SERVICE_URL} \
                                        -e STORE_SERVICE_URL=${env.STORE_SERVICE_URL} \
                                        -e ORDER_SERVICE_URL=${env.ORDER_SERVICE_URL} \
                                        -e NOTIFICATION_SERVICE_URL=${env.NOTIFICATION_SERVICE_URL} \
                                        -e BFF_HMAC_USER_SECRET=${env.BFF_HMAC_USER_SECRET} \
                                        -e BFF_HMAC_STORE_SECRET=${env.BFF_HMAC_STORE_SECRET} \
                                        -e BFF_HMAC_ORDER_SECRET=${env.BFF_HMAC_ORDER_SECRET} \
                                        -e BFF_HMAC_NOTIF_SECRET=${env.BFF_HMAC_NOTIF_SECRET} \
                                        --restart unless-stopped ${env.DOCKER_HUB_USER}/bff-service:${env.IMAGE_TAG}

                                    docker pull ${env.DOCKER_HUB_USER}/front-service:${env.IMAGE_TAG}
                                    docker stop front-service || true
                                    docker rm   front-service || true
                                    docker run -d --name front-service --network host --restart unless-stopped ${env.DOCKER_HUB_USER}/front-service:${env.IMAGE_TAG}
                                '
                            """
                        }
                    }

                    // single-service backend VMs — identical pattern, different host/service
                    def backendVMs = [
                        [name: 'vm-user',         host: env.USER_HOST,         svc: 'user-service',
                         extraEnv: "-e DB_URL=${env.USER_DB_URL} -e DB_USERNAME=${env.USER_DB_USERNAME} -e DB_PASSWORD=${env.USER_DB_PASSWORD} -e JWT_SECRET=${env.JWT_SECRET} -e BFF_HMAC_USER_SECRET=${env.BFF_HMAC_USER_SECRET}"],
                        [name: 'vm-store',        host: env.STORE_HOST,        svc: 'store-service',
                         extraEnv: "-e DB_URL=${env.STORE_DB_URL} -e DB_USERNAME=${env.STORE_DB_USERNAME} -e DB_PASSWORD=${env.STORE_DB_PASSWORD} -e JWT_SECRET=${env.JWT_SECRET} -e BFF_HMAC_STORE_SECRET=${env.BFF_HMAC_STORE_SECRET}"],
                        [name: 'vm-order',        host: env.ORDER_HOST,        svc: 'order-service',
                         extraEnv: "-e DB_URL=${env.ORDER_DB_URL} -e DB_USERNAME=${env.ORDER_DB_USERNAME} -e DB_PASSWORD=${env.ORDER_DB_PASSWORD} -e JWT_SECRET=${env.JWT_SECRET} -e BFF_HMAC_ORDER_SECRET=${env.BFF_HMAC_ORDER_SECRET}"],
                        [name: 'vm-notification', host: env.NOTIFICATION_HOST, svc: 'notification-service',
                         extraEnv: "-e DB_URL=${env.NOTIF_DB_URL} -e DB_USERNAME=${env.NOTIF_DB_USERNAME} -e DB_PASSWORD=${env.NOTIF_DB_PASSWORD} -e JWT_SECRET=${env.JWT_SECRET} -e BFF_HMAC_NOTIF_SECRET=${env.BFF_HMAC_NOTIF_SECRET} -e REDIS_HOST=${env.REDIS_HOST} -e REDIS_PORT=${env.REDIS_PORT}"]
                    ]
                    for (vm in backendVMs) {
                        def name     = vm.name
                        def host     = vm.host
                        def svc      = vm.svc
                        def extraEnv = vm.extraEnv
                        tasks[name] = { deployService(host, svc, extraEnv) }
                    }

                    // vm-monitoring: distinct — config substitution + scp + 3 containers
                    tasks['vm-monitoring'] = {
                        withCredentials([
                            string(credentialsId: 'telegram-bot-token', variable: 'TELEGRAM_BOT_TOKEN'),
                            string(credentialsId: 'telegram-chat-id',   variable: 'TELEGRAM_CHAT_ID')
                        ]) {
                            sshagent(credentials: [env.SSH_CRED]) {
                                sh """
                                    envsubst < monitoring/prometheus.yml   > /tmp/baemin-prometheus.yml
                                    envsubst < monitoring/alertmanager.yml > /tmp/baemin-alertmanager.yml
                                """
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${env.MONITORING_HOST} 'mkdir -p /opt/monitoring/grafana/provisioning/datasources /opt/monitoring/grafana/provisioning/dashboards /opt/monitoring/grafana/dashboards'
                                    scp -o StrictHostKeyChecking=no /tmp/baemin-prometheus.yml                                        ${env.MONITORING_HOST}:/opt/monitoring/prometheus.yml
                                    scp -o StrictHostKeyChecking=no /tmp/baemin-alertmanager.yml                                      ${env.MONITORING_HOST}:/opt/monitoring/alertmanager.yml
                                    scp -o StrictHostKeyChecking=no monitoring/alerting-rules.yml                                     ${env.MONITORING_HOST}:/opt/monitoring/alerting-rules.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/provisioning/datasources/prometheus.yml         ${env.MONITORING_HOST}:/opt/monitoring/grafana/provisioning/datasources/prometheus.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/provisioning/dashboards/dashboard.yml           ${env.MONITORING_HOST}:/opt/monitoring/grafana/provisioning/dashboards/dashboard.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/dashboards/baemin.json                         ${env.MONITORING_HOST}:/opt/monitoring/grafana/dashboards/baemin.json
                                """
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${env.MONITORING_HOST} '
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

                    parallel tasks
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
