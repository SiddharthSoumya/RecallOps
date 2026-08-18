# RecallOps

> **An AI SRE agent that never forgets an investigation.**

RecallOps is a fault-tolerant incident investigation system designed around one core idea:

> **An AI agent should be able to survive an application crash without losing the investigation it was working on.**

When an incident occurs, RecallOps creates a persistent investigation, maintains its state and working memory, executes a reasoning engine, and continuously checkpoints important investigation context into **CockroachDB**.

If the application crashes during the investigation, the process can be restarted and the investigation can be reconstructed from durable state.

Instead of starting the investigation from scratch, RecallOps resumes from the last persisted checkpoint.

---

# The Problem

AI agents are increasingly being used for operational workflows such as:

- Incident investigation
- Root-cause analysis
- Log analysis
- Troubleshooting
- Remediation planning
- Infrastructure operations

However, an agent's current reasoning context is often treated as temporary application state.

Consider an SRE agent investigating a production incident:

```text
Incident Created
      ↓
Agent starts investigation
      ↓
Hypothesis generated
      ↓
Evidence collected
      ↓
Next investigation step selected
      ↓
💥 Application crashes
      ↓
Application restarts
      ↓
Agent has forgotten its context
      ↓
Investigation starts again
````

For long-running or expensive investigations, losing this state is unacceptable.

The key question behind RecallOps is:

> **What happens to an AI agent's investigation when the process running it suddenly disappears?**

---

# The Solution

RecallOps separates **ephemeral execution** from **durable investigation memory**.

The application process may crash, restart, or disappear.

The investigation itself does not have to disappear with it.

Important investigation state is persisted to CockroachDB:

- Incident state
    
- Investigation state
    
- State transition history
    
- Working memory
    
- Hypotheses
    
- Evidence
    
- Reasoning context
    
- Next recommended action
    
- Investigation progress
    

After a restart, RecallOps can reconstruct the investigation from durable state and recover the context required to continue the investigation.

```text
                 ┌─────────────────────┐
                 │       Incident       │
                 │       Created        │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │    Investigation    │
                 │       Started       │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │   Reasoning Engine  │
                 │                     │
                 │ Analyze             │
                 │ Hypothesize         │
                 │ Evaluate Evidence   │
                 │ Select Next Action  │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │   Durable Memory    │
                 │                     │
                 │ Investigation State │
                 │ State History       │
                 │ Working Memory      │
                 └──────────┬──────────┘
                            │
                            │
                     💥 APPLICATION CRASH
                            │
                            ▼
                 ┌─────────────────────┐
                 │   Application       │
                 │      Restart        │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │   Recovery Service  │
                 │                     │
                 │ Load durable state  │
                 │ Load working memory │
                 │ Reconstruct context │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Investigation       │
                 │      Resumes        │
                 └─────────────────────┘
```

---

# Core Idea

RecallOps models an investigation as a **durable state machine** rather than simply an in-memory conversation.

The agent's execution is ephemeral.

The investigation is durable.

### Ephemeral

```text
Application process
Agent execution
Java objects
HTTP requests
Runtime memory
```

### Durable

```text
Incident
Investigation
Current state
State transition history
Working memory
Reasoning context
Investigation progress
```

This separation allows the application to fail without necessarily losing the investigation's durable context.

---

# MVP Demonstration

The MVP focuses on one end-to-end failure-recovery workflow.

## 1. Create an incident

A user creates an incident such as:

```text
Service: payment-service

Severity: HIGH

Description:
Payment requests are returning HTTP 500 errors.
```

RecallOps creates a persistent incident record.

---

## 2. Start an investigation

An investigation is created for the incident.

The investigation follows a controlled lifecycle:

```text
CREATED
   ↓
INVESTIGATING
   ↓
ANALYZING
   ↓
WAITING_FOR_EVIDENCE
   ↓
RESOLVED
```

Invalid state transitions are rejected by the state machine.

---

## 3. Agent investigates

The reasoning engine analyzes the investigation context and produces a structured decision.

For example:

```text
Hypothesis:
Database connection exhaustion is causing payment-service failures.

Evidence:
Recent requests are failing with database connection errors.

