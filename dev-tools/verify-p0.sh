#!/usr/bin/env bash
# P0 静态回归自检：不连数据库、不启动服务，用于每次提交/上线前快速确认止血项没被回退。
set -uo pipefail
PROJ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJ" || exit 1
fail=0
ok()   { echo "OK   $1"; }
bad()  { echo "FAIL $1"; fail=1; }
has()  { grep -q -- "$2" "$1" 2>/dev/null; }

has mixPet/src/main/java/com/mix/util/JwtUtil.java 'checkSecret' \
  && ok '验证码密钥启动强校验' || bad '验证码密钥启动强校验'
has mixPet/src/main/java/com/mix/util/JwtUtil.java 'validateAndGetJti' \
  && ! has mixPet/src/main/java/com/mix/util/JwtUtil.java 'claims.put("code"' \
  && ok '验证码不放进 JWT / 仅服务端存储' || bad '验证码不放进 JWT / 仅服务端存储'
has mixPet/src/main/java/com/mix/security/login/RememberService.java 'HmacSHA256' \
  && ! has mixPet/src/main/java/com/mix/security/login/RememberService.java 'MixPetDefaultRememberKey' \
  && ok '记住我 HMAC-SHA256 且无硬编码兜底密钥' || bad '记住我 HMAC-SHA256 且无硬编码兜底密钥'
has mixPet/src/main/resources/application.yml 'verify-code-secret: ${VERIFY_CODE_SECRET}' \
  && has mixPet/src/main/resources/application.yml 'key: ${REMEMBER_ME_KEY}' \
  && ok '配置无弱默认值兜底' || bad '配置无弱默认值兜底'
has mixPet/src/main/java/com/mix/controller/MsgController.java '@Secured({RoleConst.MANAGER, RoleConst.ADMIN})' \
  && ok '清理聊天记录限管理员' || bad '清理聊天记录限管理员'
has mixPet/src/main/java/com/mix/service/impl/BackupServiceImpl.java 'doCreate("pre_restore_")' \
  && ok '还原不再自抢 busy 锁（灾备可用）' || bad '还原不再自抢 busy 锁（灾备可用）'
has db-backup/pet_mix.sql 'CREATE TABLE IF NOT EXISTS `audit_log`' \
  && has db-backup/pet_mix.sql 'ADD COLUMN `pay_time`' \
  && has db-backup/pet_mix.sql 'ADD COLUMN `refund_money`' \
  && ok '基线含审计表与资金字段' || bad '基线含审计表与资金字段'
has mixPet/src/main/java/com/mix/service/impl/BillServiceImpl.java 'eq(Bill::getPayStatus, false)' \
  && has mixPet/src/main/java/com/mix/service/impl/BillServiceImpl.java 'eq(Bill::getDispenseStatus, false)' \
  && ok '收款/发药状态 CAS 抢占' || bad '收款/发药状态 CAS 抢占'
has mixPet/src/main/java/com/mix/common/BizException.java 'RuntimeException' \
  && ! has mixPet/src/main/java/com/mix/service/impl/BillServiceImpl.java 'return Result.error(' \
  && ok '账单事务失败走异常回滚' || bad '账单事务失败走异常回滚'
! has mix-ui/src/views/backstage/y-billList.vue '发票代码' \
  && has mix-ui/src/views/backstage/y-billList.vue 'const esc =' \
  && ok '收费凭证文案 + 打印转义' || bad '收费凭证文案 + 打印转义'
has mixPet/src/main/java/com/mix/service/impl/AppointmentServiceImpl.java 'lockDoctor' \
  && has mixPet/src/main/java/com/mix/service/impl/AppointmentServiceImpl.java 'boolean reserve' \
  && ok 'P1 号源统一入口 + 医生行锁' || bad 'P1 号源统一入口 + 医生行锁'
has db-backup/pet_mix.sql '`is_del` bigint(20) NOT NULL DEFAULT 0' \
  && ok 'P1 排班 is_del 默认值' || bad 'P1 排班 is_del 默认值'
