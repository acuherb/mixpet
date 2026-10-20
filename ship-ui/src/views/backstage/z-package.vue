<template>
  <!--操作框-->
  <BackOpCol role="套餐" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.packageName" clearable placeholder="套餐名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-select v-model="qp.category" clearable placeholder="全部分类" size="large" style="width: 120px" @change="nowQuery">
      <el-option v-for="c in packageCategories" :key="c" :label="c" :value="c"/>
    </el-select>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">已选 <b>{{ roleIdList.length }}</b> 项</span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 130px)"
            row-key="packageId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="套餐名" min-width="150" prop="packageName" show-overflow-tooltip/>
    <el-table-column label="分类" width="90">
      <template #default="scope">
        <el-tag size="small">{{ scope.row.category || '未分类' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="价格(元)" prop="price" sortable width="100"/>
    <el-table-column label="包含项目" min-width="180">
      <template #default="scope">
        <span v-if="scope.row.items && scope.row.items.length">
          {{ scope.row.items!.map((i: any) => i.itemName).join('、') }}
        </span>
        <span v-else style="color:#c0c4cc">未配置项目</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="120">
      <template #default="scope">
        <el-switch v-model="scope.row.active" size="small" active-text="启用" inactive-text="停用"
                   @change="(val: any) => switchActive(scope.row, val)"/>
      </template>
    </el-table-column>
    <el-table-column fixed="right" label="操作" width="120">
      <template #default="scope">
        <el-button-group>
          <el-button :icon="Edit" circle title="编辑" type="warning" @click="showDialog(scope.row)"/>
          <el-button :icon="Delete" circle title="删除" type="danger" @click="delOne(scope.row)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="900px" :close-on-click-modal="false">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="套餐名" prop="packageName">
        <el-input v-model="form.packageName" clearable maxlength="64" placeholder="如：基础体检套餐"/>
      </el-form-item>
      <div style="display:flex;gap:16px">
        <el-form-item label="分类" prop="category" style="flex:1">
          <el-select v-model="form.category" allow-create filterable placeholder="选择或输入分类" style="width:100%">
            <el-option v-for="c in packageCategories" :key="c" :label="c" :value="c"/>
          </el-select>
        </el-form-item>
        <el-form-item label="价格(元)" prop="price" style="flex:1">
          <el-input-number v-model="form.price" :max="99999" :min="0" :precision="2" :step="10" style="width: 100%"/>
        </el-form-item>
      </div>

      <!-- 检查项目：套餐由现有检验/影像项目组合而成（全宽，不占 label 位） -->
      <div class="pkg-items">
        <div class="pkg-items-head">
          <span>检查项目 · 已选 {{ checkedLab.length + checkedImage.length }} 项</span>
          <span class="pkg-sum">
            项目原价合计 <b>￥{{ itemsOriginTotal().toFixed(2) }}</b>
            <em>套餐价 ￥{{ (form.price ?? 0).toFixed(2) }}</em>
            <em :class="{off: packageDiscount() > 0}">优惠 ￥{{ packageDiscount().toFixed(2) }}</em>
          </span>
        </div>
        <div class="pkg-items-body">
          <div class="pkg-group">
            <div class="pkg-group-tit">检验项目</div>
            <el-checkbox-group v-model="checkedLab" class="pkg-checks">
              <el-checkbox v-for="t in labTests" :key="t.testId" :label="t.testId" class="pkg-check">
                {{ t.testName }}<span class="pkg-price">￥{{ t.testPrice ?? 0 }}</span>
              </el-checkbox>
            </el-checkbox-group>
            <el-empty v-if="!labTests.length" :image-size="40" description="暂无检验项（去检验项字典维护）"/>
          </div>
          <div class="pkg-group">
            <div class="pkg-group-tit">影像项目</div>
            <el-checkbox-group v-model="checkedImage" class="pkg-checks">
              <el-checkbox v-for="t in imageTests" :key="t.itemId" :label="t.itemId" class="pkg-check">
                {{ t.itemName }}<span class="pkg-price">￥{{ t.price ?? 0 }}</span>
              </el-checkbox>
            </el-checkbox-group>
            <el-empty v-if="!imageTests.length" :image-size="40" description="暂无影像项（去影像项字典维护）"/>
          </div>
        </div>
      </div>

      <el-form-item label="补充说明">
        <el-input v-model="form.content" :rows="2" maxlength="255" type="textarea" placeholder="如：含医生解读报告；项目清单以外的文字补充"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.active" active-text="启用" inactive-text="停用"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">确认</el-button>
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
import {PageQuery} from "@/model/VO/BackQuery";
import {examplePackage, packageCategories, ServicePackage} from "@/model/entiy/ServicePackage";
import {Page} from "@/model/DO/Page";
import {reqAddPackage, reqDelPackage, reqPackagePage, reqSwitchPackage, reqUpdatePackage} from "@/request/PackageApi";
import {reqLabTestList} from "@/request/LabApi";
import {reqImageTestPage} from "@/request/ImageApi";
import {LabTest} from "@/model/entiy/LabTest";
import {ImageTest} from "@/model/entiy/ImageTest";
import {PackageItem} from "@/model/entiy/ServicePackage";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

onMounted(() => {
  sendQuery()
})

const qp = reactive({packageName: '', category: '', numPage: 1, pageSize: 15})
const list = ref<Page<ServicePackage>>({records: [], total: 0})

const changePuB = (val: PageQuery): void => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  reqPackagePage(qp).then(res => {
    list.value = res
  })
}
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: ServicePackage[]): void => {
  roleIdList.value = val.map(obj => obj.packageId)
}
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm('确定删除选中的套餐吗？历史就诊记录不受影响。', '删除多个确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    Promise.all(roleIdList.value.map(id => reqDelPackage(id))).then(() => sendQuery())
  }).catch(() => ElMessage.info('删除取消'))
}
const delOne = (row: ServicePackage): void => {
  ElMessageBox.confirm(`确定删除【${row.packageName}】吗？历史就诊记录不受影响。`, '删除确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelPackage(row.packageId).then(res => { if (res.code === 1) sendQuery() })
  }).catch(() => ElMessage.info('删除取消'))
}