Next Action:
Inspect database connection pool utilization.

Confidence:
0.82
```

The important part is that this context is not required to exist only in process memory.

It can be persisted as working memory.

---

## 4. Working memory is checkpointed

RecallOps stores the investigation's reasoning context in durable storage.

Example:

```json
{
  "hypotheses": [
    "Database connection exhaustion"
  ],
  "evidence": [
    "HTTP 500 responses",
    "Database connection errors"
  ],
  "nextAction": "Inspect connection pool utilization",
  "confidence": 0.82
}
```

This becomes persistent working memory for the investigation.

---

## 5. Simulate a crash

The MVP intentionally allows the application to be terminated while an investigation is in progress.

```text
Investigation
      ↓
Reasoning
      ↓
Working memory persisted
      ↓
💥 APPLICATION CRASH
```

The Java process disappears.

The durable investigation data remains in CockroachDB.

---

## 6. Restart the application

When RecallOps starts again, the recovery layer can load the persisted investigation context.

```text
Application restart
       ↓
Recovery service
       ↓
Load investigation
       ↓
Load state history
       ↓
Load working memory
       ↓
Reconstruct investigation context
       ↓
Resume investigation
```

The investigation does not need to be reconstructed manually.

---

## 7. Investigation resumes

The key demonstration is:

```text
BEFORE CRASH

Investigation #123
State: ANALYZING

Hypothesis:
Database connection exhaustion

Next Action:
Inspect connection pool


             💥


AFTER RESTART

Investigation #123
State: ANALYZING

Hypothesis:
Database connection exhaustion

Next Action:
Inspect connection pool

             ↓

Investigation continues
```

The database has preserved the state required to recover the investigation.

---

# Architecture

```text
                    ┌──────────────────────┐
                    │       Frontend       │
                    │      React/Vite      │
                    └──────────┬───────────┘
                               │
                               │ REST API
                               ▼
                    ┌──────────────────────┐
                    │    Spring Boot API   │
                    │                      │
                    │ Incident Controller  │
                    │ Investigation API    │
                    │ Working Memory API   │
                    │ Agent API            │
                    └──────────┬───────────┘
                               │
             ┌─────────────────┼─────────────────┐
             │                 │                 │
             ▼                 ▼                 ▼
     ┌──────────────┐  ┌────────────────┐  ┌────────────────┐
     │   Incident   │  │ Investigation  │  │    Reasoning   │
     │   Service    │  │    Service     │  │     Engine     │
     └──────────────┘  └───────┬────────┘  └───────┬────────┘
                               │                   │
                               ▼                   │
                       ┌────────────────┐          │
                       │ State Machine  │          │
                       └───────┬────────┘          │
                               │                   │
                               └─────────┬─────────┘
                                         ▼
                              ┌──────────────────────┐
                              │   Recovery Service   │
                              │                      │
                              │ Load investigation   │
                              │ Load state history   │
                              │ Load working memory  │
                              │ Reconstruct context  │
                              └──────────┬───────────┘
                                         │
                                         ▼
                              ┌──────────────────────┐
                              │     CockroachDB       │
                              │                      │
                              │ Incidents             │
                              │ Investigations        │
                              │ State History         │
                              │ Working Memory        │
                              └──────────────────────┘
```

---

# Technology Stack

## Backend

- Java 21+
    
- Spring Boot
    
- Spring Data JPA
    
- Hibernate
    
- REST APIs
    
- Maven
    

## Database

- CockroachDB
    
- PostgreSQL-compatible SQL
    
- Transactional persistence
    

## Agent / Reasoning

The current MVP uses a **deterministic reasoning engine**.

This is intentional.

The deterministic engine provides:

- Reproducible demonstrations
    
- Predictable decisions
    
- Reliable automated tests
    
- No dependency on an external LLM for the core crash/recovery workflow
    

The reasoning layer is abstracted behind an interface, allowing an LLM-powered implementation to be introduced later without redesigning the persistence architecture.

## Frontend

- React
    
- TypeScript
    
- Vite
    
- CSS
    

The frontend provides the interactive MVP interface for interacting with the incident and investigation workflow.

---

# Why CockroachDB?

The central requirement of RecallOps is **durable agent memory**.

CockroachDB provides the transactional persistence layer required to keep investigation state durable and consistent.

Conceptually:

```text
Incident
   │
   ├── Investigation
   │       │
   │       ├── Current State
   │       ├── State History
   │       └── Working Memory
   │
   └── Resolution
