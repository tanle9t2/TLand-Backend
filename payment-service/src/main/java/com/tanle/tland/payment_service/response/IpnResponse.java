package com.tanle.tland.payment_service.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpnResponse {
    @JsonProperty("RspCode")
    private String code;

    @JsonProperty("Message")
    private String msg;
}
