# RecallOps

> **An AI SRE agent that never forgets an investigation.**

RecallOps is a fault-tolerant incident investigation system designed to demonstrate one critical capability:

**An AI agent can crash without losing its investigation state.**

When an incident occurs, RecallOps creates an investigation, allows an autonomous reasoning agent to analyze it, persists its working memory and state transitions in **CockroachDB**, and can recover the investigation after an application crash.

Instead of restarting the investigation from scratch, the agent resumes from the last durable checkpoint.

---

## The Problem

AI agents are increasingly being used for operational tasks such as:

- Incident investigation
- Root-cause analysis
- Log analysis
- Troubleshooting
- Remediation planning

However, many agent implementations treat the agent's current reasoning state as temporary application memory.

That creates a serious reliability problem.

Imagine an SRE agent investigating a production incident:

````text
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
Agent has forgotten everything
      ↓
Investigation starts again
````
```
For long-running or expensive investigations, losing the agent's working state is unacceptable.

The key question behind RecallOps is:

> **What happens to an AI agent's reasoning when the process running it suddenly disappears?**

---

# The Solution

RecallOps separates **ephemeral execution** from **durable investigation memory**.

The application process may crash, restart, or disappear.

The investigation itself does not.

Important state is persisted to CockroachDB:

- Investigation state
    
- State transition history
    
- Working memory
    
- Current hypotheses
    
- Evidence collected
    
- Reasoning context
    
- Next recommended action
    
- Investigation progress
    

After restart, RecallOps reconstructs the investigation from durable state and resumes from the last known checkpoint.

````text
                 ┌─────────────────────┐
                 │       Incident       │
                 │       Created        │
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │  Investigation      │
                 │      Started        │
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │   Reasoning Agent   │
                 │                     │
                 │ Analyze → Hypothesis│
                 │ → Evidence → Action │
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │   Durable Memory    │
                 │                     │
                 │ CockroachDB         │
                 │                     │
                 │ State               │
                 │ History             │
                 │ Working Memory      │
                 └──────────┬──────────┘
                            │
                            │
                     💥 APPLICATION CRASH
                            │
                            ↓
                 ┌─────────────────────┐
                 │   Application       │
                 │      Restart        │
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │ Recovery Service    │
                 │                     │
                 │ Load durable state  │
                 │ Reconstruct context │
                 │ Resume investigation│
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │ Investigation       │
                 │      Resumes        │
                 └─────────────────────┘
````
```
---

# Core Idea

RecallOps treats an agent investigation as a **durable state machine** rather than an in-memory conversation.

The agent's execution is temporary.

The investigation is durable.

```text
Ephemeral
──────────────────────────────
Application process
Agent execution
Current Java objects
HTTP requests
Runtime memory


Durable
──────────────────────────────
Incident
Investigation
State
State history
Working memory
Reasoning context
Investigation progress
```
````
This distinction allows the system to recover from process failure without losing the investigation.

---
# MVP Demonstration

The MVP focuses on one powerful end-to-end workflow.

### 1. Create an incident

A user creates an incident such as:

```text
Service: payment-service

Severity: HIGH

Description:
Payment requests are returning HTTP 500 errors.
```

RecallOps creates a persistent incident record.

---

### 2. Start an investigation

An investigation is created for the incident.

The investigation moves through controlled states:

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

### 3. Agent investigates

The reasoning engine analyzes the incident and produces a structured decision.

Example:

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

The important part is that this reasoning context is not kept only in RAM.

It is persisted.

---

### 4. Working memory is checkpointed

RecallOps stores the agent's investigation context in durable storage.

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

This becomes the agent's persistent working memory.

---

### 5. Simulate a crash

The MVP intentionally allows the application to be terminated while an investigation is in progress.

```text
Investigation
      ↓
Agent reasoning
      ↓
Working memory persisted
      ↓
💥 CRASH
```

The Java process disappears.

But the investigation data remains in CockroachDB.

---

### 6. Restart the application

When RecallOps starts again, the recovery layer loads the persisted investigation.

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
Resume
```

The agent does not start from zero.

---

### 7. Investigation resumes

The recovered agent continues from the durable checkpoint.

The demonstration proves:

```text
Before crash:

Investigation #123
State: ANALYZING

