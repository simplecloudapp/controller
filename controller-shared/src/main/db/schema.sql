/**
  This file represents the sql schema of the v3 backend.
  Execute jooqCodegen to create java classes for these files.
 */

CREATE TYPE cloud_server_state AS ENUM('starting', 'available', 'ingame', 'stopping');

CREATE TABLE IF NOT EXISTS cloud_servers(
    unique_id text NOT NULL,
    group_name text NOT NULL,
    host_id text NOT NULL,
    numerical_id int NOT NULL,
    template_id text NOT NULL,
    port int NOT NULL,
    minimum_memory int NOT NULL,
    maximum_memory int NOT NULL,
    player_count int NOT NULL,
    name text NOT NULL,
    state cloud_server_state NOT NULL
);

CREATE TABLE IF NOT EXISTS cloud_server_properties(
    server_id text NOT NULL,
    key text NOT NULL,
    value text
);