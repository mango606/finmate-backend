@echo off
echo "=== 주의: 모든 데이터가 삭제됩니다! ==="
set /p confirm="계속하시겠습니까? (Y/N): "
if /i "%confirm%"=="Y" (
    echo "=== 컨테이너 및 볼륨 삭제 ==="
    docker-compose down -v
    docker volume prune -f
    echo "=== 컨테이너 재시작 ==="
    docker-compose up -d
    echo "=== 완료! ==="
) else (
    echo "=== 취소되었습니다 ==="
)
pause