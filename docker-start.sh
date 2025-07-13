echo "=== FinMate 도커 컨테이너 시작 ==="
docker-compose up -d
echo "=== 컨테이너 상태 확인 ==="
docker-compose ps
echo "=== MySQL 연결 대기 중... ==="
sleep 10
echo "=== 완료! ==="
echo "MySQL: localhost:3306"
echo "phpMyAdmin: http://localhost:8081"
echo "사용자: finmate / 비밀번호: 1234"