--liquibase formatted sql

--changeset cool-online-shop:004-add-product-deleted
alter table products
    add column deleted boolean not null default false;
