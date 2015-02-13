Testing project for preparation a presentation on transaction in Java.


Checklist of what to do

1. Start PosgreSQL server
2. Configure server to show SQL commands in log
   * log_statement = 'all' in `/var/lib/pgsql/data/postgresql.conf` 
   * logs at: `/var/lib/pgsql/data/pg_log/`
3. Showing all databases from PostgreSQL schema
   * `SELECT *  FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema = 'public' ORDER BY table_type, table_name;`