Hypothesis:
Database connection exhaustion

Next action:
Inspect connection pool


        💥 CRASH


After restart:

Investigation #123
State: ANALYZING

Hypothesis:
Database connection exhaustion

Next action:
Inspect connection pool

        ↓

Investigation continues
```

That is the core RecallOps story.

---

# Architecture

```text
                    ┌──────────────────────┐
                    │      Frontend        │
                    │      React/Vite      │
                    └──────────┬───────────┘
                               │
                               │ REST API
                               ↓
                    ┌──────────────────────┐
                    │   Spring Boot API    │
                    │                      │
                    │ Incident Controller  │
                    │ Investigation API    │
                    │ Working Memory API   │
                    │ Agent API            │
                    └──────────┬───────────┘
                               │
             ┌─────────────────┼──────────────────┐
             │                 │                  │
             ↓                 ↓                  ↓
     ┌──────────────┐  ┌────────────────┐  ┌───────────────┐
     │ Incident     │  │ Investigation  │  │ Agent         │
     │ Service      │  │ Service        │  │ Reasoning     │
     └──────────────┘  └────────────────┘  └───────────────┘
             │                 │                  │
             │                 ↓                  │
             │        ┌────────────────┐          │
             │        │ State Machine  │          │
             │        └────────────────┘          │
             │                 │                  │
             └─────────────────┼──────────────────┘
                               ↓
                    ┌──────────────────────┐
                    │   Recovery Service   │
                    │                      │
                    │ Load state           │
                    │ Load history         │
                    │ Load working memory  │
                    │ Reconstruct context  │
                    └──────────┬───────────┘
                               │
                               ↓
                    ┌──────────────────────┐
                    │     CockroachDB      │
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

- Java
    
- Spring Boot
    
- Spring Data JPA
    
- Hibernate
    
- REST APIs
    
- Maven
    

## Database

- CockroachDB
    
- PostgreSQL-compatible SQL
    
- Transactional persistence
    

## Agent

RecallOps currently uses a deterministic reasoning engine for the MVP.

This provides:

- Reproducible demonstrations
    
- Predictable agent decisions
    
- Reliable testing
    
- No dependency on an external LLM during the core crash/recovery demonstration
    

The reasoning layer is intentionally abstracted behind an interface so that an LLM-powered implementation can be introduced without redesigning the persistence architecture.

## Frontend

- React
    
- TypeScript
    
- Vite
    
- CSS
    

The frontend provides the interactive MVP experience for:

- Creating incidents
    
- Starting investigations
    
- Viewing investigation state
    
- Inspecting working memory
    
- Simulating failures
    
- Observing recovery
    

---

# Why CockroachDB?

The central requirement of RecallOps is durable agent memory.

CockroachDB provides the transactional persistence layer required to keep the investigation state consistent.

RecallOps uses the database for:

```text
Incident
    │
    ├── Investigation
    │       │
    │       ├── State
    │       ├── State History
    │       └── Working Memory
    │
    └── Resolution
```

The database is therefore not simply used as application storage.

It becomes the durable memory layer that allows the agent to survive application failure.

---

# Persistent Memory Model

RecallOps separates persistent memory into structured components.

## Incident

Represents the operational problem being investigated.

Typical information:

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

Typical information:

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

This provides an auditable history of the investigation.

---

## Working Memory

Working memory represents the agent's current reasoning context.

It can contain:

- Hypotheses
    
- Evidence
    
- Observations
    
- Reasoning notes
    
- Current objective
    
- Next action
    
- Confidence
    
- Investigation context
    

The purpose is simple:

> **If the process disappears, the agent should still know what it was doing.**

---

# State Machine

RecallOps prevents arbitrary investigation state changes.

The investigation lifecycle is controlled by a state machine.

Conceptually:

```text
CREATED
   │
   ↓
INVESTIGATING
   │
   ↓
ANALYZING
   │
   ├──────────────→ WAITING_FOR_EVIDENCE
   │                         │
   │                         ↓
   └──────────────────── ANALYZING
                             │
                             ↓
                          RESOLVED
```

Invalid transitions are rejected.

This protects the integrity of the investigation lifecycle.

---

# Failure Recovery

The most important architectural property of RecallOps is recovery.

Suppose the agent reaches:

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

The process crashes.

