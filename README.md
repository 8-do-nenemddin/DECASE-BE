# DECASE-BE

### 포트 충돌 해결 
```commandline
netstat -ano | findstr :8080

taskkill /PID {PID} /F
```