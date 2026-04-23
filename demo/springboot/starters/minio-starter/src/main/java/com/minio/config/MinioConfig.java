package com.minio.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 *@author 13225
 *@date 2025/7/21 11:30
 */
@Setter
@Getter
@Slf4j
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    /**
     * minio的endpoint (网段)
     */
    private String endpoint;
    /**
     * minio的accessKey
     */
    private String accessKey;
    /**
     * minio的secretKey
     */
    private String secretKey;

    /**
     * 是否使用 Spring Cloud Gateway 等网关代理（预签名 URL 会带 {@link #minioUrl} 前缀，如 /oss-minio）。
     * 与 {@link #useNginxProxy} 请勿同时为 true；若均为 true，则按 nginx 逻辑生效。
     */
    private boolean useGatewayProxy;

    /**
     * 是否使用 Nginx 反代 MinIO（外链 host 使用本机局域网 IP）。
     * 与 {@link #useGatewayProxy} 请勿同时为 true；若均为 true，则按本项（nginx）生效。
     */
    private boolean useNginxProxy;

    /**
     * Nginx 对外端口；默认 80。
     * 80/443 会在外链中省略端口显示（如 http://192.168.1.2/...）。
     */
    private Integer nginxPublicPort = 80;
    @Value("${MINIO_NGINX_PUBLIC_HOST:#{null}}")
    private String configuredPublicHost;

    /**
     * 仅 Spring Cloud Gateway：对外入口端口（与 {@link #minioUrl} 组成 {@code 本机IP:gatewayPort/oss-minio}）。
     */
    private String gatewayPort;

    /**
     * 仅 Spring Cloud Gateway 场景：拼在 host:port 后的路径前缀，如 /oss-minio，供网关路由过滤。
     * Nginx 直连反代时一般不需要，可留空；{@link #useNginxProxy} 为 true 时不会拼入外链。
     */
    private String minioUrl;

    @Bean
    public MinioClient minioClient() {
        // 创建 MinioClient 客户端
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    private String resolveAgentHostPort() throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        return inetAddress.getHostAddress() + ":" + gatewayPort;
    }

    /**
     * Nginx 外链 host[:port]：本机局域网 IP + 对外端口。
     * 端口为 80/443 时省略端口显示，以隐藏内部 9000。
     */
    public String minioNginxAgentUrl() throws Exception {
        String host = configuredPublicHost;
        if (host == null || host.isEmpty()) {
            host = InetAddress.getLocalHost().getHostAddress();
        }
        int port = nginxPublicPort == null ? 80 : nginxPublicPort;
        if (port == 80 || port == 443) {
            return host;
        }
        return host + ":" + port;
    }

    /**
     * Spring Cloud Gateway 外链前缀：{@code 本机IP:端口} + {@link #minioUrl}。
     */
    @Bean
    public String minioGatewayAgentUrl() throws Exception {
        String suffix = minioUrl != null ? minioUrl : "";
        return resolveAgentHostPort() + suffix;
    }

    @Bean
    public String globalOssBucket(){
        return "global-oss";
    }

    @Override
    public String toString() {
        return "MinIOConfig{" +
                "endpoint='" + endpoint + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", useGatewayProxy=" + useGatewayProxy +
                ", useNginxProxy=" + useNginxProxy +
                ", nginxPublicPort=" + nginxPublicPort +
                ", gatewayPort='" + gatewayPort + '\'' +
                ", minioUrl='" + minioUrl + '\'' +
                '}';
    }
}
