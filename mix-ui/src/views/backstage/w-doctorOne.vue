<template>
  <!--顶部操作条：回工作台是最高频动作，固定两个明确入口 -->
  <div class="top-bar">
    <span class="bar-title">我的资料</span>
    <div class="bar-actions">
      <router-link to="/doctorPage/d-flow"><el-button plain>诊疗流全景</el-button></router-link>
      <router-link to="/doctorPage/d-console"><el-button type="success">返回医生操作台</el-button></router-link>
    </div>
  </div>

  <div class="profile-page">
    <!--封面横幅-->
    <div class="banner"></div>

    <!--主体卡片-->
    <div class="profile-card">
      <!--头部：头像 + 身份-->
      <div class="card-head">
        <div class="avatar-wrap">
          <el-avatar :size="108" :src="avatarUrl" class="avatar">
            {{ doctor.doctorName?.charAt(0) || '医' }}
          </el-avatar>
        </div>
        <div class="head-info">
          <div class="name-row">
            <span class="d-name">{{ doctor.doctorName || '未设置' }}</span>
            <el-tag round effect="light" type="info">{{ doctor.jobName || '未设职称' }}</el-tag>
            <el-tag round effect="plain" :type="isAdmin ? 'danger' : 'success'">{{ permName(doctor.authLv) }}</el-tag>
            <el-tag v-for="dn in (doctor.departmentNames && doctor.departmentNames.length ? doctor.departmentNames : (doctor.departmentName ? [doctor.departmentName] : []))" :key="dn" round effect="plain">{{ dn }}</el-tag>
          </div>
          <div class="account">账号：{{ doctor.username }}&nbsp;&nbsp;·&nbsp;&nbsp;工号：{{ doctorNo }}</div>
          <div class="head-btns">
            <el-button type="primary" round @click="updateRole">
              <el-icon><Edit/></el-icon>&nbsp;修改资料
            </el-button>
            <el-button type="warning" plain round @click="updatePwd">
              <el-icon><Key/></el-icon>&nbsp;重置密码
            </el-button>
            <el-button type="danger" plain round @click="doLogout">
              <el-icon><SwitchButton/></el-icon>&nbsp;退出登录
            </el-button>
          </div>
        </div>
      </div>

      <!--信息网格-->
      <div class="info-grid">
        <div class="info-item">
          <span class="lbl">性别</span>
          <span class="val">{{ doctor.doctorGender ? '男' : '女' }}</span>
        </div>
        <div class="info-item">
          <span class="lbl">年龄</span>
          <span class="val">{{ getAge(doctor.doctorAge) }}</span>
        </div>
        <div class="info-item">
          <span class="lbl">电话</span>
          <span class="val">{{ doctor.doctorTel || '--' }}</span>
        </div>
        <div class="info-item">
          <span class="lbl">邮箱</span>
          <span class="val">{{ doctor.doctorEmail || '--' }}</span>
        </div>
        <div class="info-item">
          <span class="lbl">入职日期</span>
          <span class="val">{{ entryDate }}<span v-if="tenure" style="opacity:.85">（{{ tenure }}）</span></span>
        </div>
        <div class="info-item">
          <span class="lbl">座右铭</span>
          <span class="val motto" :title="doctor.doctorMotto || undefined">{{ doctor.doctorMotto || '——' }}</span>
        </div>
      </div>

      <!--简介-->
      <div class="intro">
        <div class="intro-title"><el-icon><Document/></el-icon>&nbsp;个人简介</div>
        <div class="intro-body">{{ doctor.doctorInfo || '暂无个人简介，可点「修改资料」补充' }}</div>
      </div>
    </div>
  </div>

  <!--修改个人资料时的模态框-->
  <el-dialog v-model="modalView" draggable title="修改个人资料" width="60%">
    <!--头像上传框-->
    <UpImg :photoNm="form.doctorPhoto" @upPhoto="changePhoto"/>
    <!--表单-->
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="姓名" prop="doctorName">
        <el-input v-model="form.doctorName" clearable placeholder="医生姓名"/>
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.doctorGender">
          <el-radio :label="true" border>男</el-radio>
          <el-radio :label="false" border>女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="生日" prop="doctorAge">
        <el-date-picker
            v-model="form.doctorAge"
            format="YYYY-MM-DD"
            placeholder="选择出生日期"
            type="date"
            value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="form.doctorTel" maxlength="11" show-word-limit inputmode="numeric" @input="sanitizeTel" clearable placeholder="11位手机号"/>
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.doctorEmail" clearable placeholder="用于接收验证码"/>
      </el-form-item>
      <el-form-item label="职称">
        <el-select v-model="form.jobId" filterable placeholder="请选择职称（职务/职称管理维护）">
          <el-option v-for="j in jobNameL" :key="j.roleId" :label="j.roleName" :value="j.roleId"/>
        </el-select>
      </el-form-item>
      <el-form-item v-if="isAdmin" label="权限">
        <el-select v-model="form.authLv" placeholder="请选择权限">
          <el-option v-for="p in PERMISSIONS" :key="p.lv" :value="p.lv" :label="p.name"/>
        </el-select>
        <div class="perm-desc">{{ permDesc(form.authLv) }}</div>
      </el-form-item>
      <el-form-item v-if="isAdmin" label="科室">
        <el-select v-model="form.departmentIds" multiple filterable placeholder="请选择科室（可多选）">
          <el-option v-for="c in departNameL" :key="c.roleId" :label="c.roleName" :value="c.roleId"/>
        </el-select>
        <el-button v-if="departNameL.length" size="small" type="primary" link @click="checkAllDepart">全科</el-button>
      </el-form-item>
      <el-form-item label="座右铭">
        <el-input v-model="form.doctorMotto" clearable maxlength="100" show-word-limit placeholder="一句话座右铭（可选）"/>
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.doctorInfo" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="医生简介"/>
      </el-form-item>
      <el-form-item label="执业资格证号">
        <el-input v-model="form.doctorCert" clearable maxlength="64" placeholder="执业兽医资格证号（用于国标处方笺）"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">
          确认
        </el-button>
      </span>
    </template>
  </el-dialog>
  <!--重置密码时的模态框-->
  <el-dialog v-model="pwdModalView" draggable title="重置密码" width="60%">
    <!--表单-->
    <el-form ref="myFormRef2" :model="form2" :rules="rules2" label-width="120px">
      <el-form-item label="验证码" prop="checkCode">
        <el-input v-model="form2.checkCode" clearable placeholder="验证码"/>
        <el-button :disabled="mailBtn.dis" type="primary" @click="sendCheckMail">{{ mailBtn.btnTxt }}</el-button>
      </el-form-item>
      <el-form-item label="新密码" prop="password">
        <el-input v-model="form2.password" clearable show-password placeholder="新密码"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="pwdModalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit2">
          确认
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {permName, permDesc, PERMISSIONS} from "@/utils/PermissionUtil";
import {getAge} from "@/utils/TimeUtil";
import {computed, onMounted, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {DoctorDto, exampleDoctorOne} from "@/model/DO/DoctorDto";
import {reqDoctorResetPwd, reqDoctorSendMail, reqGetLoginD, reqUpdateLoginD, reqUpdateDoctor} from "@/request/DoctorApi";
import {reqDepartName} from "@/request/DepartApi";
import {reqJobName} from "@/request/JobApi";
import {NameDo} from "@/model/DO/NameDo";
import {Doctor, exampleDoctor} from "@/model/entiy/Doctor";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";
import UpImg from "@/components/UpImg.vue";
import {DoctorResetPwdVo} from "@/model/VO/DoctorResetPwdVo";
import {reqLogout} from "@/request/PowerApi";
import {clearPwdCheckFlag} from "@/router";
import {Edit, Key, Document, SwitchButton} from "@element-plus/icons-vue";
// 手机号只保留数字，最多 11 位
const sanitizeTel = () => {
  if (form.value.doctorTel) {
    form.value.doctorTel = form.value.doctorTel.replace(/\D/g, '').slice(0, 11)
  }
}

// 设置展示信息
const doctor = ref<DoctorDto>(exampleDoctorOne())

// 头像地址（有照片用下载地址，无则显示默认）
const avatarUrl = computed(() =>
    doctor.value.doctorPhoto ? `/api/common/download?name=${doctor.value.doctorPhoto}` : ''
)

// 是否院长（authLv=8）
const isAdmin = computed(() => doctor.value.authLv === 8)

const doctorNo = computed(() =>
  doctor.value.doctorId ? 'DOCTOR-' + String(doctor.value.doctorId).padStart(4, '0') : '--'
)

const fmtDate = (d: any): string => {
  if (!d) return '--'
  if (Array.isArray(d)) {
    const y = d[0]; const mo = d[1]; const da = d[2]
    return y + '-' + String(mo).padStart(2, '0') + '-' + String(da).padStart(2, '0')
  }
  if (typeof d === 'string') return d.slice(0, 10)
  return String(d)
}
const entryDate = computed(() => fmtDate(doctor.value.createTime))

// 工龄：从入职日期（createTime）推算
const tenure = computed(() => {
  const c = doctor.value.createTime
  if (!c) return ''
  let base: Date
  if (Array.isArray(c)) {
    base = new Date(c[0], (c[1] || 1) - 1, c[2] || 1)
  } else if (typeof c === 'string') {
    const s = c.slice(0, 10)
    const p = s.split('-').map(Number)
    if (p.length < 3 || isNaN(p[0])) return ''
    base = new Date(p[0], (p[1] || 1) - 1, p[2] || 1)
  } else {
    return ''
  }
  const now = new Date()
  let years = now.getFullYear() - base.getFullYear()
  let months = now.getMonth() - base.getMonth()
  if (months < 0) { years--; months += 12 }
  if (years < 0) return ''
  if (years === 0 && months === 0) return '入职不久'
  return years > 0 ? years + '年' + (months > 0 ? months + '个月' : '') + '工龄' : months + '个月工龄'
})

// 科室下拉列表
const departNameL = ref<NameDo[]>([])
// 职称下拉列表
const jobNameL = ref<NameDo[]>([])
onMounted(() => {
  reqDepartName().then(res => {
    departNameL.value = res
  })
  reqJobName().then(res => {
    jobNameL.value = res
  })
})
// 一键全选科室
const checkAllDepart = () => {
  if (departNameL.value.length) {
    form.value.departmentIds = departNameL.value.map(c => c.roleId)
  }
}
// 发起请求
onMounted(() => {
  reqGetLoginD().then(res => {
    doctor.value = res
  })
})

/**
 ┌───────────────────────────────────┐
 │=============修改个人资料============│
 └───────────────────────────────────┘
 */
const modalView = ref(false)
const form = ref<Doctor>(exampleDoctor()) // 空的默认值
const updateRole = () => {
  myFormRef.value?.clearValidate()
  // 赋值，防止响应式引用
  form.value.doctorId = doctor.value.doctorId
  form.value.doctorAge = doctor.value.doctorAge
  form.value.doctorInfo = doctor.value.doctorInfo
  form.value.doctorMotto = doctor.value.doctorMotto ?? ''
  form.value.doctorPhoto = doctor.value.doctorPhoto
  form.value.doctorName = doctor.value.doctorName
  form.value.doctorGender = doctor.value.doctorGender
  form.value.doctorTel = doctor.value.doctorTel
  form.value.doctorEmail = doctor.value.doctorEmail
  form.value.authLv = doctor.value.authLv
  form.value.jobId = doctor.value.jobId ?? null
  form.value.departmentId = doctor.value.departmentId
  form.value.departmentIds = doctor.value.departmentIds ? [...doctor.value.departmentIds] : []
  form.value.username = doctor.value.username
  form.value.doctorCert = doctor.value.doctorCert
  // 显示
  modalView.value = true
}

// 表单的数据
const myFormRef = ref<FormInstance>()
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  try {
    await myFormRef.value?.validate()
  } catch {
    return
  }
  // admin 走管理员接口（可改职位、科室），普通医生走个人接口
  const api = isAdmin.value ? reqUpdateDoctor : reqUpdateLoginD
  api(form.value).then((res: Res) => {
    if (res.code === 1) {
      modalView.value = false
      ElMessage.success('修改成功')
      reqGetLoginD().then(d => { if (d) doctor.value = d })
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  })
}
// 表单校验规则
const rules = ref<FormRules>({
  "doctorName": [
    {required: true, message: '请输入姓名', trigger: 'blur'},
  ],
  "doctorAge": [
    {required: true, message: '请选择出生日期', trigger: 'blur'},
  ],
  "doctorTel": [
    {pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur'},
  ],
})
// 头像参数的改变:子传父
const changePhoto = (val: string): void => {
  form.value.doctorPhoto = val
}

/**
 ┌───────────────────────────────────┐
 │=============重置个人密码============│
 └───────────────────────────────────┘
 */
const pwdModalView = ref(false)
const updatePwd = () => {
  pwdModalView.value = true;
  form2.password = "";
  form2.checkCode = "";
}
// 发送验证码的按钮数据
const mailBtn = reactive({btnTxt: "发送验证码", dis: false})
// 发送验证码邮件
const sendCheckMail = () => {
  mailBtn.dis = true;
  let countDown = 30;
  let intVal = setInterval(() => {
    if (countDown === 0) {
      clearInterval(intVal)
      mailBtn.dis = false;
      mailBtn.btnTxt = "发送验证码";
      return
    }
    mailBtn.btnTxt = "等待中(" + countDown + ")";
    countDown--;
  }, 1000)
  reqDoctorSendMail()
}
// 重置密码的数据
const form2 = reactive<DoctorResetPwdVo>({
  checkCode: "",
  password: ""
})
// 校验表单并提交
const myFormRef2 = ref<FormInstance>()
const formSubmit2 = async (): Promise<void> => {
  await myFormRef2.value?.validate((valid: boolean): void => {
    if (!valid) return
    reqDoctorResetPwd(form2).then((res: Res) => {
      if (res.code === 1) {
        pwdModalView.value = false
        form2.checkCode = ''
        form2.password = ''
      }
    })
  })
}
// 表单校验规则
const rules2 = ref<FormRules>({
  "checkCode": [
    {required: true, message: '请输入验证码', trigger: 'blur'},
  ],
  "password": [
    {required: true, message: '请输入新密码', trigger: 'blur'},
  ],
})

// 退出登录
const router = useRouter()
const doLogout = (): void => {
  ElMessageBox.confirm('确定退出登录吗？', '退出登录', {
    confirmButtonText: '确定退出', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqLogout().then(res => {
      clearPwdCheckFlag()
      if (res.code === 204) router.push('/login')
    })
  }).catch(() => ElMessage.info('已取消'))
}
</script>

<style scoped>
/* 顶部操作条 */
.top-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 24px;
  background: rgba(255,255,255,0.6);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(108,92,231,0.12);
}
.bar-title { font-size: 16px; font-weight: 700; color: #1F2D5A; }
.bar-actions { display: flex; gap: 10px; }

/* 页面容器 */
.profile-page {
  max-width: 1120px;
  margin: 8px auto 40px;
  padding: 0 20px;
}

/* 封面横幅：深色科技渐变 + 光斑 */
.banner {
  height: 190px;
  border-radius: 20px 20px 0 0;
  background:
      radial-gradient(420px 220px at 90% -20%, rgba(138,124,255,0.55), transparent 70%),
      radial-gradient(460px 240px at 8% 120%, rgba(64,151,255,0.4), transparent 70%),
      linear-gradient(135deg, #0F172A 0%, #1E2A5A 48%, #3B3B98 100%);
  position: relative;
  overflow: hidden;
}
.banner::after {
  content: "";
  position: absolute;
  right: -70px;
  top: -70px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.18);
  box-shadow: inset 0 0 60px rgba(108,92,231,0.35);
}
.banner::before {
  content: "";
  position: absolute;
  left: 16%;
  bottom: -90px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(64,151,255,0.35), rgba(108,92,231,0.25));
  filter: blur(6px);
}

/* 主体卡片：玻璃拟态 */
.profile-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(244,246,255,0.96));
  border: 1px solid rgba(108,92,231,0.16);
  border-radius: 0 0 20px 20px;
  box-shadow:
      0 24px 60px -20px rgba(30, 42, 90, 0.35),
      0 0 0 1px rgba(108,92,231,0.06) inset;
  padding: 0 34px 34px;
  overflow: hidden;
}

