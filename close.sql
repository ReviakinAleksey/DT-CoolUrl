alter table "production"."clicks" drop constraint "fk_clicks_to_link"
alter table "production"."folders" drop constraint "fK_folders_to_user"
alter table "production"."links" drop constraint "fK_links_to_user"
alter table "production"."links" drop constraint "fk_links_to_folder"
drop table "production"."clicks"
drop table "production"."folders"
alter table production.links alter column code set default ''
DROP SEQUENCE production.link_code_seq
drop table "production"."links"
drop table "production"."users"
