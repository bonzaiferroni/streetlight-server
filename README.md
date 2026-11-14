# Module server

## Introduction

The Streetlight server: a Ktor application that serves the pages, the API and the webhooks over its database.

## Packages

| Package | Holds |
|---|---|
| `streetlight.server` | The application, its entry and its environment |
| [`streetlight.server.plugins`](src/main/kotlin/streetlight/server/plugins/README.md) | Ktor installation and configuration, and the routing tree |
| [`streetlight.server.routes`](src/main/kotlin/streetlight/server/routes/README.md) | Implementations of the endpoints declared in `Api` |
| [`streetlight.server.service`](src/main/kotlin/streetlight/server/service/README.md) | The server's work, coordinating daos and clients |
| [`streetlight.server.model`](src/main/kotlin/streetlight/server/model/README.md) | The dependency container and the server's own DTOs |
| `streetlight.server.db` | The setup of the database and its policies |
| [`streetlight.server.db.dao`](src/main/kotlin/streetlight/server/db/dao/README.md) | The read and write operations over the tables |
| [`streetlight.server.db.tables`](src/main/kotlin/streetlight/server/db/tables/README.md) | Exposed table definitions |
| `streetlight.server.external` | The clients of outside services, such as Stripe, Postmark and OpenStreetMap |
| `streetlight.server.utils` | Utilities for files, images, ids, sessions, subdomains and time zones |
| `streetlight.server.tools` | Tasks run by hand, such as generating a migration |
| [`streetlight.server.api`](src/test/kotlin/streetlight/server/api/README.md) | Tests that enter through an HTTP request and assert on the database |
| [`streetlight.server.integration`](src/test/kotlin/streetlight/server/integration/README.md) | Integration tests, one user-facing flow per file |
| [`streetlight.server.e2e`](src/test/kotlin/streetlight/server/e2e/README.md) | End-to-end tests through a real browser, server and database |
