# ec2-postgre11-kensyo

## ec2上にある鍵ファイルをwinscpで自環境に持ってくるためファイルをコピーして権限を777にする
~~~
sudo cp /postgre/pgdata/server.key /home/ec2-user
sudo cp /postgre/pgdata/server.crt /home/ec2-user
sudo chmod 777 /home/ec2-user/server.key
sudo chmod 777 /home/ec2-user/server.crt
~~~
## ファイルをteraterm上で編集するコマンド
~~~
sudo nano /postgre/pgdata/postgresql.conf
sudo nano /postgre/pgdata/pg_hba.conf
~~~
## postgresql11にアクセスするコマンド
~~~
psql -h 172.31.2.81 -U postgres
~~~
## ec2上でwebを起動させる方法
https://qiita.com/NoOne/items/76be0d03560f0b77e91a
### mavenでspringアプリをjarファイル化する①
https://stronger.hatenablog.com/entry/2016/11/19/003404
### ec2にjavaをインストール（最初だけ）
~~~
sudo yum install java-11-amazon-corretto-headless
~~~
### WinSCPでjarをec2上に配置する②
### jarを実行③
~~~
java -jar /home/ec2-user/web-0.0.1-SNAPSHOT.jar
~~~

### 画面URL④
http://パブリック IPv4 アドレス:8080/hello
### アプリ停止する⑤
ctrl+c

## wal archive log
~~~
#wal_archiveディレクトリを作成
sudo mkdir /postgre/wal_archive

#権限を変更
sudo chmod 700 /postgre/wal_archive

#wal_archiveディレクトリの管理者を変更
sudo chown postgres:postgres /postgre/wal_archive
~~~

## backup
~~~
#backupディレクトリを作成
sudo mkdir /postgre/basebackup
sudo chmod 700 /postgre/basebackup
sudo chown postgres:postgres /postgre/basebackup

sudo su - postgres
pg_basebackup -D /postgre/basebackup/ -Ft -z -Xs -P -U postgres
~~~

## restore
１．停止
~~~
sudo systemctl stop postgresql-11
~~~
２．PGDATA（最新WALログ）の退避
~~~
sudo mv /postgre/pgdata/ /postgre/pgdata.bak/
sudo mkdir /postgre/pgdata
sudo chmod 700 /postgre/pgdata
sudo chown postgres:postgres /postgre/pgdata
~~~
３．ベースバックアップの復旧
~~~
tar xzfv /postgre/basebackup/base.tar.gz -C /postgre/pgdata
~~~
４．古いWALログの削除
~~~
rm -rf /postgre/pgdata/pg_wal
~~~
５．最新WALログの復旧
~~~
cp -p /postgre/pgdata.bak/pg_wal/ /postgre/pgdata/pg_wal/
~~~
６．リカバリ設定 vi /postgre/pgdata/recovery.conf
~~~
restore_command = 'gunzip < /postgre/wal_archive/%f > %p'
#restore_command = 'cp /postgre/wal_archive/%f "%p"'
~~~
７．起動
~~~
sudo systemctl start postgresql-11

~~~

## postgresql.confのパラメータlog_filename検証
### ①log_filename='postgresql-%a.log' (デフォルト)
~~~
-bash-4.2$ pwd
/postgre/pgdata/log
-bash-4.2$ ls -la
total 32
drwx------  2 postgres postgres  136 Dec  6 01:32 .
drwx------ 20 postgres postgres 4096 Dec  6 01:32 ..
-rw-------  1 postgres postgres 4368 Dec  2 11:51 postgresql-Fri.log
-rw-------  1 postgres postgres 3800 Nov 14 09:38 postgresql-Mon.log
-rw-------  1 postgres postgres 2522 Nov 10 08:45 postgresql-Thu.log
-rw-------  1 postgres postgres  186 Dec  6 01:32 postgresql-Tue.log
-rw-------  1 postgres postgres 5661 Nov  9 08:58 postgresql-Wed.log
-bash-4.2$
~~~

### ②log_filename='postgresql-%Y%m%d.log'
~~~
-bash-4.2$ pwd
/postgre/pgdata/log
-bash-4.2$ ls -la
total 36
drwx------  2 postgres postgres  167 Dec  6 02:45 .
drwx------ 20 postgres postgres 4096 Dec  6 02:45 ..
-rw-------  1 postgres postgres  188 Dec  6 02:45 postgresql-20221206.log 
-rw-------  1 postgres postgres 4368 Dec  2 11:51 postgresql-Fri.log
-rw-------  1 postgres postgres 3800 Nov 14 09:38 postgresql-Mon.log
-rw-------  1 postgres postgres 2522 Nov 10 08:45 postgresql-Thu.log
-rw-------  1 postgres postgres  774 Dec  6 02:45 postgresql-Tue.log
-rw-------  1 postgres postgres 5661 Nov  9 08:58 postgresql-Wed.log
-bash-4.2$
~~~

