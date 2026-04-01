#!/bin/bash

echo "=== TESTING COURSE CART RESTRICTIONS ==="
echo

# Get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"bob@example.com","password":"password123"}' | python -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
echo "✓ Authenticated as bob@example.com"
echo

# Clear cart first
curl -s -X DELETE http://localhost:8080/api/cart -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✓ Cart cleared"
echo

# Test 1: Add course with quantity=5 (should be forced to 1)
echo "Test 1: Add course with quantity=5 (should be forced to 1)"
RESULT=$(curl -s -X POST http://localhost:8080/api/cart/items -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"goodsId":"15","quantity":5}')
QTY=$(echo "$RESULT" | python -c "import sys, json; print(json.load(sys.stdin).get('quantity', 'ERROR'))" 2>/dev/null || echo "ERROR")
if [ "$QTY" = "1" ]; then
  echo "  ✓ PASS: Quantity forced to 1"
else
  echo "  ✗ FAIL: Quantity = $QTY (expected 1)"
fi
echo

# Test 2: Try to add same course again (should get 400)
echo "Test 2: Try to add same course again (should get 400)"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/cart/items -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"goodsId":"15","quantity":1}')
if [ "$HTTP_CODE" = "400" ]; then
  echo "  ✓ PASS: Got HTTP 400 Bad Request"
else
  echo "  ✗ FAIL: Got HTTP $HTTP_CODE (expected 400)"
fi
echo

# Test 3: Try to update course quantity to 5 (should get 400)
echo "Test 3: Try to update course quantity to 5 (should get 400)"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT http://localhost:8080/api/cart/items/15 -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"quantity":5}')
if [ "$HTTP_CODE" = "400" ]; then
  echo "  ✓ PASS: Got HTTP 400 Bad Request"
else
  echo "  ✗ FAIL: Got HTTP $HTTP_CODE (expected 400)"
fi
echo

# Test 4: Add regular item with quantity=5 (should work)
echo "Test 4: Add regular item with quantity=5 (should work)"
RESULT=$(curl -s -X POST http://localhost:8080/api/cart/items -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"goodsId":"1","quantity":5}')
QTY=$(echo "$RESULT" | python -c "import sys, json; print(json.load(sys.stdin).get('quantity', 'ERROR'))" 2>/dev/null || echo "ERROR")
if [ "$QTY" = "5" ]; then
  echo "  ✓ PASS: Regular item added with quantity=5"
else
  echo "  ✗ FAIL: Quantity = $QTY (expected 5)"
fi
echo

# Test 5: Check final cart state
echo "Test 5: Check final cart state"
CART=$(curl -s http://localhost:8080/api/cart -H "Authorization: Bearer $TOKEN")
echo "$CART" | python -c "
import sys, json
data = json.load(sys.stdin)
print('  Cart items:')
for item in data.get('items', []):
    name = item['goods']['name']
    qty = item['quantity']
    type_ = item['goods']['category']['type']
    print(f'    - {name}: qty={qty}, type={type_}')
print(f\"  Total items: {data.get('totalItems')}\")"

echo
echo "=== ALL TESTS COMPLETED ==="
