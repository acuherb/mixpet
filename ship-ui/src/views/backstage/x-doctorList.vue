<template>
  <!--操作框-->
  <BackOpCol role="医生" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.doctorName" clearable placeholder="医生姓名 / 手机号 / 账号" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-input v-model="qp.departmentName" clearable placeholder="科室名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <!-- 注册号缺失筛选：一键筛出未维护执业注册号的医生（处方笺必需项） -->
    <el-checkbox v-model="qp.certNull" label="注册号缺失" size="large" border @change="nowQuery"/>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="doctorList.records"
            max-height="calc(100vh - 130px)"
            row-key="doctorId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="图像">
      <template #default="scope">
        <MyAvatar :name="scope.row.doctorPhoto"/>
      </template>
    </el-table-column>
    <el-table-column label="医生名" prop="doctorName"/>
    <el-table-column label="帐号" prop="username"/>
    <el-table-column label="性别">
      <template #default="scope">
        <TagSex :sex="scope.row.doctorGender"/>
      </template>
    </el-table-column>
    <el-table-column label="年龄" prop="doctorAge" sortable>
      <template #default="scope">{{ getAge(scope.row.doctorAge)}}</template>
    </el-table-column>
    <el-table-column label="电话" prop="doctorTel"/>
    <el-table-column label="执业注册号">
      <template #default="scope">
        <span v-if="scope.row.doctorCert">{{ scope.row.doctorCert }}</span>
        <span v-else style="color:#f56c6c;font-weight:600">未填</span>
      </template>
    </el-table-column>
    <el-table-column label="邮箱" prop="doctorEmail"/>
    <el-table-column label="简介" prop="doctorInfo"/>
    <el-table-column label="科室">
      <template #default="scope">
        <div class="depart-tags">
          <el-tag v-if="scope.row.departmentNames && scope.row.departmentNames.length" v-for="(nm,i) in scope.row.departmentNames" :key="i" size="small" class="depart-tag">{{ nm }}</el-tag>
          <el-tag v-else>{{ scope.row.departmentName }}</el-tag>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="职称" prop="jobName">
      <template #default="scope">
        <el-tag v-if="scope.row.jobName" size="small" effect="plain" type="info">{{ scope.row.jobName }}</el-tag>
        <span v-else>—</span>
      </template>
    </el-table-column>
    <el-table-column label="权限" prop="authLv" sortable width="110">
      <template #default="scope">
        <TagJob :auth-lv="scope.row.authLv"/>
      </template>
    </el-table-column>
    <el-table-column label="工龄" prop="createTime" sortable>
      <template #default="scope">{{ getDaysFromToday(scope.row.createTime) }}</template>
    </el-table-column>
    <el-table-column fixed="right" label="操作">
      <template #default="scope">
        <el-button-group>
          <el-button :icon="Edit" circle type="warning" @click="showDialog(scope.row)"/>
          <el-button :icon="Delete" circle type="danger" @click="delOne(scope.row)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="doctorList.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="60%">
    <!--头像上传框-->
    <UpImg :photoNm="form.doctorPhoto" @upPhoto="changePhoto"/>
    <!--表单-->
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="帐号" prop="username">
        <el-input v-model="form.username" clearable placeholder="医生帐号"/>
      </el-form-item>
      <el-form-item label="姓名" prop="doctorName">
        <el-input v-model="form.doctorName" clearable placeholder="医生姓名"/>
      </el-form-item>
      <el-form-item label="科室" prop="departmentIds">
        <div class="depart-select">
          <el-select v-model="form.departmentIds" multiple filterable collapse-tags placeholder="选一个或多个科室（可全选=全科）" style="width:100%">
            <el-option v-for="c in departNameL" :key="c.roleId" :label="c.roleName" :value="c.roleId"/>
          </el-select>
          <el-button v-if="!isAllDepart" size="small" type="primary" plain @click="selectAllDepart">全科（全选）</el-button>
          <el-button v-else size="small" type="info" plain @click="clearAllDepart">取消全选</el-button>
        </div>
      </el-form-item>
      <el-form-item label="权限" prop="authLv">
        <el-select v-model="form.authLv" placeholder="选择权限">
          <el-option v-for="p in PERMISSIONS" :key="p.lv" :value="p.lv" :label="p.name"/>
        </el-select>
        <div class="perm-desc">{{ permDesc(form.authLv) }}</div>
      </el-form-item>
      <el-form-item label="职称" prop="jobId">
        <el-select v-model="form.jobId" filterable placeholder="选择职称（在职务/职称管理维护）">
          <el-option v-for="j in jobNameL" :key="j.roleId" :label="j.roleName" :value="j.roleId"/>
        </el-select>
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
      <el-form-item label="简介">
        <el-input v-model="form.doctorInfo" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="医生简介"/>
      </el-form-item>
      <el-form-item label="执业资格证号">
        <el-input v-model="form.doctorCert" clearable maxlength="64" placeholder="执业兽医资格证号（用于国标处方笺）"/>
      </el-form-item>
      <el-form-item v-if="modalTit==='新增医生'" label="密码">
        <el-input v-model="form.doctorPassword" clearable placeholder="123456"/>
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
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {Delete, Edit} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import UpImg from "@/components/UpImg.vue";
import {PageQuery, YDoctorList} from "@/model/VO/BackQuery";
import {getAge, getDaysFromToday} from "@/utils/TimeUtil";
import {DoctorDto} from "@/model/DO/DoctorDto";
import TagSex from "@/components/show/TagSex.vue";
import {Page} from "@/model/DO/Page";
import {reqAddDoctor, reqDelDoctor, reqDelDoctorBatch, reqDoctorList, reqUpdateDoctor} from "@/request/DoctorApi";
import {Res} from "@/request/Res";
import {Doctor} from "@/model/entiy/Doctor";
import {exampleDoctorOne} from "@/model/DO/DoctorDto";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {reqDepartName} from "@/request/DepartApi";
import {NameDo} from "@/model/DO/NameDo";
import MyAvatar from "@/components/show/MyAvatar.vue";
import TagJob from "@/components/show/TagJob.vue";
import {PERMISSIONS, permDesc} from "@/utils/PermissionUtil";
import {reqJobName} from "@/request/JobApi";