## postgresql.confのパラメータlog_line_prefix検証
### ①log_line_prefix = '%m [%p] '　デフォルト
cat postgresql-20221206.logコマンドで確認
~~~
2022-12-06 12:02:51.227 JST [3697] LOG:  database system was shut down at 2022-12-06 12:02:51 JST
2022-12-06 12:02:51.231 JST [3693] LOG:  database system is ready to accept connections
~~~
### ②log_line_prefix = '[%t] %u %d %p [%l] '
cat postgresql-20221206.logコマンドで確認
~~~
[2022-12-06 15:39:32 JST]   17370 [1] LOG:  database system was shut down at 2022-12-06 15:39:32 JST
[2022-12-06 15:39:32 JST]   17366 [7] LOG:  database system is ready to accept connections
~~~
### ③log_line_prefix = '[%m] %u %d %p [%l] '
cat postgresql-20221206.logコマンドで確認
ミリ秒が表示された
~~~
[2022-12-06 15:42:43.319 JST]   18603 [1] LOG:  database system was shut down at 2022-12-06 15:42:43 JST
[2022-12-06 15:42:43.323 JST]   18594 [7] LOG:  database system is ready to accept connections
~~~

## postgresql.confのパラメータlog_timezoneとtimezone検証
~~~
#insert文の情報を出力するため
#log_statement = 'none'　→　log_statement = 'all'
~~~
### ①log_timezone = 'UTC', timezone = 'UTC' (デフォルト)
ログの時間の表示が異常
~~~
-bash-4.2$ pwd
/postgre/pgdata/log

-bash-4.2$ cat postgresql-Tue.log
2022-12-06 01:32:59.495 UTC [2959] LOG:  database system was shut down at 2022-12-02 11:51:11 UTC
2022-12-06 01:32:59.517 UTC [2939] LOG:  database system is ready to accept connections
-bash-4.2$
~~~


### ②log_timezone = 'Asia/Tokyo', timezone = 'Asia/Tokyo' 
ログファイル作成時間が異常だが、ログの中身の時間の表示は正常
/postgre/pgdata/log/postgresql-20221206.log

<details>
＜summary＞-bash-4.2$ cat postgresql-20221206.log＜/summary＞
[2022-12-06 16:02:17.055 JST]   25699 [1] LOG:  database system was shut down at 2022-12-06 16:02:16 JST
[2022-12-06 16:02:17.059 JST]   25694 [7] LOG:  database system is ready to accept connections
[2022-12-06 16:02:50.814 JST] postgres postgres 25906 [1] LOG:  statement: SET DateStyle=ISO; SET client_min_messages=notice; SELECT set_config('bytea_output','hex',false) FROM pg_settings WHERE name = 'bytea_output'; SET client_encoding='UNICODE';
[2022-12-06 16:02:50.823 JST] postgres postgres 25906 [2] LOG:  statement: SELECT version()
[2022-12-06 16:02:50.831 JST] postgres postgres 25906 [3] LOG:  statement:
        SELECT
            db.oid as did, db.datname, db.datallowconn,
            pg_encoding_to_char(db.encoding) AS serverencoding,
            has_database_privilege(db.oid, 'CREATE') as cancreate,
            datistemplate
        FROM
            pg_catalog.pg_database db
        WHERE db.datname = current_database()
[2022-12-06 16:02:50.842 JST] postgres postgres 25906 [4] LOG:  statement:
                SELECT
                    roles.oid as id, roles.rolname as name,
                    roles.rolsuper as is_superuser,
                    CASE WHEN roles.rolsuper THEN true ELSE roles.rolcreaterole END as
                    can_create_role,
                    CASE WHEN roles.rolsuper THEN true
                    ELSE roles.rolcreatedb END as can_create_db,
                    CASE WHEN 'pg_signal_backend'=ANY(ARRAY(WITH RECURSIVE cte AS (
                    SELECT pg_roles.oid,pg_roles.rolname FROM pg_roles
                        WHERE pg_roles.oid = roles.oid
                    UNION ALL
                    SELECT m.roleid,pgr.rolname FROM cte cte_1
                        JOIN pg_auth_members m ON m.member = cte_1.oid
                        JOIN pg_roles pgr ON pgr.oid = m.roleid)
                    SELECT rolname  FROM cte)) THEN True
                    ELSE False END as can_signal_backend
                FROM
                    pg_catalog.pg_roles as roles
                WHERE
                    rolname = current_user
