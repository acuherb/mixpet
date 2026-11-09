package com.mix.security.details;

import com.mix.security.model.RoleConst;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限档 → 角色映射：管理账号不得具备临床开方/写病历资格（用户 2026-11-09 定下的规则）。
 * 防止以后有人把 switch 改回 fall-through 累加，把 DOCTOR 白送给 6/8 档。
 */
class DoctorDetailsRoleTest {

    @Test
    void managerAndAdminAreNotClinicians() {
        assertFalse(DoctorDetails.rolesOf(8).contains(RoleConst.DOCTOR), "超管不应继承临床资格");
        assertFalse(DoctorDetails.rolesOf(6).contains(RoleConst.DOCTOR), "管理员不应继承临床资格");
        assertTrue(DoctorDetails.rolesOf(4).contains(RoleConst.DOCTOR), "医生档必须能开方写病历");
        assertFalse(DoctorDetails.rolesOf(2).contains(RoleConst.DOCTOR), "医护不能开方");
    }

    @Test
    void staffKeepNurseBaselineAndManagementRoles() {
        for (int lv : List.of(2, 4, 6, 8)) {
            assertTrue(DoctorDetails.rolesOf(lv).contains(RoleConst.NURSE), lv + " 档需保留在职员工基线");
            assertTrue(DoctorDetails.rolesOf(lv).contains(RoleConst.LOSER));
        }
        assertTrue(DoctorDetails.rolesOf(6).contains(RoleConst.MANAGER));
        assertTrue(DoctorDetails.rolesOf(8).contains(RoleConst.ADMIN));
        assertFalse(DoctorDetails.rolesOf(6).contains(RoleConst.ADMIN));
        assertEquals(List.of(RoleConst.LOSER), DoctorDetails.rolesOf(0), "0 档仅可登录");
    }
}
