--liquibase formatted sql

--changeset cool-online-shop:002-add-user-auth-user-id
alter table users add column auth_user_id bigint not null;
alter table users add constraint users_auth_user_id_unique unique (auth_user_id);
