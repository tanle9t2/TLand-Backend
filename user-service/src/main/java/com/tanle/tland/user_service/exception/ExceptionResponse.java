package com.tanle.tland.user_service.exception;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public  class ExceptionResponse {
    private String type;
    private String title;
    private int status;
    private String detail;
    private long timeStamp;
}
