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
  && has db-backup/feature_20261107_p4_clinical.sql 'diagnosis_dict' \
  && has mixPet/src/main/java/com/mix/controller/RefRangeController.java 'LabRefRange' \
  && has mixPet/src/main/java/com/mix/controller/DiagnosisDictController.java 'DiagnosisDict' \
  && has mix-ui/src/views/doctorPage/d-appoint.vue 'differentialDiagnosis' \
  && has mix-ui/src/views/backstage/z-refRange.vue 'reqAddRefRange' \
  && has mix-ui/src/views/backstage/z-diagnosis.vue 'reqDiagnosisPage' \
  && has mixPet/src/main/java/com/mix/service/impl/LabOrderServiceImpl.java 'handleCritical' \
  && has mixPet/src/main/java/com/mix/service/impl/MedicalRecordServiceImpl.java 'rejectRecord' \
  && ok 'P4 SOAP/诊断编码/参考区间/危急处置/审核退回/复核撤销' || bad 'P4 SOAP/诊断编码/参考区间/危急处置/审核退回/复核撤销'
has mixPet/src/main/java/com/mix/controller/FlowController.java 'doctorPipeline' \
  && has mixPet/src/main/java/com/mix/service/FlowService.java 'FlowStage' \
  && has mix-ui/src/request/FlowApi.ts 'reqDoctorPipeline' \
  && has mix-ui/src/views/doctorPage/FlowPipeline.vue 'navigate' \
  && has mix-ui/src/views/doctorPage/d-flow.vue 'FlowPipeline' \
  && has mix-ui/src/router/doctorPage.ts 'children' \
  && has mix-ui/src/views/backstage/r-flow.vue 'reqReceptionPipeline' \
  && has mix-ui/src/views/backstage/m-flow.vue 'reqManagerPipeline' \
  && ok 'P5 诊疗流脊柱（后端聚合/内联深页/前台/管理端复用）' || bad 'P5 诊疗流脊柱（后端聚合/内联深页/前台/管理端复用）'
has mixPet/src/main/java/com/mix/ws/FlowNotify.java 'FLOW_REFRESH' \
  && has mixPet/src/main/java/com/mix/annotation/FlowChanged.java 'String stage()' \
  && has mixPet/src/main/java/com/mix/aspect/FlowNotifyAspect.java 'flowChanged.stage()' \
  && has mixPet/src/main/java/com/mix/controller/LabOrderController.java 'FlowChanged' \
  && has mixPet/src/main/java/com/mix/controller/BillController.java 'FlowChanged' \
  && has mixPet/src/main/java/com/mix/ws/ChatEndpoint.java 'broadcastFlow' \
  && has mix-ui/src/utils/chatSocket.ts 'subscribeFlow' \
  && has mix-ui/src/views/doctorPage/FlowInline.vue '__flowTarget' \
  && has mix-ui/src/router/index.ts '__flowStandalone' \
  && has mixPet/src/main/java/com/mix/controller/FlowController.java '@Secured(RoleConst.MANAGER)' \
  && has mix-ui/src/router/backstage.ts 'minAuthLv: 6' \
  && ok 'P6 状态变更广播/三端实时/权限隔离/内联不脱离' || bad 'P6 状态变更广播/三端实时/权限隔离/内联不脱离'
# mapper <sql> 自引用会令 MyBatis 在构建 SqlSessionFactory 时无限递归 StackOverflowError，
# 且纯单元测试不解析 XML 会全绿、直到 spring-boot:run 才炸。静态挡一层 + JUnit 解析挡一层。
python3 - <<'PY' && ok 'mapper <sql> 片段无自引用' || bad 'mapper <sql> 片段无自引用'
import re, pathlib, sys
bad = []
for f in pathlib.Path('mixPet/src/main/resources/com/mix/mapper').glob('*.xml'):
    for m in re.finditer(r'<sql\s+id="([^"]+)"\s*>(.*?)</sql>', f.read_text(encoding='utf-8'), re.S):
        sid, body = m.group(1), m.group(2)
        if sid in re.findall(r'<include\s+refid="([^"]+)"', body):
            bad.append(f'{f.name}:{sid}')
