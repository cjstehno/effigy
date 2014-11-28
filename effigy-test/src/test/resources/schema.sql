CREATE TABLE people (
  id            BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  version       BIGINT                NOT NULL,
  first_name    VARCHAR(25)           NOT NULL,
  middle_name   VARCHAR(25),
  last_name     VARCHAR(25)           NOT NULL,
  date_of_birth TIMESTAMP             NOT NULL,
  married       BOOL                  NOT NULL
);

CREATE TABLE pets (
  id     BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name   VARCHAR(10)           NOT NULL,
  animal VARCHAR(10)           NOT NULL
);

create table peoples_pets (
  person_id BIGINT REFERENCES people(id),
  pet_id BIGINT REFERENCES pets(id),
  UNIQUE (person_id, pet_id)
);