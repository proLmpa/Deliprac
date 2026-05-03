pipeline {
    agent any

    environment {
        DOCKER_HUB_USER   = 'prolmpa'
        DOCKER_HUB_CRED   = credentials('docker-hub-cred')
        SSH_CRED          = 'deploy-ssh-key'
        IMAGE_TAG         = "${BUILD_NUMBER}"
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
                                    docker pull ${DOCKER_HUB_USER}/bff-service:latest
                                    docker stop bff-service || true
                                    docker rm   bff-service || true
                                    docker run -d --name bff-service --network host --env-file /etc/environment --restart unless-stopped ${DOCKER_HUB_USER}/bff-service:latest

                                    docker pull ${DOCKER_HUB_USER}/front-service:latest
                                    docker stop front-service || true
                                    docker rm   front-service || true
                                    docker run -d --name front-service --network host --restart unless-stopped ${DOCKER_HUB_USER}/front-service:latest
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
                                    docker pull ${DOCKER_HUB_USER}/user-service:latest
                                    docker stop user-service || true
                                    docker rm   user-service || true
                                    docker run -d --name user-service --network host --env-file /etc/environment --restart unless-stopped ${DOCKER_HUB_USER}/user-service:latest
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
                                    docker pull ${DOCKER_HUB_USER}/store-service:latest
                                    docker stop store-service || true
                                    docker rm   store-service || true
                                    docker run -d --name store-service --network host --env-file /etc/environment --restart unless-stopped ${DOCKER_HUB_USER}/store-service:latest
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
                                    docker pull ${DOCKER_HUB_USER}/order-service:latest
                                    docker stop order-service || true
                                    docker rm   order-service || true
                                    docker run -d --name order-service --network host --env-file /etc/environment --restart unless-stopped ${DOCKER_HUB_USER}/order-service:latest
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
                                    docker pull ${DOCKER_HUB_USER}/notification-service:latest
                                    docker stop notification-service || true
                                    docker rm   notification-service || true
                                    docker run -d --name notification-service --network host --env-file /etc/environment --restart unless-stopped ${DOCKER_HUB_USER}/notification-service:latest
                                '
                            """
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
