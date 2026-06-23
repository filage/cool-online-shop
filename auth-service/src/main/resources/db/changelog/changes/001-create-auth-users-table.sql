--liquibase formatted sql

--changeset cool-online-shop:001-create-auth-users-table
create table auth_users (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    role varchar(50) not null,
    created_at timestamp not null
);
