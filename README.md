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
### mavenでspringアプリをjarファイル化する
https://stronger.hatenablog.com/entry/2016/11/19/003404
### ec2にjavaをインストール
~~~
sudo yum install java-11-amazon-corretto-headless
~~~
### jarを実行
~~~
java -jar /home/ec2-user/web-0.0.1-SNAPSHOT.jar
~~~
### 画面URL
http://52.68.155.58:8080/hello
### アプリ停止する
ctrl+c
