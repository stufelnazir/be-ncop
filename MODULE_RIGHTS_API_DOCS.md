# Authentication & Module Rights Implementation - API Documentation

## New Features Implemented

### 1. Refresh Token Support
- **Access Token Expiry**: 30 minutes
- **Refresh Token Expiry**: 7 days
- New JWT methods in `JwtUtil`:
  - `generateAccessToken()` - Creates 30-min access token with roles
  - `generateRefreshToken()` - Creates 7-day refresh token

### 2. Module Rights Management
Complete CRUD operations for module rights:

#### Create Module Right
```
POST /auth/module-rights
Content-Type: application/json

{
  "name": "USER_MANAGEMENT",
  "description": "Permission to manage users"
}

Response: 201 Created
{
  "id": "generated-id",
  "name": "USER_MANAGEMENT",
  "description": "Permission to manage users",
  "createdOn": 1691679600000,
  "lastUpdatedOn": 1691679600000
}
```

#### Get Module Right by ID
```
GET /auth/module-rights/{id}
Response: 200 OK
```

#### Get Module Right by Name
```
GET /auth/module-rights/by-name/{name}
Response: 200 OK
```

#### Get All Module Rights
```
GET /auth/module-rights
Response: 200 OK
[
  { id, name, description, createdOn, lastUpdatedOn },
  ...
]
```

#### Update Module Right
```
PUT /auth/module-rights/{id}
Content-Type: application/json

{
  "name": "USER_MANAGEMENT",
  "description": "Updated description"
}

Response: 200 OK
```

#### Delete Module Right
```
DELETE /auth/module-rights/{id}
Response: 204 No Content
```

### 3. Enhanced Login Response

#### Login Endpoint
```
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response: 200 OK
{
  "token": "eyJhbGc...",              // Access token (30 min expiry)
  "refresh_token": "eyJhbGc...",      // Refresh token (7 day expiry)
  "expiresIn": 1800,                  // Seconds (30 minutes)
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["ADMIN", "USER"],
  "userType": "EMPLOYEE",
  "moduleRights": [                   // All module rights from assigned roles
    "USER_MANAGEMENT",
    "REPORT_VIEW",
    "SETTINGS_EDIT"
  ],
  "lastLoginDate": 1723315200000,
  "lastLoginDateUtcDateTimeFormatted": "10/08/2026 15:30:45",
  "lastLoginDateCurrentTimezoneDateFormatted": "10/08/2026 21:30:45"
}
```

## Project Structure

### New Files Created:
```
src/main/java/com/ncop/auth/
├── model/
│   └── ModuleRight.java          # Entity with @Document
├── repository/
│   └── ModuleRightRepository.java # MongoRepository interface
├── service/
│   └── ModuleRightService.java    # CRUD service with business logic
└── controller/
    └── ModuleRightController.java # REST endpoints
```

### Updated Files:
```
src/main/java/com/ncop/
├── security/
│   └── JwtUtil.java               # Added access/refresh token generation
├── auth/
│   ├── controller/
│   │   └── AuthController.java     # Enhanced login with new fields
│   ├── dto/
│   │   └── AuthResponse.java       # Added refresh_token, moduleRights, etc.
│   └── model/
│       └── Role.java               # Already has moduleRights field
```

## Key Implementation Details

### JwtUtil Changes:
- **ACCESS_TOKEN_EXPIRY_MS**: 1000 * 60 * 30 = 30 minutes
- **REFRESH_TOKEN_EXPIRY_MS**: 1000 * 60 * 60 * 24 * 7 = 7 days
- Token type claim added: "type": "access" or "type": "refresh"

### AuthController Login Flow:
1. Validate email & password
2. Update lastLoginDate in database
3. Fetch all roles by role IDs
4. Collect moduleRights from all assigned roles
5. Generate both access and refresh tokens
6. Format lastLoginDate (UTC and current timezone)
7. Return comprehensive AuthResponse

### ModuleRight Features:
- MongoDB document with auto-generated ID
- Name (unique) and description fields
- Timestamps (createdOn, lastUpdatedOn) auto-managed
- Duplicate name validation
- Full CRUD operations

## Error Handling
- Duplicate module right name: 409 Conflict
- Module right not found: 404 Not Found
- Invalid input: 400 Bad Request
- All errors include UTC and timezone-formatted timestamps

## Testing the Features

### 1. Create a Module Right:
```bash
curl -X POST http://localhost:8080/auth/module-rights \
  -H "Content-Type: application/json" \
  -d '{
    "name": "USER_MANAGEMENT",
    "description": "Manage users"
  }'
```

### 2. Assign Module Rights to Role:
Update role document in MongoDB to include module right IDs in moduleRights array

### 3. Login and Get All Features:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

## Notes
- Module rights are stored by ID in Role.moduleRights array
- AuthResponse includes all module rights from all assigned roles
- Token expiry is returned in seconds (expiresIn: 1800)
- Refresh token should be stored securely on client
- All timestamps include both Instant and formatted versions

