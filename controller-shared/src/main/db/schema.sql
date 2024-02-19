/**
  This file represents the sql schema of the v3 backend.
  Execute jooqCodegen to create java classes for these files.
 */

CREATE TYPE cloud_server_state AS ENUM('starting', 'available', 'ingame', 'stopping');

CREATE TABLE IF NOT EXISTS cloud_servers(
    unique_id varchar NOT NULL,
    group_name varchar NOT NULL,
    host_id varchar NOT NULL,
    numerical_id int NOT NULL,
    template_id varchar NOT NULL,
    port int NOT NULL,
    minimum_memory int NOT NULL,
    maximum_memory int NOT NULL,
    player_count int NOT NULL,
    name varchar NOT NULL,
    state cloud_server_state NOT NULL
);

CREATE TABLE IF NOT EXISTS cloud_server_properties(
    server_id varchar NOT NULL,
    key varchar NOT NULL,
    value varchar,
    CONSTRAINT compound_key PRIMARY KEY (server_id, key)
);