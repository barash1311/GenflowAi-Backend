# GeoFlow AI – Backend

GeoFlow AI is a backend system designed to manage datasets, user prompts, and machine learning predictions in a clean and scalable way. The project follows a **microservice-oriented architecture** where the Java backend acts as the central orchestrator and a Python service is responsible for ML computation.

This repository focuses on the **Java Spring Boot backend** and its integration with PostgreSQL and a Python-based ML service.

---

## Project Overview

The core idea behind GeoFlow AI is simple:

* Users interact with the system through prompts
* Prompts are linked to datasets
* The backend sends validated requests to an ML service
* Predictions are stored and tracked for reproducibility

The backend is intentionally designed to be:

* Secure
* Asynchronous
* Traceable
* Easy to extend

---

## Architecture

**High-level flow:**

Frontend → Java Backend → Python ML Service → Java Backend → Database

**Responsibilities split:**

* **Java (Spring Boot):** Authentication, prompt management, job tracking, database persistence
* **Python:** Model loading, prediction logic, result generation
* **PostgreSQL:** Persistent storage for users, prompts, models, and predictions

---

## Tech Stack

**Backend**

* Java 17+
* Spring Boot
* Spring Security (JWT)
* Spring Data JPA / Hibernate

**Database**

* PostgreSQL
* JSONB for prediction results

**ML Layer**

* Python
* FastAPI / Flask (for prediction API)

**Other**

* UUID-based identifiers
* RESTful APIs
* Async-safe job handling

---

## Database Design

The backend uses a normalized schema focused on traceability and reproducibility.

Main tables:

* `users` – authentication and roles
* `datasets` – dataset metadata (raw data stored separately)
* `models` – ML model versions and metrics
* `prompts` – user input prompts
* `prediction_jobs` – async job tracking
* `predictions` – ML outputs stored as JSON

Each prediction can be traced back to:

* the user
* the dataset
* the prompt
* the model version



