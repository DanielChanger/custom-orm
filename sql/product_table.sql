create table PRODUCT
(
    NAME  CHARACTER LARGE OBJECT not null,
    ID    BIGINT                 not null,
    PRICE DOUBLE PRECISION       not null,
    constraint PRODUCT_ID
        primary key (ID)
);