```

The database is therefore more than simple application storage.

It acts as the durable memory layer that allows the investigation to survive application failure.

---

# Persistent Memory Model

RecallOps separates persistent memory into structured components.

## Incident

Represents the operational problem being investigated.

Typical information includes:

- Incident ID
    
- Title
    
- Description
    
- Service
    
- Severity
    
- Status
    
- Creation timestamp
    

---

## Investigation

Represents the agent's investigation lifecycle.

Typical information includes:

- Investigation ID
    
- Incident ID
    
- Current state
    
- Started timestamp
    
- Completion timestamp
    

---

## State History

Every important state transition is recorded.

Example:

```text
CREATED
    ↓
INVESTIGATING
    ↓
ANALYZING
    ↓
WAITING_FOR_EVIDENCE
    ↓
ANALYZING
    ↓
RESOLVED
```

This provides an auditable history of the investigation lifecycle.

---

## Working Memory

Working memory represents the investigation's current reasoning context.

It can contain:

- Hypotheses
    
- Evidence
    
- Observations
    
- Reasoning notes
    
- Current objective
    
- Next action
    
- Confidence
    
- Investigation context
    

The goal is simple:

> **If the process disappears, the investigation should still know what it was doing.**

---

# State Machine

RecallOps prevents arbitrary investigation state changes.

The investigation lifecycle is controlled by an explicit state machine.

Conceptually:

```text
CREATED
   │
   ▼
INVESTIGATING
   │
   ▼
ANALYZING
   │
   ├──────────────► WAITING_FOR_EVIDENCE
   │                         │
   │                         ▼
   └──────────────────── ANALYZING
                             │
                             ▼
                          RESOLVED
```

Invalid transitions are rejected.

This protects the integrity of the investigation lifecycle and makes recovery deterministic.

---

# Failure Recovery

Failure recovery is the central architectural property of RecallOps.

Suppose the investigation reaches:

```text
State:
ANALYZING

Working Memory:
Hypothesis = database connection exhaustion

Evidence:
HTTP 500
Database connection errors

Next Action:
Inspect connection pool
```

The process then crashes.

The durable state remains:

```text
CockroachDB
    │
    ├── Investigation
    ├── Current State
    ├── State History
    └── Working Memory
```

After restart:

```text
RecoveryService
      ↓
Load investigation
      ↓
Load working memory
      ↓
Load state history
      ↓
Reconstruct context
      ↓
Continue investigation
```

This is the core reliability property demonstrated by the MVP.

---

# API Overview

The backend exposes REST APIs for the MVP.

## Incident APIs

```text
POST   /api/incidents
GET    /api/incidents
GET    /api/incidents/{id}
```

Used to create and retrieve incidents.

---

## Investigation APIs

```text
POST   /api/investigations
GET    /api/investigations/{id}
POST   /api/investigations/{id}/transition
```

Used to create investigations, inspect them, and perform controlled state transitions.

---

## Agent APIs

```text
POST   /api/investigations/{id}/agent/run
```

Triggers the reasoning engine for an investigation.

---

## Working Memory APIs

```text
GET    /api/investigations/{id}/memory
PUT    /api/investigations/{id}/memory
```

Used to inspect and update persistent working memory.

---

## Recovery API

```text
GET    /api/investigations/{id}/recover
```

Loads durable investigation state and reconstructs the investigation context.

See [`backend/recallops-api-mappings.txt`](https://chatgpt.com/c/backend/recallops-api-mappings.txt) for the current API mapping.

---

# Project Structure

```text
RecallOps/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/recallops/
│   │   │   │
│   │   │   ├── common/
│   │   │   │   ├── entity/
│   │   │   │   ├── exception/
│   │   │   │   └── response/
│   │   │   │
│   │   │   ├── config/
│   │   │   │
│   │   │   ├── incident/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   │
│   │   │   ├── investigation/
│   │   │   │   ├── agent/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   └── state/
│   │   │   │
│   │   │   └── memory/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── repository/
│   │   │       └── service/
│   │   │
│   │   └── test/
│   │
│   ├── pom.xml
│   ├── mvnw
│   └── src/main/resources/
│
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── vite.config.ts
│
├── .env.example
├── .gitignore
└── README.md
```

---

# Running Locally

## Prerequisites

Install:

- Java 21+
    
- Node.js 20+
    
- npm
    
- Git
    
- CockroachDB
    

A CockroachDB cluster can be used locally or through CockroachDB Cloud.

---

## 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd RecallOps
```

