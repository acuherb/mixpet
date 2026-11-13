<template>
  <div class="list-page">
    <!--操作框-->
    <BackOpCol :can-delete="isManager" role="宠物" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
      <el-input v-model="qp.petName" clearable placeholder="宠物名 / 品种 / 芯片号" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
      <el-input v-model="qp.clientName" clearable placeholder="主人姓名 / 手机号" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
      <template #left>
        <span v-if="roleIdList.length > 0" class="sel-tip">
          已选 <b>{{ roleIdList.length }}</b> 项
        </span>
      </template>
    </BackOpCol>
    <!--列表展示-->
    <el-table :data="petList.records"
              :row-style="rowStyle"
              max-height="calc(100vh - 130px)"
              row-key="petId"
              stripe
              @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="30"/>
      <el-table-column label="图像" width="60">
        <template #default="scope">
          <MyAvatar :name="scope.row.petPhoto"/>
        </template>
      </el-table-column>
      <el-table-column label="宠物名" prop="petName" width="100" show-overflow-tooltip/>
      <el-table-column label="品种" width="80">
        <template #default="scope">
          <el-tag>{{ scope.row.petVariety }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="性别" width="60">
        <template #default="scope">
          <TagSex :sex="scope.row.petSex"/>
        </template>
      </el-table-column>
      <el-table-column label="绝育" width="70">
        <template #default="scope">
          <el-tag :type="scope.row.petSterilization ? 'success' : 'info'">
            {{ scope.row.petSterilization ? '已绝育' : '未绝育' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生日" prop="petBirthday" sortable width="100"/>
      <el-table-column label="年龄" width="80">
        <template #default="scope">{{ getAge(scope.row.petBirthday, scope.row.petBirthdayEstimated) }}</template>
      </el-table-column>
      <el-table-column label="体重(kg)" width="80">
        <template #default="scope">{{ scope.row.petWeight ?? '--' }}</template>
      </el-table-column>
      <el-table-column label="毛色" width="80">
        <template #default="scope">{{ scope.row.petColor || '--' }}</template>
      </el-table-column>
      <el-table-column label="健康状态" width="90">
        <template #default="scope">
          <el-tag>{{ scope.row.petStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="近况" prop="petInfo" min-width="100" show-overflow-tooltip/>
      <el-table-column label="主人" prop="clientName" width="80" show-overflow-tooltip/>
      <el-table-column label="状态" width="86">
        <template #default="scope">
          <el-tag v-if="scope.row.active === false && scope.row.petSource === 1" type="warning">待审核</el-tag>
          <el-tag v-else-if="scope.row.active === false" type="info">已停用</el-tag>
          <el-tag v-else type="success">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="240">
        <template #default="scope">
          <el-button-group>
            <el-button :icon="Edit" circle type="warning" title="编辑" @click="showDialog(scope.row)"/>
            <el-button v-if="isManager && scope.row.clientId" circle type="primary" title="过户给新主人" @click="openTransfer(scope.row)">过</el-button>
            <el-button v-if="isManager && scope.row.clientId" circle type="warning" title="入栏（有主变无主，可被领养）" @click="intakePet(scope.row)">栏</el-button>
            <el-button circle type="info" title="查看过户历史" @click="openHistory(scope.row)">档</el-button>
            <el-button circle type="success" title="完整就诊档案：病历/处方/免疫/住院/手术/化验/影像" @click="openHealth(scope.row)">康</el-button>
            <template v-if="isManager">
              <el-button v-if="scope.row.active !== false"
                         :icon="Delete" circle type="danger" title="删除/停用" @click="delOne(scope.row)"/>
              <template v-else-if="scope.row.petSource === 1">
                <!-- 顾客自建宠物待审核：可审核通过，也可驳回删除 -->
                <el-button :icon="Refresh" circle type="success" title="审核通过" @click="restorePet(scope.row)"/>
                <el-button :icon="Delete" circle type="danger" title="驳回（删除该宠物）" @click="delOne(scope.row)"/>
              </template>
              <el-button v-else :icon="Refresh" circle type="success" title="恢复启用" @click="restorePet(scope.row)"/>
            </template>
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
          <div v-if="modalTit==='修改宠物'" class="readonly-field">
            <span>{{ currentOwnerName }}</span>
            <span class="tip">如需更换主人，请用列表中的「过户」按钮</span>
          </div>
          <div v-else style="display:flex; gap:8px; width:100%;">
            <el-select v-model="form.clientId" filterable placeholder="选择用户" style="flex:1;">
              <el-option label="无（暂未认领）" :value="null"/>
              <el-option v-for="c in clientNameL" :key="c.roleId" :label="c.roleName" :value="c.roleId"/>
            </el-select>
            <el-button type="primary" plain @click="openQuickClient">＋新建主人</el-button>
          </div>
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

    <!-- 过户弹窗 -->
    <el-dialog v-model="transferView" width="620px" :show-close="false"
               :close-on-click-modal="false" class="transfer-dialog">
      <template #header>
        <div class="tr-header">
          <div class="tr-icon-wrap">
            <span class="tr-icon">🔄</span>
          </div>
          <div class="tr-title">
            <h3>宠物过户</h3>
            <p>将此宠物的归属权转移给其他主人 · 历史就诊记录仍算原主人</p>
          </div>
          <div class="tr-close" @click="transferView = false">✕</div>
        </div>
      </template>

      <!-- 流程可视化 -->
      <div v-if="transferRow" class="tr-flow">
        <div class="tr-party">
          <div class="tr-avatar-wrap">
            <img :src="'/api/common/download?name=' + transferRow.petPhoto" class="tr-avatar" alt="">
          </div>
          <div class="tr-party-name">{{ getOwnerName(transferRow.clientId) }}</div>
          <div class="tr-party-role">原主人</div>
        </div>
        <div class="tr-arrow">
          <div class="tr-line"></div>
          <div class="tr-arrow-icon">→</div>
        </div>
        <div class="tr-party">
          <div class="tr-avatar-wrap">
            <div v-if="!transferForm.toClientId" class="tr-avatar tr-avatar-empty">?</div>
            <div v-else class="tr-avatar tr-avatar-new">
              {{ getOwnerName(transferForm.toClientId).charAt(0) }}
            </div>
          </div>
          <div class="tr-party-name" :class="{ muted: !transferForm.toClientId }">
            {{ transferForm.toClientId ? getOwnerName(transferForm.toClientId) : '待选择' }}
          </div>
          <div class="tr-party-role">新主人</div>
        </div>
      </div>

      <div v-if="transferRow" class="tr-pet-meta">
        <span class="meta-name">{{ transferRow.petName }}</span>
        <span class="dot">·</span>
        <span>{{ transferRow.petVariety }}</span>
        <span class="dot">·</span>
        <span>{{ transferRow.petSex ? '公' : '母' }}</span>
      </div>

      <el-form label-position="top" class="tr-form">
        <el-form-item label="新主人">
          <el-select v-model="transferForm.toClientId" filterable placeholder="搜索并选择新主人"
                     size="large" style="width:100%">
            <el-option
                v-for="c in clientNameL.filter(x => x.roleId !== transferRow?.clientId)"
                :key="c.roleId"
                :label="c.roleName"
                :value="c.roleId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="过户时间">
          <el-date-picker
              v-model="transferForm.transferDate"
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="默认当前时间"
              size="large"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width:100%"
          />
        </el-form-item>
        <el-form-item label="过户原因（可选）">
          <el-input v-model="transferForm.reason" type="textarea" :rows="3"
                    maxlength="200" show-word-limit
                    placeholder="例如：转让、赠送、家人变更"/>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="tr-footer">
          <el-button size="large" @click="transferView = false">取消</el-button>
          <el-button size="large" type="primary" @click="confirmTransfer">确认过户</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 快速新建主人弹窗 -->
    <el-dialog v-model="quickClientView" title="新建主人" width="420px" draggable
               :close-on-click-modal="false">
      <el-form ref="quickFormRef" :model="quickForm" :rules="quickRules" label-width="80px">
        <el-form-item label="手机号" prop="clientTel">
          <el-input v-model="quickForm.clientTel" maxlength="11" inputmode="numeric"
                    @input="sanitizeQuickTel" placeholder="11位手机号（将作为登录账号）"/>
        </el-form-item>
        <el-form-item label="姓名" prop="clientName">
          <el-input v-model="quickForm.clientName" placeholder="主人姓名"/>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="quickForm.clientGender">
            <el-radio :label="true" border>男</el-radio>
            <el-radio :label="false" border>女</el-radio>
          </el-radio-group>
        </el-form-item>
        <div class="quick-tip">
          初始密码 123456，请告知客户首次登录后修改
        </div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="quickClientView = false">取消</el-button>
          <el-button type="primary" @click="confirmQuickClient">创建并选中</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 过户历史弹窗 -->
    <el-dialog v-model="historyView" width="640px" :show-close="false"
               :close-on-click-modal="false" class="history-dialog">
      <template #header>
        <div class="his-header">
          <div class="his-icon-wrap"><span>📜</span></div>
          <div class="his-title">
            <h3>{{ historyPet ? historyPet.petName : '' }} · 宠物档案</h3>
            <p>查看该宠物的过户历史记录（未来会加入免疫、就诊等记录）</p>
          </div>
          <div class="his-close" @click="historyView = false">✕</div>
        </div>
      </template>

      <div v-if="historyList.length === 0" class="his-empty">
        <div class="his-empty-icon">📭</div>
        <div>这只宠物暂无过户记录</div>
      </div>
      <div v-else class="his-timeline">
        <div v-for="(h, i) in historyList" :key="h.transferId" class="his-item">
          <div class="his-dot"></div>
          <div class="his-line" v-if="i < historyList.length - 1"></div>
          <div class="his-body">
            <div class="his-time">{{ h.transferDate }}</div>
            <div class="his-flow">
              <span class="his-from">{{ getOwnerName(h.fromClientId) }}</span>
              <span class="his-arrow">→</span>
              <span class="his-to">{{ getOwnerName(h.toClientId) }}</span>
            </div>
            <div v-if="h.reason" class="his-reason">原因：{{ h.reason }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <div style="text-align:right;">
          <el-button size="large" @click="historyView = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 一页式健康档案 -->
    <PetHealthDialog v-model="healthView" :pet-id="healthPetId"/>
  </div>
</template>

<script lang="ts" setup>

import {Delete, Edit, Refresh} from "@element-plus/icons-vue"
import {computed, onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'

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
import {canManage, useAuthLv} from '@/utils/PermissionUtil'

const authLv = useAuthLv()
const isManager = computed(() => canManage(authLv.value))
import {debounce} from "@/utils/DebounceUtil";
import {PageQuery, YPetList} from "@/model/VO/BackQuery";
import {PetDto} from "@/model/DO/PetDto";
import {getAge, parseAgeToBirthday} from "@/utils/TimeUtil";
import {examplePet, Pet} from "@/model/entiy/Pet";
import TagSex from "@/components/show/TagSex.vue";
import BackPage from "@/components/row/BackPage.vue";
import {reqAddPet, reqDelPet, reqActivatePet, reqDelPetBatch, reqIntakePet, reqPetList, reqUpdatePet, reqTransferPet, reqTransferHistory} from "@/request/PetApi";
import {reqAddClient} from "@/request/ClientApi";
import {Page} from "@/model/DO/Page";
import {NameDo} from "@/model/DO/NameDo";
import {reqClientName} from "@/request/ClientApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import UpImg from "@/components/UpImg.vue";
import PetHealthDialog from "@/components/PetHealthDialog.vue";
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
  pageSize: 20
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
const handleSelectionChange = (val: Pet[]): void => {
  roleIdList.value = val.map(obj => obj.petId)
}
// 批量删除
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm(
      `确定删除选中的 ${roleIdList.value.length} 只宠物吗？有就诊/寄养/领养/过户记录的不删除，仅改为停用。`,
      '批量删除确认',
      {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelPetBatch(roleIdList.value).then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '操作完成')
        sendQuery()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    })
  }).catch(() => ElMessage.info('已取消'))
}
// 删除单个
const delOne = (row: Pet): void => {
  ElMessageBox.confirm(
      `确定删除【${row.petName}】吗？有就诊/寄养/领养/过户记录的不删除，仅改为停用。`,
      '删除确认',
      {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelPet(row.petId).then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '操作完成')
        sendQuery()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    })
  }).catch(() => ElMessage.info('已取消'))
}

