create table "production"."users" ("id" BIGINT NOT NULL,"token" VARCHAR(254) NOT NULL PRIMARY KEY);
create unique index "idx_user_token" on "production"."users" ("token");
create table "production"."links" ("code" VARCHAR(254) NOT NULL PRIMARY KEY,"user_token" VARCHAR(254) NOT NULL,"url" VARCHAR(254) NOT NULL,"id_folder" BIGINT);
create sequence production.link_code_seq;

alter table production.links
 alter column code set default 'lk' || nextval('production.link_code_seq');
create table "production"."folders" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"user_token" VARCHAR(254) NOT NULL,"title" VARCHAR(254) NOT NULL);
create unique index "idx_folders_id_and_token" on "production"."folders" ("id","user_token");
create unique index "idx_folders_token_and_title" on "production"."folders" ("user_token","title");
create table "production"."clicks" ("link_code" VARCHAR(254) NOT NULL,"date" TIMESTAMP NOT NULL,"referer" VARCHAR(254) NOT NULL,"remote_ip" VARCHAR(254) NOT NULL);
create index "link_code_index" on "production"."clicks" ("link_code");
alter table "production"."links" add constraint "fK_links_to_user" foreign key("user_token") references "production"."users"("token") on update RESTRICT on delete CASCADE;
alter table "production"."links" add constraint "fk_links_to_folder" foreign key("id_folder","user_token") references "production"."folders"("id","user_token") on update RESTRICT on delete CASCADE;
alter table "production"."folders" add constraint "fK_folders_to_user" foreign key("user_token") references "production"."users"("token") on update RESTRICT on delete CASCADE;
alter table "production"."clicks" add constraint "fk_clicks_to_link" foreign key("link_code") references "production"."links"("code") on update RESTRICT on delete CASCADE;