---

## 2. Configure environment variables

Create a local environment file:

```bash
cp .env.example .env
```

Configure:

```env
DB_PASSWORD=your_actual_database_password
```

The `.env` file is ignored by Git.

**Never commit real credentials.**

---

## 3. Configure the database

Configure the CockroachDB connection in:

```text
backend/src/main/resources/application.yml
```

The database password is injected through:

```text
${DB_PASSWORD}
```

This keeps credentials outside the source code.

---

## 4. Start the backend

From the repository root:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

---

## 5. Start the frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Vite will provide the local development URL.

---

# Running Tests

From the backend directory:

```bash
cd backend
./mvnw test
```

The test suite covers important persistence and state-machine behavior, including:

- Incident persistence
    
- Investigation state transitions
    
- Investigation state history
    

---

# Crash Recovery Demo

The intended MVP demonstration is:

```text
1. Open RecallOps
        ↓
2. Create an incident
        ↓
3. Start an investigation
        ↓
4. Run the reasoning engine
        ↓
5. Persist working memory
        ↓
6. Simulate application crash
        ↓
7. Restart backend
        ↓
8. Recover investigation
        ↓
9. Restore durable context
        ↓
10. Continue investigation
```

The key comparison is:

```text
BEFORE CRASH

Investigation #123
State: ANALYZING

Hypothesis:
Database connection exhaustion

Next Action:
Inspect connection pool


             💥


AFTER RESTART

Investigation #123
State: ANALYZING

Hypothesis:
Database connection exhaustion

Next Action:
Inspect connection pool

             ↓

RESUMED
```

The demonstration shows that the investigation's durable state survives the failure of the application process.

---

# Engineering Decisions

## Durable state instead of process memory

Important investigation context is persisted rather than relying exclusively on Java runtime memory.

---

## Explicit state transitions

Investigation lifecycle changes are controlled by a state machine.

This prevents arbitrary status changes and creates an auditable lifecycle.

---

## Deterministic MVP

The current reasoning engine is deterministic.

This makes the core demonstration:

- Reproducible
    
- Testable
    
- Predictable
    
- Independent of external LLM availability
    

The architecture remains open to replacing the deterministic engine with an LLM-powered implementation.

---

## Separation of concerns

The system separates:

```text
Incident Management
        ↓
Investigation Lifecycle
        ↓
Reasoning
        ↓
Working Memory
        ↓
Recovery
```

This keeps the system easier to reason about, test, and extend.

---

# Why This Matters for AI Agents

Traditional fault-tolerant systems often focus on recovering:

```text
Database state
Queues
Transactions
User sessions
```

AI agents introduce another important form of state:

```text
Reasoning state
```

An agent may spend significant time developing:

- Hypotheses
    
- Plans
    
- Evidence chains
    
- Tool results
    
- Intermediate conclusions
    
- Next actions
    

If that context exists only in process memory, a crash can destroy the agent's progress.

RecallOps explores a more resilient architecture:

```text
             AI Agent
                │
                │ reasoning
                ▼
         Working Memory
                │
                │ checkpoint
                ▼
          CockroachDB
                │
                │ application crash
                ▼
       Recovery Service
                │
                │ reconstruct
                ▼
          AI Agent
                │
                ▼
           Resume
```

This is the foundation for building reliable long-running AI systems.

---

# Current MVP Scope

## Included

- Incident management
    
- Investigation lifecycle
    
- Investigation state machine
    
- Persistent state history
    
- Persistent working memory
    
- Deterministic reasoning engine
    
- Investigation recovery
    