// 恢复被停用的宠物
const restorePet = (row: Pet): void => {
  ElMessageBox.confirm(
      `恢复【${row.petName}】到正常列表吗？`,
      '恢复宠物',
      {confirmButtonText: '恢复', cancelButtonText: '取消', type: 'info'}
  ).then(() => {
    reqActivatePet(row.petId).then(res => {
      if (res.code === 1) {
        ElMessage.success('已恢复')
        sendQuery()
      } else {
        ElMessage.error(res.msg || '恢复失败')
      }
    })
  }).catch(() => ElMessage.info('已取消'))
}

// 宠物入栏（有主变无主：主人遗弃/托管）
const intakePet = (row: Pet): void => {
  ElMessageBox.prompt(
      `确定把【${row.petName}】入栏吗？入栏后宠物变为无主，可被领养。`,
      '宠物入栏',
      {
        confirmButtonText: '入栏', cancelButtonText: '取消', type: 'warning',
        inputValue: '主人托管', inputPlaceholder: '入栏原因（如：主人遗弃 / 主人托管）'
      }
  ).then(({value}) => {
    reqIntakePet(row.petId, value || '主人托管').then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '已入栏')
        sendQuery()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    })
  }).catch(() => ElMessage.info('已取消'))
}

// 停用行的样式
const rowStyle = ({row}: { row: Pet }) => {
  return row.active === false ? {opacity: 0.5, background: '#fafafa'} : {}
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
      else ElMessageBox.alert('操作未完成，请检查填写内容后重试')
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
// ============ 档案 / 历史 ============
const historyView = ref(false)
const historyPet = ref<Pet | null>(null)
const historyList = ref<any[]>([])

const openHistory = (row: Pet): void => {
  historyPet.value = row
  reqTransferHistory(row.petId).then(list => {
    historyList.value = list || []
    historyView.value = true
  })
}

// ============ 一页式健康档案 ============
const healthView = ref(false)
const healthPetId = ref<number | null>(null)
// 完整就诊档案页（z-petArchive）：时间线聚合病历/处方/免疫/住院/手术/化验/影像
const router = useRouter()
const openHealth = (row: Pet): void => {
  router.push({path: '/backstage/z-petArchive', query: {petId: row.petId}})
}

// ============ 过户相关 ============
const transferView = ref(false)
const transferRow = ref<Pet | null>(null)
const transferForm = reactive({
  toClientId: null as number | null,
  transferDate: "",
  reason: ""
})

const getOwnerName = (clientId: number | null): string => {
  if (clientId == null) return "无主"
  const c = clientNameL.value.find(x => x.roleId === clientId)
  return c ? c.roleName : "未知"
}

const currentOwnerName = computed(() => {
  if (form.value.clientId == null) return "无主"
  return getOwnerName(form.value.clientId)
})

const openTransfer = (row: Pet): void => {
  transferRow.value = row
  transferForm.toClientId = null
  const n = new Date()
  const pad = (x: number) => String(x).padStart(2, "0")
  transferForm.transferDate =
      n.getFullYear() + "-" + pad(n.getMonth() + 1) + "-" + pad(n.getDate()) +
      " " + pad(n.getHours()) + ":" + pad(n.getMinutes()) + ":00"
  transferForm.reason = ""
  transferView.value = true
}

const confirmTransfer = (): void => {
  if (!transferForm.toClientId) {
    ElMessage.warning("请选择新主人")
    return
  }
  if (!transferRow.value) return
  reqTransferPet({
    petId: transferRow.value.petId,
    toClientId: transferForm.toClientId,
    transferDate: transferForm.transferDate,
    reason: transferForm.reason
  }).then(res => {
    if (res.code === 1) {
      ElMessage.success("过户成功")
      transferView.value = false
      sendQuery()
    } else {
      ElMessage.error(res.msg || "过户失败")
    }
  })
}

// ============ 快速新建主人 ============
const quickClientView = ref(false)
const quickFormRef = ref<FormInstance>()
const quickForm = reactive({
  clientTel: "",
  clientName: "",
  clientGender: true as boolean
})
const quickRules: FormRules = {
  clientTel: [
    {required: true, message: "请输入手机号", trigger: "blur"},
    {pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur"}
  ],
  clientName: [
    {required: true, message: "请输入姓名", trigger: "blur"}
  ]
}

const sanitizeQuickTel = (): void => {
  quickForm.clientTel = quickForm.clientTel.replace(/\D/g, "").slice(0, 11)
}

const openQuickClient = (): void => {
  quickForm.clientTel = ""
  quickForm.clientName = ""
  quickForm.clientGender = true
  quickClientView.value = true
}

const confirmQuickClient = async (): Promise<void> => {
  try {
    await quickFormRef.value?.validate()
  } catch {
    return
  }
  const data = {
    clientId: 0,
    clientUsername: quickForm.clientTel,
    clientName: quickForm.clientName,
    clientGender: quickForm.clientGender,
    clientTel: quickForm.clientTel
  } as any
  reqAddClient(data).then(res => {
    if (res.code === 1) {
      ElMessage.success("主人创建成功，密码 123456")
      reqClientName().then(list => {
        clientNameL.value = list
        const newC = list.find(c => c.roleName === quickForm.clientName)
        if (newC) form.value.clientId = newC.roleId
      })
      quickClientView.value = false
    } else {
      ElMessage.error(res.msg || "创建失败")
    }
  })
}

// 头像参数的改变:子传父
const changePhoto = (val: string): void => {
  form.value.petPhoto = val
}
</script>

<style lang="scss" scoped>
.sel-tip {
  margin-right: 12px;
  font-size: 14px;
  color: #409eff;
}
.sel-tip b {
  font-size: 16px;
  margin: 0 2px;
}

.list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.list-page :deep(.el-table) {
  flex: 1;
  min-height: 0;
}

/* 过户弹窗 */
.transfer-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}
.transfer-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}
.transfer-dialog :deep(.el-dialog__footer) {
  padding: 12px 24px 20px;
  border-top: 1px solid #F2F4F8;
}

