<template>
  <div>
    <!--操作框-->
    <BackOpCol role="宠物" @addRole="addRoleB" @delBatch="delBatchB" @query="sendQuery">
      <el-input v-model="qp.petName" placeholder="宠物姓名" size="large" @keyup.enter="sendQuery"/>
      <el-input v-model="qp.clientName" placeholder="用户姓名" size="large" @keyup.enter="sendQuery"/>
    </BackOpCol>
    <!--列表展示-->
    <el-table :data="petList.records"
              max-height="80vh"
              row-key="petId"
              stripe
              @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="30"/>
      <el-table-column label="图像">
        <template #default="scope">
          <MyAvatar :name="scope.row.petPhoto"/>
        </template>
      </el-table-column>
      <el-table-column label="宠物名" prop="petName"/>
      <el-table-column label="品种">
        <template #default="scope">
          <el-tag>{{ scope.row.petVariety }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="性别">
        <template #default="scope">
          <TagSex :sex="scope.row.petSex"/>
        </template>
      </el-table-column>
      <el-table-column label="绝育" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.petSterilization ? 'success' : 'info'">
            {{ scope.row.petSterilization ? '已绝育' : '未绝育' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生日" prop="petBirthday" sortable width="120"/>
      <el-table-column label="年龄" width="120">
        <template #default="scope">{{ getAge(scope.row.petBirthday, scope.row.petBirthdayEstimated) }}</template>
      </el-table-column>
      <el-table-column label="体重(kg)" width="100">
        <template #default="scope">{{ scope.row.petWeight ?? '--' }}</template>
      </el-table-column>
      <el-table-column label="毛色" width="100">
        <template #default="scope">{{ scope.row.petColor || '--' }}</template>
      </el-table-column>
      <el-table-column label="健康状态">
        <template #default="scope">
          <el-tag>{{ scope.row.petStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="近况" prop="petInfo"/>
      <el-table-column label="主人" prop="clientName"/>
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
    <BackPage :total="petList.total" @changePu="changePuB"/>
    <!--修改、新增时的模态框-->
    <el-dialog v-model="modalView" :title="modalTit" draggable width="60%" @open="onDialogOpen">
      <!--头像上传框-->
      <UpImg :photoNm="form.petPhoto" @upPhoto="changePhoto"/>
      <!--表单-->
      <el-form ref="myFormRef" :model="form" :rules="rules" label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="宠物姓名" prop="petName">
          <el-input v-model="form.petName" clearable placeholder="宠物姓名"/>
        </el-form-item>
        <el-form-item label="宠物品种" prop="petVariety">
          <el-input v-model="form.petVariety" clearable placeholder="宠物品种"/>
        </el-form-item>
        <el-form-item label="宠物性别">
          <el-radio-group v-model="form.petSex">
            <el-radio :label="true" border>公</el-radio>
            <el-radio :label="false" border>母</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
              v-model="form.petBirthday"
              format="YYYY-MM-DD"
              placeholder="选择出生日期"
              type="date"
              value-format="YYYY-MM-DD"
              @change="onBirthdayChange"
          />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input
              v-model="ageInput"
              clearable
              placeholder="例如：2岁3个月 / 3个月 / 45天 / 约2岁"
              @change="onAgeInput"
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">
            输入年龄将自动推算生日，前置"约"表示估算
          </div>
        </el-form-item>
        <el-form-item label="毛色">
          <el-input v-model="form.petColor" clearable placeholder="例如：橘白、纯黑"/>
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="form.petWeight" :min="0" :max="200" :precision="2" :step="0.1" controls-position="right"/>
        </el-form-item>

        <el-divider content-position="left">健康信息</el-divider>
        <el-form-item label="绝育情况">
          <el-radio-group v-model="form.petSterilization">
            <el-radio :label="true" border>已绝育</el-radio>
            <el-radio :label="false" border>未绝育</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="健康状况">
          <el-input v-model="form.petStatus" clearable placeholder="例如：健康、感冒中"/>
        </el-form-item>
        <el-form-item label="芯片号">
          <el-input v-model="form.petChip" clearable placeholder="例如：900123456789012"/>
        </el-form-item>
        <el-form-item label="过敏史">
          <el-input v-model="form.petAllergy" type="textarea" :rows="2" placeholder="例如：青霉素过敏"/>
        </el-form-item>
        <el-form-item label="既往病史">
          <el-input v-model="form.petHistory" type="textarea" :rows="2" placeholder="例如：2023年做过膀胱结石手术"/>
        </el-form-item>
        <el-form-item label="是否在世">
          <el-radio-group v-model="form.petAlive">
            <el-radio :label="true" border>在世</el-radio>
            <el-radio :label="false" border>离世</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">归属与备注</el-divider>
        <el-form-item label="宠物主人">
          <el-select v-model="form.clientId" filterable placeholder="选择用户">
            <el-option label="无" value=""/>
            <el-option v-for="c in clientNameL" :key="c.roleId" :label="c.roleName" :value="c.roleId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="宠物近况">
          <el-input v-model="form.petInfo" clearable placeholder="宠物近况"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.petNote" type="textarea" :rows="2" placeholder="前台随手记"/>
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
  </div>
</template>

<script lang="ts" setup>
import {Delete, Edit} from "@element-plus/icons-vue"
import {onMounted, reactive, ref} from 'vue'

const ageInput = ref<string>('')
const onDialogOpen = () => {
  ageInput.value = form.value.petBirthday ? getAge(form.value.petBirthday) : ''
}
const onBirthdayChange = () => {
  ageInput.value = form.value.petBirthday ? getAge(form.value.petBirthday) : ''
  form.value.petBirthdayEstimated = false
}
const onAgeInput = () => {
  if (!ageInput.value) return
  const r = parseAgeToBirthday(ageInput.value)
  if (r) {
    form.value.petBirthday = r.birthday
    form.value.petBirthdayEstimated = r.estimated
  }
}
import BackOpCol from "@/components/row/BackOpCol.vue";
import {PageQuery, YPetList} from "@/model/VO/BackQuery";
import {PetDto} from "@/model/DO/PetDto";
import {getAge, parseAgeToBirthday} from "@/utils/TimeUtil";
import {examplePet, Pet} from "@/model/entiy/Pet";
import TagSex from "@/components/show/TagSex.vue";
import BackPage from "@/components/row/BackPage.vue";
import {reqAddPet, reqDelPet, reqDelPetBatch, reqPetList, reqUpdatePet} from "@/request/PetApi";
import {Page} from "@/model/DO/Page";
import {NameDo} from "@/model/DO/NameDo";
import {reqClientName} from "@/request/ClientApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import UpImg from "@/components/UpImg.vue";
import {Res} from "@/request/Res";
import MyAvatar from "@/components/show/MyAvatar.vue";

/**
 ┌───────────────────────────────────┐
 │=============生命周期相关============│
 └───────────────────────────────────┘
 */
onMounted(() => {
  sendQuery()
  reqClientName().then(res => {
    clientNameL.value = res
  })
})

/**
 ┌───────────────────────────────────┐
 │=============表格查询相关============│
 └───────────────────────────────────┘
 */
// 查询的参数
const qp = reactive<YPetList>({
  petName: '',
  clientName: '',
  numPage: 1,
  pageSize: 6
})
// 列表展示
const petList = ref<Page<PetDto>>({records: [], total: 0})
// 分页条
const changePuB = (val: PageQuery) => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
// 数据总览
const sendQuery = (): void => {
  reqPetList(qp).then(res => {
    petList.value = res
  })
}

/**
 ┌───────────────────────────────────┐
 │=============数据删除相关============│
 └───────────────────────────────────┘
 */
// 多选与反选
const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: Pet[]): void => {
  roleIdList.value = val.map(obj => obj.petId)
}
// 批量删除
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm(
      '此处犹如寒冰之墓，飘落的雪消声无息。一群疲惫的动物在这冰冷的静寂里深深铭记，无尽等待，等待着凄凉的宿命落幕。',
      '删除多个确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqDelPetBatch(roleIdList.value).then(res => sureFlush(res))
  }).catch(() => {
    ElMessage.info('删除取消')
  })
}
// 删除单个
const delOne = (row: Pet): void => {
  ElMessageBox.confirm(
      `此地死寂如冰，落雪无声，一只憔悴的【${row.petName}】于静默中铭记与等待`,
      '删除单个确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqDelPet(row.petId).then(res => sureFlush(res))
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
const modalTit = ref<"新增宠物" | "修改宠物">("修改宠物")
// 新增
const addRoleB = (): void => {
  modalTit.value = "新增宠物"
  form.value = examplePet()
  myFormRef.value?.resetFields()
  modalView.value = true
}
// 修改
const showDialog = (row: Pet): void => {
  myFormRef.value?.clearValidate()
  modalTit.value = "修改宠物"
  // 赋值，防止响应式引用
  form.value.petId = row.petId
  form.value.petName = row.petName
  form.value.petVariety = row.petVariety
  form.value.petSex = row.petSex
  form.value.petBirthday = row.petBirthday
  form.value.petStatus = row.petStatus
  form.value.clientId = row.clientId
  form.value.petInfo = row.petInfo
  form.value.petPhoto = row.petPhoto
  form.value.petBirthdayEstimated = row.petBirthdayEstimated
  form.value.petColor = row.petColor
  form.value.petWeight = row.petWeight
  form.value.petSterilization = row.petSterilization
  form.value.petChip = row.petChip
  form.value.petAllergy = row.petAllergy
  form.value.petHistory = row.petHistory
  form.value.petAlive = row.petAlive
  form.value.petNote = row.petNote
  // 显示
  modalView.value = true
}

/**
 ┌───────────────────────────────────┐
 │=============表单校验相关============│
 └───────────────────────────────────┘
 */
// 表单的数据
const clientNameL = ref<NameDo[]>([]) // 下拉框用户名
const form = ref<Pet>(examplePet()) // 空的默认值
const myFormRef = ref<FormInstance>()
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      // 校验通过，提交
      modalView.value = false
      if (modalTit.value == "新增宠物") reqAddPet(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改宠物") reqUpdatePet(form.value).then(res => sureFlush(res))
      else ElMessageBox.alert('模块框出错')
    }
  })
}
// 表单校验规则
const rules = reactive<FormRules>({
  "petName": [
    {required: true, message: '请给宠物起一个名字', trigger: 'blur'},
  ],
  "petVariety": [
    {required: true, message: '请输入宠物的品种', trigger: 'blur'},
  ],
})
// 头像参数的改变:子传父
const changePhoto = (val: string): void => {
  form.value.petPhoto = val
}
</script>

<style scoped>
</style>
