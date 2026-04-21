package com.shopflow.orderservice.client;

import com.shopflow.orderservice.exception.BadRequestException;
import com.shopflow.orderservice.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder  implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 404:
                return new ResourceNotFoundException(
                        "Servis yanıtı: kaynak bulunamadı — " + methodKey);
            case 400:
                return new BadRequestException(
                        "Servis yanıtı: geçersiz istek — " + methodKey);
            case 503:
                return new RuntimeException(
                        "Servis şu an kullanılamıyor — " + methodKey);
            default:
                return defaultDecoder.decode(methodKey, response);
        }
    }
}
