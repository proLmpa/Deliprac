pipeline {
    agent any

    stages {

        // ── 1. Bring in sources ──────────────────────────────────────────────
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/proLmpa/Deliprac.git',
                    credentialsId: 'github-cred'
            }
        }

        // ── 2. Build ─────────────────────────────────────────────────────────
        stage('Build') {
            steps {
                sh './gradlew build -x test'
                sh 'cd front-service && npm ci && npm run build'
            }
        }

        // ── 3. Copy  4. Stop  5. Start  (all services in parallel) ──────────
        stage('Deploy') {
            parallel {
                stage('bff-service') {
                    steps { script { deploy('bff-service', env.BFF_HOST) } }
                }
                stage('user-service') {
                    steps { script { deploy('user-service', env.USER_HOST) } }
                }
                stage('store-service') {
                    steps { script { deploy('store-service', env.STORE_HOST) } }
                }
                stage('order-service') {
                    steps { script { deploy('order-service', env.ORDER_HOST) } }
                }
                stage('notification-service') {
                    steps { script { deploy('notification-service', env.NOTIFICATION_HOST) } }
                }
                stage('front-service') {
                    steps { script { deployFront(env.FRONT_HOST) } }
                }
            }
        }
    }

    post {
        success { echo 'All services deployed successfully.' }
        failure  { echo 'Deployment failed. Check the logs above.' }
    }
}

// ── Front-service deploy function (static files via nginx) ──────────────────
def deployFront(String host) {
    def credId   = env.SSH_CRED
    def frontDir = env.FRONT_DEPLOY_DIR

    sshagent(credentials: [credId]) {
        // Clear old build and copy new dist/
        sh "ssh -o StrictHostKeyChecking=no ${host} 'rm -rf ${frontDir} && mkdir -p ${frontDir}'"
        sh "scp -o StrictHostKeyChecking=no -r front-service/dist/. ${host}:${frontDir}/"
        // Reload nginx to pick up new files
        sh "ssh -o StrictHostKeyChecking=no ${host} 'sudo nginx -s reload' || true"
    }
}

// ── Shared deploy function ───────────────────────────────────────────────────
// Per service: (3) scp jar  →  (4) pkill old process  →  (5) start new jar
def deploy(String service, String host) {
    def credId  = env.SSH_CRED
    def destDir = env.DEPLOY_DIR

    def jar = sh(
        script: "find ${service}/build/libs -name '*.jar' ! -name '*plain*' | head -1",
        returnStdout: true
    ).trim()

    sshagent(credentials: [credId]) {
        // 3. Copy build result to server
        sh "scp -o StrictHostKeyChecking=no ${jar} ${host}:${destDir}/${service}.jar"

        // 4. Terminate running server (ignore error if not running)
        sh "ssh -o StrictHostKeyChecking=no ${host} 'pkill -f ${service}.jar' || true"
        sh 'sleep 3'

        // 5. Run newly copied server
        sh """
            ssh -o StrictHostKeyChecking=no ${host} \
            'set -a; . /etc/environment; set +a; \
             nohup java -jar ${destDir}/${service}.jar \
                --spring.profiles.active=prod \
                > ${destDir}/${service}.log 2>&1 < /dev/null &'
        """
    }
}
