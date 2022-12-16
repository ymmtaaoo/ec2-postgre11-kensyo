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
sudo mkdir /postgre/basebackup/yyyymmdd
sudo chmod 700 /postgre/basebackup/yyyymmdd
sudo chown postgres:postgres /postgre/basebackup/yyyymmdd

sudo su - postgres
pg_basebackup -D /postgre/basebackup/yyyymmdd -Ft -z -Xs -P -U postgres

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
cp - /postgre/pgdata.bak/pg_wal/ /postgre/pgdata/pg_wal/
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
~~~
#insert文の情報を出力するため
#log_statement = 'none'　→　log_statement = 'all'
~~~

ログの中身の時間の表示は正常
/postgre/pgdata/log/postgresql-20221206.log
~~~
-bash-4.2$ cat postgresql-20221206.log
[2022-12-06 16:02:17.055 JST]   25699 [1] LOG:  database system was shut down at 2022-12-06 16:02:16 JST
[2022-12-06 16:02:17.059 JST]   25694 [7] LOG:  database system is ready to accept connections

[2022-12-06 16:03:00.760 JST] postgres postgres 25967 [5] LOG:  statement: insert into item (id, name, date_data, date_timestamp_without_tz, date_timestamp_with_tz) values
        ('20221206_1', '検証データ', now(), now(), now());

~~~
登録データについても正常な時間で登録できていることを確認した。

## postgresql.confのパラメータcheckpoint_timeoutとarchive_timeoutの動作確認
### ①checkpoint_timeout = 5min, #archive_timeout = 0
5分経ってもアーカイブファイルが増えない
/postgre/wal_archive
~~~
-bash-4.2$ ls -la
total 20
drwx------ 2 postgres postgres    38 Dec  6 08:15 .
drwxr-xr-x 6 root     root        75 Dec  2 10:10 ..
-rw------- 1 postgres postgres 16440 Dec  6 08:15 000000040000000000000011　
-bash-4.2$ ls -la
total 20
drwx------ 2 postgres postgres    38 Dec  6 08:15 .
drwxr-xr-x 6 root     root        75 Dec  2 10:10 ..
-rw------- 1 postgres postgres 16440 Dec  6 08:15 000000040000000000000011
~~~

### ②checkpoint_timeout = 5min, archive_timeout = 5min
5分経ったらアーカイブファイルが増える
/postgre/wal_archive
~~~
-bash-4.2$ ls -la
total 60
drwx------ 2 postgres postgres   102 Dec  6 08:51 .
drwxr-xr-x 6 root     root        75 Dec  2 10:10 ..
-rw------- 1 postgres postgres 17864 Dec  6 08:41 000000040000000000000016
-rw------- 1 postgres postgres 17450 Dec  6 08:46 000000040000000000000017
-rw------- 1 postgres postgres 17952 Dec  6 08:51 000000040000000000000018
~~~

## バックアップ検証
### ディレクトリ状態
~~~
[ec2-user@ip-172-31-2-81 ~]$ sudo ls -la /postgre/basebackup/
total 0
drwx------ 6 postgres postgres 69 Dec  7 17:20 .
drwxr-xr-x 6 root     root     75 Dec  2 19:10 ..
drwx------ 2 postgres postgres 46 Dec  7 15:40 20221120
drwx------ 2 postgres postgres 46 Dec  7 17:15 20221127
drwx------ 2 postgres postgres 46 Dec  7 17:20 20221204
drwx------ 2 postgres postgres 46 Dec  7 17:24 current

-bash-4.2$ pwd
/postgre/wal_archive
-bash-4.2$ ls -la
total 204
drwx------ 2 postgres postgres  4096 Dec  8 10:21 .
drwxr-xr-x 6 root     root        75 Dec  7 19:04 ..
-rw------- 1 postgres postgres 17924 Dec  7 17:13 000000040000000000000020
-rw------- 1 postgres postgres 16466 Dec  7 17:13 000000040000000000000021
-rw------- 1 postgres postgres 16461 Dec  7 17:13 000000040000000000000022
-rw------- 1 postgres postgres   199 Dec  7 17:13 000000040000000000000022.00000028.backup
-rw------- 1 postgres postgres 18002 Dec  7 17:18 000000040000000000000023
-rw------- 1 postgres postgres 16392 Dec  7 17:19 000000040000000000000024
-rw------- 1 postgres postgres 16466 Dec  7 17:19 000000040000000000000025
-rw------- 1 postgres postgres   200 Dec  7 17:19 000000040000000000000025.00000028.backup
-rw------- 1 postgres postgres 25198 Dec  7 17:24 000000040000000000000026
-rw------- 1 postgres postgres 16471 Dec  7 17:24 000000040000000000000027
-rw------- 1 postgres postgres 16468 Dec  7 17:24 000000040000000000000028
-rw------- 1 postgres postgres   197 Dec  7 17:24 000000040000000000000028.00000028.backup
~~~
### ①currentのバックアップを使用してリストア作業 
【結果】直近のDB状態でリストアされました。

### ②20221127のバックアップを使用してリストア作業 
【結果】直近のDB状態でリストアされました。

## WALログ削除コマンド検証
・コマンドの第二引数の指定した.backupファイルより古いWALログが削除される。

・古い.backupファイルは削除されない。

