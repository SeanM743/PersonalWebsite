# .env File Deployment Example

## 📍 **Current Situation (Your Dev Machine)**

**Location:** `/home/seanmah/workspace/personal_webpage/`

**Files you have:**
```
personal_webpage/
├── .env                    # ← Contains real secrets (NOT in git)
├── .env.example           # ← Template (IS in git)
├── docker-compose.prod.yml # ← References ${VARIABLES} (IS in git)
└── backend/src/...        # ← Your code (IS in git)
```

**Your .env file contains:**
```env
FINNHUB_API_KEY=d56snvhr01qkvkasbedgd56snvhr01qkvkasbee0
POSTGRES_PASSWORD=password
JWT_SECRET=mySecretKey123...
```

## 🚀 **Scenario: Deploying to Production Server**

### **Step 1: What Gets Pushed to Git**
```bash
git add .env.example docker-compose.prod.yml
git commit -m "Add deployment config"
git push origin main
```

**❌ .env is NOT pushed** (it's in .gitignore)
**✅ .env.example IS pushed** (template only)

### **Step 2: On Production Server**
```bash
# SSH into production server
ssh user@production-server.com

# Clone your repository
git clone https://github.com/SeanM743/PersonalWebsite.git
cd PersonalWebsite

# At this point you have:
ls -la
# .env.example          ← Template from git
# docker-compose.prod.yml ← Config from git
# NO .env file!         ← This is the key point
```

### **Step 3: Create .env on Production Server**
```bash
# Copy template to create actual .env
cp .env.example .env

# Edit with PRODUCTION values (different from dev!)
nano .env
```

**Production .env might contain:**
```env
FINNHUB_API_KEY=prod_key_different_from_dev
POSTGRES_PASSWORD=super_secure_prod_password
JWT_SECRET=different_prod_jwt_secret
FRONTEND_URL=https://myapp.com
```

### **Step 4: Deploy**
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 🎯 **Key Points**

1. **Each host needs its own .env file**
2. **You manually create it on each host**
3. **It contains host-specific values**
4. **Docker Compose reads it automatically**

## 📊 **Visual Example**

```
Development Machine          Production Server
├── .env (dev secrets)      ├── .env (prod secrets)
├── .env.example    ────────→ .env.example
├── docker-compose.yml ─────→ docker-compose.yml
└── code/           ────────→ code/

    Git Repository
    ├── .env.example ✅
    ├── docker-compose.yml ✅
    ├── code/ ✅
    └── .env ❌ (never stored)
```