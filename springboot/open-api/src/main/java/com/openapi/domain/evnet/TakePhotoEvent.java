package com.openapi.domain.evnet;

import com.openapi.domain.evnet.body.TakePhotoEventBody;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * @author 13225
 * @date 2025/10/29 14:07
 */
@Getter
@Setter
public class TakePhotoEvent extends ApplicationEvent {

    private TakePhotoEventBody eventBody;

    public TakePhotoEvent(Object source, TakePhotoEventBody eventBody) {
        super(source);
        this.eventBody = eventBody;
    }

}