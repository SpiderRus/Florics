# Verification Guide

This document describes how to verify that the multi-module Maven project is working correctly.

## Build Verification

```bash
# Clean and build entire project
mvn clean install
```

**Expected Result:**
```
[INFO] Reactor Summary for Plant Shop - Parent 1.0.0:
[INFO]
[INFO] Plant Shop - Parent ................................ SUCCESS
[INFO] Plant Shop - Frontend .............................. SUCCESS
[INFO] Plant Shop - Backend ............................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### What Happens During Build:

1. **Frontend module builds first:**
   - Downloads Node.js 20.11.0 and npm 10.2.4 (first time only)
   - Runs `npm install` (installs React, TypeScript, Bootstrap, etc.)
   - Runs `npm run build` (compiles TypeScript + builds with Vite)
   - Creates `frontend/dist/` with:
     - `index.html`
     - `assets/index-*.css`
     - `assets/index-*.js`

2. **Backend module builds second:**
   - Copies `frontend/dist/*` to `backend/target/classes/static/`
   - Compiles Kotlin code with JVM target 17
   - Runs tests
   - Packages Spring Boot JAR with embedded frontend

## Runtime Verification

### Start Application

```bash
cd backend
mvn spring-boot:run
```

Wait for:
```
Netty started on port 8080
Started ApplicationKt in X.XXX seconds
```

### Test Endpoints

**1. Frontend (React App)**
```bash
curl -I http://localhost:8080/
```
Expected: `HTTP/1.1 200 OK` + HTML content with GreenLeaf title

**2. Frontend Assets**
```bash
curl -I http://localhost:8080/assets/index-D-GsGz26.css
curl -I http://localhost:8080/assets/index-D6xU1IIe.js
```
Expected: `HTTP/1.1 200 OK` for both

**3. API - Hello Endpoint**
```bash
curl http://localhost:8080/api/hello
```
Expected: `Hello from WebFlux with Coroutines!`

**4. API - Users Endpoint**
```bash
curl http://localhost:8080/api/users
```
Expected: JSON array with 3 users
```json
[
  {"id":1,"name":"Alice","email":"alice@example.com"},
  {"id":2,"name":"Bob","email":"bob@example.com"},
  {"id":3,"name":"Charlie","email":"charlie@example.com"}
]
```

**5. Swagger UI**
```bash
curl -I http://localhost:8080/swagger-ui.html
```
Expected: `HTTP/1.1 302 Found` with redirect to `/webjars/swagger-ui/index.html`

Open in browser:
- http://localhost:8080/swagger-ui.html

### Browser Verification

Open http://localhost:8080/ in a browser:

**Expected:**
- ✅ Green/nature themed design loads
- ✅ "GreenLeaf" branding visible
- ✅ Navigation links work (Home, API Docs, Swagger)
- ✅ Three feature cards display (Indoor Plants, Terrariums, Care Guide)
- ✅ Footer displays copyright
- ✅ No console errors in DevTools
- ✅ CSS loads from `/assets/index-*.css`
- ✅ JS loads from `/assets/index-*.js`

**Check DevTools Network Tab:**
- All assets should load from `localhost:8080`
- Status codes should be 200
- No CORS errors
- No 404 errors

## File Structure Verification

```bash
# Check frontend was built
ls -la frontend/dist/

# Check files were copied to backend
ls -la backend/target/classes/static/

# Check JAR contains static files
jar -tf backend/target/backend-1.0.0.jar | grep static
```

Expected output:
```
BOOT-INF/classes/static/
BOOT-INF/classes/static/index.html
BOOT-INF/classes/static/assets/
BOOT-INF/classes/static/assets/index-*.css
BOOT-INF/classes/static/assets/index-*.js
```

## Troubleshooting

### Frontend not building
```bash
cd frontend
./node/node
./node/npm --version
npm install
npm run build
```

### Static files not copied
```bash
# Check frontend/dist exists
ls frontend/dist/

# Rebuild backend only
cd backend
mvn clean install
```

### Port 8080 already in use
```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (Windows)
taskkill /PID <PID> /F
```

### Application crashes on startup
Check logs for:
- Missing static resources (should copy during build)
- Port conflicts
- Java version (needs Java 17+)

## Success Criteria

✅ All three modules build successfully
✅ Frontend dist files exist
✅ Static files copied to backend/target/classes/static
✅ Application starts without errors
✅ Frontend loads at http://localhost:8080/
✅ API endpoints return correct responses
✅ Swagger UI is accessible
✅ No errors in browser console
✅ All assets load correctly
