<template>
  <div class="q-page">
    <!-- 工具栏（不打印） -->
    <div class="no-print q-toolbar">
      <el-button @click="closeWin">返回</el-button>
      <el-button v-if="!cert.existed" type="primary" :loading="saving" @click="issue">开具检疫单并打印</el-button>
      <el-button v-else type="primary" @click="doPrint">打印检疫单</el-button>
      <span class="q-hint">
        {{ cert.existed ? '本单已开具，再次打印为补打（单号不变）' : '开具后单号落库留痕，补打所见即当时所开' }}
      </span>
    </div>

    <!-- 开具表单（不打印）：文字记录必填 -->
    <el-card v-if="!cert.existed && loaded" class="no-print q-form" shadow="never">
      <div class="q-form-title">开具检疫单</div>
      <el-form label-width="90px" size="default">
        <el-form-item label="文字记录" required>
          <el-input v-model="cert.content" maxlength="500" placeholder="如：疫苗：身体正常，要求年免；或 体外驱虫：福来恩0.5ml"
                    :rows="3" show-word-limit type="textarea"/>
        </el-form-item>
        <el-form-item label="检疫清单">
          <div class="q-list-tip">自动带入本次未作废处方 {{ cert.items?.length || 0 }} 项（如需修改请回接诊页调整处方后重进）</div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 打印区：A5 横向 -->
    <div v-if="cert.existed && loaded" class="q-sheet">
      <div class="q-head">
        <div class="q-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
        <div class="q-title">检 疫 单</div>
        <div class="q-subtitle">免疫 / 驱虫记录</div>
        <div class="q-meta">
          <span>检疫单号：{{ cert.certNo || '—' }}</span>
          <span>病例号：No.{{ cert.recordId }}</span>
        </div>
      </div>

      <table class="q-info">
        <tr>
          <td><b>主人名称：</b>{{ cert.ownerName || '—' }}</td>
          <td><b>联系电话：</b>{{ cert.ownerTel || '—' }}</td>
          <td><b>动物名称：</b>{{ cert.petName || '—' }}</td>
          <td><b>种类：</b>{{ cert.species || '—' }}</td>
        </tr>
        <tr>
          <td><b>品种：</b>{{ cert.breed || '—' }}</td>
          <td><b>性&emsp;别：</b>{{ cert.sex || '—' }}</td>
          <td><b>年龄：</b>{{ cert.ageDesc || '—' }}</td>
          <td><b>体重：</b>{{ cert.weight != null ? cert.weight + ' KG' : '—' }}</td>
        </tr>
      </table>

      <div class="q-sec-tit">检疫清单</div>
      <table class="q-items">
        <thead>
        <tr>
          <th style="width: 34%">药品名称</th>
          <th style="width: 22%">规格</th>
          <th style="width: 12%">数量</th>
          <th style="width: 32%">实际用量</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(it, i) in cert.items" :key="i">
          <td>{{ it.drugName }}</td>
          <td>{{ it.drugSpec || '—' }}</td>
          <td>{{ it.quantity != null ? it.quantity + (it.drugUnit || '') : '—' }}</td>
          <td>{{ it.dosage || it.usageInfo || '—' }}</td>
        </tr>
        <tr v-if="!cert.items || !cert.items.length">
          <td class="q-empty" colspan="4">本次未开具药品（仅健康检查结论）</td>
        </tr>
        </tbody>
      </table>

      <div class="q-sec-tit">文字记录</div>
      <div class="q-content">{{ cert.content || '—' }}</div>

      <div class="q-sign">
        <span>主人签字处：＿＿＿＿＿＿＿＿</span>
        <span>开单人：{{ cert.doctorName || '＿＿＿＿' }}</span>
        <span>打印日期：{{ nowText() }}</span>
      </div>
      <div class="q-foot">（此方三日内有效）　　本检疫单由本院诊疗系统生成，仅证明本次临床检查情况，不作官方检疫证明使用。</div>
    </div>

    <div v-else-if="loaded === false" class="no-print q-empty">未找到该病历，无法开具检疫单</div>
  </div>
</template>

<script lang="ts" setup>
import {onBeforeUnmount, onMounted, reactive, ref} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage} from 'element-plus'
import {QuarantineCert} from '@/model/entiy/QuarantineCert'
import {reqQuarantineByNo, reqQuarantineDraft, reqQuarantineIssue} from '@/request/QuarantineApi'
import {useBrand} from '@/utils/UseBrand'

const route = useRoute()
const {hosp, load: loadBrand} = useBrand()
const loaded = ref<boolean | null>(null)
const saving = ref(false)
const cert = reactive<QuarantineCert>({
  certNo: null, recordId: 0, existed: false, content: '', items: []
})

