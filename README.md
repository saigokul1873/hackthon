# ZipRun AI Reassignment Engine

Hackathon submission — reactive reassignment engine with pluggable routing strategies, AI advisor, and agentic re-planning loop.

## Quick Start (< 5 minutes)

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+

### 1. Start Backend

```bash
cd backend
mvn spring-boot:run
```

Backend runs at **http://localhost:8080** with H2 in-memory DB and seed data (5 agents, 8 orders).

Default routing strategy is **ruleBased** (no API key needed). To enable AI routing:

```powershell
$env:ROUTING_STRATEGY="ai"
$env:LLM_API_KEY="your-groq-or-gemini-key"
mvn spring-boot:run
```

Watch backend logs for `Calling LLM provider=...` to confirm live AI calls.

### 2. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at **http://localhost:5173**

## Demo Flow (for evaluators)

1. Open the ops dashboard — header shows active routing strategy
2. Click **Simulate Crash** on a busy agent (e.g. AGT-001 or AGT-005)
3. Within ~2 seconds, **Reassignment Queue** shows suggestions with **⚡ AGENTIC REPLAN** badge
4. Review reasoning + confidence, then **Accept** or **Reject**
5. Optional: click **✨ AI Reassign** on an ASSIGNED order (shows **📋 MANUAL** badge)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/orders` | Create order pre-assigned to agent |
| GET | `/orders?status=` | List orders (filter by status) |
| PATCH | `/orders/{id}/status` | Mark order `DELIVERED` (from `REASSIGNED`) |
| POST | `/orders/{id}/suggest` | Run routing strategy, create suggestion |
| GET | `/orders/{id}/suggest/stream` | SSE stream of AI reasoning |
| GET | `/agents` | List all agents |
| PATCH | `/agents/{id}/status` | Update agent status (OFFLINE triggers re-plan) |
| GET | `/suggestions` | List pending suggestions |
| PATCH | `/suggestions/{id}` | Accept or reject suggestion |
| GET | `/routing/strategy` | Active routing strategy + available strategies |

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `routing.strategy` | `ruleBased` | Active strategy (`ruleBased` or `ai`) |
| `llm.provider` | `groq` | LLM provider (`gemini`, `groq`, `ollama`) |
| `llm.api-key` | — | Set via `LLM_API_KEY` env var |

## Submission Checklist

- [ ] Public GitHub repo with `/backend` and `/frontend`
- [ ] `ADR.md` at repo root
- [ ] README runs in under 5 minutes
- [ ] 5-minute demo video (agent offline → re-plan → accept)
- [ ] No API keys committed

## Project Structure

```
backend/     Spring Boot 3.x — domain, routing engine, agentic loop
frontend/    React 18 + Vite — ops dashboard
ADR.md       Architecture decision records
```

## Architecture Highlights

- **Pluggable routing**: `RoutingStrategy` interface with `ruleBased` and `ai` implementations
- **Agentic loop**: `PATCH agent → OFFLINE` → async re-plan → suggestions queued for ops approval
- **AI resilience**: LLM failures fall back to rule-based; agent IDs validated against roster
- **Human checkpoint**: System proposes, ops disposes — no auto-assignment

See [ADR.md](ADR.md) for full architectural decisions.
