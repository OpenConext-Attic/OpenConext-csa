DROP TABLE IF EXISTS screenshot;
DROP TABLE IF EXISTS field_string;
DROP TABLE IF EXISTS field_image;
DROP TABLE IF EXISTS compound_service_provider;
DROP TABLE IF EXISTS facet_value_compound_service_provider;
DROP TABLE IF EXISTS localized_string;
DROP TABLE IF EXISTS facet_value;
DROP TABLE IF EXISTS facet;
DROP TABLE IF EXISTS multilingual_string;

CREATE TABLE compound_service_provider (id                         BIGINT GENERATED BY DEFAULT AS IDENTITY (
START WITH 1 ),                         available_for_end_user BIT, lmng_id VARCHAR(255), service_provider_entity_id VARCHAR(255), PRIMARY KEY (id), UNIQUE (service_provider_entity_id));

CREATE TABLE facet (id                     BIGINT GENERATED BY DEFAULT AS IDENTITY (
START WITH 1 ),     multilingual_string_id BIGINT NOT NULL, facet_parent_id BIGINT, PRIMARY KEY (id));

CREATE TABLE facet_value (id       BIGINT GENERATED BY DEFAULT AS IDENTITY (
START WITH 1 ),           facet_id BIGINT NOT NULL, multilingual_string_id BIGINT NOT NULL, PRIMARY KEY (id));

CREATE TABLE facet_value_compound_service_provider (compound_service_provider_id BIGINT NOT NULL, facet_value_id BIGINT NOT NULL, PRIMARY KEY (compound_service_provider_id, facet_value_id));

CREATE TABLE field_image (id        BIGINT GENERATED BY DEFAULT AS IDENTITY (
START WITH 1 ),           field_key INTEGER, field_source INTEGER, field_image LONGVARBINARY, compound_service_provider_id BIGINT NOT NULL, PRIMARY KEY (id));

CREATE TABLE field_string (id        BIGINT GENERATED BY DEFAULT AS IDENTITY (
START WITH 1 ),            field_key INTEGER, field_source INTEGER, field_value VARCHAR(65535), compound_service_provider_id BIGINT NOT NULL, PRIMARY KEY (id));

CREATE TABLE localized_string (id     BIGINT GENERATED BY DEFAULT AS IDENTITY (
START WITH 1 ),                locale VARCHAR(255), value VARCHAR(255), multilingual_string_id BIGINT NOT NULL, PRIMARY KEY (id));

CREATE TABLE multilingual_string (id BIGINT GENERATED BY DEFAULT AS IDENTITY (
START WITH 1 ), PRIMARY KEY (id));

CREATE TABLE screenshot (id          BIGINT GENERATED BY DEFAULT AS IDENTITY (
START WITH 1 ),          field_image LONGVARBINARY, compound_service_provider_id BIGINT NOT NULL, PRIMARY KEY (id));

INSERT INTO compound_service_provider (available_for_end_user, lmng_id, service_provider_entity_id)
VALUES (TRUE, '{DEAD-CAFE-BABE}', 'https://delete-me');