.tr-header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 24px 24px 16px;
  background: linear-gradient(135deg, #F0F7FD 0%, #EAF3FA 100%);
  border-bottom: 1px solid #E4E7ED;
  position: relative;

  .tr-icon {
    font-size: 28px;
    line-height: 1;
    flex-shrink: 0;
  }

  .tr-title {
    flex: 1;

    h3 {
      margin: 0 0 4px;
      font-size: 17px;
      font-weight: 600;
      color: #1F2D3D;
    }

    p {
      margin: 0;
      font-size: 12px;
      color: #909399;
      line-height: 1.5;
    }
  }

  .tr-close {
    position: absolute;
    top: 12px;
    right: 16px;
    width: 26px;
    height: 26px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: #909399;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.15s;

    &:hover {
      background: #fff;
      color: #1F2D3D;
    }
  }
}

.tr-pet-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #F8FBFF;
  border: 1px solid #E4E7ED;
  border-radius: 10px;
  margin-bottom: 20px;
}

.tr-pet-img {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  background: #fff;
  flex-shrink: 0;
}

.tr-pet-body {
  flex: 1;
  min-width: 0;
}

.tr-pet-name {
  font-size: 16px;
  font-weight: 600;
  color: #1F2D3D;
  margin-bottom: 4px;
}

