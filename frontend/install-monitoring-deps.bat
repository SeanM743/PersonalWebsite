@echo off
REM Install WebSocket dependencies for monitoring
echo Installing monitoring dependencies...

call npm install sockjs-client @stomp/stompjs

echo.
echo Dependencies installed successfully!
echo.
echo Required dependencies:
echo   - sockjs-client: WebSocket client library
echo   - @stomp/stompjs: STOMP protocol for WebSocket messaging
echo.
echo You can now start the frontend with: npm run dev
pause
