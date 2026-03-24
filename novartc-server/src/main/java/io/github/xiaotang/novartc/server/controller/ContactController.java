package io.github.xiaotang.novartc.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ContactVO;
import io.github.xiaotang.novartc.server.dto.StartChatRequest;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created on 2026/3/24 12:16
 *
 * @author tangj
 * @description
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final IImUserService imUserService;
    private final IImSingleChatService imSingleChatService;
    private final UserChannelManager userChannelManager;

    @PostMapping("/list")
    public List<ContactVO> list() {
        String currentUserId = UserContextHolder.getUserId();
        return imUserService.list(new LambdaQueryWrapper<ImUser>()
                        .eq(ImUser::getIsDeleted, false)
                        .ne(ImUser::getId, currentUserId)
                        .orderByAsc(ImUser::getUserNum))
                .stream()
                .map(this::toContactVO)
                .toList();
    }

    @PostMapping("/start")
    public ChatInfoVO start(@RequestBody StartChatRequest request) {
        ImUser targetUser = imUserService.selectByUserId(request.getTargetUserId());
        if (targetUser == null) {
            throw new ApplicationException("target user not found");
        }

        ImSingleChat singleChat = imSingleChatService.createOrGet(UserContextHolder.getUserId(), targetUser.getId());

        ChatInfoVO vo = new ChatInfoVO();
        vo.setChatType("single");
        vo.setChatId(singleChat.getChatId());
        vo.setTargetUserId(targetUser.getId());
        vo.setTargetUserNum(targetUser.getUserNum());
        vo.setTitle(targetUser.getUsername());
        vo.setAvatarUrl(targetUser.getAvatarUrl());
        vo.setOnline(userChannelManager.isOnline(targetUser.getId()));
        vo.setLastMessage("");
        vo.setLastMessageType("SYSTEM");
        vo.setUnreadCount(0);
        return vo;
    }

    private ContactVO toContactVO(ImUser user) {
        ContactVO vo = new ContactVO();
        vo.setUserId(user.getId());
        vo.setUserNum(user.getUserNum());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setOnline(userChannelManager.isOnline(user.getId()));
        return vo;
    }
}

