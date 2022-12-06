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
###①log_filename='postgresql-%a.log' (デフォルト)
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

###②log_filename='postgresql-%Y%m%d.log'


## postgresql.confのパラメータlog_timezoneとtimezone検証
###①log_timezone = 'UTC', timezone = 'UTC' (デフォルト)
ファイルの更新時間がずれている
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

~~~
-bash-4.2$ cat postgresql-Tue.log
2022-12-06 01:32:59.495 UTC [2959] LOG:  database system was shut down at 2022-12-02 11:51:11 UTC
2022-12-06 01:32:59.517 UTC [2939] LOG:  database system is ready to accept connections
-bash-4.2$
~~~

###②log_timezone = 'Asia/Tokyo', timezone = 'UTC' 

###③log_timezone = 'UTC', timezone = 'Asia/Tokyo' 

###④log_timezone = 'Asia/Tokyo', timezone = 'Asia/Tokyo' 
