CREATE TABLE people (
  id            BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  version       BIGINT                NOT NULL,
  first_name    VARCHAR(25)           NOT NULL,
  middle_name   VARCHAR(25),
  last_name     VARCHAR(25)           NOT NULL,
  date_of_birth TIMESTAMP             NOT NULL,
  married       BOOL                  NOT NULL,
  home_line1    VARCHAR(30),
  home_line2    VARCHAR(30),
  home_city     VARCHAR(20),
  home_state    VARCHAR(2),
  home_zip      VARCHAR(10)
);

CREATE TABLE pets (
  id     BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name   VARCHAR(10)           NOT NULL,
  animal VARCHAR(10)           NOT NULL
);

CREATE TABLE peoples_pets (
  person_id BIGINT REFERENCES people (id),
  pet_id    BIGINT REFERENCES pets (id),
  UNIQUE (person_id, pet_id)
);

CREATE TABLE jobs (
  id    BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  title VARCHAR(25)           NOT NULL
);

CREATE TABLE people_job (
  people_id BIGINT REFERENCES people (id),
  jobs_id   BIGINT REFERENCES jobs (id),
  UNIQUE (people_id, jobs_id)
);

CREATE TABLE employers (
  people_id BIGINT REFERENCES people (id),
  line1     VARCHAR(30),
  line2     VARCHAR(30),
  city      VARCHAR(20),
  state     VARCHAR(2),
  zip       VARCHAR(10)
);

CREATE TABLE rooms (
  id       BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name     VARCHAR(25)           NOT NULL,
  capacity BIGINT                NOT NULL
);

CREATE TABLE features (
  id   BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  type VARCHAR(10)           NOT NULL,
  name VARCHAR(25)           NOT NULL
);

CREATE TABLE rooms_features (
  rooms_id    BIGINT REFERENCES rooms (id),
  features_id BIGINT REFERENCES features (id),
  UNIQUE (rooms_id, features_id)
);

CREATE TABLE images (
  id          BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  version     BIGINT                NOT NULL,
  description VARCHAR(255)          NOT NULL,
  cont_len    VARCHAR(100)          NOT NULL
)