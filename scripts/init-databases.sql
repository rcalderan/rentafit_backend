-- Creates the costume_rental_nfe database alongside rentafit (which is created by POSTGRES_DB env var)
-- This script runs only once during postgres container initialization.
SELECT 'CREATE DATABASE costume_rental_nfe'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'costume_rental_nfe')\gexec