.tr-pet-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;

  .dot {
    margin: 0 6px;
    color: #C0C4CC;
  }
}

.tr-flow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;

  .from {
    color: #606266;
    font-weight: 500;
  }

  .arrow {
    color: #1E6FA8;
    font-size: 16px;
    font-weight: 700;
  }

  .to {
    color: #B0B3B8;
    font-weight: 500;

    &.active {
      color: #1E6FA8;
    }
  }
}

.tr-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
  padding-bottom: 4px;
}

.tr-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}


.readonly-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  font-size: 14px;
  color: #1F2D3D;

  .tip {
    font-size: 12px;
    color: #909399;
  }
}

.transfer-tip {
  padding: 12px 16px;
  background: #F0F7FD;
  border-radius: 8px;
  font-size: 13px;
  color: #1F2D3D;
  margin-bottom: 16px;
  line-height: 1.6;

  b {
    color: #1E6FA8;
  }
}

.quick-tip {
  padding: 8px 12px;
  background: #FEF7E8;
  border-radius: 6px;
  font-size: 12px;
  color: #B8860B;
  margin-top: 4px;
}


/* ========== 过户弹窗 v2 ========== */
.transfer-dialog {
  border-radius: 16px !important;
  overflow: hidden;
}

.transfer-dialog :deep(.el-dialog__header) {
  padding: 0 !important;
  margin: 0 !important;
}