const modalView = ref(false)
const modalTit = ref<"新增套餐" | "修改套餐">("修改套餐")
// 检验/影像项目字典（套餐勾选用）
const labTests = ref<LabTest[]>([])
const imageTests = ref<ImageTest[]>([])
const loadItems = (): void => {
  if (labTests.value.length === 0) reqLabTestList().then(r => { labTests.value = r || [] })
  if (imageTests.value.length === 0) {
    reqImageTestPage({itemName: '', groupName: '', numPage: 1, pageSize: 200})
        .then(r => { imageTests.value = r?.records || [] })
  }
}
// 勾选态（按类型分开，回显/组装都走它）
const checkedLab = ref<number[]>([])
const checkedImage = ref<number[]>([])
const syncChecksFromForm = (): void => {
  const items = form.value.items || []
  checkedLab.value = items.filter(i => i.itemType === 'lab').map(i => i.itemId)
  checkedImage.value = items.filter(i => i.itemType === 'image').map(i => i.itemId)
}
/** 勾选态 → 套餐项目（价格取字典当前价，保存时快照） */
const buildItems = (): PackageItem[] => {
  const list: PackageItem[] = []
  labTests.value.filter(t => checkedLab.value.includes(t.testId)).forEach(t => {
    list.push({itemType: 'lab', itemId: t.testId, itemName: t.testName, itemPrice: t.testPrice ?? 0})
  })
  imageTests.value.filter(t => checkedImage.value.includes(t.itemId)).forEach(t => {
    list.push({itemType: 'image', itemId: t.itemId, itemName: t.itemName, itemPrice: t.price ?? 0})
  })
  return list
}
/** 项目原价合计 / 套餐优惠额 */
const itemsOriginTotal = (): number =>
    buildItems().reduce((s, i) => s + (i.itemPrice ?? 0), 0)
const packageDiscount = (): number =>
    Math.max(0, itemsOriginTotal() - (form.value.price ?? 0))

const addRoleB = (): void => {
  form.value = examplePackage()
  myFormRef.value?.resetFields()
  syncChecksFromForm()
  loadItems()
  modalTit.value = "新增套餐"
  modalView.value = true
}
const showDialog = (row: ServicePackage): void => {
  myFormRef.value?.clearValidate()
  form.value = {...row, items: row.items ? [...row.items] : []}
  syncChecksFromForm()
  loadItems()
  modalView.value = true
  modalTit.value = "修改套餐"
}

/** 列表行内切换启用状态 */
const switchActive = (row: ServicePackage, val: any): void => {
  const active = !!val
  reqSwitchPackage(row.packageId, active).then(res => {
    if (res.code === 1) ElMessage.success(active ? '已启用' : '已停用')
    else { row.active = !active; ElMessage.error(res.msg || '操作失败') }
  }).catch(() => { row.active = !active; ElMessage.error('网络异常') })
}

const form = ref<ServicePackage>(examplePackage())
const myFormRef = ref<FormInstance>()
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      // 组装勾选的检查项目（主子表一并保存）
      form.value.items = buildItems()
      modalView.value = false
      if (modalTit.value == "新增套餐") reqAddPackage(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改套餐") reqUpdatePackage(form.value).then(res => sureFlush(res))
      else ElMessageBox.alert('模块框出错')
    }
  })
}
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}
const rules = reactive<FormRules>({
  "packageName": [
    {required: true, message: '请输入套餐名', trigger: 'blur'},
  ],
  "price": [
    {required: true, message: '请输入价格', trigger: 'blur'},
  ]
})
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

/* 套餐项目选择区 */
.pkg-items {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;

  .pkg-items-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    padding: 8px 12px;
    background: #f5f7fa;
    font-size: 12px;
    color: #606266;

    .pkg-sum {
      display: flex;
      align-items: center;
      gap: 12px;

      b { color: #1F2D3D; }
      em {
        font-style: normal;
        color: #606266;

        &.off { color: #F56C6C; font-weight: 600; }
      }
    }
  }

  .pkg-items-body {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0;
    max-height: 260px;
    overflow-y: auto;
  }

  .pkg-group {
    padding: 10px 12px;

    & + .pkg-group { border-left: 1px solid #ebeef5; }

    .pkg-group-tit {
      font-size: 13px;
      font-weight: 700;
      color: #1F2D3D;
      margin-bottom: 6px;
    }
  }

  .pkg-checks {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .pkg-check {
    margin: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .pkg-price { color: #E6A23C; font-size: 12px; margin-left: 8px; }
  }
}
</style>