has mixPet/src/main/java/com/mix/service/impl/FosterServiceImpl.java 'addCharge' \
  && has mixPet/src/main/java/com/mix/service/impl/HospitalServiceImpl.java '已冲抵' \
  && ok 'P1 寄养/领养入账 + 押金冲抵' || bad 'P1 寄养/领养入账 + 押金冲抵'
has mixPet/src/main/java/com/mix/common/SendMail.java 'sendReminderMail' \
  && ok 'P1 提醒邮件触达' || bad 'P1 提醒邮件触达'
has mix-ui/src/utils/chatSocket.ts 'scheduleReconnect' \
  && has mix-ui/src/router/index.ts 'minAuthLv' \
  && ok 'P1 WS 单例重连 + 后台路由守卫' || bad 'P1 WS 单例重连 + 后台路由守卫'
has mix-ui/src/views/doctorPage/d-appoint.vue 'sessionStorage' \
  && has mix-ui/src/views/doctorPage/d-appoint.vue 'restoreDraft' \
  && ok 'P1 接诊向导草稿可恢复' || bad 'P1 接诊向导草稿可恢复'
has deploy.sh 'limit_req_zone' \
  && has deploy.sh 'Strict-Transport-Security' \
  && has deploy.sh 'proxy_set_header Upgrade' \
  && ok 'P2 nginx 反代 / WSS / 安全头 / 限流' || bad 'P2 nginx 反代 / WSS / 安全头 / 限流'
has deploy.sh 'Restart=always' \
  && has deploy.sh 'logrotate' \
  && has deploy.sh 'systemctl enable' \
  && ok 'P2 systemd 守护 + 日志轮转' || bad 'P2 systemd 守护 + 日志轮转'
[ -f mixPet/src/main/resources/application-prod.yml ] \
  && has mixPet/src/main/resources/application-prod.yml '${PET_AVATAR}' \
  && has mixPet/src/main/resources/application-prod.yml '${BACKUP_DIR}' \
  && ok 'P2 prod profile 路径外置' || bad 'P2 prod profile 路径外置'
has mix-ui/vite.config.ts 'manualChunks' \
  && has mix-ui/src/views/doctorPage/d-rx.vue "import('html2pdf.js')" \
  && ok 'P2 前端分包 + PDF 按需加载' || bad 'P2 前端分包 + PDF 按需加载'
has db-backup/feature_20261107_p3_clinical.sql 'medical_record_revision' \
  && has mixPet/src/main/java/com/mix/common/LabResultEvaluator.java 'critical' \
  && has mixPet/src/main/java/com/mix/controller/ClientNotificationController.java 'selfClientId' \
  && has mix-ui/src/views/reception/clientBill.vue 'feesOf' \
  && ok 'P3 病历留痕 / 化验危急值 / 顾客闭环' || bad 'P3 病历留痕 / 化验危急值 / 顾客闭环'
has mix-ui/src/router/doctorPage.ts 'y-billList' \
  && has mix-ui/src/router/backstage.ts 'redirect: "/doctorPage/z-examResult"' \
  && ok 'P3 医生闭环路由归一' || bad 'P3 医生闭环路由归一'
bash -n deploy.sh && bash -n dev-tools/start.sh \
  && ok '部署脚本语法' || bad '部署脚本语法'

# 本机环境检查（CI 无 .env 时跳过）
if [ -f "$PROJ/.env" ]; then
  # shellcheck disable=SC1091
  set -a; source "$PROJ/.env"; set +a
  for k in REMEMBER_ME_KEY VERIFY_CODE_SECRET APP_DEFAULT_PASSWORD; do
    v="${!k:-}"
    if [ "${#v}" -ge 16 ] && [[ "$v" != *YourSecure* ]]; then ok ".env $k 已配置强值"; else bad ".env $k 仍为弱值/模板值"; fi
  done
else
  echo "SKIP .env 检查（CI 环境）"
fi

echo "----"
if [ "$fail" -eq 0 ]; then echo "P0/P1/P2/P3 静态自检全部通过"; else echo "P0/P1/P2/P3 静态自检存在失败项"; fi
exit "$fail"