.transfer-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
}

.transfer-dialog :deep(.el-dialog__footer) {
  padding: 16px 28px 24px !important;
  border-top: 1px solid #F2F4F8;
  background: #FAFBFC;
}

.transfer-dialog .tr-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 28px;
  background: linear-gradient(135deg, #1E6FA8 0%, #2A82C2 100%);
  color: #fff;
  position: relative;
}

.transfer-dialog .tr-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.transfer-dialog .tr-icon {
  font-size: 22px;
  line-height: 1;
}

.transfer-dialog .tr-title {
  flex: 1;
}

.transfer-dialog .tr-title h3 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.transfer-dialog .tr-title p {
  margin: 0;
  font-size: 12px;
  opacity: 0.85;
}

.transfer-dialog .tr-close {
  position: absolute;
  top: 18px;
  right: 20px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  transition: all 0.15s;
}

.transfer-dialog .tr-close:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.transfer-dialog .tr-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 28px 28px 20px;
  background: linear-gradient(180deg, #F8FBFF 0%, #FFFFFF 100%);
}

.transfer-dialog .tr-party {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 100px;
}

.transfer-dialog .tr-avatar-wrap {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  background: #EAF3FA;
  border: 3px solid #fff;
  box-shadow: 0 4px 12px rgba(30, 111, 168, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.transfer-dialog .tr-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 600;
  color: #fff;
  background: #B0B3B8;
}

.transfer-dialog .tr-avatar-empty {
  background: #E4E7ED;
  color: #B0B3B8;
}

.transfer-dialog .tr-avatar-new {
  background: linear-gradient(135deg, #1E6FA8 0%, #7BD1A8 100%);
}

.transfer-dialog .tr-party-name {
  font-size: 14px;
  font-weight: 600;
  color: #1F2D3D;
  margin-top: 4px;
}

.transfer-dialog .tr-party-name.muted {
  color: #B0B3B8;
  font-weight: 400;
}

.transfer-dialog .tr-party-role {
  font-size: 11px;
  color: #909399;
  letter-spacing: 1px;
}

.transfer-dialog .tr-arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.transfer-dialog .tr-line {
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, #1E6FA8 0%, #7BD1A8 100%);
  border-radius: 1px;
}

.transfer-dialog .tr-arrow-icon {
  font-size: 20px;
  color: #1E6FA8;
  font-weight: 700;
  line-height: 1;
}

.transfer-dialog .tr-pet-meta {
  text-align: center;
  font-size: 13px;
  color: #606266;
  padding-bottom: 20px;
  background: #fff;
}

.transfer-dialog .tr-pet-meta .meta-name {
  font-weight: 600;
  color: #1F2D3D;
}

.transfer-dialog .tr-pet-meta .dot {
  margin: 0 8px;
  color: #C0C4CC;
}

.transfer-dialog .tr-form {
  padding: 0 28px 8px;
}

.transfer-dialog .tr-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
  padding-bottom: 6px;
}

.transfer-dialog .tr-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* ========== 历史弹窗 ========== */
.history-dialog {
  border-radius: 16px !important;
  overflow: hidden;
}

.history-dialog :deep(.el-dialog__header) {
  padding: 0 !important;
  margin: 0 !important;
}

.history-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
  max-height: 500px;
  overflow-y: auto;
}

