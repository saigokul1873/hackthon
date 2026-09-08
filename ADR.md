# Architecture Decision Records

> Also available at `backend/ADR.md`

## ADR-1: Where does routing logic live?
**Context**
The core functionality of this engine is reassigning stranded orders to available agents. We needed to decide where this complex logic (handling rules, calling external AI services, managing fallback logic) should reside.
**Options considered**
(a) Inside `AgentOfflineEventListener` or `OrderService`.
(b) A dedicated `RoutingService` orchestrating `RoutingStrategy` implementations.
(c) Domain-driven approach putting logic inside the `Order` entity.
**Decision**
Chose option (b) — A dedicated `RoutingService` orchestrating strategies. This enforces a strict boundary: `AgentService` and `OrderService` simply manage entity states, while `RoutingService` encapsulates the complex decision-making of assignment. It avoids the anti-pattern of a bloated god-service.
**Tradeoffs accepted**
It introduces an extra layer of abstraction. Developers tracing the assignment flow must jump from the Event Listener -> Routing Service -> Strategy Interface -> Concrete Strategy, which increases cognitive load slightly compared to a single procedural method.

## ADR-2: How does runtime strategy switching work?
**Context**
We have a `RuleBasedStrategy` and an `AIRoutingStrategy`. We need to switch between them at runtime without restarting, and we must ensure both the manual HTTP endpoint and async event loop use the same active strategy. Sprint 2 adds `ZoneAffinityStrategy`.
**Options considered**
(a) Hardcoded `if/else` factory based on a config property.
(b) Spring's `@Qualifier` and manual bean injection.
(c) Auto-wired `Map<String, RoutingStrategy>` populated by Spring, keyed by bean name.
**Decision**
Chose option (c). We inject `Map<String, RoutingStrategy> strategies` into the `RoutingService`. The active strategy name is read from `routing.strategy` (or `routing.active-strategy`) via Spring's `Environment` on **every call**, so changing the env var `ROUTING_STRATEGY` switches behavior without a restart. To add `ZoneAffinityStrategy` in Sprint 2, we just create the class and annotate it with `@Component("zoneAffinity")`.
**Tradeoffs accepted**
Since Spring populates the map automatically by bean name, it's slightly less explicit than a manual factory. Also, if the strategy property is misspelled, it will throw a runtime exception. We mitigated this with a startup validation check in the `RoutingService` constructor.

## ADR-3: How does the system stay resilient when the LLM is unavailable?
**Context**
The LLM integration introduces network unreliability. It can timeout, exhaust quotas, return malformed JSON, or hallucinate agent identifiers. If it fails during an async re-plan, we cannot silently drop the assignment.
**Options considered**
(a) Fail and log, leaving the order in `REASSIGNMENT_PENDING` indefinitely.
(b) Retry the LLM call with exponential backoff.
(c) Immediately fall back to the deterministic rule-based strategy.
**Decision**
Chose option (c). The `AIRoutingStrategy` wraps the API call, JSON parsing, and Agent ID validation in a `try-catch` block. If any step fails (e.g. invalid JSON or an Agent ID that doesn't exist), it logs the error and delegates to the `RuleBasedStrategy` to ensure a `ReassignmentSuggestion` is always created.
**Tradeoffs accepted**
We lose the AI's reasoning capabilities during an outage, but we gain critical operational continuity. The Ops manager might briefly see "Rule-based: Picked agent..." instead of a smart AI explanation, but the system keeps moving.

## ADR-4: How is the agentic loop triggered and kept off the request path?
**Context**
When a manager sets an agent to OFFLINE via `PATCH /agents/{id}/status`, that HTTP request must return immediately. The heavy lifting of finding stranded orders and calling the LLM must happen asynchronously.
**Options considered**
(a) A scheduled cron job that sweeps for OFFLINE agents and stranded orders every X minutes.
(b) A manual thread pool (`CompletableFuture.runAsync()`) inside the controller.
(c) Spring's `ApplicationEventPublisher` with an `@Async` annotated `@EventListener`.
**Decision**
Chose option (c) — Spring Application Events with `@TransactionalEventListener(AFTER_COMMIT)`. The `AgentService` publishes an `AgentOfflineEvent` when status changes to OFFLINE. The `AgentOfflineEventListener` catches it asynchronously on a dedicated thread pool. Idempotency is handled by checking if a `PENDING` suggestion already exists for the stranded order with `triggerReason = AGENT_OFFLINE`.
**Tradeoffs accepted**
Async events in Spring happen outside the original transaction boundary. If the `AgentOfflineEventListener` fails unexpectedly, the agent remains OFFLINE but no reassignments are suggested. We accept this because our fallback strategy makes failures highly unlikely, and a simple dashboard refresh allows manual intervention.

## ADR-5: Extensibility and Deliberate Exclusions
**Context**
Sprint 2 introduces `zoneId` and `capacity`. Sprint 3 introduces SLA-breach proactive re-planning and a full dispatch board. We need to prepare for these without over-engineering today.
**Decision & Extension Seam**
- **Extension Seam:** The `RoutingStrategy.suggest()` interface with `RoutingContext` is shaped for Sprint 2. Nullable `zoneId`, `maxCapacity`, and `weightClass` fields exist on domain models. `ZoneAffinityStrategy` plugs in via `@Component("zoneAffinity")` with no changes to selection logic.
- **Deliberate Exclusion:** We deferred the full dispatch board and proactive SLA-breach loop. The agentic loop is a correctness requirement; the board is a visibility enhancement on top of `GET /orders`. SLA-breach would publish an `OrderSLABreachEvent` reusing the same event architecture.

## ADR-6: Frontend Framework Choice
**Context**
The Ops interface needs to be built rapidly while ensuring polling and SSE streaming for agentic loop visibility.
**Options considered**
(a) React 18 (b) Angular 17
**Decision**
Chose React 18 with Vite. `useEffect` handles 5-second polling; `EventSource` handles SSE streaming for the AI Reassign button.
**Tradeoffs accepted**
Manual state management via `useState` instead of Angular's structured services — acceptable for a single-page ops dashboard.