/** A5 横向纸张（与老系统 ReportPris_jianyi 一致：21×14.8cm） */
let pageStyle: HTMLStyleElement | null = null
const mountPageStyle = (): void => {
  pageStyle = document.createElement('style')
  pageStyle.textContent = '@page { size: A5 landscape; margin: 6mm; }'
  document.head.appendChild(pageStyle)
}

const apply = (v: QuarantineCert): void => {
  Object.assign(cert, v)
  if (cert.items == null) cert.items = []
}

onMounted(() => {
  loadBrand()
  mountPageStyle()
  const certNo = String(route.query.certNo || '')
  const recordId = Number(route.query.recordId)
  if (certNo) {
    reqQuarantineByNo(certNo).then(res => {
      apply(res)
      loaded.value = true
      if (route.query.auto === '1') setTimeout(() => window.print(), 500)
    }).catch(() => { loaded.value = false })
  } else if (recordId) {
    reqQuarantineDraft(recordId).then(res => {
      apply(res)
      loaded.value = true
      // 已有单据直接进打印态；补打链路自动打印
      if (res.existed && route.query.auto === '1') setTimeout(() => window.print(), 500)
    }).catch(() => { loaded.value = false })
  } else {
    loaded.value = false
  }
})
onBeforeUnmount(() => pageStyle?.remove())

const issue = (): void => {
  if (!cert.content || !cert.content.trim()) {
    ElMessage.warning('请填写文字记录（检疫结论）后再开具')
    return
  }
  saving.value = true
  reqQuarantineIssue({...cert, content: cert.content.trim()}).then(res => {
    apply(res)
    saving.value = false
    ElMessage.success('检疫单已开具 ' + res.certNo)
    setTimeout(() => window.print(), 300)
  }).catch(() => { saving.value = false })
}

const nowText = (): string => new Date().toLocaleString('zh-CN')
const doPrint = (): void => window.print()
const closeWin = (): void => window.close()
</script>

<style lang="scss" scoped>
.q-page {
  padding: 14px;
  background: #f3f6fb;
  min-height: 100vh;
}
.q-toolbar {
  max-width: 210mm;
  margin: 0 auto 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.q-hint {
  color: #8a93a6;
  font-size: 12px;
}
.q-form {
  max-width: 210mm;
  margin: 0 auto 12px;
}
.q-form-title {
  font-weight: 600;
  margin-bottom: 10px;
}
.q-list-tip {
  color: #8a93a6;
  font-size: 12px;
}

/* ============ 打印版式（A5 横向，参照老系统检疫单） ============ */
.q-sheet {
  width: 210mm;
  min-height: 140mm;
  margin: 0 auto;
  background: #fff;
  padding: 8mm 10mm;
  box-shadow: 0 2px 16px rgba(30, 111, 168, .10);
  border-radius: 4px;
  color: #1f2733;
  font-size: 12px;
  box-sizing: border-box;
}
.q-head {
  text-align: center;
  position: relative;
  border-bottom: 2px solid #1E6FA8;
  padding-bottom: 6px;
  margin-bottom: 8px;
}
.q-hosp {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
}
.q-title {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 8px;
  margin-top: 2px;
  color: #1E6FA8;
}
.q-subtitle {
  font-size: 11px;
  color: #6b7686;
  letter-spacing: 2px;
  margin-top: 1px;
}
.q-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 11.5px;
  color: #444;
}
.q-info {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;
  td {
    padding: 3px 2px;
    font-size: 12px;
  }
}
.q-sec-tit {
  font-weight: 700;
  margin: 6px 0 4px;
  font-size: 12.5px;
}
.q-items {
  width: 100%;
  border-collapse: collapse;
  th, td {
    border: 1px solid #b9c4d2;
    padding: 4px 5px;
    font-size: 11.5px;
    text-align: left;
  }
  th {
    background: #eef4fa;
    font-weight: 600;
  }
  .q-empty {
    text-align: center;
    color: #8a93a6;
    padding: 8px;
  }
}
.q-content {
  min-height: 26mm;
  border: 1px dashed #c7d2e0;
  border-radius: 3px;
  padding: 6px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.q-sign {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 12px;
}
.q-foot {
  margin-top: 6px;
  font-size: 10.5px;
  color: #6b7686;
  text-align: center;
}
.q-empty {
  max-width: 210mm;
  margin: 40px auto;
  text-align: center;
  color: #8a93a6;
}

@media print {
  .q-page {
    padding: 0;
    background: #fff;
  }
  .q-sheet {
    box-shadow: none;
    border-radius: 0;
    width: auto;
    min-height: auto;
    padding: 0;
  }
}
@media (max-width: 900px) {
  .q-sheet { width: 100%; }
  .q-toolbar { max-width: 100%; }
}
</style>
