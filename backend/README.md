# Backend (Spring Boot + MyBatis) - SCUT-Shop

This is the backend skeleton for SCUT-Shop.

Quick start (local, using docker-compose from project root):

1. Copy `.env.example` to `.env` and fill DB credentials.
2. Build and run services:
   docker compose up -d --build

The backend will be available at `http://localhost:8081` (proxied by docker-compose to container port 8080).

Endpoints (sample):
- GET `/api/health` - health check
- POST `/api/auth/register` - register (payload: `{username,email,password}`)

Next steps:
- Implement JWT authentication and login endpoint
- Add MyBatis XML mappers or use annotations for more complex queries
- Add unit & integration tests
