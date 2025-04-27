drop table if exists customer;
drop table if exists orders;
drop table if exists lineitem;
drop table if exists part;
drop table if exists partsupp;
drop table if exists supplier;
drop table if exists nation;
drop table if exists region;

CREATE TABLE IF NOT EXISTS customer USING ${DATA_GEN_FORMAT} AS SELECT * FROM ${DATA_GEN_NS}.customer;
CREATE TABLE IF NOT EXISTS orders USING ${DATA_GEN_FORMAT} AS SELECT * FROM ${DATA_GEN_NS}.orders;
CREATE TABLE IF NOT EXISTS lineitem USING ${DATA_GEN_FORMAT} AS SELECT * FROM ${DATA_GEN_NS}.lineitem;
CREATE TABLE IF NOT EXISTS part USING ${DATA_GEN_FORMAT} AS SELECT * FROM ${DATA_GEN_NS}.part;
CREATE TABLE IF NOT EXISTS partsupp USING ${DATA_GEN_FORMAT} AS SELECT * FROM ${DATA_GEN_NS}.partsupp;
CREATE TABLE IF NOT EXISTS supplier USING ${DATA_GEN_FORMAT} AS SELECT * FROM ${DATA_GEN_NS}.supplier;
CREATE TABLE IF NOT EXISTS nation USING ${DATA_GEN_FORMAT} AS SELECT * FROM ${DATA_GEN_NS}.nation;
CREATE TABLE IF NOT EXISTS region USING ${DATA_GEN_FORMAT} AS SELECT * FROM ${DATA_GEN_NS}.region;
