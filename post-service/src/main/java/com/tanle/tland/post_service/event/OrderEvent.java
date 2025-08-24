//package com.tanle.tland.post_service.event;
//
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;
//import com.tanle.tland.payment_service.request.OrderRequest;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.util.Date;
//import java.util.UUID;
//
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//public class OrderEvent implements Event {
//    private UUID eventId = UUID.randomUUID();
//    private Date eventDate = new Date();
//    @JsonSerialize
//    private OrderRequest orderRequestDto;
//    @JsonSerialize
//    private OrderStatus orderStatus;
//
//    @Override
//    public UUID getEventId() {
//        return eventId;
//    }
//
//    @Override
//    public Date getDate() {
//        return eventDate;
//    }
//
//    public OrderEvent(OrderRequest orderRequestDto, OrderStatus orderStatus) {
//        this.orderRequestDto = orderRequestDto;
//        this.orderStatus = orderStatus;
//    }
//}
