CREATE TABLE people (
  id            BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  version       BIGINT                NOT NULL,
  first_name    VARCHAR(25)           NOT NULL,
  middle_name   VARCHAR(25),
  last_name     VARCHAR(25)           NOT NULL,
  date_of_birth TIMESTAMP             NOT NULL,
  married    BOOL NOT NULL,
  home_line1 VARCHAR(30),
  home_line2 VARCHAR(30),
  home_city  VARCHAR(20),
  home_state VARCHAR(2),
  home_zip   VARCHAR(10)
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