- REST API
    
- React frontend
    
- Crash/restart demonstration
    
- Automated persistence/state tests
    
- CockroachDB persistence
    

## Intentionally Out of Scope

RecallOps is not intended to be a complete production SRE platform.

The MVP deliberately focuses on one architectural problem:

> **Persistent agent memory and recovery across application crashes.**

Future versions can add:

- Real LLM reasoning
    
- Tool calling
    
- Log analysis
    
- Vector similarity search
    
- Historical incident retrieval
    
- Agent-to-agent collaboration
    
- Streaming events
    
- Observability
    
- Authentication and authorization
    
- Production deployment
    
- Kubernetes-based failure testing
    
- Distributed worker execution
    
- Event-driven orchestration
    

---

# Future Architecture

The deterministic reasoning engine can eventually be replaced or extended with an LLM-powered agent.

```text
                    ┌──────────────────┐
                    │     LLM Agent    │
                    └────────┬─────────┘
                             │
                       Reasoning / Tools
                             │
                             ▼
                    ┌──────────────────┐
                    │ Working Memory   │
                    └────────┬─────────┘
                             │
                         Checkpoint
                             │
                             ▼
                    ┌──────────────────┐
                    │   CockroachDB    │
                    └────────┬─────────┘
                             │
                           Crash
                             │
                             ▼
                    ┌──────────────────┐
                    │ Recovery Service │
                    └────────┬─────────┘
                             │
                         Reconstruct
                             │
                             ▼
                    ┌──────────────────┐
                    │  Resume Agent    │
                    └──────────────────┘
```

The persistence architecture therefore remains useful even as the intelligence layer evolves.

---

# Security

Secrets are intentionally kept outside source control.

The repository ignores sensitive local configuration such as:

```text
.env
.env.*
*.pem
*.key
```

Developers should use:

```text
.env.example
```

as the template for local configuration.

**Never commit production credentials, database passwords, API keys, or private certificates.**

---

# Hackathon Context

RecallOps was built around the idea of **persistent memory for AI agents**.

The project focuses on demonstrating how an agent can maintain durable investigation state even when the application process executing the agent fails.

The central demonstration is intentionally simple:

```text
Agent investigates
       ↓
Agent stores memory
       ↓
Application crashes
       ↓
Application restarts
       ↓
Memory is recovered
       ↓
Investigation continues
```

The goal is not to build the world's most sophisticated SRE agent.

The goal is to demonstrate a reliable foundation on which more capable autonomous agents can be built.

---

# Roadmap

### Phase 1 — MVP

-  Incident management
    
-  Investigation lifecycle
    
-  State machine
    
-  Persistent state history
    
-  Working memory
    
-  Deterministic reasoning engine
    
-  Recovery service
    
-  REST APIs
    
-  React frontend
    
-  Crash/restart workflow
    
-  Persistence tests
    

### Phase 2 — AI Agent

-  LLM-powered reasoning
    
-  Tool calling
    
-  Structured outputs
    
-  Agent planning
    
-  Tool result persistence
    
-  Reasoning checkpointing
    

### Phase 3 — SRE Intelligence

-  Log analysis
    
-  Metrics analysis
    
-  Historical incident retrieval
    
-  Vector search
    
-  Root-cause analysis
    
-  Remediation recommendations
    

### Phase 4 — Production Reliability

-  Authentication
    
-  Authorization
    
-  Observability
    
-  Distributed workers
    
-  Event-driven execution
    
-  Kubernetes deployment
    
-  Automated failure injection
    
-  Horizontal scaling
    

---

# Project Status

**Status: MVP complete**

The current implementation demonstrates the core architectural concept:

> **Persistent investigation state can survive application failure and be recovered after restart.**

The project is intentionally small in scope so that the crash-recovery mechanism remains clear, observable, and demonstrable.

---

# Author

**Siddharth Soumya**

Built as an exploration of:

- Reliable AI agents
    
- Persistent agent memory
    
- Fault-tolerant backend systems
    
- Java Spring Boot
    
- CockroachDB
    
- AI-native system architecture
    

---

# License

This project is currently an MVP / hackathon project.

License information can be added when the project is prepared for broader public distribution.
