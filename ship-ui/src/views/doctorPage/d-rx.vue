<template>
  <div class="rx-page">
    <!-- 工具栏（不打印） -->
    <div class="no-print rx-toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" :disabled="!rx" @click="doPrint">打印处方笺</el-button>
    </div>

    <div v-if="rx" class="rx-sheet">
      <div class="rx-title">兽医处方笺</div>
      <div class="rx-no">编号：{{ rx.rxNo }}</div>

      <table class="rx-table">
        <tbody>
          <tr>
            <td class="k">畜主</td><td class="v">{{ rx.clientName }}</td>
            <td class="k">宠物名</td><td class="v">{{ rx.petName }}</td>
            <td class="k">品种</td><td class="v">{{ rx.petVariety || '—' }}</td>
          </tr>
          <tr>
            <td class="k">性别</td><td class="v">{{ rx.petSex == null ? '—' : (rx.petSex ? '公' : '母') }}</td>
            <td class="k">年龄</td><td class="v">{{ rx.petBirthday ? getAge(rx.petBirthday) : '—' }}</td>
            <td class="k">体重</td><td class="v">{{ rx.petWeight != null ? rx.petWeight + 'kg' : '—' }}</td>
          </tr>
          <tr>
            <td class="k">芯片号</td><td class="v" colspan="3">{{ rx.petChip || '—' }}</td>
            <td class="k">科室</td><td class="v">{{ rx.departmentName || '—' }}</td>
          </tr>
          <tr>
            <td class="k">临床诊断</td><td class="v" colspan="5">{{ rx.diagnosis || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <div class="rx-rp">Rp（请取）</div>
      <table class="rx-table rx-items">
        <thead>
          <tr>
            <th style="width: 34%">药品名称</th>
            <th style="width: 16%">规格</th>
            <th style="width: 12%">数量</th>
            <th style="width: 26%">用法用量</th>
            <th style="width: 12%">小计</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(it, i) in rx.items" :key="i">
            <td>{{ it.drugName }}{{ it.drugUnit ? '（' + it.drugUnit + '）' : '' }}</td>
            <td>{{ it.drugSpec || '—' }}</td>
            <td>{{ it.quantity }}</td>
            <td>{{ it.usageInfo || '—' }}</td>
            <td>￥{{ ((it.drugPrice ?? 0) * it.quantity).toFixed(2) }}</td>
          </tr>
          <tr v-if="!rx.items.length">
            <td colspan="5" style="text-align: center; color: #999">无处方药品</td>
          </tr>
        </tbody>
      </table>

      <div class="rx-amount">
        金额（小写）：￥{{ (rx.totalAmount ?? 0).toFixed(2) }}
        <span style="margin-left: 18px">（大写）：{{ amountToCn(rx.totalAmount ?? 0) }}</span>
      </div>

      <table class="rx-table rx-sign">
        <tbody>
          <tr>
            <td class="k">开方兽医</td><td class="v">{{ rx.doctorName }}</td>
            <td class="k">资格证号</td><td class="v">{{ rx.doctorCert || '____' }}</td>
          </tr>
          <tr>
            <td class="k">开方日期</td><td class="v">{{ fmt(rx.createTime) }}</td>
            <td class="k">兽药房(调配)</td><td class="v">________</td>
          </tr>
        </tbody>
      </table>

      <div class="rx-foot">备注：本处方仅限本院药房调配；兽用处方药须凭执业兽医处方调配。</div>
    </div>

    <div v-else-if="!loading" class="no-print rx-empty">未找到该病历的处方笺</div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqRxSheet} from "@/request/RecordApi";
import {RxSheet} from "@/model/entiy/RxSheet";
import {getAge} from "@/utils/TimeUtil";

const route = useRoute()
const router = useRouter()
const rx = ref<RxSheet | null>(null)
const loading = ref(true)

onMounted(() => {
  const recordId = Number(route.query.recordId)
  if (!recordId) { loading.value = false; return }
  reqRxSheet(recordId).then(res => {
    rx.value = res
    loading.value = false
  })
})

const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
const doPrint = (): void => window.print()

/** 金额转人民币大写 */
const amountToCn = (num: number): string => {
  if (isNaN(num)) return ''
  const cn = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const unit = ['', '拾', '佰', '仟']
  const group = ['', '万', '亿', '兆']
  let integer = Math.floor(Math.abs(num))
  const decimal = Math.round((Math.abs(num) - integer) * 100)
  let s = ''
  if (integer === 0) {
    s = '零'
  } else {
    let gi = 0
    while (integer > 0) {
      const g = integer % 10000
      if (g !== 0) {
        let gs = ''
        let u = 0
        let gg = g
        let zero = false
        while (gg > 0) {
          const d = gg % 10
          if (d === 0) {
            if (!zero && gs !== '') zero = true
          } else {
            if (zero && gs !== '') gs = '零' + gs
            gs = cn[d] + unit[u] + gs
            zero = false
          }
          gg = Math.floor(gg / 10)
          u++
        }
        s = gs + group[gi] + s
      } else if (s !== '' && !s.startsWith('零')) {
        s = '零' + s
      }
      integer = Math.floor(integer / 10000)
      gi++
    }
  }
  s += '元'
  const jiao = Math.floor(decimal / 10)
  const fen = decimal % 10
  if (jiao === 0 && fen === 0) {
    s += '整'
  } else {
    if (jiao > 0) s += cn[jiao] + '角'
    else if (fen > 0) s += '零'
    if (fen > 0) s += cn[fen] + '分'
  }
  return (num < 0 ? '负' : '') + s
}
</script>

<style scoped>
.rx-page {
  padding: 20px;
  background: #f5f6fa;
  min-height: 100vh;
}
.rx-toolbar {
  max-width: 720px;
  margin: 0 auto 16px;
  text-align: right;
}
.rx-sheet {
  max-width: 720px;
  margin: 0 auto;
  background: #fff;
  border: 2px solid #333;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  position: relative;
}
.rx-title {
  text-align: center;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 6px;
  margin-bottom: 4px;
}
.rx-no {
  text-align: right;
  font-size: 13px;
  color: #555;
  margin-bottom: 12px;
}
.rx-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 12px;
}
.rx-table td, .rx-table th {
  border: 1px solid #333;
  padding: 7px 8px;
  font-size: 14px;
}
.rx-table .k {
  background: #f0f2f5;
  width: 84px;
  text-align: center;
  white-space: nowrap;
}
.rx-table .v {
  min-width: 60px;
}
.rx-items th {
  background: #f0f2f5;
  text-align: center;
}
.rx-rp {
  font-weight: 700;
  margin: 6px 0 6px;
}
.rx-amount {
  text-align: right;
  font-size: 15px;
  font-weight: 600;
  margin: 6px 0 12px;
}
.rx-sign td {
  height: 42px;
}
.rx-foot {
  font-size: 12px;
  color: #888;
  margin-top: 10px;
}
.rx-empty {
  text-align: center;
  color: #999;
  margin-top: 60px;
}

@media print {
  .rx-page {
    padding: 0;
    background: #fff;
  }
  .no-print {
    display: none !important;
  }
  .rx-sheet {
    box-shadow: none;
    border: 2px solid #000;
    max-width: 100%;
    margin: 0;
  }
}
</style>