.history-dialog :deep(.el-dialog__footer) {
  padding: 14px 24px 20px;
  border-top: 1px solid #F2F4F8;
  background: #FAFBFC;
}

.history-dialog .his-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #4A5D7E 0%, #64779A 100%);
  color: #fff;
  position: relative;
}

.history-dialog .his-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.history-dialog .his-title h3 {
  margin: 0 0 2px;
  font-size: 16px;
  font-weight: 600;
}

.history-dialog .his-title p {
  margin: 0;
  font-size: 12px;
  opacity: 0.8;
}

.history-dialog .his-close {
  position: absolute;
  top: 16px;
  right: 18px;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.history-dialog .his-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.history-dialog .his-empty {
  padding: 60px 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.history-dialog .his-empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.history-dialog .his-timeline {
  padding: 20px 28px;
}

.history-dialog .his-item {
  position: relative;
  padding-left: 28px;
  padding-bottom: 20px;
}

.history-dialog .his-item:last-child {
  padding-bottom: 0;
}

.history-dialog .his-dot {
  position: absolute;
  left: 0;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #1E6FA8;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #1E6FA8;
}

.history-dialog .his-line {
  position: absolute;
  left: 5px;
  top: 18px;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg, #1E6FA8 0%, #E4E7ED 100%);
}

.history-dialog .his-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.history-dialog .his-flow {
  font-size: 14px;
  margin-bottom: 4px;
}

.history-dialog .his-from {
  color: #606266;
}

.history-dialog .his-arrow {
  margin: 0 8px;
  color: #1E6FA8;
  font-weight: 700;
}

.history-dialog .his-to {
  color: #1E6FA8;
  font-weight: 600;
}

.history-dialog .his-reason {
  font-size: 12px;
  color: #909399;
  background: #F5F7FA;
  padding: 6px 10px;
  border-radius: 6px;
  display: inline-block;
  margin-top: 4px;
}

</style>
