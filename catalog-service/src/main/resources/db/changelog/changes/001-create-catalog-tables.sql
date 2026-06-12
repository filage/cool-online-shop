--liquibase formatted sql

--changeset cool-online-shop:001-create-categories-table
create table categories (
    id bigserial primary key,
    name varchar(255) not null,
    description text
);

--changeset cool-online-shop:002-create-products-table
create table products (
    id bigserial primary key,
    name varchar(255) not null,
    description text,
    price numeric(10, 2) not null,
    category_id bigint not null,
    available_quantity integer not null,
    created_at timestamp not null,
    updated_at timestamp not null,

    constraint fk_products_category
        foreign key (category_id)
        references categories(id)
);

--changeset cool-online-shop:003-insert-test-catalog-data
insert into categories (name, description)
values
    ('Electronics', 'Electronic devices and accessories'),
    ('Books', 'Printed and digital books');

insert into products (
    name,
    description,
    price,
    category_id,
    available_quantity,
    created_at,
    updated_at
)
values
    (
        'Wireless Mouse',
        'Compact wireless mouse',
        29.99,
        1,
        50,
        now(),
        now()
    ),
    (
        'Spring Boot Guide',
        'Practical guide for Spring Boot applications',
        39.99,
        2,
        20,
        now(),
        now()
    );