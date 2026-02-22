#!/bin/bash

echo "Testing authentication flow..."

# Test login
echo "1. Testing login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}')

echo "Login response: $LOGIN_RESPONSE"

# Extract token from response
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Login failed - no token received"
  exit 1
fi

echo "✅ Login successful - token received"
echo "Token: ${TOKEN:0:20}..."

# Test /me endpoint
echo ""
echo "2. Testing /me endpoint..."
ME_RESPONSE=$(curl -s -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN")

echo "Me response: $ME_RESPONSE"

if echo $ME_RESPONSE | grep -q "username"; then
  echo "✅ /me endpoint working correctly"
else
  echo "❌ /me endpoint failed"
fi

# Test protected endpoint
echo ""
echo "3. Testing protected endpoint..."
PROTECTED_RESPONSE=$(curl -s -X GET http://localhost:8080/api/content/facts \
  -H "Authorization: Bearer $TOKEN")

echo "Protected endpoint response: $PROTECTED_RESPONSE"

if echo $PROTECTED_RESPONSE | grep -q "error\|unauthorized" -i; then
  echo "❌ Protected endpoint failed"
else
  echo "✅ Protected endpoint working correctly"
fi

echo ""
echo "4. Testing without token (should fail)..."
NO_TOKEN_RESPONSE=$(curl -s -X GET http://localhost:8080/api/content/facts)

if echo $NO_TOKEN_RESPONSE | grep -q "error\|unauthorized" -i; then
  echo "✅ Unauthorized access properly blocked"
else
  echo "❌ Unauthorized access not blocked"
fi

echo ""
echo "Authentication test complete!"