■WALログ削除コマンド
~~~
/usr/pgsql-11/bin/pg_archivecleanup /postgre/wal_archive 000000040000000000000025.00000028.backup
~~~
■WALログ削除コマンド実行前の/postgre/wal_archiveのディレクトリ状態
~~~
-bash-4.2$ ls -la
total 228
drwx------ 4 postgres postgres  4096 Dec 12 17:30 .
drwxr-xr-x 6 root     root        75 Dec  8 10:24 ..
-rw------- 1 postgres postgres 17924 Dec 12 17:30 000000040000000000000020
-rw------- 1 postgres postgres 16466 Dec 12 17:30 000000040000000000000021
-rw------- 1 postgres postgres 16461 Dec 12 17:30 000000040000000000000022
-rw------- 1 postgres postgres   199 Dec 12 17:30 000000040000000000000022.00000028.backup
-rw------- 1 postgres postgres 18002 Dec 12 17:30 000000040000000000000023
-rw------- 1 postgres postgres 16392 Dec 12 17:30 000000040000000000000024
-rw------- 1 postgres postgres 16466 Dec 12 17:30 000000040000000000000025
-rw------- 1 postgres postgres   200 Dec 12 17:30 000000040000000000000025.00000028.backup
-rw------- 1 postgres postgres 25198 Dec 12 17:30 000000040000000000000026
-rw------- 1 postgres postgres 16471 Dec 12 17:30 000000040000000000000027
-rw------- 1 postgres postgres 16468 Dec 12 17:30 000000040000000000000028
-rw------- 1 postgres postgres   197 Dec 12 17:30 000000040000000000000028.00000028.backup
-rw------- 1 postgres postgres 16487 Dec 12 09:10 00000007000000000000002A
drwx------ 2 postgres postgres     6 Dec  8 10:39 20221127
drwx------ 2 postgres postgres  4096 Dec 12 17:27 bk
~~~
■WALログ削除コマンド実行後の/postgre/wal_archiveのディレクトリ状態
~~~
-bash-4.2$ ls -la
total 124
drwx------ 4 postgres postgres   336 Dec 12 18:41 .
drwxr-xr-x 6 root     root        75 Dec  8 10:24 ..
-rw------- 1 postgres postgres   199 Dec 12 17:30 000000040000000000000022.00000028.backup
-rw------- 1 postgres postgres 16466 Dec 12 17:30 000000040000000000000025
-rw------- 1 postgres postgres   200 Dec 12 17:30 000000040000000000000025.00000028.backup
-rw------- 1 postgres postgres 25198 Dec 12 17:30 000000040000000000000026
-rw------- 1 postgres postgres 16471 Dec 12 17:30 000000040000000000000027
-rw------- 1 postgres postgres 16468 Dec 12 17:30 000000040000000000000028
-rw------- 1 postgres postgres   197 Dec 12 17:30 000000040000000000000028.00000028.backup
-rw------- 1 postgres postgres 16487 Dec 12 09:10 00000007000000000000002A
drwx------ 2 postgres postgres     6 Dec  8 10:39 20221127
drwx------ 2 postgres postgres  4096 Dec 12 17:27 bk
~~~

## opensslでパスワード暗号化検証
https://colabmix.co.jp/tech-blog/password_encrypt_shell/
https://www.tohoho-web.com/ex/openssl.html#encrypt
https://kaworu.jpn.org/security/OpenSSL%E3%82%B3%E3%83%9E%E3%83%B3%E3%83%89%E3%82%92%E7%94%A8%E3%81%84%E3%81%9F%E5%85%B1%E9%80%9A%E9%8D%B5%E6%9A%97%E5%8F%B7
### 共通鍵・暗号化済パスワードファイル作成手順
~~~
#PostgreSQLのパスワードの共通鍵を作成
nano /home/postgres/decrypt_db_key
123456789012345678901234567890ab
#PostgreSQLのパスワード平文を共通鍵を使って暗号化し、暗号化済パスワードファイルに出力する。
echo "｛postgreSQLのパスワード平文｝" | openssl enc -e -aes-256-cbc -kfile /home/postgres/decrypt_db_key -out /home/postgres/encrypted_db_password
#共通鍵ファイルと暗号化済パスワードファイルの権限を修正
chmod 600 /home/postgres/decrypt_db_key /home/postgres/encrypted_db_password
chown postgres:postgres /home/postgres/decrypt_db_key /home/postgres/encrypted_db_password

~~~
### 暗号・復号確認
~~~
-bash-4.2$ echo "｛postgreSQLのパスワード平文｝" | openssl enc -e -aes-256-cbc -kfile /home/postgres/decrypt_db_key -out /home/postgres/encrypted_db_password
-bash-4.2$ openssl enc -d -aes-256-cbc -kfile /home/postgres/decrypt_db_key -in /home/postgres/encrypted_db_password
｛postgreSQLのパスワード平文｝
~~~

## WEBログの再起動要否」の検証

logback動作確認用のアプリ
別ポートで起動して同じファイル（1階層上）にログ出力します。
https://github.com/namickey/postgresql/tree/main/web_log_1
https://github.com/namickey/postgresql/tree/main/web_log_2
![image](https://user-images.githubusercontent.com/52730146/208034647-b9cf66db-b75f-424e-88f4-91f1314e5711.png)


