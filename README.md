On transactions in Java


Checklist
=========
...what to do before running tests


Configuration
--------------
Configuration is under `ts-presentation-utils/src/main/resources/resource.properties`

Standalone part
---------------
1. Start PosgreSQL server
2. Configure server to show SQL commands in log (Fedora 20)
   * log_statement = 'all' in `/var/lib/pgsql/data/postgresql.conf` 
   * logs could be found at: `/var/lib/pgsql/data/pg_log/`
3. Showing all databases from PostgreSQL schema
   * `SELECT *  FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema = 'public' ORDER BY table_type, table_name;`
4. WildFly used as message broker - download WildFly (http://wildfly.org/downloads)
5. export JBOSS_HOME=path/to/unzipped/widfly/distro
6. Define 'guest' user in WildFly server
   * `echo 'guest=b5d048a237bfd2874b6928e1f37ee15e' >> $JBOSS_HOME/standalone/configuration/application-users.properties`
   * `echo 'guest=guest' >> $JBOSS_HOME/standalone/configuration/application-roles.properties`
5. Start WildFly as HornetQ server and add testing queue
   * $JBOSS_HOME/bin/standalone.sh -c standalone-full.xml
   * $JBOSS_HOME/bin/jboss-cli.sh -c --file=./ts-presentation/ts-presentation-utils/src/main/resources/hornetq-jms-jboss.cli


Application server part
-----------------------
 1. Set JBOSS_HOME to WildFly 8.2.0 and Arquillian should do the setting on its own


Do not forget
--------------
 * presentation template to Eclipse
 * take a notebook with a pencil to make a note of some question from audience
 * open JTA spec in browser

Reveal.js keyboard shortcuts
----------------------------
 * `s` - show window with notes (chrome does have trouble with it?)
 * `o` - show slides one by one (as when AlT-TAB is used for windows), back to slides by touching `o` once again
 * `b` - pause - screen will go dark
 * `f` - full screen (escape by ESC)
 * `END/HOME` - first slide/last slide

