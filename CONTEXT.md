# 项目上下文

> 重开 AI 窗口时，第一句：先读 CONTEXT.md

## 项目背景
- 原名 MixPet，GitHub: https://github.com/MixJade/MixPet
- 现名 pet-hms，GitHub: https://github.com/acuherb/pet-hms（私有）
- 用途：自建宠物医院管理系统
- 部署：Debian 13 + MariaDB，局域网访问

## 技术栈
- 后端：Java 17 + Spring Boot 3.1 + Spring Security 6 + MyBatis-Plus 3.5
- 前端：Vue 3 + Vite 4 + TypeScript + Element Plus
- 数据库：MariaDB 11
- 后端端口 8085，前端端口 5932

## 已完成（截至 2026-10-10）
- 数据库扩展：pet 表 +9 字段，client 表 +11 字段
- 后端：手机号即账号 + 唯一性校验、绑定邮箱、改密码、重置密码接口
- 前端：侧边栏折叠、详情/表单分组、生日年龄联动、窄屏适配

## 待办
- 待测试：邮件发送、手机端按钮错位、手机号唯一性、改邮箱/改密码
- 待开发：首次登录强制改密、忘记密码邮箱找回、顾客加宠物、顾客预约挂号
- 上线前：删测试数据、改默认密码、配 HTTPS

## 关键决策
1. 账号=手机号，不开放自主注册
2. 密码统一 123456，顾客可自己改
3. 只存生日，年龄前端实时计算
4. pet_source 字段暂未加

## 数据库
- 库名 pet_ship，账号 mixpet / MixPet2024

## 给下一个 AI
先读 CONTEXT.md，然后看我这次想做什么。
