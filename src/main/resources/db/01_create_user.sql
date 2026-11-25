-- OWNER: Sufiyan. Run ONCE as SYSTEM (or another DBA) in SQL Developer / SQL*Plus, connected to the PDB:
--   Oracle Database 23ai Free:  service FREEPDB1     Oracle XE 21c:  service XEPDB1
-- Creates the application schema BLMS. Change the password here AND in config/local.properties.
CREATE USER blms IDENTIFIED BY blms DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO blms;