/* 头部 */
.card-head {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: -58px;
  position: relative;
  z-index: 2;
}
.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}
.avatar {
  border: 4px solid #fff;
  background: linear-gradient(135deg, #3A7BFF, #6C5CE7);
  color: #fff;
  font-size: 40px;
  box-shadow: 0 10px 26px rgba(108,92,231,0.4);
}
.level-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  border: 2px solid #fff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(0,0,0,0.15);
}
.level-admin { background: linear-gradient(135deg, #F26D78, #E5495F); }
.level-doc   { background: linear-gradient(135deg, #2BC48C, #17A36E); }

.head-info { flex: 1; padding-top: 78px; }
.name-row { display: flex; align-items: center; gap: 12px; }
.d-name { font-size: 28px; font-weight: 800; color: #16213E; letter-spacing: .4px; }
.account { margin-top: 8px; color: #8A93A6; font-size: 13px; font-variant-numeric: tabular-nums; }
.head-btns { margin-top: 16px; display: flex; gap: 10px; }

/* 信息网格：科技亮块 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  background:
      linear-gradient(135deg, rgba(58,123,255,0.06), rgba(108,92,231,0.06));
  border: 1px solid rgba(108,92,231,0.12);
  border-radius: 16px;
  padding: 20px 22px;
  margin-top: 30px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #fff;
  border: 1px solid rgba(108,92,231,0.1);
  border-radius: 12px;
  padding: 12px 14px;
  transition: transform .18s, box-shadow .18s;
}
.info-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(108,92,231,0.12);
}
.info-item .lbl { font-size: 12px; color: #9AA3B5; }
.info-item .val { font-size: 15px; font-weight: 650; color: #1F2D5A; word-break: break-all; }
.info-item .val.motto {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: normal;
}

/* 简介 */
.intro { margin-top: 28px; }
.intro-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 800;
  color: #1F2D5A;
  padding-bottom: 12px;
  border-bottom: 2px solid linear-gradient(90deg,#3A7BFF,#6C5CE7);
  border-image: linear-gradient(90deg,#3A7BFF,#6C5CE7) 1;
}
.intro-body {
  margin-top: 16px;
  height: 150px;
  overflow-y: auto;
  line-height: 1.9;
  color: #3D4663;
  font-size: 15px;
  white-space: pre-wrap;
  background: rgba(108,92,231,0.04);
  border-radius: 12px;
  padding: 16px 18px;
}

.perm-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  padding-top: 2px;
}

</style>