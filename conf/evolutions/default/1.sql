# --- !Ups

create table "ACCOUNTS" (
  "ID" CHAR(7) PRIMARY KEY NOT NULL,
  "PW_HASH_WITH_SALT" VARCHAR(254) NOT NULL
);

create table "USERS" (
  "ID" CHAR(7) NOT NULL,
  "FIRST_NAME" VARCHAR(254) NOT NULL,
  "LAST_NAME" VARCHAR(254) NOT NULL,
  constraint "ID_FK" foreign key("ID") references "ACCOUNTS"("ID") on update RESTRICT on delete CASCADE
);

create table "POSTS" (
  "POST_ID" VARCHAR(254) PRIMARY KEY NOT NULL,
  "AUTHOR_ID" CHAR(7) NOT NULL,
  "TITLE" VARCHAR(254) NOT NULL,
  "BODY" VARCHAR(254) NOT NULL,
  "CATEGORY" VARCHAR(254) NOT NULL,
  constraint "AUTHOR_ID_FK" foreign key("AUTHOR_ID") references "ACCOUNTS"("ID") on update RESTRICT on delete CASCADE
);

# --- !Downs

drop table "ACCOUNTS";

drop table "USERS";

drop table "POSTS";