[2022-12-06 16:02:50.853 JST] postgres postgres 25906 [5] LOG:  statement: BEGIN;
[2022-12-06 16:02:50.863 JST] postgres postgres 25906 [6] LOG:  statement: DELETE FROM public.item
            WHERE id IN
                ('20221206_1');
[2022-12-06 16:02:50.872 JST] postgres postgres 25906 [7] LOG:  statement: COMMIT;
[2022-12-06 16:03:00.714 JST] postgres postgres 25967 [1] LOG:  statement: SET DateStyle=ISO; SET client_min_messages=notice; SELECT set_config('bytea_output','hex',false) FROM pg_settings WHERE name = 'bytea_output'; SET client_encoding='UNICODE';
[2022-12-06 16:03:00.725 JST] postgres postgres 25967 [2] LOG:  statement: SELECT version()
[2022-12-06 16:03:00.732 JST] postgres postgres 25967 [3] LOG:  statement:
        SELECT
            db.oid as did, db.datname, db.datallowconn,
            pg_encoding_to_char(db.encoding) AS serverencoding,
            has_database_privilege(db.oid, 'CREATE') as cancreate,
            datistemplate
        FROM
            pg_catalog.pg_database db
        WHERE db.datname = current_database()
[2022-12-06 16:03:00.750 JST] postgres postgres 25967 [4] LOG:  statement:
                SELECT
                    roles.oid as id, roles.rolname as name,
                    roles.rolsuper as is_superuser,
                    CASE WHEN roles.rolsuper THEN true ELSE roles.rolcreaterole END as
                    can_create_role,
                    CASE WHEN roles.rolsuper THEN true
                    ELSE roles.rolcreatedb END as can_create_db,
                    CASE WHEN 'pg_signal_backend'=ANY(ARRAY(WITH RECURSIVE cte AS (
                    SELECT pg_roles.oid,pg_roles.rolname FROM pg_roles
                        WHERE pg_roles.oid = roles.oid
                    UNION ALL
                    SELECT m.roleid,pgr.rolname FROM cte cte_1
                        JOIN pg_auth_members m ON m.member = cte_1.oid
                        JOIN pg_roles pgr ON pgr.oid = m.roleid)
                    SELECT rolname  FROM cte)) THEN True
                    ELSE False END as can_signal_backend
                FROM
                    pg_catalog.pg_roles as roles
                WHERE
                    rolname = current_user
[2022-12-06 16:03:00.760 JST] postgres postgres 25967 [5] LOG:  statement: insert into item (id, name, date_data, date_timestamp_without_tz, date_timestamp_with_tz) values
        ('20221206_1', '検証データ', now(), now(), now());
[2022-12-06 16:03:04.193 JST] postgres postgres 25992 [1] LOG:  statement: SET DateStyle=ISO; SET client_min_messages=notice; SELECT set_config('bytea_output','hex',false) FROM pg_settings WHERE name = 'bytea_output'; SET client_encoding='UNICODE';
[2022-12-06 16:03:04.204 JST] postgres postgres 25992 [2] LOG:  statement: SELECT version()
[2022-12-06 16:03:04.216 JST] postgres postgres 25992 [3] LOG:  statement:
        SELECT
            db.oid as did, db.datname, db.datallowconn,
            pg_encoding_to_char(db.encoding) AS serverencoding,
            has_database_privilege(db.oid, 'CREATE') as cancreate,
            datistemplate
        FROM
            pg_catalog.pg_database db
        WHERE db.datname = current_database()
[2022-12-06 16:03:04.226 JST] postgres postgres 25992 [4] LOG:  statement:
                SELECT
                    roles.oid as id, roles.rolname as name,
                    roles.rolsuper as is_superuser,
                    CASE WHEN roles.rolsuper THEN true ELSE roles.rolcreaterole END as
                    can_create_role,
                    CASE WHEN roles.rolsuper THEN true
                    ELSE roles.rolcreatedb END as can_create_db,
                    CASE WHEN 'pg_signal_backend'=ANY(ARRAY(WITH RECURSIVE cte AS (
                    SELECT pg_roles.oid,pg_roles.rolname FROM pg_roles
                        WHERE pg_roles.oid = roles.oid
                    UNION ALL
                    SELECT m.roleid,pgr.rolname FROM cte cte_1
                        JOIN pg_auth_members m ON m.member = cte_1.oid
                        JOIN pg_roles pgr ON pgr.oid = m.roleid)
                    SELECT rolname  FROM cte)) THEN True
                    ELSE False END as can_signal_backend
                FROM
                    pg_catalog.pg_roles as roles
                WHERE
                    rolname = current_user
