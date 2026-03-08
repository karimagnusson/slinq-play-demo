# slinq-play-demo

Slinq is a PostgreSQL query builder for Scala that mirrors SQL structure directly in code. See full documentation at [https://slinq.kotturinn.com/](https://slinq.kotturinn.com/).

slinq-play-demo is an example REST API using [slinq-pg-pekko](https://github.com/karimagnusson/slinq) and [Play Framework](https://www.playframework.com/). Most of the example queries in this demo return rows as Play JSON. There are also examples where rows are returned from the database as a JSON string. Slinq has the ability to build complex JSON objects on the database using subqueries and return the result as a JSON string that can be returned directly to the client.

This version uses Play 3.0.6 with [Pekko](https://pekko.apache.org/). To use [Slinq](https://github.com/karimagnusson/slinq) with [Play](https://www.playframework.com/) as a module it uses [slinq-play](https://github.com/karimagnusson/slinq-play). It depends on [slinq-pg-pekko](https://github.com/karimagnusson/slinq) which provides Pekko Streams support (Source/Sink).

Examples:
- Select, insert, update, delete
- Cached queries
- Subqueries
- Jsonb field
- Array field
- Date/Time methods
- Streaming
- Type-safe queries

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

If you use [Postman](https://www.postman.com/) you can import `postman/play-demo.json` where all the endpoints are set up.
