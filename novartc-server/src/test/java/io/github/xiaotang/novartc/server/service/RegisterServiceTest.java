package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.util.MD5Util;
import io.github.xiaotang.novartc.server.dto.RegisterDTO;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户注册服务测试，覆盖参数合法性校验、用户编号/邮箱重复校验，
 * 以及正常注册时的入库字段（MD5 密码、默认标记、主键长度）。
 */
@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private IImUserService imUserService;

    @InjectMocks
    private RegisterService registerService;

    /**
     * 构造一份合法的注册请求参数，供各用例在此基础上制造非法或重复场景。
     */
    private RegisterDTO buildValidDto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUserNum("u1001");
        dto.setUsername("张三");
        dto.setPassword("pass123");
        dto.setEmail("zhangsan@example.com");
        return dto;
    }

    /**
     * 非法参数（空白用户编号、过短密码、非法邮箱）均应抛出业务异常，
     * 且任何非法场景下都不允许执行入库。
     */
    @Test
    void shouldRejectInvalidParams() {
        RegisterDTO emptyUserNum = buildValidDto();
        emptyUserNum.setUserNum(" ");
        assertThrows(BusinessException.class, () -> registerService.register(emptyUserNum));

        RegisterDTO shortPassword = buildValidDto();
        shortPassword.setPassword("12345");
        assertThrows(BusinessException.class, () -> registerService.register(shortPassword));

        RegisterDTO badEmail = buildValidDto();
        badEmail.setEmail("not-an-email");
        assertThrows(BusinessException.class, () -> registerService.register(badEmail));

        verify(imUserService, never()).save(org.mockito.ArgumentMatchers.any(ImUser.class));
    }

    /**
     * 用户编号已被占用时拒绝注册，提示「用户编号已存在」且不入库。
     */
    @Test
    void shouldRejectDuplicatedUserNum() {
        RegisterDTO dto = buildValidDto();
        when(imUserService.selectByUserNum(dto.getUserNum())).thenReturn(new ImUser());

        BusinessException ex = assertThrows(BusinessException.class, () -> registerService.register(dto));
        assertEquals("用户编号已存在", ex.getMessage());
        verify(imUserService, never()).save(org.mockito.ArgumentMatchers.any(ImUser.class));
    }

    /**
     * 邮箱已被注册时拒绝注册，提示「邮箱已被注册」且不入库。
     */
    @Test
    void shouldRejectDuplicatedEmail() {
        RegisterDTO dto = buildValidDto();
        when(imUserService.selectByUserNum(dto.getUserNum())).thenReturn(null);
        when(imUserService.selectByEmail(dto.getEmail())).thenReturn(new ImUser());

        BusinessException ex = assertThrows(BusinessException.class, () -> registerService.register(dto));
        assertEquals("邮箱已被注册", ex.getMessage());
        verify(imUserService, never()).save(org.mockito.ArgumentMatchers.any(ImUser.class));
    }

    /**
     * 正常注册：校验入库实体各字段正确，密码以 MD5 加密存储，
     * 默认非管理员、未删除，主键为 32 位字符串。
     */
    @Test
    void shouldSaveUserWithMd5Password() {
        RegisterDTO dto = buildValidDto();
        when(imUserService.selectByUserNum(dto.getUserNum())).thenReturn(null);
        when(imUserService.selectByEmail(dto.getEmail())).thenReturn(null);

        registerService.register(dto);

        ArgumentCaptor<ImUser> captor = ArgumentCaptor.forClass(ImUser.class);
        verify(imUserService).save(captor.capture());
        ImUser saved = captor.getValue();
        assertEquals("u1001", saved.getUserNum());
        assertEquals("张三", saved.getUsername());
        assertEquals("zhangsan@example.com", saved.getEmail());
        assertEquals(MD5Util.md5("pass123"), saved.getPassword());
        assertEquals(Boolean.FALSE, saved.getIsAdmin());
        assertEquals(Boolean.FALSE, saved.getIsDeleted());
        assertTrue(saved.getId() != null && saved.getId().length() == 32);
    }
}