The following information is still available:

```text
CockroachDB
    │
    ├── Investigation
    ├── State
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

No manual reconstruction is required.

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

Triggers the reasoning engine for the investigation.

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

Loads the durable investigation state and reconstructs the investigation context after a restart.

> See `backend/recallops-api-mappings.txt` for the current API mapping.

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
│   │   │       ├── controller
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── repository/
│   │   │       └── service/
│   │   │
│   │   └── test/
│   │
│   ├── pom.xml
│   ├── mvnw
│   └── application.yml
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
    

A CockroachDB cluster can be used either locally or through CockroachDB Cloud.

---

## 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd RecallOps
```

---

## 2. Configure environment variables

Create a local `.env` file:

```bash
cp .env.example .env
```

Then configure:

```env
DB_PASSWORD=your_actual_database_password
```

The `.env` file is intentionally ignored by Git.

Never commit real credentials.

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

Vite will provide the local frontend URL.

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
2. Create incident
        ↓
3. Start investigation
        ↓
4. Agent begins reasoning
        ↓
5. Working memory is persisted
        ↓
6. Simulate application crash
        ↓
7. Restart backend
        ↓
8. Recover investigation
        ↓
9. Agent resumes from persisted state
        ↓
10. Complete investigation
```

The key visual comparison is:

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

This demonstrates that the agent's memory survived the application failure.

---

# Design Principles

## 1. Durable state over process memory

Important agent state must survive process termination.

---

## 2. Explicit state transitions

The investigation lifecycle is modeled as a state machine rather than arbitrary status updates.

---

## 3. Recoverability

A restarted process should be able to reconstruct an investigation from durable state.

---

## 4. Deterministic MVP

The core demonstration should remain reliable and reproducible.

Therefore, the MVP reasoning engine is deterministic while the architecture remains extensible toward LLM-based reasoning.

---

## 5. Separation of concerns

The system separates:

```text
Incident Management
        ↓
Investigation Lifecycle
        ↓
Agent Reasoning
        ↓
Persistent Working Memory
        ↓
Recovery
```

This keeps the system easier to test and evolve.

---

# Why This Matters for AI Agents

Traditional application recovery often focuses on restoring:

```text
Database state
Queues
Transactions
User sessions
```

AI agents introduce another important dimension:

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
    

If that state exists only in process memory, a crash can destroy the agent's progress.

RecallOps explores a more reliable model:

```text
AI Agent
   │
   │ reasoning
   ↓
Working Memory
   │
   │ checkpoint
   ↓
Durable Database
   │
   │ recovery
   ↓
AI Agent
```

This is the foundation for building more resilient long-running AI systems.

---

# Current MVP Scope

### Included

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
    

### Intentionally Out of Scope

The MVP does not attempt to build a complete production SRE platform.

It intentionally focuses on one architectural problem:

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
    

---

# Future Architecture

The deterministic reasoning engine can eventually be replaced or extended with an LLM-powered agent.

```text
                    ┌──────────────────┐
                    │    LLM Agent     │
                    └────────┬─────────┘
                             │
                    Tool calls / reasoning
                             │
                             ↓
                    ┌──────────────────┐
                    │ Working Memory  │
                    └────────┬─────────┘
                             │
                       checkpoint
                             ↓
                    ┌──────────────────┐
                    │  CockroachDB     │
                    └──────────────────┘
                             │
                           crash
                             ↓
                    ┌──────────────────┐
                    │ Recovery Service │
                    └────────┬─────────┘
                             │
                             ↓
                    ┌──────────────────┐
                    │ Resume Agent     │
                    └──────────────────┘
```

The persistence architecture therefore remains useful even as the intelligence layer evolves.

---

# Hackathon Story

RecallOps is built around a simple demonstration:

> **An AI agent should not forget an investigation just because the process running it crashed.**

The demo intentionally creates a failure at the most interesting point in the investigation.

The application disappears.

The database does not.

The investigation survives.

When the application returns, RecallOps reconstructs the agent's durable context and continues.

That is the central idea behind RecallOps:

**Agents should have memory that survives failure.**

---

# Security

Secrets are intentionally kept outside source control.

The repository ignores:

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

# License

This project is currently an MVP / hackathon project.

License information can be added when the project is prepared for public distribution.

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
    

