/**
  This file represents the sql schema of the v3 backend.
  Execute jooqCodegen to create java classes for these files.
 */

CREATE TABLE IF NOT EXISTS cloud_servers(
    unique_id varchar NOT NULL PRIMARY KEY,
    group_name varchar NOT NULL,
    host_id varchar NOT NULL,
    numerical_id int NOT NULL,
    ip varchar NOT NULL,
    port int NOT NULL,
    minimum_memory int NOT NULL,
    maximum_memory int NOT NULL,
    max_players int NOT NULL,
    player_count int NOT NULL,
    state varchar NOT NULL,
    type varchar NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cloud_server_properties(
    server_id varchar NOT NULL,
    key varchar NOT NULL,
    value varchar,
    CONSTRAINT compound_key PRIMARY KEY (server_id, key)
);