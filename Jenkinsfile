pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'hana/novelcraft'
        DOCKER_TAG = "${BUILD_NUMBER}"
        APP_EC2_IP = '3.35.6.46'           // ← 본인 App EC2 퍼블릭 IP
        FRONTEND_URL = 'http://3.35.6.46:8080'    // ← 본인 도메인
        REDIS_HOST = 'localhost'
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
                    // ===== DB =====
                    string(credentialsId: 'db-url',              variable: 'DB_URL'),
                    string(credentialsId: 'db-username',         variable: 'DB_USERNAME'),
                    string(credentialsId: 'db-password',         variable: 'DB_PASSWORD'),
                    // ===== AWS S3 =====
                    string(credentialsId: 'aws-access-key',      variable: 'AWS_ACCESS_KEY'),
                    string(credentialsId: 'aws-secret-key',      variable: 'AWS_SECRET_KEY'),
                    // ===== OAuth2 =====
                    string(credentialsId: 'google-client-id',    variable: 'GOOGLE_CLIENT_ID'),
                    string(credentialsId: 'google-client-secret', variable: 'GOOGLE_CLIENT_SECRET'),
                    string(credentialsId: 'kakao-client-id',     variable: 'KAKAO_CLIENT_ID'),
                    string(credentialsId: 'kakao-client-secret', variable: 'KAKAO_CLIENT_SECRET'),
                    string(credentialsId: 'naver-client-id', variable: 'NAVER_CLIENT_SECRET'),
                    string(credentialsId: 'naver-client-secret', variable: 'NAVER_CLIENT_SECRET'),
                    // ===== JWT =====
                    string(credentialsId: 'jwt-secret-key',      variable: 'JWT_SECRET_KEY'),
                    // ===== PortOne =====
                    string(credentialsId: 'portone-channel-key',     variable: 'PORTONE_CHANNEL_KEY'),
                    string(credentialsId: 'portone-api-secret',      variable: 'PORTONE_API_SECRET'),
                    string(credentialsId: 'portone-webhook-secret',  variable: 'PORTONE_WEBHOOK_SECRET'),
                    // ===== CoolSMS =====
                    string(credentialsId: 'coolsms-api-key',     variable: 'COOLSMS_API_KEY'),
                    string(credentialsId: 'coolsms-secret-key',  variable: 'COOLSMS_SECRET_KEY'),
                    // ===== 국립도서관 =====
                    string(credentialsId: 'library-api-key',     variable: 'LIBRARY_API_KEY')
                ]) {
                    sshagent(['app-ec2-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ec2-user@${APP_EC2_IP} << 'ENDSSH'

                                # 기존 컨테이너 정지 & 삭제
                                docker stop novelcraft || true
                                docker rm novelcraft || true

                                # 새 이미지 pull
                                docker pull ${DOCKER_IMAGE}:latest

                                # 새 컨테이너 실행 (모든 환경 변수 주입)
                                docker run -d \\
                                    --name novelcraft \\
                                    --network host \\
                                    -e SPRING_PROFILES_ACTIVE=prod \\
                                    -e FRONTEND_URL=${FRONTEND_URL} \\
                                    -e DB_URL=${DB_URL} \\
                                    -e DB_USERNAME=${DB_USERNAME} \\
                                    -e DB_PASSWORD=${DB_PASSWORD} \\
                                    -e AWS_ACCESS_KEY=${AWS_ACCESS_KEY} \\
                                    -e AWS_SECRET_KEY=${AWS_SECRET_KEY} \\
                                    -e REDIS_HOST=${REDIS_HOST} \\
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
                                    --restart always \\
                                    ${DOCKER_IMAGE}:latest

                                # 안 쓰는 이미지 정리
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