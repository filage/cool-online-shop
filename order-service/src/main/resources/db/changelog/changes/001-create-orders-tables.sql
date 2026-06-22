--liquibase formatted sql

--changeset cool-online-shop:001-create-orders-table
create table orders (
    id bigserial primary key,
    user_id bigint not null,
    status varchar(50) not null,
    total_amount numeric(10, 2) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

--changeset cool-online-shop:002-create-order-items-table
create table order_items (
    id bigserial primary key,
    order_id bigint not null,
    product_id bigint not null,
    product_name varchar(255) not null,
    product_price numeric(10, 2) not null,
    quantity integer not null,

    constraint fk_order_items_order
        foreign key (order_id)
        references orders(id)
);
