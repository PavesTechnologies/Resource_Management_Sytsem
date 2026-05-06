# CDC Database Migration Examples

This document shows how to configure CDC for different database types when migrating PMS from one database to another.

## Current Configuration (MySQL)

```properties
# Database Type Configuration (MySQL/PostgreSQL/SQL Server/etc.)
cdc.database.type=mysql
cdc.connector.class=io.debezium.connector.mysql.MySqlConnector

# Database Connection
cdc.database.hostname=pms-db-service-ruchithacloud-9d59.c.aivencloud.com
cdc.database.port=14189
cdc.database.user=avnadmin
cdc.database.password=AVNS_GAUyWTQz-MGNiSAVpPS
cdc.database.name=ajay

# Table Configuration
cdc.database.include.list=ajay
cdc.table.include.list=ajay.projects

# CDC Storage Configuration
cdc.base.directory=${user.home}/rms-cdc
cdc.server.name=pms_mysql
cdc.topic.prefix=pms
```

## Migration to PostgreSQL

```properties
# Database Type Configuration (MySQL/PostgreSQL/SQL Server/etc.)
cdc.database.type=postgresql
cdc.connector.class=io.debezium.connector.postgresql.PostgresConnector

# Database Connection
cdc.database.hostname=postgres-pms-cluster.example.com
cdc.database.port=5432
cdc.database.user=postgres_admin
cdc.database.password=SecurePostgresPassword123
cdc.database.name=pms_production

# Table Configuration
cdc.database.include.list=pms_production
cdc.table.include.list=pms_production.projects

# CDC Storage Configuration
cdc.base.directory=${user.home}/rms-cdc
cdc.server.name=pms_postgres
cdc.topic.prefix=pms
```

## Migration to SQL Server

```properties
# Database Type Configuration (MySQL/PostgreSQL/SQL Server/etc.)
cdc.database.type=sqlserver
cdc.connector.class=io.debezium.connector.sqlserver.SqlServerConnector

# Database Connection
cdc.database.hostname=sqlserver-pms.company.local
cdc.database.port=1433
cdc.database.user=sql_admin
cdc.database.password=SqlServerSecurePass456
cdc.database.name=PMS_Database

# Table Configuration
cdc.database.include.list=PMS_Database
cdc.table.include.list=PMS_Database.dbo.projects

# CDC Storage Configuration
cdc.base.directory=${user.home}/rms-cdc
cdc.server.name=pms_sqlserver
cdc.topic.prefix=pms
```

## Migration to Oracle

```properties
# Database Type Configuration (MySQL/PostgreSQL/SQL Server/etc.)
cdc.database.type=oracle
cdc.connector.class=io.debezium.connector.oracle.OracleConnector

# Database Connection
cdc.database.hostname=oracle-pms-db.company.com
cdc.database.port=1521
cdc.database.user=oracle_admin
cdc.database.password=OracleSecurePass789
cdc.database.name=PMSDB

# Table Configuration
cdc.database.include.list=PMSDB
cdc.table.include.list=PMSDB.PROJECTS

# CDC Storage Configuration
cdc.base.directory=${user.home}/rms-cdc
cdc.server.name=pms_oracle
cdc.topic.prefix=pms
```

## Migration Checklist

### Before Migration:
1. ✅ **Backup existing CDC data**: Copy `${user.home}/rms-cdc` directory
2. ✅ **Stop CDC service**: Ensure no active CDC processes are running
3. ✅ **Document current configuration**: Save current `application.properties`

### During Migration:
1. ✅ **Update database type**: Change `cdc.database.type` and `cdc.connector.class`
2. ✅ **Update connection details**: Modify hostname, port, credentials
3. ✅ **Update database name**: Set correct database/schema name
4. ✅ **Update table lists**: Adjust include lists for new database structure
5. ✅ **Update server name**: Change `cdc.server.name` to reflect new database type

### After Migration:
1. ✅ **Clear CDC storage**: Delete old offset and schema history files
2. ✅ **Start CDC service**: Begin capturing changes from new database
3. ✅ **Verify data flow**: Confirm CDC events are being received
4. ✅ **Monitor performance**: Check CDC performance with new database

## Database-Specific Notes

### MySQL:
- Uses `database.dbname` for database name
- Default connector: `io.debezium.connector.mysql.MySqlConnector`
- Supports binlog-based CDC

### PostgreSQL:
- Uses `database.dbname` for database name
- Requires `plugin.name=pgoutput` for logical decoding
- Default connector: `io.debezium.connector.postgresql.PostgresConnector`
- Requires PostgreSQL logical replication setup

### SQL Server:
- Uses `database.dbname` for database name
- Default connector: `io.debezium.connector.sqlserver.SqlServerConnector`
- Requires SQL Server CDC to be enabled on database

### Oracle:
- Uses `database.dbname` and `database.pdb.name` for pluggable databases
- Default connector: `io.debezium.connector.oracle.OracleConnector`
- Requires Oracle LogMiner or XStream setup

## Quick Migration Template

Copy this template and fill in your new database details:

```properties
# Database Type Configuration
cdc.database.type=[NEW_DATABASE_TYPE]
cdc.connector.class=[APPROPRIATE_DEBEZIUM_CONNECTOR]

# Database Connection
cdc.database.hostname=[NEW_HOSTNAME]
cdc.database.port=[NEW_PORT]
cdc.database.user=[NEW_USERNAME]
cdc.database.password=[NEW_PASSWORD]
cdc.database.name=[NEW_DATABASE_NAME]

# Table Configuration
cdc.database.include.list=[NEW_DATABASE_SCHEMA]
cdc.table.include.list=[NEW_TABLE_LIST]

# CDC Storage Configuration
cdc.base.directory=${user.home}/rms-cdc
cdc.server.name=pms_[NEW_DATABASE_TYPE]
cdc.topic.prefix=pms
```

## Supported Database Types

The CDC configuration currently supports:
- ✅ MySQL
- ✅ PostgreSQL (postgres)
- ✅ SQL Server
- ✅ Oracle

For any other database types, update the `addDatabaseSpecificConfig()` method in `DebeziumConfig.java`.