/**
 ┌───────────────────────────────────┐
 │=============生命周期相关============│
 └───────────────────────────────────┘
 */
onMounted(() => {
  sendQuery()
  reqDepartName().then(res => {
    departNameL.value = res
  })
  reqJobName().then(res => {
    jobNameL.value = res
  })
})

/**
 ┌───────────────────────────────────┐
 │=============表格查询相关============│
 └───────────────────────────────────┘
 */
// 查询的参数
const qp: YDoctorList = reactive({
  doctorName: '',
  departmentName: '',
  certNull: false,
  numPage: 1,
  pageSize: 6
})
// 列表展示
const doctorList = ref<Page<DoctorDto>>({records: [], total: 0})
// 分页条
const changePuB = (val: PageQuery) => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
// 数据总览
const sendQuery = (): void => {
  reqDoctorList(qp).then(res => {
    doctorList.value = res
  })
}
// 搜索：输入即查（防抖），查询回到第一页
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

/**
 ┌───────────────────────────────────┐
 │=============数据删除相关============│
 └───────────────────────────────────┘
 */
// 多选与反选
const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: Doctor[]): void => {
  roleIdList.value = val.map(obj => obj.doctorId)
}
// 批量删除
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm(
      '这些被选中者，将迎来一场浩大而又凄美的结局。这是对他们选择和坚持的回报，是既悲痛又美丽的终结。',
      '删除多个确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqDelDoctorBatch(roleIdList.value).then(res => sureFlush(res));
  }).catch(() => {
    ElMessage.info('删除取消')
  })
}
// 删除单个
const delOne = (row: Doctor): void => {
  ElMessageBox.confirm(
      `【${row.doctorName}】已无路可退，只能接受命运的裁决。`,
      '删除单个确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqDelDoctor(row.doctorId).then(res => sureFlush(res))
  }).catch(() => {
    ElMessage.info('删除取消')
  })
}
// 确定请求的返回值，然后刷新
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}

