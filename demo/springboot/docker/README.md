# SpringBoot Docker Run Guide

please set `MINIO_NGINX_PUBLIC_HOST` first!

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

## Check Database Availability

```bash
docker exec springboot-mysql mysql -uroot -p123456 -e "SHOW DATABASES; USE vector_demo; SHOW TABLES;"
```
