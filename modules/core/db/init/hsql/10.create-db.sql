-- begin DEMO_PUBLIC_FILE
create table DEMO_PUBLIC_FILE (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    FILE_ID varchar(36) not null,
    LINK_ID varchar(255) not null,
    --
    primary key (ID)
)^
-- end DEMO_PUBLIC_FILE
