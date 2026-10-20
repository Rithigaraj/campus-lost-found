<div align="center">

# Campus Lost & Found Network

**Report lost belongings. Register found items. Recover them with smart matching.**

A full-stack Java application with a Spring Boot REST API, a responsive web UI,
a JDBC/MySQL data layer, and an optional console client.

[![Live Demo](https://img.shields.io/badge/Live%20Demo-onrender.com-46E3B7?logo=render&logoColor=white)](https://campus-lost-found-idnf.onrender.com)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-rithigarajkumar-2496ED?logo=docker&logoColor=white)](https://hub.docker.com/r/rithigarajkumar/campus-lost-found)

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-compose-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

**🔗 Live demo: [campus-lost-found-idnf.onrender.com](https://campus-lost-found-idnf.onrender.com)**

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Screenshots](#screenshots)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Smart Matching Algorithm](#smart-matching-algorithm)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Setup (Maven + MySQL)](#local-setup-maven--mysql)
  - [Run with Docker](#run-with-docker)
- [REST API Reference](#rest-api-reference)
- [Console Client](#console-client)
- [Configuration](#configuration)
- [Testing](#testing)
- [Deployment](#deployment)
  - [Live deployment (Render)](#live-deployment-render)
  - [Docker (self-hosted)](#docker-self-hosted)
  - [Pre-built Docker image](#pre-built-docker-image)
- [Roadmap](#roadmap)
- [License](#license)

---

## Overview

The **Campus Lost & Found Network** helps students and faculty report lost belongings
and register items found anywhere on campus. All reports are stored in a centralized
MySQL database, and the built-in **smart matching** engine compares new reports
against existing ones to suggest likely matches based on category, description
keywords, location, and date proximity.

This repository contains **three interfaces** sharing one data layer:

| Interface | Technology | Entry point |
|-----------|------------|-------------|
| **Web app** (primary) | Spring Boot + HTML/CSS/JS | `CampusLostFoundApplication` |
| **Console client** | Java menu-driven CLI | `com.campuslostfound.Main` |
| **REST API** | Spring MVC (JSON) | served at `/api/**` |

## Features

- **Report Lost Item** – register an item reported missing.
- **Report Found Item** – record an item found on campus.
- **View Reports** – browse all lost and found items; filter by lost or found.
- **Smart Matching** – compute a similarity score (0–100) between reports and
  suggest possible matches; confirm a match with one click.
- **Update Reports** – edit any field or change the report status.
- **Delete Reports** – remove incorrect or unnecessary reports.
- **Mark as Returned** – flag an item as handed back to its owner.
- Automatic **match suggestions right after** a report is created.
- Status lifecycle: `OPEN` → `MATCHED` → `RETURNED` / `CLOSED`.
- Consistent **JSON error handling** (400 / 404 / 500).
- Fully **containerized** with Docker Compose (app + MySQL).

## Screenshots

> Add your own screenshots after running the app, e.g.:

| Dashboard / View Reports | Smart Matching |
|--------------------------|----------------|
| _`docs/screenshot-reports.png`_ | _`docs/screenshot-matching.png`_ |

## Tech Stack

- **Java 17+** (builds and runs on 21/26)
- **Spring Boot 4.1.1** – REST API, embedded Tomcat, Jackson 3
- **JDBC** (no ORM) – hand-written DAO layer with `PreparedStatement`
- **MySQL 8** – `campus_lost_found` database
- **HTML5 / CSS3 / vanilla JavaScript** – no frontend build step
- **Maven** – build & dependency management
- **Docker / Docker Compose** – one-command deployment
- **GitHub Actions** – continuous integration

## Project Structure

```
Campus_Lost/
├── pom.xml
├── schema.sql                     # MySQL DDL (creates database + tables)
├── Dockerfile                     # multi-stage build (Maven -> JRE)
├── docker-compose.yml             # app + mysql services
├── .env.example                   # sample DB password for compose
├── .dockerignore
├── .gitignore
├── LICENSE
├── README.md
├── .github/workflows/ci.yml       # CI: build + upload jar
└── src/main/
    ├── java/com/campuslostfound/
    │   ├── CampusLostFoundApplication.java   # Spring Boot entry point
    │   ├── Main.java                         # console menu client
    │   ├── web/                              # REST controllers
    │   │   ├── LostItemController.java
    │   │   ├── FoundItemController.java
    │   │   ├── MatchController.java
    │   │   └── ApiExceptionHandler.java
    │   ├── dao/                              # JDBC data access
    │   │   ├── LostItemDAO.java
    │   │   └── FoundItemDAO.java
    │   ├── model/                            # domain objects
    │   │   ├── LostItem.java
    │   │   ├── FoundItem.java
    │   │   └── ItemStatus.java
    │   ├── service/                          # matching engine
    │   │   ├── MatchingService.java
    │   │   └── MatchResult.java
    │   └── db/
    │       └── DatabaseManager.java          # JDBC connectivity
    └── resources/
        ├── application.properties            # Spring Boot config
        ├── db.properties(.example)           # DB credentials
        └── static/                           # web frontend
            ├── index.html
            ├── styles.css
            └── app.js
```

## Database Schema

```sql
CREATE DATABASE IF NOT EXISTS campus_lost_found;

CREATE TABLE LOST_ITEMS (
    lost_id      INT AUTO_INCREMENT PRIMARY KEY,
    item_name    VARCHAR(100) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    description  TEXT,
    location     VARCHAR(150),
    date_lost    DATE         NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_info VARCHAR(150) NOT NULL,
    status       ENUM('OPEN','MATCHED','RETURNED','CLOSED') NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE FOUND_ITEMS (
    found_id     INT AUTO_INCREMENT PRIMARY KEY,
    item_name    VARCHAR(100) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    description  TEXT,
    location     VARCHAR(150),
    date_found   DATE         NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_info VARCHAR(150) NOT NULL,
    status       ENUM('OPEN','MATCHED','RETURNED','CLOSED') NOT NULL DEFAULT 'OPEN'
);
```

## Smart Matching Algorithm

`MatchingService` scores each lost/found pair out of **100**:

| Signal | Weight | Scoring |
|--------|--------|---------|
| Category | 25 | exact match = 25, partial/substring = 12 |
| Location | 20 | exact = 20, containment = 15, else token Jaccard × 20 |
| Date proximity | 20 | same day = 20, ≤3d = 15, ≤7d = 10, ≤14d = 5 |
| Name + description keywords | 35 | Jaccard similarity over tokenized text × 35 |

Pairs scoring **≥ 40** are suggested. Confirming a match sets both reports to
`MATCHED`. Stop-words are stripped and text is normalized before comparison.

## Getting Started

### Prerequisites

- JDK 17 or newer
- Maven 3.9+
- MySQL 8 (local install) **or** Docker Desktop

### Local Setup (Maven + MySQL)

```bash
# 1. Create the database and tables
mysql -u root -p < schema.sql

# 2. Configure credentials
#    edit src/main/resources/db.properties (see db.properties.example)

# 3. Build
mvn clean package

# 4. Run the web app
java -jar target/campus-lost-found-1.0.0.jar
```

Open **http://localhost:8080** in your browser.

### Run with Docker

The whole stack (Spring Boot app + MySQL) starts with one command:

```bash
# Optional: copy the sample env file and set your own DB password
cp .env.example .env          # Windows: copy .env.example .env

docker compose up --build
# ...or run detached:
docker compose up --build -d
```

Then open **http://localhost:8080**.

- MySQL is exposed on host port **3307** (mapped to container 3306).
- The schema is applied automatically on first start via `schema.sql`.
- Data persists in the named volume `mysql-data`.
- Verified with Docker Compose v2 (`docker compose ps`, both containers healthy).

#### Pre-built image (Docker Hub)

A pre-built image is published at **https://hub.docker.com/r/rithigarajkumar/campus-lost-found**:

```bash
docker pull rithigarajkumar/campus-lost-found:latest
```

Run it against any MySQL instance by supplying the connection settings:

```bash
docker run -d --name clf-app -p 8080:8080 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/campus_lost_found?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e DB_USER=root \
  -e DB_PASSWORD=YourPassword \
  rithigarajkumar/campus-lost-found:latest
```

Stop and remove:

```bash
docker compose down          # keep data
docker compose down -v       # also delete the database volume
```

## REST API Reference

Base URL: `http://localhost:8080`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/lost` | List all lost reports |
| `GET` | `/api/lost/{id}` | Get one lost report |
| `POST` | `/api/lost` | Create a lost report |
| `PUT` | `/api/lost/{id}` | Update a lost report |
| `PATCH` | `/api/lost/{id}/status` | Update status only |
| `DELETE` | `/api/lost/{id}` | Delete a lost report |
| `GET` | `/api/found` | List all found reports |
| `GET` | `/api/found/{id}` | Get one found report |
| `POST` | `/api/found` | Create a found report |
| `PUT` | `/api/found/{id}` | Update a found report |
| `PATCH` | `/api/found/{id}/status` | Update status only |
| `DELETE` | `/api/found/{id}` | Delete a found report |
| `GET` | `/api/matches?scope=all\|lost\|found&id={id}` | List possible matches |
| `POST` | `/api/matches/confirm` | Confirm a lost/found pair (both → `MATCHED`) |

**Example – create a lost report**

```bash
curl -X POST http://localhost:8080/api/lost \
  -H "Content-Type: application/json" \
  -d '{
        "itemName": "Blue Water Bottle",
        "category": "Electronics",
        "description": "Steel bottle with college sticker",
        "location": "Cafeteria",
        "dateLost": "2026-10-18",
        "contactName": "Rithiga",
        "contactInfo": "9876543210",
        "status": "OPEN"
      }'
```

**Example – confirm a match**

```bash
curl -X POST http://localhost:8080/api/matches/confirm \
  -H "Content-Type: application/json" \
  -d '{"lostId": 1, "foundId": 1}'
```

## Console Client

The original menu-driven CLI is still available:

```bash
java -cp "target/classes;target/lib/*" com.campuslostfound.Main      # Windows
java -cp "target/classes:target/lib/*" com.campuslostfound.Main      # macOS/Linux
```

## Configuration

Connection settings live in `src/main/resources/db.properties` and can be
overridden with environment variables (used by Docker):

| Property | Env var | Default |
|----------|---------|---------|
| `db.url` | `DB_URL` | `jdbc:mysql://127.0.0.1:3306/campus_lost_found?...` |
| `db.user` | `DB_USER` | `root` |
| `db.password` | `DB_PASSWORD` | _(empty)_ |
| `server.port` | `SERVER_PORT` | `8080` |

> **Security note:** never commit real credentials. Use `db.properties.example`
> as a template and provide secrets via environment variables in production.

## Testing

A quick end-to-end check with the app running:

```bash
# health: empty list expected
curl http://localhost:8080/api/lost

# page loads
curl -s http://localhost:8080/ | findstr "Campus Lost"

# build without running tests (CI default)
mvn clean package -DskipTests
```

## Deployment

This project is **fully containerized with Docker** and is deployed in two ways:
a self-contained **Docker Compose** stack for local/self-hosted use, and a
**Docker image on Docker Hub** that is running live on **Render**.

### Live deployment (Render)

The production instance runs the Docker image on [Render](https://render.com):

- **URL:** https://campus-lost-found-idnf.onrender.com
- **App service:** free web service built from the repo `Dockerfile`
- **Database:** Aiven free MySQL 8.4 (`LOST_ITEMS` / `FOUND_ITEMS` created from `schema.sql`)
- **Environment variables:** `DB_URL`, `DB_USER`, `DB_PASSWORD`
- **Auto-deploy:** every push to `master`

### Docker (self-hosted)

Everything (Spring Boot app + MySQL) runs from one command — Docker is the
primary way this project is packaged and shipped:

```bash
docker compose up -d --build
```

- App: http://localhost:8080 — MySQL: host port 3307
- `schema.sql` is applied automatically on first start; data persists in the `mysql-data` volume.

### Pre-built Docker image

The same build is published to Docker Hub:

```bash
docker pull rithigarajkumar/campus-lost-found:latest
```

### Railway / Fly.io / other container hosts

1. Connect the GitHub repository (or point at the Docker Hub image).
2. Add a managed MySQL add-on and note its host, port, user, password.
3. Set environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
4. Build from the `Dockerfile`, or: build `mvn clean package -DskipTests`,
   start `java -jar target/campus-lost-found-1.0.0.jar`.
5. Run `schema.sql` against the managed database once.

### Behind a reverse proxy

The app listens on `8080`; set `SERVER_PORT` if your platform injects a port.
Forward `/` to the app; static assets and API are served from the same origin.

## Roadmap

- [ ] User authentication (students / staff / admin roles)
- [ ] Image uploads for reports
- [ ] Email notifications when a match is found
- [ ] Pagination and search filters
- [ ] Swagger/OpenAPI documentation
- [ ] Unit & integration test suite (JUnit 5 + Testcontainers)

## License

Released under the [MIT License](LICENSE).
