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

        // ── 0. Pre-build cleanup ───────────────────────────────────────────
        stage('Pre-cleanup') {
            steps {
                sh 'docker image prune -af || true'
                sh 'docker container prune -f || true'
                sh 'docker volume prune -f || true'
            }
        }

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

                    // ── ArgoCD: update image tag and sync ──────────────────
                    tasks['k8s'] = {
                        withCredentials([
                            string(credentialsId: 'argocd-token',  variable: 'ARGOCD_AUTH_TOKEN'),
                            string(credentialsId: 'argocd-server', variable: 'ARGOCD_SERVER')
                        ]) {
                            sh """
                                argocd app terminate-op baemin \
                                    --auth-token \$ARGOCD_AUTH_TOKEN \
                                    --server \$ARGOCD_SERVER \
                                    --grpc-web \
                                    --insecure || true
                                argocd app set baemin \
                                    --auth-token \$ARGOCD_AUTH_TOKEN \
                                    --server \$ARGOCD_SERVER \
                                    --grpc-web \
                                    --insecure \
                                    -p images.tag=${env.IMAGE_TAG}
                                argocd app wait baemin \
                                    --auth-token \$ARGOCD_AUTH_TOKEN \
                                    --server \$ARGOCD_SERVER \
                                    --grpc-web \
                                    --insecure \
                                    --operation \
                                    --timeout 180
                            """
                        }
                    }

                    // ── VM-monitoring: config + scp + systemctl reload ────────
                    tasks['vm-monitoring'] = {
                        withCredentials([
                            string(credentialsId: 'telegram-bot-token', variable: 'TELEGRAM_BOT_TOKEN'),
                            string(credentialsId: 'telegram-chat-id',   variable: 'TELEGRAM_CHAT_ID'),
                            string(credentialsId: 'MONITORING_HOST',    variable: 'MONITORING_HOST')
                        ]) {
                            sshagent(credentials: ['deploy-ssh-key']) {
                                // prometheus.yml has no variable placeholders — copy directly.
                                // alertmanager.yml still needs Telegram credentials substituted.
                                sh 'envsubst < monitoring/alertmanager.yml > /tmp/baemin-alertmanager.yml'
                                sh '''
                                    ssh -o StrictHostKeyChecking=no $MONITORING_HOST 'mkdir -p /opt/monitoring/grafana/provisioning/datasources /opt/monitoring/grafana/provisioning/dashboards /opt/monitoring/grafana/dashboards'
                                    scp -o StrictHostKeyChecking=no monitoring/prometheus.yml                                                $MONITORING_HOST:/opt/monitoring/prometheus.yml
                                    scp -o StrictHostKeyChecking=no /tmp/baemin-alertmanager.yml                                             $MONITORING_HOST:/opt/monitoring/alertmanager.yml
                                    scp -o StrictHostKeyChecking=no monitoring/alerting-rules.yml                                            $MONITORING_HOST:/opt/monitoring/alerting-rules.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/provisioning/datasources/prometheus.yml        $MONITORING_HOST:/opt/monitoring/grafana/provisioning/datasources/prometheus.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/provisioning/dashboards/dashboard.yml          $MONITORING_HOST:/opt/monitoring/grafana/provisioning/dashboards/dashboard.yml
                                    scp -o StrictHostKeyChecking=no monitoring/grafana/dashboards/baemin.json                        $MONITORING_HOST:/opt/monitoring/grafana/dashboards/baemin.json
                                '''
                                // Copy staged files to the locations each native service reads from
                                sh '''
                                    ssh -o StrictHostKeyChecking=no $MONITORING_HOST '
                                        sudo cp /opt/monitoring/prometheus.yml              /etc/prometheus/prometheus.yml
                                        sudo cp /opt/monitoring/alerting-rules.yml          /etc/prometheus/alerting-rules.yml
                                        sudo cp /opt/monitoring/alertmanager.yml            /etc/prometheus/alertmanager.yml
                                        sudo mkdir -p /etc/grafana/provisioning/datasources /etc/grafana/provisioning/dashboards /var/lib/grafana/dashboards
                                        sudo cp /opt/monitoring/grafana/provisioning/datasources/prometheus.yml /etc/grafana/provisioning/datasources/prometheus.yml
                                        sudo cp /opt/monitoring/grafana/provisioning/dashboards/dashboard.yml   /etc/grafana/provisioning/dashboards/dashboard.yml
                                        sudo cp /opt/monitoring/grafana/dashboards/baemin.json                  /var/lib/grafana/dashboards/baemin.json
                                        sudo chown grafana:grafana /var/lib/grafana/dashboards/baemin.json
                                    '
                                '''
                                // Reload Prometheus and Alertmanager (config-only reload — no restart needed)
                                // Restart Grafana to pick up provisioning changes
                                sh '''
                                    ssh -o StrictHostKeyChecking=no $MONITORING_HOST '
                                        sudo systemctl reload prometheus
                                        sudo systemctl reload prometheus-alertmanager
                                        sudo systemctl restart grafana-server
                                    '
                                '''
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