/**
 ┌───────────────────────────────────┐
 │=============新增修改按钮============│
 └───────────────────────────────────┘
 */
// 模态框
const modalView = ref(false)
const modalTit = ref<"新增医生" | "修改医生">("修改医生")
// 新增
const addRoleB = (): void => {
  form.value = exampleDoctorOne()
  myFormRef.value?.resetFields()
  modalTit.value = "新增医生"
  modalView.value = true
}
// 修改时展示模态框
const showDialog = (row: DoctorDto) => {
  myFormRef.value?.clearValidate()
  // 赋值，防止响应式引用
  form.value.doctorId = row.doctorId
  form.value.departmentId = row.departmentId
  form.value.departmentIds = row.departmentIds ? [...row.departmentIds] : []
  form.value.authLv = row.authLv
  form.value.jobId = row.jobId ?? null
  form.value.doctorAge = row.doctorAge
  form.value.doctorInfo = row.doctorInfo
  form.value.doctorTel = row.doctorTel
  form.value.doctorEmail = row.doctorEmail
  form.value.doctorPhoto = row.doctorPhoto
  form.value.doctorName = row.doctorName
  form.value.doctorGender = row.doctorGender
  form.value.username = row.username
  form.value.doctorCert = row.doctorCert
  // 显示
  modalView.value = true
  modalTit.value = "修改医生"
}

/**
 ┌───────────────────────────────────┐
 │=============表单校验相关============│
 └───────────────────────────────────┘
 */
// 表单的数据
const departNameL = ref<NameDo[]>([]) // 下拉框部门名称
const jobNameL = ref<NameDo[]>([]) // 下拉框职称
// 是否已选择全部科室
const isAllDepart = () => !!(departNameL.value.length > 0 && form.value.departmentIds && form.value.departmentIds.length === departNameL.value.length)
// 全科：选中全部科室
const selectAllDepart = (): void => {
  form.value.departmentIds = departNameL.value.map(c => c.roleId)
}
// 取消全选
const clearAllDepart = (): void => {
  form.value.departmentIds = []
}
// 手机号只保留数字，最多 11 位
const sanitizeTel = () => {
  if (form.value.doctorTel) {
    form.value.doctorTel = form.value.doctorTel.replace(/\D/g, '').slice(0, 11)
  }
}

const form = ref<DoctorDto>(exampleDoctorOne()) // 空的默认值
const myFormRef = ref<FormInstance>()
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      // 校验通过，提交
      modalView.value = false
      if (modalTit.value == "新增医生") reqAddDoctor(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改医生") reqUpdateDoctor(form.value).then(res => sureFlush(res))
      else ElMessageBox.alert('模块框出错')
    }
  })
}
// 表单校验规则
const rules = reactive<FormRules>({
  "doctorTel": [
    {pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur'},
  ],
  "username": [
    {required: true, message: '帐号不能为空', trigger: 'blur'},
  ],
  "doctorName": [
    {required: true, message: '请输入姓名', trigger: 'blur'},
  ],
  "doctorAge": [
    {required: true, message: '请选择出生日期', trigger: 'blur'},
  ],
  "departmentIds": [
    {required: true, type: 'array', min: 1, message: '请至少选择一个科室', trigger: 'change'},
  ],
  "jobId": [
    {required: true, type: 'number', message: '请选择职称', trigger: 'change'},
  ],
})
// 头像参数的改变:子传父
const changePhoto = (val: string): void => {
  form.value.doctorPhoto = val
}
</script>

<style scoped>
.sel-tip {
  margin-right: 12px;
  font-size: 14px;
  color: #409eff;
}
.sel-tip b {
  font-size: 16px;
  margin: 0 2px;
}
.depart-select {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.depart-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.depart-tag {
  margin-right: 4px;
}
.perm-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  padding-top: 2px;
}

</style>