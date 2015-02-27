Testing project for preparation a presentation on transaction in Java.


Checklist of what to do

== Standalone part
1. Start PosgreSQL server
2. Configure server to show SQL commands in log
   * log_statement = 'all' in `/var/lib/pgsql/data/postgresql.conf` 
   * logs at: `/var/lib/pgsql/data/pg_log/`
3. Showing all databases from PostgreSQL schema
   * `SELECT *  FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema = 'public' ORDER BY table_type, table_name;`
4. Start HornetQ server
   * the easiest way is to start EAP 6.4.0.GA and run `ts-presentation-standalone/src/main/resources/hornetq-jms-jboss.cli`
5. Define users in EAP server
   `echo 'guest=b5d048a237bfd2874b6928e1f37ee15e' >> $JBOSS_HOME/standalone/configuration/application-users.properties`
   `echo 'guest=guest' >> $JBOSS_HOME/standalone/configuration/application-roles.properties`

== Application server part
