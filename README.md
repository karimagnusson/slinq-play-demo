# slinq-play-demo

A comprehensive demonstration REST API showcasing [Slinq](https://github.com/karimagnusson/slinq), a type-safe SQL query builder for PostgreSQL, integrated with [Play Framework](https://www.playframework.com/) for building web services. This demo uses [slinq-play](https://github.com/karimagnusson/slinq-play) as a Play module.

## Tech Stack

- **Scala** 3.3.7
- **Play Framework** 3.0.6
- **Pekko** 1.0.3
- **Slinq** 0.9.6-RC2
- **PostgreSQL** (World database sample)

## Features

This demo provides practical examples of:

### Core Operations

- **CRUD Operations** - Type-safe SELECT, INSERT, UPDATE, DELETE queries
- **Type-Safe Queries** - Compile-time verified SQL with case class mapping using Play JSON
- **Query Caching** - Pre-compiled queries with dynamic WHERE conditions for improved performance

### Advanced Features

- **Streaming** - Efficient CSV export/import with Pekko Streams
- **JSONB Support** - PostgreSQL JSONB field operations (query, update, nested access)
- **Array Operations** - PostgreSQL array field manipulation
- **Date/Time Functions** - PostgreSQL timestamp methods and operations
- **JOIN Queries** - Multi-table queries with subqueries and aggregates
- **Conditional WHERE** - Optional query parameters with type-safe builders

### Code Examples

Each route file demonstrates specific functionality:

- `SelectPlayJsonRoute.scala` - SELECT queries returning Play JSON via `.colsNamed`
- `SelectDbJsonRoute.scala` - SELECT queries returning JSON strings built on the database
- `OperationRoute.scala` - INSERT, UPDATE, DELETE with RETURNING
- `SelectTypeRoute.scala` - Type-safe queries with case class serialization
- `CacheRoute.scala` - Cached queries with pickWhere
- `StreamRoute.scala` - Streaming CSV export/import
- `JsonbRoute.scala` - JSONB field operations
- `ArrayRoute.scala` - PostgreSQL array operations
- `DateRoute.scala` - Timestamp and date functions

## Getting Started

### Prerequisites

- PostgreSQL installed and running
- Scala and sbt installed

### Setup

1. Create a new Play project:

```bash
sbt new playframework/play-scala-seed.g8
```

2. Copy the contents of the `play/` folder from this project into the new Play project, replacing `app`, `conf` and `build.sbt`.

3. Copy the `play/lib/` folder into the new Play project. It contains the Slinq JARs that are picked up automatically by sbt as unmanaged dependencies.

### Database Setup

1. Create the database:

```sql
CREATE DATABASE world;
```

2. Import the sample data:

```bash
psql world < db/world.pg
```

### Configuration

Update the database credentials in `conf/application.conf`:

```hocon
slinq = {
  db = "world"
  user = "<YOUR_USERNAME>"
  password = "<YOUR_PASSWORD>"
}
```

### Running the Application

Start the server on port 9000:

```bash
sbt run
```

### Testing with Postman

If you use [Postman](https://www.postman.com/), import the collection from `postman/play-demo.json` to get all endpoints pre-configured with example requests.

## API Examples

The demo includes various endpoints demonstrating Slinq features:

- `GET /select/country/:code` - Simple SELECT query
- `GET /select/cities/:code` - JOIN with custom field names
- `POST /type/insert/trip` - Type-safe INSERT with case class
- `GET /stream/export/:coin` - Stream database results as CSV
- `POST /stream/import` - Stream CSV file to database
- `GET /jsonb/country/:code` - Query JSONB fields
- `PATCH /array/add/lang` - Add element to PostgreSQL array

See the route files in `play/app/controllers/` for complete implementation details.

## Learn More

- [Slinq Documentation](https://slinq.kotturinn.com/)
- [Slinq GitHub](https://github.com/karimagnusson/slinq)
- [Play Framework Documentation](https://www.playframework.com/documentation)
- [Pekko Documentation](https://pekko.apache.org/docs/)
