package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.WelcomeBoardCreateRequest;
import com.example.devicemanagement.dto.request.WelcomeBoardRemoveRequest;
import com.example.devicemanagement.dto.response.BoardRoomAvailabilityVO;
import com.example.devicemanagement.dto.response.WelcomeBoardVO;

import java.util.List;

public interface WelcomeBoardService {

    /**
     * 行政登记欢迎牌排期（文案、上墙时间、撤下时间）。
     * 同一接待室与未撤下欢迎牌时段重叠必须拦住。
     */
    WelcomeBoardVO createBoard(WelcomeBoardCreateRequest request);

    /**
     * 分页查询欢迎牌排期（按楼层、接待室、排期状态、仅待撤筛选；状态按当前时间懒推进）。
     */
    IPage<WelcomeBoardVO> getBoardsPage(int pageNum, int pageSize,
                                        Long floorId, Long roomId,
                                        Integer status, Boolean overdueOnly);

    /**
     * 欢迎牌详情。
     */
    WelcomeBoardVO getBoardById(Long boardId);

    /**
     * 到期未撤的欢迎牌列表（待撤）。
     */
    List<WelcomeBoardVO> getOverdueBoards();

    /**
     * 登记撤下回执并撤下欢迎牌；未写回执不能撤，只有在墙上（含待撤）的欢迎牌可撤。
     */
    WelcomeBoardVO removeBoard(Long boardId, WelcomeBoardRemoveRequest request);

    /**
     * 各接待室当前可接待情况：启用且墙上无欢迎牌才可接待。
     */
    List<BoardRoomAvailabilityVO> getRoomAvailability(Long floorId);
}
