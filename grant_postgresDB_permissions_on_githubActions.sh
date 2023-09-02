#!/bin/bash

# Wait for PostgreSQL to be ready (you can adjust the timeout as needed)
echo "Waiting for PostgreSQL to be ready..."
until pg_isready -h localhost -U postgres -q; do
  sleep 5
done

# Grant permissions on the public schema
echo "Granting permissions on the public schema..."
psql -h localhost -U postgres -d fineract_default -c "GRANT ALL PRIVILEGES ON SCHEMA public TO fineract_default;"
psql -h localhost -U postgres -d fineract_default -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO fineract_default;"
