CREATE TABLE people (
  id            BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  first_name    VARCHAR(25)           NOT NULL,
  middle_name   VARCHAR(25),
  last_name     VARCHAR(25)           NOT NULL,
  date_of_birth TIMESTAMP             NOT NULL,
  married       BOOL                  NOT NULL
);