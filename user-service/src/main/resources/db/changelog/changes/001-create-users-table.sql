--liquibase formatted sql

--changeset cool-online-shop:001-create-users-table
create table users (
    id bigserial primary key,
    email varchar(255) not null unique,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    phone varchar(50),
    created_at timestamp not null,
    updated_at timestamp not null
);
