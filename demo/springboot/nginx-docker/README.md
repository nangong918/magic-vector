# nginx-docker（Docker 专用 Nginx）

本目录只在 **Docker Compose** 中使用，对应服务名为 `nginx`，镜像 `alfg/nginx-rtmp:latest`。与仓库根目录下的 `nginx-rtmp-win32-dev`（Windows 本机 RTMP）相互独立。

## 目录说明

| 路径 | 作用 |
|------|------|
| `conf/nginx.conf` | 容器内主配置（挂载到 `/etc/nginx/nginx.conf`） |
| `conf/mime.types` | MIME（挂载到 `/etc/nginx/mime.types`） |
| `html/` | 流媒体测试页与 `stat.xsl`（挂载到 `/etc/nginx/html`），由 `nginx-rtmp-win32-dev/html` 拷贝而来 |

## 端口（宿主机映射）

由 `docker/docker-compose.yml` 中 `nginx` 服务定义，一般为：

- **1935**：RTMP 推流
- **8080**：HTTP —— 流状态与测试页（见下）
- **80**：HTTP —— MinIO 反向代理入口

## 8080 上的页面（联调）

在容器启动且端口映射无误时，本机浏览器可访问：

- `http://localhost:8080/stat` — RTMP 状态（XML）
- `http://localhost:8080/index.html` — 直播推流/播放测试页（Flash）
- `http://localhost:8080/vod.html` — RTMP / HLS 点播测试页（Flash）

HLS 切片目录在容器内为 `/tmp/hls`，Compose 使用命名卷 `nginx-hls` 挂载；HTTP 路径为 `http://localhost:8080/hls/...`。

## 修改配置后

在项目根目录执行：

```bash
docker compose -f docker/docker-compose.yml -p dockerdemo up -d nginx
```

无需改应用镜像；若改了 `html/` 静态文件，重启 `nginx` 容器即可生效。
