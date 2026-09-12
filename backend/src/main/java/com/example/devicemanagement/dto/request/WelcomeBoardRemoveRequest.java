package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class WelcomeBoardRemoveRequest {

    /** 撤下回执（必填，未写回执不能拿掉欢迎牌/待撤标记） */
    private String receipt;

    /** 撤下登记人 */
    private String operator;
}
