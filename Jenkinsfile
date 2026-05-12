pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'seohana/novelcraft'
        DOCKER_TAG = "${BUILD_NUMBER}"
        APP_EC2_IP = '43.200.129.39'
        FRONTEND_URL = 'https://43.200.129.39:8080'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    dockerImage = docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                }
            }
        }

        stage('Docker Push') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'dockerhub-credentials') {
                        dockerImage.push("${DOCKER_TAG}")
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                withCredentials([
                    string(credentialsId: 'db-url',              variable: 'DB_URL'),
                    string(credentialsId: 'db-username',         variable: 'DB_USERNAME'),
                    string(credentialsId: 'db-password',         variable: 'DB_PASSWORD'),
                    string(credentialsId: 'aes-secret-key',      variable: 'AES_SECRET_KEY'),
                    string(credentialsId: 'aes-iv',              variable: 'AES_IV'),
                    string(credentialsId: 'aws-access-key',      variable: 'AWS_ACCESS_KEY'),
                    string(credentialsId: 'aws-secret-key',      variable: 'AWS_SECRET_KEY'),
                    string(credentialsId: 's3-bucket-name',      variable: 'S3_BUCKET_NAME'),
                    string(credentialsId: 'google-client-id',    variable: 'GOOGLE_CLIENT_ID'),
                    string(credentialsId: 'google-client-secret', variable: 'GOOGLE_CLIENT_SECRET'),
                    string(credentialsId: 'kakao-client-id',     variable: 'KAKAO_CLIENT_ID'),
                    string(credentialsId: 'kakao-client-secret', variable: 'KAKAO_CLIENT_SECRET'),
                    string(credentialsId: 'naver-client-id',     variable: 'NAVER_CLIENT_ID'),
                    string(credentialsId: 'naver-client-secret', variable: 'NAVER_CLIENT_SECRET'),
                    string(credentialsId: 'jwt-secret-key',      variable: 'JWT_SECRET_KEY'),
                    string(credentialsId: 'portone-channel-key',     variable: 'PORTONE_CHANNEL_KEY'),
                    string(credentialsId: 'portone-api-secret',      variable: 'PORTONE_API_SECRET'),
                    string(credentialsId: 'portone-webhook-secret',  variable: 'PORTONE_WEBHOOK_SECRET'),
                    string(credentialsId: 'coolsms-api-key',     variable: 'COOLSMS_API_KEY'),
                    string(credentialsId: 'coolsms-secret-key',  variable: 'COOLSMS_SECRET_KEY'),
                    string(credentialsId: 'library-api-key',     variable: 'LIBRARY_API_KEY'),
                    string(credentialsId: 'openai-api-key',      variable: 'OPENAI_API_KEY'),
                    string(credentialsId: 'gemini-api-key',      variable: 'GEMINI_API_KEY'),
                    string(credentialsId: 'pgvector-url',        variable: 'PGVECTOR_URL'),
                    string(credentialsId: 'pgvector-username',   variable: 'PGVECTOR_USERNAME'),
                    string(credentialsId: 'pgvector-password',   variable: 'PGVECTOR_PASSWORD'),
                    string(credentialsId: 'kafka-bootstrap-servers', variable: 'KAFKA_BOOTSTRAP_SERVERS'),
                    string(credentialsId: 'redis-sentinel-nodes',  variable: 'REDIS_SENTINEL_NODES'),
                ]) {
                    sshagent(['app-ec2-ssh-key']) {
                        sh """
                            # 파일 전송
                            scp -o StrictHostKeyChecking=no docker-compose.yml ec2-user@${APP_EC2_IP}:~/
                            scp -o StrictHostKeyChecking=no -r monitoring ec2-user@${APP_EC2_IP}:~/
                            scp -o StrictHostKeyChecking=no -r init ec2-user@${APP_EC2_IP}:~/

                            ssh -o StrictHostKeyChecking=no ec2-user@${APP_EC2_IP} << 'ENDSSH'

                                # 인프라 실행 (모니터링 제외)
                                docker-compose up -d redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 kafka-1 postgres-vector
                                docker stop novelcraft || true
                                docker rm novelcraft || true

                                docker pull ${DOCKER_IMAGE}:latest

                                docker run -d \\
                                    --name novelcraft \\
                                    --network host \\
                                    -e SPRING_PROFILES_ACTIVE=prod \\
                                    -e FRONTEND_URL=${FRONTEND_URL} \\
                                    -e AES_SECRET_KEY=${AES_SECRET_KEY} \\
                                    -e AES_IV=${AES_IV} \\
                                    -e DB_URL=${DB_URL} \\
                                    -e DB_USERNAME=${DB_USERNAME} \\
                                    -e DB_PASSWORD=${DB_PASSWORD} \\
                                    -e AWS_ACCESS_KEY=${AWS_ACCESS_KEY} \\
                                    -e AWS_SECRET_KEY=${AWS_SECRET_KEY} \\
                                    -e S3_BUCKET_NAME=${S3_BUCKET_NAME} \\
                                    -e GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID} \\
                                    -e GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET} \\
                                    -e KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID} \\
                                    -e KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET} \\
                                    -e NAVER_CLIENT_ID=${NAVER_CLIENT_ID} \\
                                    -e NAVER_CLIENT_SECRET=${NAVER_CLIENT_SECRET} \\
                                    -e JWT_SECRET_KEY=${JWT_SECRET_KEY} \\
                                    -e PORTONE_CHANNEL_KEY=${PORTONE_CHANNEL_KEY} \\
                                    -e PORTONE_API_SECRET=${PORTONE_API_SECRET} \\
                                    -e PORTONE_WEBHOOK_SECRET=${PORTONE_WEBHOOK_SECRET} \\
                                    -e COOLSMS_API_KEY=${COOLSMS_API_KEY} \\
                                    -e COOLSMS_SECRET_KEY=${COOLSMS_SECRET_KEY} \\
                                    -e LIBRARY_API_KEY=${LIBRARY_API_KEY} \\
                                    -e OPENAI_API_KEY=${OPENAI_API_KEY} \\
                                    -e GEMINI_API_KEY=${GEMINI_API_KEY} \\
                                    -e PGVECTOR_URL=${PGVECTOR_URL} \\
                                    -e PGVECTOR_USERNAME=${PGVECTOR_USERNAME} \\
                                    -e PGVECTOR_PASSWORD=${PGVECTOR_PASSWORD} \\
                                    -e KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS} \\
                                    -e REDIS_SENTINEL_MASTER=mymaster \\
                                    -e REDIS_SENTINEL_NODES=${REDIS_SENTINEL_NODES} \\
                                    --restart always \\
                                    ${DOCKER_IMAGE}:latest

                                docker image prune -f
ENDSSH
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo '✅ 배포 성공!'
        }
        failure {
            echo '❌ 배포 실패! 로그를 확인하세요.'
        }
    }
}