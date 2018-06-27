update DEMO_PUBLIC_FILE set LINK_ID = '' where LINK_ID is null ;
alter table DEMO_PUBLIC_FILE alter column LINK_ID set not null ;