[2022-12-06 16:03:04.241 JST] postgres postgres 25992 [5] LOG:  statement: SELECT at.attname, at.attnum, ty.typname
        FROM pg_catalog.pg_attribute at LEFT JOIN pg_catalog.pg_type ty ON (ty.oid = at.atttypid)
        WHERE attrelid=16385::oid AND attnum = ANY (
            (SELECT con.conkey FROM pg_catalog.pg_class rel LEFT OUTER JOIN pg_catalog.pg_constraint con ON con.conrelid=rel.oid
            AND con.contype='p' WHERE rel.relkind IN ('r','s','t', 'p') AND rel.oid = 16385::oid)::oid[])

[2022-12-06 16:03:04.258 JST] postgres postgres 25992 [6] LOG:  statement: SELECT rel.relhasoids AS has_oids
        FROM pg_catalog.pg_class rel
        WHERE rel.oid = 16385::oid

[2022-12-06 16:03:04.274 JST] postgres postgres 25992 [7] LOG:  statement: SELECT at.attname, at.attnum, ty.typname
        FROM pg_catalog.pg_attribute at LEFT JOIN pg_catalog.pg_type ty ON (ty.oid = at.atttypid)
        WHERE attrelid=16385::oid AND attnum = ANY (
            (SELECT con.conkey FROM pg_catalog.pg_class rel LEFT OUTER JOIN pg_catalog.pg_constraint con ON con.conrelid=rel.oid
            AND con.contype='p' WHERE rel.relkind IN ('r','s','t', 'p') AND rel.oid = 16385::oid)::oid[])

[2022-12-06 16:03:04.290 JST] postgres postgres 25992 [8] LOG:  statement: SELECT rel.relhasoids AS has_oids
        FROM pg_catalog.pg_class rel
        WHERE rel.oid = 16385::oid

[2022-12-06 16:03:04.301 JST] postgres postgres 25906 [8] LOG:  statement: SELECT * FROM public.item
        ORDER BY id ASC
[2022-12-06 16:03:04.392 JST] postgres postgres 25906 [9] LOG:  statement: SELECT rel.relhasoids AS has_oids
        FROM pg_catalog.pg_class rel
        WHERE rel.oid = 16385::oid

[2022-12-06 16:03:04.454 JST] postgres postgres 25906 [10] LOG:  statement: SELECT DISTINCT att.attname as name, att.attnum as OID, pg_catalog.format_type(ty.oid,NULL) AS datatype,
        att.attnotnull as not_null, att.atthasdef as has_default_val
        FROM pg_catalog.pg_attribute att
            JOIN pg_catalog.pg_type ty ON ty.oid=atttypid
            JOIN pg_catalog.pg_namespace tn ON tn.oid=ty.typnamespace
            JOIN pg_catalog.pg_class cl ON cl.oid=att.attrelid
            JOIN pg_catalog.pg_namespace na ON na.oid=cl.relnamespace
            LEFT OUTER JOIN pg_catalog.pg_type et ON et.oid=ty.typelem
            LEFT OUTER JOIN pg_catalog.pg_attrdef def ON adrelid=att.attrelid AND adnum=att.attnum
            LEFT OUTER JOIN (pg_catalog.pg_depend JOIN pg_catalog.pg_class cs ON classid='pg_class'::regclass AND objid=cs.oid AND cs.relkind='S') ON refobjid=att.attrelid AND refobjsubid=att.attnum
            LEFT OUTER JOIN pg_catalog.pg_namespace ns ON ns.oid=cs.relnamespace
            LEFT OUTER JOIN pg_catalog.pg_index pi ON pi.indrelid=att.attrelid AND indisprimary
        WHERE
            att.attrelid = 16385::oid
            AND att.attnum > 0
            AND att.attisdropped IS FALSE
        ORDER BY att.attnum
[2022-12-06 16:03:04.471 JST] postgres postgres 25906 [11] LOG:  statement: SELECT oid, pg_catalog.format_type(oid, NULL) AS typname FROM pg_catalog.pg_type WHERE oid IN (1043, 1043, 1082, 1114, 1184) ORDER BY oid;
-bash-4.2$
<details>



