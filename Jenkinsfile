pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'seohana/novelcraft'
        DOCKER_TAG = "${BUILD_NUMBER}"
        APP_EC2_IP = '43.203.216.98'
        FRONTEND_URL = 'https://43.203.216.98:8080'
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
                    string(credentialsId: 'portone-channel-key', variable: 'PORTONE_CHANNEL_KEY'),
                    string(credentialsId: 'portone-api-secret',  variable: 'PORTONE_API_SECRET'),
                    string(credentialsId: 'portone-webhook-secret', variable: 'PORTONE_WEBHOOK_SECRET'),
                    string(credentialsId: 'coolsms-api-key',     variable: 'COOLSMS_API_KEY'),
                    string(credentialsId: 'coolsms-secret-key',  variable: 'COOLSMS_SECRET_KEY'),
                    string(credentialsId: 'library-api-key',     variable: 'LIBRARY_API_KEY'),
                    string(credentialsId: 'openai-api-key',      variable: 'OPENAI_API_KEY'),
                    string(credentialsId: 'gemini-api-key',      variable: 'GEMINI_API_KEY'),
                    string(credentialsId: 'pgvector-url',        variable: 'PGVECTOR_URL'),
                    string(credentialsId: 'pgvector-username',   variable: 'PGVECTOR_USERNAME'),
                    string(credentialsId: 'pgvector-password',   variable: 'PGVECTOR_PASSWORD'),
                    string(credentialsId: 'kafka-bootstrap-servers', variable: 'KAFKA_BOOTSTRAP_SERVERS'),
                    string(credentialsId: 'redis-sentinel-nodes', variable: 'REDIS_SENTINEL_NODES'),
                ]) {
                    sshagent(['app-ec2-ssh-key']) {

                        // 1. 젠킨스 워크스페이스에 .env 파일 안전하게 생성
                        sh """
                            cat <<EOF > .env
SPRING_PROFILES_ACTIVE=prod
FRONTEND_URL=${FRONTEND_URL}
AES_SECRET_KEY=${AES_SECRET_KEY}
AES_IV=${AES_IV}
DB_URL=${DB_URL}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
AWS_ACCESS_KEY=${AWS_ACCESS_KEY}
AWS_SECRET_KEY=${AWS_SECRET_KEY}
S3_BUCKET_NAME=${S3_BUCKET_NAME}
GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID}
KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
NAVER_CLIENT_ID=${NAVER_CLIENT_ID}
NAVER_CLIENT_SECRET=${NAVER_CLIENT_SECRET}
JWT_SECRET_KEY=${JWT_SECRET_KEY}
PORTONE_CHANNEL_KEY=${PORTONE_CHANNEL_KEY}
PORTONE_API_SECRET=${PORTONE_API_SECRET}
PORTONE_WEBHOOK_SECRET=${PORTONE_WEBHOOK_SECRET}
COOLSMS_API_KEY=${COOLSMS_API_KEY}
COOLSMS_SECRET_KEY=${COOLSMS_SECRET_KEY}
LIBRARY_API_KEY=${LIBRARY_API_KEY}
OPENAI_API_KEY=${OPENAI_API_KEY}
GEMINI_API_KEY=${GEMINI_API_KEY}
PGVECTOR_URL=${PGVECTOR_URL}
PGVECTOR_USERNAME=${PGVECTOR_USERNAME}
PGVECTOR_PASSWORD=${PGVECTOR_PASSWORD}
KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=${REDIS_SENTINEL_NODES}
EOF
                        """

                        // 2. 생성된 .env 파일을 포함하여 EC2로 전송
                        sh "ssh -o StrictHostKeyChecking=no ec2-user@${APP_EC2_IP} 'mkdir -p ~/monitoring ~/init'"
                        sh """
                            scp -o StrictHostKeyChecking=no docker-compose.yml ec2-user@${APP_EC2_IP}:~/
                            scp -o StrictHostKeyChecking=no -r monitoring ec2-user@${APP_EC2_IP}:~/
                            scp -o StrictHostKeyChecking=no -r init ec2-user@${APP_EC2_IP}:~/
                            scp -o StrictHostKeyChecking=no .env ec2-user@${APP_EC2_IP}:~/.env
                        """

                        // 3. EC2 내부에서 도커 실행
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${APP_EC2_IP} << 'ENDSSH'
                                # 에러 발생 시 스크립트 즉시 중단 (매우 중요!)
                                set -e

                                # 인프라 실행 (모니터링 제외)
                                docker-compose up -d redis-master redis-slave-1 redis-sentinel-1 redis-slave-2 redis-sentinel-2 redis-sentinel-3 kafka-1 kafka-2 kafka-3 postgres-vector

                                # 기존 컨테이너 정리
                                docker stop novelcraft || true
                                docker rm novelcraft || true

                                # 최신 이미지 풀
                                docker pull ${DOCKER_IMAGE}:latest

                                # --env-file 옵션을 사용하여 .env 파일의 모든 환경변수를 한 번에 주입!
                                docker run -d \\
                                    --name novelcraft \\
                                    --network host \\
                                    --env-file ~/.env \\
                                    --restart always \\
                                    ${DOCKER_IMAGE}:latest

                                # 보안을 위해 사용이 끝난 .env 파일 삭제
                                rm ~/.env

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