if bad:
    print('自引用片段: ' + ', '.join(bad), file=sys.stderr)
    sys.exit(1)
PY
has mixPet/src/test/java/com/mix/mapper/MapperXmlParseTest.java 'XMLMapperBuilder' \
  && ok 'mapper XML 离线解析测试（挡 StackOverflow）' || bad 'mapper XML 离线解析测试（挡 StackOverflow）'
# 旧经典工作台已删除，旧 URL 统一落到医生操作台；全景看板与操作台双视图互切
! has mix-ui/src/views/doctorPage/d-flow.vue '经典视图' \
  && [ ! -f mix-ui/src/views/doctorPage/index.vue ] \
  && has mix-ui/src/router/doctorPage.ts "redirect: 'd-console'" \
  && ! has mix-ui/src/router/index.ts "path !== '/doctorPage/d-home'" \
  && ok '旧经典工作台已下线' || bad '旧经典工作台已下线'
# 面向人的医生操作台：可点击、带患者上下文，命令面板只作加速器
has mix-ui/src/views/doctorPage/d-console.vue '今日待办' \
  && has mix-ui/src/views/doctorPage/d-console.vue '常用功能' \
  && has mix-ui/src/views/login.vue '/doctorPage/d-console' \
  && has mix-ui/src/views/doctorPage/d-flow.vue '医生操作台' \
  && has mix-ui/src/router/index.ts "'/doctorPage/d-console/inline'" \
  && ok '医生操作台（登录首屏/可点击/双视图互切）' || bad '医生操作台（登录首屏/可点击/双视图互切）'
# 旧库漏跑 P3/P4 迁移会令 /flow/doctorPipeline 抛 Unknown column → 前端「系统繁忙」、诊疗流无数据
has dev-tools/db-migrate.sh 'p4_clinical' \
  && has deploy.sh 'p4_clinical' \
  && has dev-tools/start.sh 'db-migrate.sh' \
  && has dev-tools/start.sh 'information_schema.COLUMNS' \
  && ok '数据库迁移链路（P0/P1/P3/P4 幂等 + 启动 schema 自检）' || bad '数据库迁移链路（P0/P1/P3/P4 幂等 + 启动 schema 自检）'
bash -n deploy.sh && bash -n dev-tools/start.sh && bash -n dev-tools/db-migrate.sh \
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

# 真连库 schema 校验：静态字符串检查抓不到「代码引用了库里不存在的列」，
# 这正是上次「诊疗流系统繁忙」的漏检点。缺列直接判 FAIL，不静默跳过。
if [ -f "$PROJ/.env" ] && command -v mysql >/dev/null 2>&1 && [ -n "${DB_USER:-}" ]; then
  DBCOLS=$(MYSQL_PWD="${DB_PASSWORD:-}" mysql -u "$DB_USER" -N pet_mix -e \
    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_mix' AND TABLE_NAME='medical_record' AND COLUMN_NAME IN ('version','review_status','diagnosis_code','differential_diagnosis')" 2>/dev/null) || DBCOLS=""
  if [ "$DBCOLS" = "4" ]; then
    ok '数据库 schema 与代码一致（临床关键列 4/4）'
  else
    bad "数据库 schema 缺列或不可达（临床列 ${DBCOLS:-不可用}/4，请跑 bash dev-tools/db-migrate.sh）"
  fi
else
  echo "SKIP 连库 schema 校验（CI 环境或缺 mysql 客户端）"
fi
# 未接入真实模型前，不允许规则/演示假建议回流到临床页面
has mixPet/src/main/resources/application.yml 'enabled: false' \
  && has mixPet/src/main/resources/application.yml 'provider: none' \
  && ! has mix-ui/src/views/doctorPage/d-appoint.vue 'reqAiAssist' \
  && ok 'AI 辅助已下线（无假建议入口）' || bad 'AI 辅助已下线（无假建议入口）'

echo "----"
if [ "$fail" -eq 0 ]; then echo "P0-P6 + 加固项 自检全部通过"; else echo "P0-P6 + 加固项 自检存在失败项"; fi
exit "$fail"
