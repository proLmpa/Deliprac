pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_HUB_USER   = 'prolmpa'
        DOCKER_HUB_CRED   = credentials('docker-hub-cred')
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
                sh './gradlew bootJar -x test --parallel --build-cache'

                script {
                    def springServices = [
                        'bff-service', 'user-service', 'store-service',
                        'order-service', 'notification-service'
                    ]
                    // Copy jars first (sequential — fast)
                    for (svc in springServices) {
                        sh "find ${svc}/build/libs -name '*.jar' ! -name '*plain*' ! -name 'app.jar' | head -1 | xargs -I{} cp {} ${svc}/build/libs/app.jar"
                    }
                    // Build all images in parallel
                    def buildTasks = [:]
                    for (svc in springServices) {
                        def s = svc
                        buildTasks[s] = {
                            sh "docker build -t ${DOCKER_HUB_USER}/${s}:${IMAGE_TAG} -t ${DOCKER_HUB_USER}/${s}:latest ./${s}"
                        }
                    }
                    buildTasks['front-service'] = {
                        sh "docker build -t ${DOCKER_HUB_USER}/front-service:${IMAGE_TAG} -t ${DOCKER_HUB_USER}/front-service:latest ./front-service"
                    }
                    parallel buildTasks
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
                    def pushTasks = [:]
                    for (svc in allServices) {
                        def s = svc
                        pushTasks[s] = {
                            sh "docker push ${DOCKER_HUB_USER}/${s}:${IMAGE_TAG}"
                            sh "docker push ${DOCKER_HUB_USER}/${s}:latest"
                            sh "docker rmi ${DOCKER_HUB_USER}/${s}:${IMAGE_TAG} ${DOCKER_HUB_USER}/${s}:latest || true"
                        }
                    }
                    parallel pushTasks
                }
            }
        }

        // ── 4. Deploy ──────────────────────────────────────────────────────
        stage('Deploy') {
            steps {
                script {
                    def tasks = [:]

                    // ── Kubernetes: all 6 API services ─────────────────────
                    tasks['k8s'] = {
                        withKubeConfig([credentialsId: 'minikube-kubeconfig']) {
                            // Namespace
                            sh 'kubectl apply -f k8s/namespace.yaml'

                            // Secrets (injected from Jenkins credentials — not stored in git)
                            sh """
                                kubectl -n baemin create secret generic common-secret \
                                    --from-literal=JWT_SECRET=${env.JWT_SECRET} \
                                    --dry-run=client -o yaml | kubectl apply -f -
                                kubectl -n baemin create secret generic bff-secret \
                                    --from-literal=BFF_HMAC_USER_SECRET=${env.BFF_HMAC_USER_SECRET} \
                                    --from-literal=BFF_HMAC_STORE_SECRET=${env.BFF_HMAC_STORE_SECRET} \
                                    --from-literal=BFF_HMAC_ORDER_SECRET=${env.BFF_HMAC_ORDER_SECRET} \
                                    --from-literal=BFF_HMAC_NOTIF_SECRET=${env.BFF_HMAC_NOTIF_SECRET} \
                                    --dry-run=client -o yaml | kubectl apply -f -
                                kubectl -n baemin create secret generic user-secret \
                                    --from-literal=DB_USERNAME=${env.USER_DB_USERNAME} \
                                    --from-literal=DB_PASSWORD=${env.USER_DB_PASSWORD} \
                                    --from-literal=BFF_HMAC_USER_SECRET=${env.BFF_HMAC_USER_SECRET} \
                                    --dry-run=client -o yaml | kubectl apply -f -
                                kubectl -n baemin create secret generic store-secret \
                                    --from-literal=DB_USERNAME=${env.STORE_DB_USERNAME} \
                                    --from-literal=DB_PASSWORD=${env.STORE_DB_PASSWORD} \
                                    --from-literal=BFF_HMAC_STORE_SECRET=${env.BFF_HMAC_STORE_SECRET} \
                                    --dry-run=client -o yaml | kubectl apply -f -
                                kubectl -n baemin create secret generic order-secret \
                                    --from-literal=DB_USERNAME=${env.ORDER_DB_USERNAME} \
                                    --from-literal=DB_PASSWORD=${env.ORDER_DB_PASSWORD} \
                                    --from-literal=BFF_HMAC_ORDER_SECRET=${env.BFF_HMAC_ORDER_SECRET} \
                                    --dry-run=client -o yaml | kubectl apply -f -
                                kubectl -n baemin create secret generic notification-secret \
                                    --from-literal=DB_USERNAME=${env.NOTIF_DB_USERNAME} \
                                    --from-literal=DB_PASSWORD=${env.NOTIF_DB_PASSWORD} \
                                    --from-literal=BFF_HMAC_NOTIF_SECRET=${env.BFF_HMAC_NOTIF_SECRET} \
                                    --dry-run=client -o yaml | kubectl apply -f -
                            """

                            // ConfigMaps and all manifests
                            sh 'kubectl apply -f k8s/configmaps/'
                            sh 'kubectl apply -f k8s/deployments/'
                            sh 'kubectl apply -f k8s/services/'

                            // Rolling update to new image tag
                            def allServices = ['bff-service', 'user-service', 'store-service',
                                               'order-service', 'notification-service', 'front-service']
                            for (svc in allServices) {
                                sh "kubectl -n baemin set image deployment/${svc} ${svc}=${env.DOCKER_HUB_USER}/${svc}:${env.IMAGE_TAG}"
                            }

                            // Wait for rollouts in parallel
                            def rolloutTasks = [:]
                            for (svc in allServices) {
                                def s = svc
                                rolloutTasks[s] = {
                                    sh "kubectl -n baemin rollout status deployment/${s} --timeout=180s"
                                }
                            }
                            parallel rolloutTasks
                        }
                    }

                    // ── VM-monitoring: config substitution + scp + systemctl reload ──
                    tasks['vm-monitoring'] = {
                        withCredentials([
                            string(credentialsId: 'telegram-bot-token', variable: 'TELEGRAM_BOT_TOKEN'),
                            string(credentialsId: 'telegram-chat-id',   variable: 'TELEGRAM_CHAT_ID'),
                            string(credentialsId: 'MINIKUBE_IP',        variable: 'MINIKUBE_IP')
                        ]) {
                            sshagent(credentials: ['deploy-ssh-key']) {
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
                                // Reload Prometheus and Alertmanager (config-only reload — no restart needed)
                                // Restart Grafana to pick up provisioning changes
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${env.MONITORING_HOST} '
                                        sudo systemctl reload prometheus
                                        sudo systemctl reload prometheus-alertmanager
                                        sudo systemctl restart grafana-server
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
            node('') {
                sh 'docker logout || true'
                sh 'docker image prune -af || true'
                cleanWs()
            }
        }
        success { echo 'All services deployed successfully.' }
        failure { echo 'Deployment failed — check the stage logs above.' }
    }
}
