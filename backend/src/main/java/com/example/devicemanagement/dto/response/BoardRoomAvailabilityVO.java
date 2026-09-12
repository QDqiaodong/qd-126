package com.example.devicemanagement.dto.response;

import lombok.Data;

/**
 * 接待室可接待情况：当前墙上有未撤欢迎牌（含待撤）即不可接待。
 */
@Data
public class BoardRoomAvailabilityVO {

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    /** 接待室是否启用 */
    private Boolean roomEnabled;

    /** 当前是否可接待（启用且墙上无欢迎牌） */
    private Boolean available;

    /** 当前在墙上的欢迎牌ID（不可接待时返回） */
    private Long boardId;
    private String boardNo;
    private String boardText;

    /** 墙上欢迎牌是否已到期待撤 */
    private Boolean overdueRemove;
}
