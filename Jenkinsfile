pipeline {
    agent any

    environment {
        DOCKER_HUB_USER = 'prolmpa'
        DOCKER_HUB_CRED = credentials('docker-hub-cred')   // Username/password credential
        APP_HOST        = 'front@192.168.160.100'
        SSH_CRED        = 'deploy-ssh-key'
        IMAGE_TAG       = "${BUILD_NUMBER}"
        COMPOSE_DIR     = '/opt/baemin'
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
                // Build all Spring Boot fat JARs in one Gradle invocation
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

        // ── 4. Terminate running containers ───────────────────────────────
        stage('Terminate') {
            steps {
                sshagent(credentials: [SSH_CRED]) {
                    sh "ssh -o StrictHostKeyChecking=no ${APP_HOST} 'cd ${COMPOSE_DIR} && docker compose down || true'"
                }
            }
        }

        // ── 5. Deploy — upload compose + schema, pull images, start ───────
        stage('Deploy') {
            steps {
                sshagent(credentials: [SSH_CRED]) {
                    // Ensure target directories exist
                    sh "ssh -o StrictHostKeyChecking=no ${APP_HOST} 'mkdir -p ${COMPOSE_DIR}/schema'"

                    // Upload compose file (overwrites on every deploy)
                    sh "scp -o StrictHostKeyChecking=no docker-compose.prod.yml ${APP_HOST}:${COMPOSE_DIR}/docker-compose.yml"

                    // Upload schema files (PostgreSQL init-scripts; only used on first start)
                    sh "scp -o StrictHostKeyChecking=no user-service/src/main/resources/db/schema.sql         ${APP_HOST}:${COMPOSE_DIR}/schema/user.sql"
                    sh "scp -o StrictHostKeyChecking=no store-service/src/main/resources/db/schema.sql        ${APP_HOST}:${COMPOSE_DIR}/schema/store.sql"
                    sh "scp -o StrictHostKeyChecking=no order-service/src/main/resources/db/schema.sql        ${APP_HOST}:${COMPOSE_DIR}/schema/order.sql"
                    sh "scp -o StrictHostKeyChecking=no notification-service/src/main/resources/db/schema.sql ${APP_HOST}:${COMPOSE_DIR}/schema/notification.sql"

                    // Pull updated images then start all containers
                    sh "ssh -o StrictHostKeyChecking=no ${APP_HOST} 'cd ${COMPOSE_DIR} && docker compose pull && docker compose up -d'"
                }
            }
        }
    }

    post {
        always  {
            sh 'docker logout || true'
            sh 'docker image prune -f || true'
        }
        success { echo 'All services deployed successfully.' }
        failure { echo 'Deployment failed — check the stage logs above.' }
    }
}
