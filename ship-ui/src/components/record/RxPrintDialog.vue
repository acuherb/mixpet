<template>
  <!-- 兽用处方笺打印前弹窗（734号）：选联次 + 补录 + 生成编号 → 打开打印视图 -->
  <el-dialog v-model="view" :title="rx?.supplementStatus === 'VOID' ? '处方笺已作废' : '打印处方笺'"
             width="92%" style="max-width: 480px" :close-on-click-modal="false"
             @open="load">
    <div v-loading="loading">
      <!-- 已作废：不允许再打 -->
      <template v-if="rx?.supplementStatus === 'VOID'">
        <el-result icon="warning" title="该处方笺编号已作废" sub-title="如需重开请联系管理员处理">
          <template #extra>
            <el-button type="primary" @click="view = false">知道了</el-button>
          </template>
        </el-result>
      </template>

      <template v-else-if="rx">
        <!-- 联次选择：默认仅第三联(畜主留存)，可勾三联全打 -->
        <el-form label-width="76px">
          <el-form-item label="联次">
            <el-radio-group v-model="copies">
              <el-radio :label="1">仅第三联（畜主留存）</el-radio>
              <el-radio :label="3">三联全打（机构/药房/畜主）</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 补录：已有值只读展示，缺失才可输入（缺项不造假，留空则由打印页出手写位） -->
          <el-form-item label="毛色">
            <el-input v-if="!rx.hairColor" v-model="form.hairColor" clearable maxlength="16" placeholder="缺失，可补录（如：黄白）"/>
            <span v-else class="ro">{{ rx.hairColor }}</span>
          </el-form-item>
          <el-form-item label="动物种类">
            <el-input v-if="!rx.species" v-model="form.species" clearable maxlength="16" placeholder="缺失，可补录（如：犬/猫）"/>
            <span v-else class="ro">{{ rx.species }}</span>
          </el-form-item>
          <el-form-item label="休药期">
            <el-input v-if="!rx.withdrawalPeriod" v-model="form.withdrawalPeriod" clearable maxlength="32" placeholder="宠物处方默认填「—」"/>
            <span v-else class="ro">{{ rx.withdrawalPeriod }}</span>
          </el-form-item>
          <el-form-item label="发药人">
            <el-input v-if="!rx.dispenser" v-model="form.dispenser" clearable maxlength="50" placeholder="未发药，可补录姓名或留空手写"/>
            <span v-else class="ro">{{ rx.dispenser }}</span>
          </el-form-item>
        </el-form>

        <el-alert :closable="false" show-icon type="info"
                  title="打印后由执业兽医师手签并盖医院章；系统不留电子签名。"/>
      </template>
    </div>

    <template #footer>
      <el-button @click="view = false">取消</el-button>
      <el-button v-if="rx && rx.supplementStatus !== 'VOID'" :loading="submitting" type="primary" @click="submit">
        生成编号并打印
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {reactive, ref} from 'vue'
import {ElMessage} from "element-plus";
import {reqRxSheet} from "@/request/RecordApi";
import {RxSheet} from "@/model/entiy/RxSheet";
import {reqRxPrintPrepare} from "@/request/RxApi";

const props = defineProps<{ recordId: number }>()
const view = defineModel<boolean>({required: true})

const rx = ref<RxSheet | null>(null)
const loading = ref(false)
const submitting = ref(false)
const copies = ref(1)
const form = reactive({hairColor: '', species: '', withdrawalPeriod: '', dispenser: ''})

const load = (): void => {
  loading.value = true
  reqRxSheet(props.recordId).then(res => {
    rx.value = res
    loading.value = false
  }).catch(() => { loading.value = false })
}

const submit = (): void => {
  submitting.value = true
  reqRxPrintPrepare({
    recordId: props.recordId,
    copies: copies.value,
    // 仅传缺失项（已有值后端已存，不重复传）
    hairColor: rx.value?.hairColor ? undefined : (form.hairColor || undefined),
    species: rx.value?.species ? undefined : (form.species || undefined),
    withdrawalPeriod: rx.value?.withdrawalPeriod ? undefined : (form.withdrawalPeriod || undefined),
    dispenser: rx.value?.dispenser ? undefined : (form.dispenser || undefined)
  }).then(res => {
    submitting.value = false
    if (res.code === 1) {
      view.value = false
      // 打开打印视图（auto=1 自动触发打印）
      window.open('/#/doctorPage/d-rx?recordId=' + props.recordId + '&auto=1', '_blank')
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  }).catch(() => { submitting.value = false })
}
</script>

<style scoped>
.ro { font-size: 13px; color: #303133; }
</style>
