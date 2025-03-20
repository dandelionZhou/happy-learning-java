package com.happy.learning.zh.rbac.notification;

import com.happy.learning.zh.rbac.entity.PermissionUpdateEvent;
import com.happy.learning.zh.rbac.feign.ResourceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

// NotificationPublisher.java
@Component
public class NotificationPublisher {

    //@Autowired(required = false)
    //private RabbitTemplate rabbitTemplate;

    @Value("${rbac.notify.http.endpoints}")
    private String httpEndpoints;

    @Autowired
    private ResourceClient resourceClient;

    public void publish(PermissionUpdateEvent event) {
        // 方式 1: RabbitMQ
        /*if (rabbitTemplate != null) {
            rabbitTemplate.convertAndSend(
                    "rbac-exchange",
                    "permission.update." + event.getRoleName(),
                    event
            );
        }*/

        // 方式 2: HTTP 回调
        resourceClient.userPermissionsEvictCache(event);
        //RestTemplate rest = new RestTemplate();
        //ResponseEntity<String> response = rest.postForEntity("localhost:8080/rbac/permission/cache/evict", event, String.class);
        //System.out.println(response);
        /*httpEndpoints.forEach(url -> {
            rest.postForEntity(url, event, Void.class);
        });*/
    }
}
