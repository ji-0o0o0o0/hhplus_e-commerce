@echo off
REM 부하 테스트 전체 실행 스크립트 (Windows)
REM 사용 방법: run-all-tests.bat

echo ==========================================
echo 부하 테스트 전체 실행
echo ==========================================
echo.

REM 결과 디렉토리 생성
if not exist results mkdir results

REM 애플리케이션 실행 확인
echo [1/6] 애플리케이션 상태 확인...
curl -s http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    echo ❌ 애플리케이션이 실행 중이 아닙니다. Docker Compose를 시작하세요.
    echo    docker-compose up -d
    exit /b 1
)
echo ✅ 애플리케이션 실행 중
echo.

REM Quick Start 테스트
echo [2/6] Quick Start 테스트 실행...
k6 run scripts\quickstart.js
echo.
timeout /t 5 /nobreak >nul

REM 시나리오 1: 선착순 쿠폰 발급
echo [3/6] 시나리오 1: 선착순 쿠폰 발급 테스트...
k6 run --out json=results\scenario1-result.json scripts\scenario1-coupon-peak.js
echo.
timeout /t 10 /nobreak >nul

REM 시나리오 2: 상품 주문 폭주
echo [4/6] 시나리오 2: 상품 주문 폭주 테스트...
k6 run --out json=results\scenario2-result.json scripts\scenario2-order-load.js
echo.
timeout /t 10 /nobreak >nul

REM 시나리오 3: 복합 트래픽
echo [5/6] 시나리오 3: 복합 트래픽 테스트...
k6 run --out json=results\scenario3-result.json scripts\scenario3-mixed-stress.js
echo.
timeout /t 10 /nobreak >nul

REM 시나리오 5: 락 방식 비교
echo [6/6] 시나리오 5: 락 방식 성능 비교...
k6 run --out json=results\scenario5-result.json scripts\scenario5-lock-comparison.js
echo.

echo ==========================================
echo 모든 부하 테스트 완료!
echo 결과 파일: loadtest\results\
echo ==========================================
pause