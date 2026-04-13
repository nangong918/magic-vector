package com.minio.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

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
     * 是否使用 Nginx 反代 MinIO：预签名 URL 仅把 {@link #endpoint} 里的 <b>host</b> 换成本机局域网 IP，
     * <b>端口与 endpoint 中一致</b>（不改用 {@link #gatewayPort}），且不带 {@link #minioUrl}。
     * 与 {@link #useGatewayProxy} 请勿同时为 true；若均为 true，则按本项（nginx）生效。
     */
    private boolean useNginxProxy;

    /**
     * 仅 Spring Cloud Gateway：对外入口端口（与 {@link #minioUrl} 组成 {@code 本机IP:gatewayPort/oss-minio}）。
     * Nginx 仅换 IP 模式不读此项。
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
     * 解析 {@link #endpoint} 中的端口（未写端口时 http 为 80、https 为 443）。
     */
    private int parseEndpointPort() {
        try {
            URI uri = URI.create(endpoint.trim());
            int port = uri.getPort();
            if (port > 0) {
                return port;
            }
            String scheme = uri.getScheme();
            if (scheme != null && scheme.equalsIgnoreCase("https")) {
                return 443;
            }
            return 80;
        } catch (Exception e) {
            log.warn("Failed to parse port from minio.endpoint={}, using 9000", endpoint, e);
            return 9000;
        }
    }

    /**
     * Nginx 外链 host:port：{@code 本机局域网IP} + {@code :} + endpoint 中的端口；不含 {@link #minioUrl}。
     */
    public String minioNginxAgentUrl() throws Exception {
        return InetAddress.getLocalHost().getHostAddress() + ":" + parseEndpointPort();
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
                ", gatewayPort='" + gatewayPort + '\'' +
                ", minioUrl='" + minioUrl + '\'' +
                '}';
    }
}
