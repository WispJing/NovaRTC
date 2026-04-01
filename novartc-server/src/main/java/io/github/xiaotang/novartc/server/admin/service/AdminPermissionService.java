package io.github.xiaotang.novartc.server.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.server.admin.vo.AdminMenuVO;
import io.github.xiaotang.novartc.server.entity.mysql.*;
import io.github.xiaotang.novartc.server.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {
    private final IAdminMenuService adminMenuService;
    private final IAdminUserMenuService adminUserMenuService;
    private final IAdminUserRoleService adminUserRoleService;
    private final IAdminRoleMenuService adminRoleMenuService;
    private final IImUserService imUserService;

    public boolean isSuperAdmin(String userId) {
        ImUser user = imUserService.selectByUserId(userId);
        return user != null && Boolean.TRUE.equals(user.getIsAdmin());
    }

    public boolean hasAdminAccess(String userId) {
        if (StringUtils.isBlank(userId)) {
            return false;
        }
        if (isSuperAdmin(userId)) {
            return true;
        }
        long directMenuCount = adminUserMenuService.count(new LambdaQueryWrapper<AdminUserMenu>()
                .eq(AdminUserMenu::getUserId, userId)
                .eq(AdminUserMenu::getIsDeleted, false));
        if (directMenuCount > 0) {
            return true;
        }
        List<String> roleIds = adminUserRoleService.list(new LambdaQueryWrapper<AdminUserRole>()
                        .eq(AdminUserRole::getUserId, userId)
                        .eq(AdminUserRole::getIsDeleted, false))
                .stream()
                .map(AdminUserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return false;
        }
        return adminRoleMenuService.count(new LambdaQueryWrapper<AdminRoleMenu>()
                .in(AdminRoleMenu::getRoleId, roleIds)
                .eq(AdminRoleMenu::getIsDeleted, false)) > 0;
    }

    public Set<String> getUserMenuCodes(String userId) {
        return getMenusForUser(userId, false).stream()
                .map(AdminMenu::getMenuCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<AdminMenuVO> getUserMenuTree(String userId) {
        List<AdminMenu> menus = getMenusForUser(userId, true);
        Map<String, AdminMenuVO> map = menus.stream()
                .map(this::toMenuVO)
                .collect(Collectors.toMap(AdminMenuVO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<AdminMenuVO> roots = new ArrayList<>();
        for (AdminMenuVO menu : map.values()) {
            if (StringUtils.isBlank(menu.getParentId()) || !map.containsKey(menu.getParentId())) {
                roots.add(menu);
            } else {
                map.get(menu.getParentId()).getChildren().add(menu);
            }
        }
        roots.sort(Comparator.comparing(item -> Optional.ofNullable(item.getSortNo()).orElse(0)));
        map.values().forEach(item -> item.getChildren().sort(Comparator.comparing(child -> Optional.ofNullable(child.getSortNo()).orElse(0))));
        return roots;
    }

    public List<String> getGrantedMenuIds(String userId) {
        return getMenusForUser(userId, false).stream().map(AdminMenu::getId).distinct().toList();
    }

    public void replaceUserMenus(String userId, List<String> menuIds) {
        adminUserMenuService.remove(new LambdaQueryWrapper<AdminUserMenu>().eq(AdminUserMenu::getUserId, userId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<AdminUserMenu> items = menuIds.stream().distinct().map(menuId -> {
            AdminUserMenu entity = new AdminUserMenu();
            entity.setId(uuid32());
            entity.setUserId(userId);
            entity.setMenuId(menuId);
            entity.setIsDeleted(false);
            return entity;
        }).toList();
        adminUserMenuService.saveBatch(items);
    }

    public boolean hasPermission(String userId, String permissionCode) {
        if (StringUtils.isBlank(permissionCode)) {
            return true;
        }
        if (isSuperAdmin(userId)) {
            return true;
        }
        return getUserMenuCodes(userId).contains(permissionCode);
    }

    private List<AdminMenu> getMenusForUser(String userId, boolean visibleOnly) {
        LambdaQueryWrapper<AdminMenu> query = new LambdaQueryWrapper<AdminMenu>()
                .eq(AdminMenu::getIsDeleted, false)
                .eq(AdminMenu::getStatus, (byte) 1)
                .orderByAsc(AdminMenu::getSortNo)
                .orderByAsc(AdminMenu::getCreatedAt);
        if (visibleOnly) {
            query.eq(AdminMenu::getVisible, (byte) 1);
        }
        if (isSuperAdmin(userId)) {
            return adminMenuService.list(query);
        }

        Set<String> menuIds = new LinkedHashSet<>();
        adminUserMenuService.list(new LambdaQueryWrapper<AdminUserMenu>()
                        .eq(AdminUserMenu::getUserId, userId)
                        .eq(AdminUserMenu::getIsDeleted, false))
                .stream().map(AdminUserMenu::getMenuId).forEach(menuIds::add);

        List<String> roleIds = adminUserRoleService.list(new LambdaQueryWrapper<AdminUserRole>()
                        .eq(AdminUserRole::getUserId, userId)
                        .eq(AdminUserRole::getIsDeleted, false))
                .stream().map(AdminUserRole::getRoleId).toList();
        if (!roleIds.isEmpty()) {
            adminRoleMenuService.list(new LambdaQueryWrapper<AdminRoleMenu>()
                            .in(AdminRoleMenu::getRoleId, roleIds)
                            .eq(AdminRoleMenu::getIsDeleted, false))
                    .stream().map(AdminRoleMenu::getMenuId).forEach(menuIds::add);
        }
        if (menuIds.isEmpty()) {
            return List.of();
        }
        query.in(AdminMenu::getId, menuIds);
        List<AdminMenu> menus = adminMenuService.list(query);
        Set<String> parentIds = menus.stream().map(AdminMenu::getParentId).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (!parentIds.isEmpty()) {
            menus = new ArrayList<>(menus);
            menus.addAll(adminMenuService.list(new LambdaQueryWrapper<AdminMenu>()
                    .in(AdminMenu::getId, parentIds)
                    .eq(AdminMenu::getIsDeleted, false)
                    .eq(AdminMenu::getStatus, (byte) 1)
                    .orderByAsc(AdminMenu::getSortNo)));
            menus = menus.stream().collect(Collectors.toMap(AdminMenu::getId, Function.identity(), (l, r) -> l, LinkedHashMap::new)).values().stream().toList();
        }
        return menus;
    }

    private AdminMenuVO toMenuVO(AdminMenu menu) {
        AdminMenuVO vo = new AdminMenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuCode(menu.getMenuCode());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setSortNo(menu.getSortNo());
        return vo;
    }

    private String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
