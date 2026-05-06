# SpringBoot Docker Run Guide

please set `MINIO_NGINX_PUBLIC_HOST` first!

## Change Docker Engine source

```json
{
  "builder": {
    "gc": {
      "defaultKeepStorage": "20GB",
      "enabled": true
    }
  },
  "debug": true,
  "experimental": false,
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.m.daocloud.io",
    "https://lispy.org",
    "https://docker-0.unsee.tech",
    "https://docker.xuanyuan.me"
  ]
}
```

## Start Services

Run from `demo/springboot`:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

## Stop Services

```bash
docker compose -f docker/docker-compose.yml down
```

## Restart Services

```bash
docker compose -f docker/docker-compose.yml down
docker compose -f docker/docker-compose.yml up -d --build
```

## Check Service Status

```bash
docker compose -f docker/docker-compose.yml ps
```

## RTSP Service (MediaMTX)

The compose file now includes a dedicated RTSP server:

- service: `mediamtx`
- host port: `8554`
- example publish URL: `rtsp://<HOST_IP>:8554/live/stream`

Start or restart with:

```bash
docker compose -f docker/docker-compose.yml up -d mediamtx
```

Quick check:

```bash
docker compose -f docker/docker-compose.yml ps mediamtx
```

Two-device verification flow:

1. Device A opens `RTSP File Push Demo` and selects a local mp4, then pushes to `rtsp://<HOST_IP>:8554/live/stream`.
2. Device B opens `Live Pull Demo`, fills `rtsp://<HOST_IP>:8554/live/stream`, and starts playback.

## Check Database Availability

```bash
docker exec springboot-mysql mysql -uroot -p123456 -e "SHOW DATABASES; USE vector_demo; SHOW TABLES;"
```

## Mysql

#### 1. List running Docker containers
```shell
docker ps
```


```log
CONTAINER ID   IMAGE                    COMMAND                  CREATED        STATUS                   PORTS                                                                                                                           NAMES
dad91dc558c6   alfg/nginx-rtmp:latest   "nginx -c /etc/nginx/nginx.conf"   42 hours ago   Up 2 minutes             0.0.0.0:80->80/tcp, [::]:80->80/tcp, 0.0.0.0:1935->1935/tcp, [::]:1935->1935/tcp, 0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp   springboot-nginx
32ecd34a45e1   docker-springboot        "java -jar /app/app.jar"   42 hours ago   Up 2 minutes             0.0.0.0:48888->48888/tcp, [::]:48888->48888/tcp                                                                                 springboot-app
dd965420dcca   mysql:8.0.42             "docker-entrypoint.sh mysqld"   42 hours ago   Up 2 minutes (healthy)   3306/tcp, 33060/tcp                                                                                                             springboot-mysql
f792a8ba3008   minio/minio:latest       "/usr/bin/docker-entrypoint.sh minio server /data --console-address ':9001'"   42 hours ago   Up 2 minutes             0.0.0.0:9000-9001->9000-9001/tcp, [::]:9000-9001->9000-9001/tcp                                                                 springboot-minio
```

#### 2. Access the MySQL container

```shell
docker exec -it dd965420dcca bash
```

#### 3. Login to MySQL

```shell
mysql -u root -p
```

```log
Enter password: 123456
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 97
Server version: 8.0.42 MySQL Community Server - GPL

Copyright (c) 2000, 2025, Oracle and/or its affiliates.

Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
Other names may be trademarks of their respective owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.
```

#### 4. Show all databases

```shell
SHOW DATABASES;
```


```log
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| sys                |
| vector_demo        |
+--------------------+
5 rows in set (0.02 sec)
```


#### 5. Use the target database

```shell
USE vector_demo;

SHOW TABLES;
```


```log
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

Database changed


+-----------------------+
| Tables_in_vector_demo |
+-----------------------+
| agent                 |
| agent_log             |
| chat_message          |
| oss                   |
| user                  |
| video_record          |
+-----------------------+
6 rows in set (0.02 sec)
```

#### 7. Query data from tables

```shell
SELECT * FROM user;
SELECT * FROM chat_message;
SELECT * FROM agent;
SELECT * FROM oss;
SELECT * FROM agent_log;
SELECT * FROM video_record;
```

```log

mysql> SELECT * FROM user;
+---------------------+------+---------+----------+--------+
| id                  | name | account | password | oss_id |
+---------------------+------+---------+----------+--------+
| 2047218072616157184 | demo | demo    | 123456   |   NULL |
+---------------------+------+---------+----------+--------+
1 row in set (0.01 sec)

mysql> SELECT * FROM chat_message;
Empty set (0.01 sec)

mysql> SELECT * FROM agent;
Empty set (0.00 sec)

mysql> SELECT * FROM oss;
+---------------------+---------------------+-------------+---------------------------------------------------------------+------------------+--------------+-
----------+------------------------------------------------------------------+---------------+---------------+
| id                  | user_id             | bucket_name | object_name                                                   | origin_file_name | content_type |
file_size | idempotent_key                                                   | created_at    | updated_at    |
+---------------------+---------------------+-------------+---------------------------------------------------------------+------------------+--------------+-
----------+------------------------------------------------------------------+---------------+---------------+
| 2047218457443549185 | 2047218072616157184 | global-oss  | 2047218072616157184/1776929889833_2047218455103127552_222.png | 222.png          | image/png    |
  1253043 | 823cad53b693136768582a86fc68ff4668e8dce73ebc08cc4db918a25a18ed38 | 1776929890391 | 1776929890391 |
+---------------------+---------------------+-------------+---------------------------------------------------------------+------------------+--------------+-
----------+------------------------------------------------------------------+---------------+---------------+
1 row in set (0.00 sec)

mysql> SELECT * FROM agent_log;
Empty set (0.00 sec)

mysql> SELECT * FROM video_record;

```
