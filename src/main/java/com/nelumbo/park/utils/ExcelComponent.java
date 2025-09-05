package com.nelumbo.park.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExcelComponent {

    @Value("${spring.mvc.contentnegotiation.media-types.xlsx}")
    private String contentType;

    public String getContentType() {
        return contentType;
    }
}
