# Migration from sqlite to postgresql

Starting from the version 2.3.0 ma1sd support postgresql for internal storage in addition to sqlite (parameters `storage.backend`).

#### Migration steps

1. create the postgresql database and user for ma1sd storage
2. create a backup for sqlite storage (default location: /var/lib/ma1sd/store.db)
3. migrate data from sqlite to postgresql
4. change ma1sd configuration to use the postgresql

For data migration is it possible to use https://pgloader.io tool.

Example of the migration command:
```shell script
pgloader --with "quote identifiers" /path/to/store.db pgsql://ma1sd_user:ma1sd_password@host:port/database
```
or (short version for database on localhost)
```shell script
pgloader --with "quote identifiers" /path/to/store.db pgsql://ma1sd_user:ma1sd_password@localhost/ma1sd
```

An option `--with "quote identifies"` used to create case sensitive tables.
ma1sd_user - postgresql user for ma1sd.
ma1sd_password - password of the postgresql user.
host - postgresql host
post - database port (default 5432)
database - database name.


Configuration example for postgresql storage:
```yaml
storage:
  backend: postgresql
  provider:
    postgresql:
      database: '//localhost/ma1sd' # or full variant //192.168.1.100:5432/ma1sd_database
      username: 'ma1sd_user'
      password: 'ma1sd_password'
```

