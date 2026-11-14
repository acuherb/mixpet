<template>
  <div>
    <PageHead>查看挂号</PageHead>
    <div class="appoint-tabbar">
      <button
          class="appoint-tab"
          :class="{active: appointTab === 'pending'}"
          @click="appointTab = 'pending'">待处理 ({{ todayPending.length }})</button>
      <button
          class="appoint-tab"
          :class="{active: appointTab === 'future', 'has-future': futureAppoint.length > 0}"
          @click="appointTab = 'future'">未来预约 ({{ futureAppoint.length }})<i v-if="futureAppoint.length" class="tab-dot"/></button>
      <button
          class="appoint-tab"
          :class="{active: appointTab === 'done'}"
          @click="appointTab = 'done'">已处理 ({{ doneAppoint.length }})</button>
      <!-- 日期筛选：留空=全部；未来预约页签下可再筛到某一天 -->
      <el-date-picker
          v-model="appointDateFilter"
          clearable
          format="YYYY-MM-DD"
          placeholder="按就诊日期筛选"
          size="default"
          style="width: 170px; margin-left: 12px"
          type="date"
          value-format="YYYY-MM-DD"/>
      <ShakeBtn style="float: right" @click="addRoleB">添加挂号</ShakeBtn>
    </div>
    <!-- 挂号列表：一屏看全貌（挂号多时不受限），字段比后台表格更贴医生视角 -->
    <el-table :data="shownAppoint"
              max-height="calc(100vh - 260px)"
              row-key="appointmentId"
              size="small"
              stripe>
      <el-table-column label="就诊时间" prop="appointmentDate" sortable width="150">
        <template #default="s">{{ moveT(s.row.appointmentDate) }}</template>
      </el-table-column>
      <el-table-column label="宠物" min-width="110">
        <template #default="s">
          {{ s.row.petName }}
          <span style="color:#909399;font-size:11px">（{{ s.row.petVariety || '—' }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="性别/年龄" width="100">
        <template #default="s">
          {{ s.row.petSex ? '公' : '母' }} · {{ s.row.petBirthday ? getAge(s.row.petBirthday) : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="主人" min-width="90" prop="clientName" show-overflow-tooltip/>
      <el-table-column label="科室/医生" min-width="120" show-overflow-tooltip>
        <template #default="s">{{ s.row.departmentName || '—' }} / {{ s.row.doctorName || '—' }}</template>
      </el-table-column>
      <el-table-column label="挂号描述" min-width="160" show-overflow-tooltip>
        <template #default="s">
          {{ s.row.appointmentInfo || '—' }}
          <el-tag v-if="s.row.originRecordId" size="small" type="info" effect="plain" style="margin-left:4px">复诊</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="s">
          <el-tag v-if="s.row.miss" size="small" type="info" effect="dark">已爽约</el-tag>
          <el-tag v-else-if="recordStatusMap[s.row.appointmentId] === 2" size="small" type="success">已完结</el-tag>
          <el-tag v-else-if="recordStatusMap[s.row.appointmentId] === 3" size="small" type="info">已作废</el-tag>
          <el-tag v-else-if="recordStatusMap[s.row.appointmentId] === 0" size="small" type="warning">接诊中</el-tag>
          <el-tag v-else size="small" type="warning">待接诊</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="210">
        <template #default="s">
          <template v-if="!s.row.miss">
            <!-- 未来预约只可改期：不提供「接诊」，避免对还没到诊的号提前开诊 -->
            <template v-if="appointTab === 'future'">
              <el-button size="small" type="success" link @click="openReschedule(s.row)">改期</el-button>
            </template>
            <template v-else>
              <el-button v-if="!s.row.isSuc" size="small" type="primary" link @click="openTreat(s.row)">接诊</el-button>
              <el-button v-else-if="recordStatusMap[s.row.appointmentId] === 0" size="small" type="warning" link @click="openTreat(s.row)">继续开单</el-button>
              <el-button v-else size="small" type="info" link @click="openTreat(s.row)">查看</el-button>
              <!-- 改期：患者打电话来换时间，不用切后台（管理员级直接改，跳过余号校验） -->
              <el-button v-if="!s.row.isSuc" size="small" type="success" link @click="openReschedule(s.row)">改期</el-button>
              <el-button v-if="!s.row.isSuc && isOverdue(s.row)" size="small" type="danger" link @click="markMiss(s.row)">标记爽约</el-button>
            </template>
          </template>
          <span v-else style="color:#909399;font-size:12px">已爽约 · 不可操作</span>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :image-size="60" description="暂无挂号单"/>
      </template>
    </el-table>
    <!--改期弹窗：医生直接改患者的就诊时间（管理员级，跳过余号校验）-->
    <el-dialog v-model="rsView" title="改期" draggable width="420px" :close-on-click-modal="false">
      <el-form v-if="rsRow" label-width="80px">
        <el-form-item label="宠物">
          <span>{{ rsRow.petName }}（{{ rsRow.clientName }}）</span>
        </el-form-item>
        <el-form-item label="原时间">
          <span style="color:#909399">{{ moveT(rsRow.appointmentDate) }}</span>
        </el-form-item>
        <el-form-item label="新时间">
          <el-date-picker
              v-model="rsDate"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择新的就诊时间"
              style="width: 100%"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="rsView = false">取消</el-button>
        <el-button round type="primary" :loading="rsSaving" @click="submitReschedule">确认改期</el-button>
      </template>
    </el-dialog>
    <!--添加的模态框-->
    <el-dialog v-model="modalView" title="新增挂号" draggable width="60%">
      <el-form ref="myFormRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="简介" prop="appointmentInfo">
          <el-input v-model="form.appointmentInfo" clearable maxlength="64" show-word-limit placeholder="无"/>
        </el-form-item>
        <el-form-item label="就诊时间" prop="appointmentDate">
          <el-date-picker
              v-model="form.appointmentDate"
              :value-format="'YYYY-MM-DD'+'T'+'HH:mm:ss'"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择就诊时间"
              type="datetime"
          />
        </el-form-item>
        <el-form-item label="宠物档案" prop="petId">
          <!-- 卡号/手机号/宠物名一框检索，选中带出档案摘要 -->
          <PetRegPicker ref="petRegRef" v-model:pet-id="form.petId" v-model:client-id="form.clientId" dense style="width: 100%"/>
        </el-form-item>
      </el-form>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">确认</el-button>
      </span>
      </template>
    </el-dialog>

    <!--挂号成功凭证卡-->
    <AppointTicket ref="ticketRef"/>

    <!--接诊弹窗：向导式（问诊检查 → 诊断医嘱 → 处置开单 → 完结结算）-->
    <!-- 接诊向导全屏化：单屏容纳病历+开单，不再层叠弹窗 -->
    <el-dialog v-model="treatView" :close-on-click-modal="false" :before-close="beforeCloseTreat" fullscreen>
      <template #header>
        <span style="font-weight: bold">接诊向导 · {{ stepTitles[activeStep] }}</span>
        <el-tag v-if="treatAppoint" style="margin-left: 8px" type="warning">
          {{ treatAppoint.petName }} · {{ treatAppoint.clientName }}
        </el-tag>
        <el-tag v-if="orderLocked" style="margin-left: 8px" type="info">
          {{ treatStatus === 2 ? '已作废' : '已完结' }} · 只读
        </el-tag>
        <el-tag v-if="hasDraft" style="margin-left: 8px" type="success" effect="plain">本地草稿已暂存</el-tag>
        <el-tag v-if="autosaveText" style="margin-left: 8px" type="info" effect="plain">
          <span class="auto-save-dot" :class="{saving: savingPending}"></span>{{ autosaveText }}
        </el-tag>
        <el-button v-if="hasDraft && !orderLocked" size="small" link type="danger" style="margin-left: 6px" @click="discardDraft">清空草稿</el-button>
      </template>

      <!-- 接诊向导步骤条 -->
      <el-steps :active="activeStep" align-center simple style="margin-bottom: 12px">
        <el-step title="问诊与检查"/>
        <el-step title="诊断与医嘱"/>
        <el-step title="处置开单"/>
        <el-step title="完结结算"/>
      </el-steps>

      <div class="treat-body">
        <div class="treat-main">

      <el-alert v-if="recordForm.reviewNote" type="error" :closable="false" show-icon
                :title="'病历已被审核退回，请按意见修订后重新完结并签名'"
                :description="'退回理由：' + recordForm.reviewNote" style="margin-bottom:12px"/>

      <!-- 接诊向导表单：始终渲染，按步骤 v-show 切换 -->
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="90px">
        <!-- 第一步：问诊与检查（模板/主诉/现病史/既往史/查体/体征） -->
        <div v-show="activeStep === 0" class="treat-step">
        <!-- 病历模板：一键填充条（醒目浅色卡） -->
        <div class="rec-tpl-bar">
          <div class="rec-tpl-ico"><el-icon><Notebook/></el-icon></div>
          <div class="rec-tpl-main">
            <div class="rec-tpl-tit">病历模板 · 一键填充</div>
            <el-select v-model="tplId" clearable filterable placeholder="选择模板（填充后仍可修改）" style="width: 100%" @change="applyTpl">
              <el-option v-for="t in templates" :key="t.templateId" :label="`${t.name}${t.category ? '（' + t.category + '）' : ''}`" :value="t.templateId"/>
            </el-select>
          </div>
        </div>

        <div class="step-head">
          <div class="step-tit"><span class="step-no">一</span>S 主观资料 · 问诊（宠物为什么来医院）</div>
        </div>
        <!-- 问诊卡片组：主诉重点卡 + 现病史/既往史 -->
        <div class="rec-card key">
          <div class="rec-card-tit">主诉 <em>主要症状 + 持续时间</em></div>
          <el-form-item prop="chiefComplaint" label-width="0">
            <el-input v-model="recordForm.chiefComplaint" :rows="2" maxlength="255" placeholder="如：呕吐2天，今日次数增加伴食欲下降（已自动带入挂号描述）" type="textarea"/>
          </el-form-item>
        </div>
        <div class="rec-card">
          <div class="rec-card-tit">现病史 <em>何时开始 → 表现 → 加重/减轻 → 伴随症状 → 治疗经过</em></div>
          <el-form-item label-width="0">
            <el-input v-model="recordForm.presentIllness" :rows="3" maxlength="1000" placeholder="如：2日前开始间断呕吐，最初每日约1次…今日加重至3次，伴食欲下降" type="textarea"/>
          </el-form-item>
        </div>
        <div class="rec-card">
          <div class="rec-card-tit">既往病史 <em>既往疾病 / 手术 / 长期用药 / 过敏 / 类似发作史</em></div>
          <el-form-item label-width="0">
            <el-input v-model="recordForm.pastHistory" :rows="2" maxlength="1000" placeholder="可参考右侧病史卡与宠物档案" type="textarea"/>
          </el-form-item>
        </div>

        <div class="step-head">
          <div class="step-tit"><span class="step-no">二</span>O 客观资料 · 检查（医生发现了什么）</div>
        </div>
        <div class="rec-card">
          <div class="rec-card-tit">体格检查 <em>视触叩听等检查发现</em></div>
          <el-form-item label-width="0">
            <el-input v-model="recordForm.physicalExam" :rows="2" maxlength="255" placeholder="如：体温39.2℃，心率120，精神沉郁，黏膜苍白" type="textarea"/>
            <!-- 查体快捷标签：点击即填入，实现"半勾选式"录入 -->
            <div class="exam-tags">
              <span v-for="t in examTags" :key="t" class="exam-tag" @click="appendExam(t)">{{ t }}</span>
            </div>
          </el-form-item>
        </div>

        <!-- 分诊与体征：网格排布（告别一列竖排） -->
        <div class="step-head">
          <div class="step-tit"><span class="step-no">三</span>O 客观资料 · 分诊与体征</div>
        </div>
        <div class="rec-vitals">
          <el-form-item label="分诊级别" class="vital-item">
            <el-select v-model="recordForm.triageLevel" placeholder="选择级别" style="width: 100%">
              <el-option :value="1" label="急诊"/>
              <el-option :value="2" label="普通"/>
              <el-option :value="3" label="复诊"/>
            </el-select>
          </el-form-item>
          <el-form-item label="体重(kg)" class="vital-item">
            <el-input-number v-model="recordForm.weight" :max="200" :min="0" :precision="2" :step="0.1" controls-position="right" style="width: 100%"/>
          </el-form-item>
          <el-form-item label="体温(℃)" class="vital-item">
            <el-input-number v-model="recordForm.temperature" :max="45" :min="30" :precision="1" :step="0.1" controls-position="right" style="width: 100%"/>
          </el-form-item>
          <el-form-item label="心率(次/分)" class="vital-item">
            <el-input-number v-model="recordForm.heartRate" :max="400" :min="0" :step="5" controls-position="right" style="width: 100%"/>
          </el-form-item>
          <el-form-item label="呼吸(次/分)" class="vital-item">
            <el-input-number v-model="recordForm.respiratoryRate" :max="200" :min="0" :step="2" controls-position="right" style="width: 100%"/>
          </el-form-item>
          <el-form-item label="血压(mmHg)" class="vital-item">
            <el-input v-model="recordForm.bloodPressure" clearable maxlength="16" placeholder="如：120/80"/>
          </el-form-item>
        </div>

        </div>

        <!-- 第二步：诊断与医嘱（含费用） -->
        <div v-show="activeStep === 1" class="treat-step">
        <div class="step-head">
          <el-divider content-position="left">A 评估 / P 计划 · 诊断与治疗</el-divider>
        </div>
        <el-form-item label="诊断" prop="diagnosis">
          <el-input v-model="recordForm.diagnosis" maxlength="255" placeholder="诊断结论"/>
        </el-form-item>
        <el-form-item label="诊断编码">
          <el-select v-model="recordForm.diagnosisCode" filterable clearable placeholder="从字典选码（可留空）" style="width: 100%" @change="onDiagnosisPick">
            <el-option v-for="d in diagnosisOptions" :key="d.code" :label="`${d.code} · ${d.name}`" :value="d.code"/>
          </el-select>
        </el-form-item>
        <el-form-item label="鉴别诊断">
          <el-input v-model="recordForm.differentialDiagnosis" :rows="2" maxlength="1000"
                    placeholder="需排除的疾病及理由" type="textarea"/>
        </el-form-item>
        <el-form-item label="诊断依据">
          <el-input v-model="recordForm.diagnosisBasis" :rows="2" maxlength="1000"
                    placeholder="症状 / 体征 / 检验 / 影像的支持点" type="textarea"/>
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-input v-model="recordForm.treatment" :rows="2" maxlength="255" placeholder="治疗措施" type="textarea"/>
        </el-form-item>
        <el-form-item label="医嘱">
          <el-input v-model="recordForm.advice" :rows="2" maxlength="255" placeholder="给宠物主人的嘱咐（顾客端可见）" type="textarea"/>
        </el-form-item>
        <el-form-item label="复诊日期">
          <el-date-picker
              v-model="recordForm.revisitDate"
              format="YYYY-MM-DD"
              placeholder="需要复诊时选择（可选）"
              style="width: 100%"
              type="date"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="内部备注">
          <el-input v-model="recordForm.internalNote" :rows="2" maxlength="255" placeholder="仅医院内部可见，顾客端不显示" type="textarea"/>
        </el-form-item>

        <el-divider content-position="left">费用</el-divider>
        <el-form-item label="挂号费(元)">
          <el-input-number v-model="recordForm.feeRegister" :max="9999" :min="0" :precision="2" :step="5"/>
          <span class="fee-sep">诊疗费(元)</span>
          <el-input-number v-model="recordForm.feeTreat" :max="99999" :min="0" :precision="2" :step="10"/>
          <div class="fee-tip">费用标准由管理层维护；最终金额以收银台收款确认为准</div>
        </el-form-item>
        <p class="phase-tip">保存病历后进入处置开单：可开处方/手术/套餐；检验、影像请点「去开检验单/影像单」在专用页面选择项目开单。</p>
        </div>
      </el-form>

      <!-- 第三步：处置开单（左：病历摘要只读 · 右：处方/手术/套餐/检验/影像，单屏完成） -->
      <div v-show="activeStep === 2 && treatRecordId > 0" class="treat-step">
        <div class="order-layout">
        <aside class="order-side">
          <div class="side-tit">病历摘要 · #{{ treatRecordId }}</div>
          <div class="side-field"><span class="k">宠物</span><span class="v">{{ treatAppoint?.petName }} · {{ treatAppoint?.clientName }}</span></div>
          <div class="side-field"><span class="k">体重</span><span class="v">{{ recordForm.weight != null ? recordForm.weight + ' kg' : '未填写（算量依据）' }}</span></div>
          <div class="side-field"><span class="k">诊断</span><span class="v">{{ recordForm.diagnosis || '（未填写）' }}</span></div>
          <div class="side-field"><span class="k">医嘱</span><span class="v">{{ recordForm.advice || '（无）' }}</span></div>
          <div class="side-fee">
            <span>挂号+诊疗 ￥{{ ((recordForm.feeRegister||0)+(recordForm.feeTreat||0)).toFixed(2) }}</span>
            <b>合计 ￥{{ treatTotal() }}</b>
          </div>
          <div class="side-orders">
            <span :class="{on: savedRx.length}">处方 {{ savedRx.length }}</span>
            <span :class="{on: savedSurgery.length}">手术 {{ savedSurgery.length }}</span>
            <span :class="{on: savedPkg.length}">套餐 {{ savedPkg.length }}</span>
            <span :class="{on: labCount}">检验 {{ labCount }}</span>
            <span :class="{on: imageCount}">影像 {{ imageCount }}</span>
          </div>
          <p class="side-tip">开单即计入账单；检验/影像在右侧对应页签开单，随时回看合计</p>
        </aside>
        <section class="order-main">
        <el-alert v-if="orderLocked" :title="treatStatus === 2 ? '该就诊已作废，单据只读' : '该就诊已完结，单据只读'"
                  show-icon :closable="false" type="warning" style="margin-bottom: 10px"/>
        <el-tabs v-model="activeTab">
          <!-- 处方 -->
          <el-tab-pane name="rx">
            <template #label>
              <span>处方<b v-if="savedRx.length" class="tab-num">{{ savedRx.length }}</b></span>
            </template>
            <!-- 体重提示：按体重算量时医生抬头可见（未填提醒回第1步补） -->
            <div class="rx-weight-bar">
              <el-icon><InfoFilled/></el-icon>
              <span v-if="recordForm.weight != null">当前宠物体重：<b>{{ recordForm.weight }} kg</b>，算量结果请结合体重核对</span>
              <span v-else style="color:#E6A23C">尚未填写体重（算量依据），请到第 1 步「分诊与体征」补充</span>
              <!-- 处方笺快捷打印：保存处方后直接打印，无需走到第 4 步 -->
              <el-button v-if="treatRecordId > 0 && !orderLocked" size="small" plain round type="primary"
                         style="margin-left:auto" @click="rxQuickView = true">
                <el-icon style="margin-right:4px"><Printer/></el-icon>打印处方笺
              </el-button>
            </div>
            <el-table :data="prescriptions" border size="small">
              <el-table-column label="药品" min-width="180">
                <template #default="scope">
                  <!-- 键盘流：药品选中后自动聚焦数量，一路 Enter 到用法，末行回车自动加行 -->
                  <el-select :ref="(el: any) => setCellRef(scope.$index, 'drug', el)" v-model="scope.row.drugId"
                             filterable remote reserve-keyword :remote-method="filterDrug"
                             placeholder="输入药品名检索" size="small" style="width: 100%"
                             @change="onDrugPicked(scope.$index, scope.row)">
                    <el-option v-for="d in drugOptions" :key="d.drugId"
                               :disabled="d.drugStock === 0"
                               :label="`${d.drugName}（￥${d.drugPrice}/${d.drugUnit}，库存${d.drugStock}）`"
                               :value="d.drugId"/>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="数量" min-width="120">
                <template #default="scope">
                  <el-input-number :ref="(el: any) => setCellRef(scope.$index, 'qty', el)" v-model="scope.row.quantity"
                                   :max="99" :min="1" size="small" style="width: 90px"
                                   @keyup.enter="focusCell(scope.$index, 'times')"/>
                  <!-- 算量提示：与自动算量一致=绿色；医生改过数量=红色警示 -->
                  <div v-if="scope.row.doseTip" :class="['dose-tip', {changed: scope.row.doseQty != null && scope.row.quantity !== scope.row.doseQty}]">{{ scope.row.doseTip }}</div>
                </template>
              </el-table-column>
              <el-table-column label="频次(次/日)" width="105">
                <template #default="scope">
                  <el-input-number :ref="(el: any) => setCellRef(scope.$index, 'times', el)" v-model="scope.row.dailyTimes"
                                   :max="12" :min="1" size="small" style="width: 80px"
                                   @change="recalcDose(scope.row)" @keyup.enter="focusCell(scope.$index, 'days')"/>
                </template>
              </el-table-column>
              <el-table-column label="天数" width="85">
                <template #default="scope">
                  <el-input-number :ref="(el: any) => setCellRef(scope.$index, 'days', el)" v-model="scope.row.durationDays"
                                   :max="90" :min="1" size="small" style="width: 70px"
                                   @change="recalcDose(scope.row)" @keyup.enter="focusCell(scope.$index, 'usage')"/>
                </template>
              </el-table-column>
              <el-table-column label="用法" min-width="140">
                <template #default="scope">
                  <el-input :ref="(el: any) => setCellRef(scope.$index, 'usage', el)" v-model="scope.row.usageInfo"
                            clearable maxlength="64" placeholder="如：每日2次，每次1片" size="small"
                            @keyup.enter="rxRowDone(scope.$index)"/>
                </template>
              </el-table-column>
              <el-table-column label="小计" width="80">
                <template #default="scope">
                  <span v-if="drugOf(scope.row.drugId)">￥{{ (drugOf(scope.row.drugId)!.drugPrice! * scope.row.quantity).toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column width="50">
                <template #default="scope">
                  <el-button :icon="Delete" circle size="small" type="danger" @click="prescriptions.splice(scope.$index, 1)"/>
                </template>
              </el-table-column>
            </el-table>
            <el-button :icon="Plus" plain size="small" style="margin-top: 6px; width: 100%" @click="addPrescription">添加药品</el-button>
            <div style="margin-top: 10px; text-align: right">
              <el-button size="small" type="primary" :loading="rxSaving" :disabled="orderLocked" @click="saveRx">保存处方（￥{{ feeDrug() }}）</el-button>
            </div>
            <el-divider content-position="left">已开处方</el-divider>
            <el-table :data="savedRx" border size="small">
              <el-table-column label="药品" min-width="140" prop="drugName" show-overflow-tooltip/>
              <el-table-column label="数量" width="80">
                <template #default="s">{{ s.row.quantity }}{{ s.row.drugUnit }}</template>
              </el-table-column>
              <el-table-column label="单价" width="80">
                <template #default="s">￥{{ s.row.drugPrice }}</template>
              </el-table-column>
              <el-table-column label="小计" width="90">
                <template #default="s">￥{{ (s.row.drugPrice * s.row.quantity).toFixed(2) }}</template>
              </el-table-column>
            </el-table>
            <p v-if="savedRx.length === 0" style="color:#999">尚未开具处方</p>
          </el-tab-pane>

          <!-- 手术 -->
          <el-tab-pane name="surgery">
            <template #label>
              <span>手术<b v-if="savedSurgery.length" class="tab-num">{{ savedSurgery.length }}</b></span>
            </template>
            <el-form-item label="需要手术">
              <el-switch v-model="needSurgery"/>
            </el-form-item>
            <template v-if="needSurgery">
              <el-form-item label="手术名称">
                <el-input v-model="surgeryForm.surgeryName" clearable maxlength="128" placeholder="如：绝育手术（母）"/>
              </el-form-item>
              <el-form-item label="类型/麻醉">
                <el-select v-model="surgeryForm.surgeryType" clearable filterable placeholder="类型" size="small" style="width: 48%">
                  <el-option v-for="t in surgeryTypeOpts" :key="t.dictId" :label="t.dictKey" :value="t.dictKey"/>
                </el-select>
                <el-select v-model="surgeryForm.anesthesiaType" clearable placeholder="麻醉" size="small" style="width: 48%; margin-left: 4%">
                  <el-option v-for="a in anesthesiaTypeOpts" :key="a.dictId" :label="a.dictKey" :value="a.dictKey"/>
                </el-select>
              </el-form-item>
              <el-form-item label="手术费(元)">
                <el-input-number v-model="surgeryForm.fee" :max="99999" :min="0" :precision="2" :step="100" style="width: 100%"/>
              </el-form-item>
              <div style="text-align: right">
                <el-button size="small" type="primary" :loading="surgerySaving" :disabled="orderLocked" @click="createSurgery">安排手术（￥{{ feeSurgery() }}）</el-button>
              </div>
            </template>
            <el-divider content-position="left">已安排手术</el-divider>
            <el-table :data="savedSurgery" border size="small">
              <el-table-column label="手术" min-width="140" prop="surgeryName" show-overflow-tooltip/>
              <el-table-column label="麻醉" width="100" prop="anesthesiaType"/>
              <el-table-column label="费用" width="80">
                <template #default="s">￥{{ s.row.fee }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="s">
                  <el-tag size="small" :type="surgeryStatusType(s.row.status)">{{ surgeryStatusText(s.row.status) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <p v-if="savedSurgery.length === 0" style="color:#999">尚未安排手术</p>
          </el-tab-pane>

          <!-- 套餐 -->
          <el-tab-pane name="pkg">
            <template #label>
              <span>套餐<b v-if="savedPkg.length" class="tab-num">{{ savedPkg.length }}</b></span>
            </template>
            <el-table :data="packages" border size="small">
              <el-table-column label="套餐" min-width="200">
                <template #default="scope">
                  <el-select v-model="scope.row.packageId" filterable placeholder="选择套餐" size="small" style="width: 100%">
                    <el-option v-for="pk in packageList" :key="pk.packageId"
                               :label="`${pk.packageName}（￥${pk.price}）`" :value="pk.packageId"/>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="分类" width="80">
                <template #default="scope">{{ pkgOf(scope.row.packageId)?.category || '—' }}</template>
              </el-table-column>
              <el-table-column label="包含项目" min-width="140">
                <template #default="scope">
                  <span v-if="pkgOf(scope.row.packageId)?.items?.length" style="font-size:12px;color:#606266">
                    {{ pkgOf(scope.row.packageId)!.items!.map((i: any) => i.itemName).join('、') }}
                  </span>
                  <span v-else style="color:#c0c4cc">—</span>
                </template>
              </el-table-column>
              <el-table-column label="价格(元)" width="90">
                <template #default="scope">￥{{ pkgOf(scope.row.packageId)?.price ?? 0 }}</template>
              </el-table-column>
              <el-table-column width="50">
                <template #default="scope">
                  <el-button :icon="Delete" circle size="small" type="danger" @click="packages.splice(scope.$index, 1)"/>
                </template>
              </el-table-column>
            </el-table>
            <el-button :icon="Plus" plain size="small" style="margin-top: 6px; width: 100%" @click="addPackage">添加套餐</el-button>
            <div style="margin-top: 10px; text-align: right">
              <el-button size="small" type="primary" :loading="pkgSaving" :disabled="orderLocked" @click="savePkg">保存套餐（￥{{ feePackage() }}）</el-button>
            </div>
            <el-divider content-position="left">已加套餐</el-divider>
            <el-table :data="savedPkg" border size="small">
              <el-table-column label="套餐" min-width="160" prop="packageName" show-overflow-tooltip/>
              <el-table-column label="价格(元)" width="100">
                <template #default="s">￥{{ s.row.price }}</template>
              </el-table-column>
            </el-table>
            <p v-if="savedPkg.length === 0" style="color:#999">尚未添加套餐</p>
          </el-tab-pane>

          <!-- 检验 -->
          <el-tab-pane name="lab">
            <template #label>
              <span>检验<b v-if="labCount" class="tab-num">{{ labCount }}</b></span>
            </template>
            <el-alert title="开单即计入账单并扣减对应费用，可多选" :closable="false" type="info" show-icon style="margin-bottom: 10px"/>
            <template v-for="g in labGroups" :key="g">
              <div class="lab-group-title">{{ g }}</div>
              <el-checkbox-group v-model="labForm.testIds">
                <el-checkbox v-for="t in labTestsOf(g)" :key="t.testId" :label="t.testId" border>
                  {{ t.testName }}<span class="price">￥{{ t.testPrice ?? '-' }}</span>
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <p v-if="labGroups.length === 0" style="color:#999">暂无可开检验项目</p>
            <el-form-item label="检验目的" style="margin-top: 8px">
              <el-input v-model="labForm.orderNote" :rows="2" maxlength="255" placeholder="填写检验目的/临床备注，可留空" type="textarea"/>
            </el-form-item>
            <div style="text-align: right">
              <el-button size="small" type="primary" :loading="labSaving" :disabled="orderLocked" @click="submitLabOrder">确认开单（￥{{ labTotalFee() }}）</el-button>
            </div>
            <el-divider content-position="left">已开检验单</el-divider>
            <el-table :data="savedLab" border size="small">
              <el-table-column label="单号" width="70" prop="orderId"/>
              <el-table-column label="费用(元)" width="90">
                <template #default="s">￥{{ s.row.fee }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="s">
                  <el-tag v-if="s.row.status" size="small" type="success">已出结果</el-tag>
                  <el-tag v-else size="small" type="warning">待检验</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="开单时间" prop="createTime"/>
            </el-table>
            <p v-if="savedLab.length === 0" style="color:#999">尚未开检验单</p>
          </el-tab-pane>

          <!-- 影像 -->
          <el-tab-pane name="image">
            <template #label>
              <span>影像<b v-if="imageCount" class="tab-num">{{ imageCount }}</b></span>
            </template>
            <el-alert title="开单即计入账单，每次一张影像申请单" :closable="false" type="info" show-icon style="margin-bottom: 10px"/>
            <el-form-item label="检查项目">
              <el-select v-model="imageForm.itemId" filterable placeholder="选择影像项目" style="width: 100%">
                <el-option v-for="it in imageList" :key="it.itemId"
                           :label="`${it.itemName}（${it.groupName} · ￥${it.price}）`" :value="it.itemId"/>
              </el-select>
            </el-form-item>
            <el-form-item label="检查部位">
              <el-input v-model="imageForm.examPart" clearable maxlength="64" placeholder="如：右前肢 / 胸部侧位"/>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="imageForm.note" :rows="2" maxlength="255" placeholder="临床指征/申请说明，可留空" type="textarea"/>
            </el-form-item>
            <div style="text-align: right">
              <el-button size="small" type="primary" :loading="imageSaving" :disabled="orderLocked" @click="submitImageOrder">确认开单（￥{{ imageFee() }}）</el-button>
            </div>
            <el-divider content-position="left">已开影像单</el-divider>
            <el-table :data="savedImage" border size="small">
              <el-table-column label="单号" width="70" prop="orderId"/>
              <el-table-column label="项目" prop="itemName" show-overflow-tooltip/>
              <el-table-column label="部位" prop="examPart" width="90"/>
              <el-table-column label="费用(元)" width="90">
                <template #default="s">￥{{ s.row.fee }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="s">
                  <el-tag v-if="s.row.status" size="small" type="success">已出报告</el-tag>
                  <el-tag v-else size="small" type="warning">待检查</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <p v-if="savedImage.length === 0" style="color:#999">尚未开影像单</p>
          </el-tab-pane>
          <!-- 体检表：按模板逐项勾选，结果关联本次就诊并可打印/导出 -->
          <el-tab-pane name="checkup">
            <template #label><span>体检表</span></template>
            <CheckupPanel :pet-id="treatAppoint?.petId || 0" :record-id="treatRecordId || undefined"/>
          </el-tab-pane>
        </el-tabs>
        </section>
        </div>
        </div>
      </div>

      <!-- 第四步：完结结算（汇总预览 + 收尾操作） -->
      <div v-show="activeStep === 3 && treatRecordId > 0" class="treat-step">
        <el-alert v-if="orderLocked" :title="treatStatus === 2 ? '该就诊已作废' : '该就诊已完结，单据只读'"
                  show-icon :closable="false" type="warning" style="margin-bottom: 10px"/>
        <div class="settle-cards">
          <div class="settle-card">
            <div class="sc-tit">病历摘要</div>
            <p><b>诊断：</b>{{ recordForm.diagnosis || '（未填写）' }}</p>
            <p><b>医嘱：</b>{{ recordForm.advice || '（无）' }}</p>
            <p><b>复诊：</b>{{ recordForm.revisitDate || '无需复诊' }}</p>
          </div>
          <div class="settle-card">
            <div class="sc-tit">已关联单据</div>
            <p>处方 <b>{{ savedRx.length }}</b> 项 · 套餐 <b>{{ savedPkg.length }}</b> 项 · 手术 <b>{{ savedSurgery.length }}</b> 项</p>
            <p>检验 <b>{{ labCount }}</b> 单 · 影像 <b>{{ imageCount }}</b> 单</p>
            <p class="sc-tip">检验/影像在专用页面开单，完成后这里显示数量</p>
          </div>
          <div class="settle-card">
            <div class="sc-tit">费用明细</div>
            <div class="sc-sub">已确认（已保存单据）</div>
            <div class="sc-fee"><span>挂号+诊疗</span><b>￥{{ ((recordForm.feeRegister||0)+(recordForm.feeTreat||0)).toFixed(2) }}</b></div>
            <div class="sc-fee"><span>药费</span><b>￥{{ savedDrugSum().toFixed(2) }}</b></div>
            <div class="sc-fee"><span>套餐费</span><b>￥{{ savedPkgSum().toFixed(2) }}</b></div>
            <div class="sc-fee" v-if="savedSurgery.length"><span>手术费</span><b>￥{{ savedSurgerySum().toFixed(2) }}</b></div>
            <div class="sc-sub">草稿（未保存，保存后计入）</div>
            <div class="sc-fee draft" v-if="Number(feeDrug()) > 0"><span>药费草稿</span><b>￥{{ feeDrug() }}</b></div>
            <div class="sc-fee draft" v-if="Number(feePackage()) > 0"><span>套餐草稿</span><b>￥{{ feePackage() }}</b></div>
            <div class="sc-fee draft" v-if="Number(feeSurgery()) > 0"><span>手术草稿</span><b>￥{{ feeSurgery() }}</b></div>
            <div class="sc-fee sc-total"><span>合计（含草稿）</span><b>￥{{ treatTotal() }}</b></div>
          </div>
        </div>
        <div class="settle-ops">
          <el-button title="补开/追开检验单，或打印检验申请单（常规开单请在第三步「检验」页签）" @click="goOrder('d-lab')">检验补开/打印<b v-if="labCount" class="tab-num">{{ labCount }}</b></el-button>
          <el-button title="补开/追开影像单，或打印影像申请单（常规开单请在第三步「影像」页签）" @click="goOrder('d-image')">影像补开/打印<b v-if="imageCount" class="tab-num">{{ imageCount }}</b></el-button>
          <el-button @click="goDoctor('z-surgery')">手术记录</el-button>
          <el-button @click="goDoctor('z-hospital')">入院登记</el-button>
          <el-button @click="goDoctor('z-immune')">免疫接种</el-button>
          <el-button @click="goDoctor('z-follow')">写随访</el-button>
          <el-button type="primary" plain @click="printChoice = true">打印</el-button>
          <el-button type="success" plain @click="goCashier">去收银台收费</el-button>
          <el-button type="info" plain @click="openRevisions">病历版本 / 审核</el-button>
        </div>
        <p class="phase-tip">确认信息无误后点「完结就诊」；有未完成事项（待出结果/待手术/待随访）会提醒，确认后仍可强制完结。</p>
      </div>

        <!-- 宠物病史侧栏：写病历时参考历史 -->
        <aside v-if="petHistory" class="pet-history">
          <div class="ph-head">宠物病史</div>
          <div class="ph-sec">
            <div class="ph-tit">档案</div>
            <p>{{ petHistory.petName }}（{{ petHistory.petVariety || '未知品种' }} · {{ petHistory.petSex ? '公' : '母' }}）</p>
            <p>芯片：{{ petHistory.petChip || '—' }} · {{ petHistory.petSterilization ? '已绝育' : '未绝育' }}</p>
          </div>
          <div v-if="petHistory.petAllergy" class="ph-sec ph-danger">
            <div class="ph-tit">⚠ 过敏史</div>
            <p>{{ petHistory.petAllergy }}</p>
          </div>
          <div v-if="petHistory.petHistory" class="ph-sec">
            <div class="ph-tit">既往病史</div>
            <p>{{ petHistory.petHistory }}</p>
          </div>
          <div class="ph-sec">
            <div class="ph-tit">最近就诊</div>
            <p v-if="!petHistory.recentRecords.length" class="ph-empty">暂无就诊记录</p>
            <div v-for="r in petHistory.recentRecords" :key="r.recordId" class="ph-rec">
              <span class="ph-diag" :title="r.diagnosis || ''">{{ r.diagnosis || '—' }}</span>
              <span class="ph-date">{{ moveT(r.createTime) }}</span>
            </div>
          </div>
          <div v-if="petHistory.recentRx.length" class="ph-sec">
            <div class="ph-tit">近期用药</div>
            <div v-for="(x, i) in petHistory.recentRx" :key="i" class="ph-rec">
              <span class="ph-diag">{{ x.drugName }}</span>
              <span class="ph-date">{{ x.usageInfo }}</span>
            </div>
          </div>
          <div v-if="petHistory.abnormalLabs.length" class="ph-sec ph-warn">
            <div class="ph-tit">近期检验异常</div>
            <div v-for="(x, i) in petHistory.abnormalLabs" :key="i" class="ph-rec">
              <span class="ph-diag">{{ x.itemName }}</span>
              <span class="ph-date">{{ x.resultValue }}{{ x.itemUnit || '' }}</span>
            </div>
          </div>
        </aside>
      </div>

      <template #footer>
        <span class="treat-total">
          已关联：处方{{ savedRx.length }} · 套餐{{ savedPkg.length }} · 手术{{ savedSurgery.length }} · 检验{{ labCount }} · 影像{{ imageCount }}
          ｜ 合计（含草稿） <b style="color:#f56c6c">￥{{ treatTotal() }}</b>
        </span>
        <el-button @click="treatView = false">{{ orderLocked ? '关闭' : '取消' }}</el-button>
        <!-- 第一步/第二步：步骤导航 + 保存病历 -->
        <template v-if="activeStep <= 1">
          <el-button v-if="activeStep === 0" @click="goNextStep">下一步：诊断医嘱</el-button>
          <el-button v-else @click="activeStep = 0">上一步</el-button>
          <el-button :loading="treatSubmitting" type="primary" @click="submitRecord">
            {{ treatRecordId > 0 ? '保存修改' : '保存病历并开单' }}
          </el-button>
        </template>
        <!-- 第三步：处置开单 -->
        <template v-else-if="activeStep === 2">
          <el-button @click="goBackRecord">返回病历</el-button>
          <el-button v-if="!orderLocked" plain type="danger" @click="cancelTreat">作废就诊</el-button>
          <el-button :disabled="orderLocked" type="primary" @click="finishTreat">完结就诊</el-button>
        </template>
        <!-- 第四步：完结结算 -->
        <template v-else>
          <el-button v-if="!orderLocked" @click="activeStep = 2">返回开单</el-button>
          <el-button v-if="!orderLocked" plain type="danger" @click="cancelTreat">作废就诊</el-button>
          <el-button v-if="!orderLocked" type="primary" @click="finishTreat">完结就诊</el-button>
          <el-button v-else @click="treatView = false">完成</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 兽用处方笺打印前弹窗（734号：联次+补录+编号） -->
    <!-- 统一打印入口：电子病历 / 兽用处方笺（国标） -->
    <PrintChoiceDialog v-model="printChoice" :record-id="treatRecordId"/>
    <!-- 处方笺快捷打印（第3步保存处方后直接用，预设处方笺模式） -->
    <PrintChoiceDialog v-model="rxQuickView" preset="rx" :record-id="treatRecordId"/>

    <!-- 病历版本留痕与审核入口 -->
    <el-dialog v-model="revView" title="病历版本与审核" width="760px" append-to-body>
      <div class="rev-tip">每次修改病历都会保存变更前的完整快照；内容修改会使原审核结论自动失效。</div>
      <el-table :data="revisions" size="small" border max-height="340" empty-text="暂无修改记录（V1 为建档版本）">
        <el-table-column label="变更前版本" width="120">
          <template #default="s">V{{ s.row.revisionNo }}</template>
        </el-table-column>
        <el-table-column prop="editorName" label="修改人" width="110"/>
        <el-table-column prop="changeReason" label="变更原因" width="130"/>
        <el-table-column label="时间" width="170">
          <template #default="s">{{ (s.row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="变更前快照">
          <template #default="s"><el-button link type="primary" size="small" @click="showSnapshot(s.row)">查看</el-button></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="revView = false">关闭</el-button>
        <el-button type="danger" plain :disabled="!treatRecordId" @click="rejectCurrent">审核退回</el-button>
        <el-button type="primary" :disabled="!treatRecordId" @click="reviewCurrent">审核病历</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="snapshotView" title="变更前病历快照" width="640px" append-to-body>
      <pre class="rev-json">{{ snapshotText }}</pre>
    </el-dialog>

  </div>
</template>

<script lang="ts" setup>
import PageHead from "@/components/row/PageHead.vue";
import {AppointDto2} from "@/model/DO/AppointDto2";
import ShakeBtn from "@/components/button/ShakeBtn.vue";
import {reqDoctorAddAppoint, reqDoctorAppoint} from "@/request/DoctorApi";
import {reqUpdateAppoint} from "@/request/AppointApi";
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from "vue";
import {onBeforeRouteLeave} from "vue-router";
import {Appoint, exampleAppoint} from "@/model/entiy/Appoint";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import PetRegPicker from "@/components/record/PetRegPicker.vue";
import AppointTicket from "@/components/card/AppointTicket.vue";
import CheckupPanel from "@/components/checkup/CheckupPanel.vue";
import {Delete, InfoFilled, Notebook, Plus, Printer} from "@element-plus/icons-vue";
import {Drug} from "@/model/entiy/Drug";
import {reqDrugList} from "@/request/DrugApi";
import {exampleMedicalRecord, examplePrescription, MedicalRecord, Prescription} from "@/model/entiy/MedicalRecord";
import {reqAddRecordPackage, reqAddRecordPrescription, reqCancelRecord, reqFinishRecord, reqRecordByAppointment, reqRecordOne, reqRecordRevisions, reqRejectRecord, reqReviewRecord, reqSaveRecord, reqUpdateRecord} from "@/request/RecordApi";
import {reqDiagnosisList} from "@/request/DiagnosisApi";
import type {DiagnosisDict} from "@/request/DiagnosisApi";
import type {RecordRevision} from "@/model/entiy/MedicalRecord";
import {reqRecordTemplateList} from "@/request/RecordTemplateApi";
import {RecordTemplate} from "@/model/entiy/RecordTemplate";
import {reqPackageList} from "@/request/PackageApi";
import {examplePackage, RecordPackage, ServicePackage} from "@/model/entiy/ServicePackage";
import {surgeryStatusText, surgeryStatusType, Surgery, SurgeryVo} from "@/model/entiy/Surgery";
import {reqDictByType} from "@/request/DictApi";
import {SysDict} from "@/model/entiy/SysDict";
import {reqCreateSurgery, reqSurgeryByRecord} from "@/request/SurgeryApi";
import {useRoute, useRouter} from 'vue-router';
import {moveT} from "@/utils/TimeUtil";
import {getAge} from "@/utils/TimeUtil";
import {reqPetHistory} from "@/request/RecordApi";
import PrintChoiceDialog from "@/components/record/PrintChoiceDialog.vue";
import {reqMissAppoint} from "@/request/AppointApi";
import {PetHistory} from "@/model/VO/PetHistory";
import {LabTest} from "@/model/entiy/LabTest";
import {reqCreateLabOrder, reqLabByRecord, reqLabTestList} from "@/request/LabApi";
import {LabOrderDto} from "@/model/DO/LabOrderDto";
import {ImageTest} from "@/model/entiy/ImageTest";
import {reqCreateImageOrder, reqImageByRecord, reqImgGroup} from "@/request/ImageApi";
import {ImageOrder} from "@/model/entiy/ImageOrder";

const router = useRouter()
const route = useRoute()
// 移动端检测
const isMobile = ref(false)
const checkMobile = (): void => { isMobile.value = window.innerWidth < 768 }
window.addEventListener('resize', checkMobile)
checkMobile()
// 手术类型/麻醉方式字典（来自系统设置）
const surgeryTypeOpts = ref<SysDict[]>([])
const anesthesiaTypeOpts = ref<SysDict[]>([])
reqDictByType('surgery_type').then(r => { surgeryTypeOpts.value = r || [] })
reqDictByType('anesthesia_type').then(r => { anesthesiaTypeOpts.value = r || [] })

const appoint = ref<AppointDto2[]>([])
// 三个页签：待处理=今日及逾期，未来预约=明日起，已处理=完结/爽约（爽约单保留可查，不占待办位）
const appointTab = ref<'pending' | 'future' | 'done'>('pending')
const pendingAppoint = computed(() => appoint.value.filter(a => !a.isSuc && !a.miss))
const doneAppoint = computed(() => appoint.value.filter(a => !!a.isSuc || !!a.miss))
// 本地日期 YYYY-MM-DD（不用 toISOString，避免 UTC 偏移把明天的号算成今天）
const todayStr = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()
const dayOf = (a: AppointDto2): string => (a.appointmentDate || '').slice(0, 10)
const isFuture = (a: AppointDto2): boolean => !!dayOf(a) && dayOf(a) > todayStr
const todayPending = computed(() => pendingAppoint.value.filter(a => !isFuture(a)))
// 未来预约按时间升序：明天排最前，医生先看最近要来的号
const futureAppoint = computed(() => pendingAppoint.value.filter(isFuture)
    .sort((a, b) => (a.appointmentDate || '').localeCompare(b.appointmentDate || '')))
// 就诊时间已过（可标记爽约）
const isOverdue = (a: AppointDto2): boolean =>
    !!a.appointmentDate && new Date(a.appointmentDate).getTime() < Date.now()

/** 改期：医生直接改就诊时间（传全对象过后端校验，updateById 只更新变化字段） */
const rsView = ref(false)
const rsSaving = ref(false)
const rsRow = ref<AppointDto2 | null>(null)
const rsDate = ref('')
const openReschedule = (row: AppointDto2): void => {
  rsRow.value = row
  // 默认新时间 = 原时间（医生在此基础上挑）
  rsDate.value = row.appointmentDate || ''
  rsView.value = true
}
const submitReschedule = (): void => {
  if (!rsRow.value || !rsDate.value) { ElMessage.warning('请选择新的就诊时间'); return }
  rsSaving.value = true
  reqUpdateAppoint({...rsRow.value, appointmentDate: rsDate.value} as Appoint).then(res => {
    rsSaving.value = false
    if (res.code === 1) {
      ElMessage.success('已改期，患者端同步可见')
      rsView.value = false
      queryAppoint()
    } else ElMessage.error(res.msg || '改期失败')
  }).catch(() => { rsSaving.value = false })
}
/** 日期筛选（留空=全部）：三个页签共用，可把未来预约再筛到某一天 */
const appointDateFilter = ref<string>('')
const byDate = (list: AppointDto2[]): AppointDto2[] => appointDateFilter.value
    ? list.filter(a => dayOf(a) === appointDateFilter.value)
    : list
const filteredPending = computed(() => byDate(todayPending.value))
const filteredFuture = computed(() => byDate(futureAppoint.value))
const filteredDone = computed(() => byDate(doneAppoint.value))
const shownAppoint = computed(() => appointTab.value === 'future' ? filteredFuture.value
    : appointTab.value === 'done' ? filteredDone.value : filteredPending.value)
onMounted(() => {
  queryAppoint()
  // 从医生工作台「未来预约」分组/全景流水线进来：直接落在未来预约页签
  if (route.query.tab === 'future') appointTab.value = 'future'
  // 从医生工作台「去接诊」直达：自动打开指定挂号单的接诊向导
  const aid = Number(route.query.appointmentId)
  if (aid) {
    appointTab.value = 'pending'
    const tryOpen = (times: number): void => {
      const target = appoint.value.find(a => a.appointmentId === aid)
      if (target) openTreat(target)
      else if (times < 5) setTimeout(() => tryOpen(times + 1), 200)
      else ElMessage.warning('该挂号不在当前医生的近期接诊队列（可能是历史单或归属其他医生），请到病例中心打开')
    }
    tryOpen(0)
  }
})
const queryAppoint = () => {
  reqDoctorAppoint().then(res => {
    // 兜底：后端已按「全部待处理 + 近 90 天已处理」封顶 300 条；
    // 即使接口异常返回全量历史，也绝不把上万张卡片交给浏览器渲染
    const list = res || []
    appoint.value = list.length > 300 ? list.slice(0, 300) : list
    if (list.length > 300) ElMessage.info('接诊列表仅显示待处理与近 90 天记录，更早就诊请到病例中心查询')
    loadRecordStatus()
  }).catch(() => {
    appoint.value = []
    ElMessage.error('接诊队列加载失败，请刷新重试')
  })
}
// 已处理挂号对应的病历状态（0接诊中 1已完结 2已作废），用于卡片标签
const recordStatusMap = ref<Record<number, number>>({})
const loadRecordStatus = (): void => {
  doneAppoint.value.forEach(a => {
    if (recordStatusMap.value[a.appointmentId] != null) return
    reqRecordByAppointment(a.appointmentId).then(rec => {
      if (rec && rec.recordId) recordStatusMap.value[a.appointmentId] = rec.status ?? 1
    })
  })
}

/**
 ┌───────────────────────────────────┐
 │=============接诊（两阶段）=========│
 └───────────────────────────────────┘
 */
const treatView = ref(false)
const treatAppoint = ref<AppointDto2 | null>(null)
/** 宠物病史侧栏（接诊时参考：档案/过敏史/既往史/最近就诊/用药/检验异常） */
const petHistory = ref<PetHistory | null>(null)
const loadPetHistory = (petId: number | null): void => {
  if (!petId) { petHistory.value = null; return }
  reqPetHistory(petId).then(res => { petHistory.value = res })
}
// 复诊强制关联历史：进入诊断步骤前，要求先核对既往病史
const goNextStep = (): void => {
  if (recordForm.value.triageLevel === 3) {
    if (!petHistory.value) {
      ElMessage.warning('复诊需先加载该宠物的历史病历')
      const pid = treatAppoint.value?.petId
      if (pid) loadPetHistory(pid)
      return
    }
    const hasHistory = (petHistory.value.recentRecords && petHistory.value.recentRecords.length > 0)
    if (!hasHistory) {
      ElMessageBox.confirm(`该宠物标注「复诊」，但未找到其历史就诊记录。请确认是否继续（可改回普通门诊）？`, '复诊无既往病历',
          {confirmButtonText: '仍按复诊继续', cancelButtonText: '返回修改', type: 'warning'}
      ).then(() => { activeStep.value = 1 }).catch(() => ElMessage.info('可返回核对，或将分诊改为普通'))
      return
    }
  }
  activeStep.value = 1
}
const treatPhase = ref<'record' | 'orders'>('record')
const treatRecordId = ref(0)
/** 当前病历状态：0接诊中 1已完结 2已作废；非0时单据只读 */
const treatStatus = ref<number>(0)
const orderLocked = computed(() => treatStatus.value !== 0)
// 检验/影像已开单数（开单调 tab 角标）
const labCount = ref<number>(0)
const imageCount = ref<number>(0)
// 接诊向导步骤：0问诊检查 1诊断医嘱 2处置开单 3完结结算
const activeStep = ref<number>(0)
const stepTitles = ['问诊与检查', '诊断与医嘱', '处置开单', '完结结算']
const activeTab = ref('rx')
const treatSubmitting = ref(false)

const recordForm = ref<MedicalRecord>(exampleMedicalRecord(0))
const recordFormRef = ref<FormInstance>()

// 开单：待保存行（处方/手术/套餐）
const prescriptions = ref<Prescription[]>([])
const packages = ref<ServicePackage[]>([])
const needSurgery = ref(false)
const surgeryForm = ref<SurgeryVo>({recordId: null, surgeryName: '', surgeryType: null, anesthesiaType: null, scheduledTime: null, surgeon: null, assistant: null, fee: 0})

// 已保存回显
const savedRx = ref<Prescription[]>([])
const savedPkg = ref<RecordPackage[]>([])
const savedSurgery = ref<Surgery[]>([])
// 检验/影像开单（向导内嵌，替代跳转独立页面）
const tests = ref<LabTest[]>([])
const savedLab = ref<LabOrderDto[]>([])
const labForm = reactive<{ testIds: number[]; orderNote: string }>({ testIds: [], orderNote: '' })
const labSaving = ref(false)
const imageList = ref<ImageTest[]>([])
const savedImage = ref<ImageOrder[]>([])
const imageForm = reactive<{ itemId: number | null; examPart: string; note: string }>({ itemId: null, examPart: '', note: '' })
const imageSaving = ref(false)

// 保存中
const rxSaving = ref(false)
const pkgSaving = ref(false)
const surgerySaving = ref(false)

// 字典
const drugList = ref<Drug[]>([])
/** 药品下拉：ETL 后主档 600+ 条且会增长，绝不在表格每行全量渲染 el-option。
 *  远程检索只显示前 40 条命中；已选药品始终保留，避免回显成 id。 */
const drugQuery = ref('')
const filterDrug = (q: string): void => { drugQuery.value = q || '' }
const drugOptions = computed<Drug[]>(() => {
  const q = (drugQuery.value || '').trim().toLowerCase()
  const all = drugList.value
  const hit = q
    ? all.filter(d => (d.drugName || '').toLowerCase().includes(q)
        || (d.drugSpec || '').toLowerCase().includes(q)
        || String(d.drugId) === q)
    : all
  const out = hit.slice(0, 40)
  for (const p of prescriptions.value) {
    const id = Number(p.drugId)
    if (!id || out.some(d => d.drugId === id)) continue
    const d = all.find(x => x.drugId === id)
    if (d) out.unshift(d)
  }
  return out
})
const packageList = ref<ServicePackage[]>([])
const templates = ref<RecordTemplate[]>([])
const tplId = ref<number | null>(null)
const loadDicts = (): void => {
  if (drugList.value.length === 0) reqDrugList().then(res => { drugList.value = res })
  if (packageList.value.length === 0) reqPackageList().then(res => { packageList.value = res })
  if (templates.value.length === 0) reqRecordTemplateList().then(res => { templates.value = res || [] })
}
// 套用病历模板
const applyTpl = (id: number): void => {
  const t = templates.value.find(x => x.templateId === id)
  if (!t) return
  const f = recordForm.value
  // 模板=打底：只填「空」字段，不覆盖已填写/挂号自动带入的内容（防静默丢失）
  if (!f.chiefComplaint) f.chiefComplaint = t.chiefComplaint ?? ''
  if (!f.presentIllness) f.presentIllness = t.presentIllness ?? ''
  if (!f.pastHistory) f.pastHistory = t.pastHistory ?? ''
  if (!f.physicalExam) f.physicalExam = t.physicalExam ?? ''
  if (!f.diagnosis) f.diagnosis = t.diagnosis ?? ''
  if (!f.treatment) f.treatment = t.treatment ?? ''
  if (!f.advice) f.advice = t.advice ?? ''
  if (!f.internalNote) f.internalNote = t.internalNote ?? ''
}
const resetOrderForms = (): void => {
  prescriptions.value = []
  packages.value = []
  needSurgery.value = false
  surgeryForm.value = {recordId: null, surgeryName: '', surgeryType: null, anesthesiaType: null, scheduledTime: null, surgeon: null, assistant: null, fee: 0}
  savedRx.value = []
  savedPkg.value = []
  savedSurgery.value = []
  savedLab.value = []
  savedImage.value = []
  labForm.testIds = []
  labForm.orderNote = ''
  imageForm.itemId = null
  imageForm.examPart = ''
  imageForm.note = ''
  labCount.value = 0
  imageCount.value = 0
  activeTab.value = 'rx'
}

const openTreat = (a: AppointDto2): void => {
  // 重置期间暂停草稿自动保存，避免把空表单写进 storage 覆盖已有草稿
  draftPaused.value = true
  hasDraft.value = false
  window.clearTimeout(draftTimer)
  treatAppoint.value = a
  loadPetHistory(a.petId)
  recordForm.value = exampleMedicalRecord(a.appointmentId)
  recordForm.value.chiefComplaint = a.appointmentInfo || ''
  tplId.value = null
  resetOrderForms()
  recordFormRef.value?.clearValidate()
  loadDicts()
  loadLabTests()
  loadImageTests()
  treatView.value = true
  treatPhase.value = 'record'
  treatRecordId.value = 0
  activeStep.value = 0
  treatStatus.value = 0
  // 已建过病历：回显内容 + 进入开单阶段（接诊中可继续追加/修改，已完结/已作废只读）
  reqRecordByAppointment(a.appointmentId).then(rec => {
    if (rec && rec.recordId) {
      treatRecordId.value = rec.recordId
      treatStatus.value = rec.status ?? 1
      recordForm.value.chiefComplaint = rec.chiefComplaint ?? recordForm.value.chiefComplaint
      recordForm.value.presentIllness = rec.presentIllness
      recordForm.value.pastHistory = rec.pastHistory
      recordForm.value.physicalExam = rec.physicalExam
      recordForm.value.diagnosis = rec.diagnosis
      recordForm.value.treatment = rec.treatment
      recordForm.value.advice = rec.advice
      recordForm.value.internalNote = rec.internalNote
      recordForm.value.revisitDate = rec.revisitDate
      recordForm.value.triageLevel = rec.triageLevel
      recordForm.value.weight = rec.weight
      recordForm.value.temperature = rec.temperature
      recordForm.value.heartRate = rec.heartRate
      recordForm.value.respiratoryRate = rec.respiratoryRate
      recordForm.value.bloodPressure = rec.bloodPressure
      recordForm.value.differentialDiagnosis = rec.differentialDiagnosis ?? ''
      recordForm.value.diagnosisBasis = rec.diagnosisBasis ?? ''
      recordForm.value.diagnosisCode = rec.diagnosisCode ?? null
      recordForm.value.diagnosisCodeSystem = rec.diagnosisCodeSystem ?? null
      recordForm.value.reviewNote = rec.reviewNote ?? null
      if (rec.feeRegister != null) recordForm.value.feeRegister = rec.feeRegister
      if (rec.feeTreat != null) recordForm.value.feeTreat = rec.feeTreat
      // 回传版本号：后端据此做乐观锁，避免两个窗口互相覆盖病历
      recordForm.value.version = rec.version ?? 1
      // 检验/影像已开单数与列表（tab 角标用）
      loadOrders()
      // 接诊中→进入开单步骤；已完结/已作废→直达完结结算（只读汇总）
      if ((rec.status ?? 1) === 0) enterOrders()
      else { treatPhase.value = 'orders'; activeStep.value = 3; loadSaved() }
    }
    restoreDraft()
  }).catch(() => { restoreDraft() })
}
const enterOrders = (): void => {
  treatPhase.value = 'orders'
  activeStep.value = 2
  activeTab.value = 'rx'
  loadSaved()
}
// 标记爽约：预约时间已过未到店，确认后从待处理队列移除（不删病历数据）
const markMiss = (a: AppointDto2): void => {
  ElMessageBox.confirm(`确认将单号 #${a.appointmentId}（${a.petName} · ${a.clientName}）标记为爽约吗？`, '标记爽约', {
    confirmButtonText: '确认爽约', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqMissAppoint(a.appointmentId).then(res => {
      if (res.code === 1) { ElMessage.success('已标记爽约'); queryAppoint() }
      else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}
// 从开单步骤返回病历（未完绪可继续修改）
const goBackRecord = (): void => {
  treatPhase.value = 'record'
  activeStep.value = 0
}
const loadSaved = (): void => {
  reqRecordOne(treatRecordId.value).then(res => {
    if (res) {
      savedRx.value = res.prescriptions || []
      savedPkg.value = res.packages || []
      if (res.record.feeRegister != null) recordForm.value.feeRegister = res.record.feeRegister
      if (res.record.feeTreat != null) recordForm.value.feeTreat = res.record.feeTreat
      // 服务端回填属于「已保存状态」，同步基线，避免被当成医生改动写进草稿
      baseline.value = draftSnapshot()
    }
  })
  reqSurgeryByRecord(treatRecordId.value).then(r => { savedSurgery.value = r || [] })
  loadOrders()
}

// 阶段一：保存病历并开单（新建走 save；已建病历未完绪走 update 只改内容）
const submitRecord = (): void => {
  recordFormRef.value?.validate((valid: boolean): void => {
    if (!valid) return
    if (treatRecordId.value > 0) {
      if (orderLocked.value) { ElMessage.warning('该就诊已完结/已作废，不能修改'); return }
      treatSubmitting.value = true
      reqUpdateRecord({...recordForm.value, recordId: treatRecordId.value}).then(res => {
        treatSubmitting.value = false
        if (res.code === 1) {
          // 服务端版本 +1，本地同步，否则下一次修改会被乐观锁判为「已被他人修改」
          recordForm.value.version = (recordForm.value.version ?? 1) + 1
          markClean()
          enterOrders()
        }
        else ElMessage.error(res.msg || '保存失败')
      })
      return
    }
    treatSubmitting.value = true
    reqSaveRecord({
      record: recordForm.value,
      prescriptions: [],
      packages: [],
      labTestIds: [],
      imageItemId: null,
      imageExamPart: '',
      surgery: null
    }).then(res => {
      if (res.code === 1) {
        reqRecordByAppointment(treatAppoint.value!.appointmentId).then(rec => {
          treatSubmitting.value = false
          if (rec) {
            treatRecordId.value = rec.recordId
            markClean()
            enterOrders()
          }
        })
      } else {
        treatSubmitting.value = false
        ElMessage.error(res.msg || '保存失败')
      }
    })
  })
}

// 字典查找
const drugOf = (drugId: number): Drug | undefined => drugList.value.find(d => d.drugId === drugId)
// 查体快捷标签：点击即填入体格检查（半勾选式录入，不用手打常用所见）
const examTags = ['精神沉郁', '精神尚可', '黏膜苍白', '黏膜黄染', '眼结膜潮红', '被毛粗乱', '皮肤红疹',
  '脱水明显', '淋巴结肿大', '心肺音减弱', '心律不齐', '腹部触痛', '肠音亢进', '关节肿胀', '跛行', '口腔溃疡', '牙结石']
const appendExam = (tag: string): void => {
  const cur = (recordForm.value.physicalExam || '').replace(/[，,]$/, '')
  recordForm.value.physicalExam = cur ? cur + '，' + tag : tag
}

// 按已算出的单次剂量×频次×天数重算数量（频次/天数变更时触发）
const recalcDose = (row: Prescription): void => {
  if (row.singleDose == null || row.singleDose <= 0) return
  row.quantity = Math.max(1, Math.ceil(row.singleDose * (row.dailyTimes || 1) * (row.durationDays || 1)))
  row.doseQty = row.quantity
}
// 处方自动算量：药品配置了剂量规则时，算单次剂量并按 单次×频次×天数 填数量（医生可改）
const doseCalc = (row: Prescription): void => {
  const d = drugOf(row.drugId)
  if (!d) return
  const unit = d.drugUnit || ''
  if (d.doseFixed != null && d.doseFixed > 0) {
    row.singleDose = d.doseFixed
    row.singleDoseUnit = unit
    recalcDose(row)
    if (!row.usageInfo) row.usageInfo = `每次${d.doseFixed}${unit}，每日${row.dailyTimes || 1}次`
    row.doseTip = `固定单次用量 ${d.doseFixed}${unit}；数量=单次×${row.dailyTimes || 1}次/日×${row.durationDays || 1}天`
  } else if (d.dosePerKg != null && d.dosePerKg > 0) {
    const w = recordForm.value.weight
    if (w == null) {
      row.doseTip = '该药按体重算量，请先在上方「体征」填写体重'
      return
    }
    const dose = w * d.dosePerKg
    row.singleDose = Math.round(dose * 100) / 100
    row.singleDoseUnit = unit
    recalcDose(row)
    if (!row.usageInfo) row.usageInfo = `每次${row.singleDose}${unit}，每日${row.dailyTimes || 1}次`
    row.doseTip = `按体重 ${w}kg 算得单次约 ${dose.toFixed(2)}${unit}；数量=单次×${row.dailyTimes || 1}次/日×${row.durationDays || 1}天`
  } else {
    row.doseTip = ''
  }
}
const pkgOf = (packageId: number): ServicePackage | undefined => packageList.value.find(p => p.packageId === packageId)
const addPrescription = (): void => { prescriptions.value.push(examplePrescription()) }
const addPackage = (): void => { packages.value.push(examplePackage()) }

/* ===== 处方键盘流：药品→数量→频次→天数→用法 一路 Enter，末行回车自动加行（老系统录入习惯） ===== */
const cellRefs = new Map<string, { focus: () => void }>()
const setCellRef = (row: number, field: string, el: { focus: () => void } | null): void => {
  const key = `${row}-${field}`
  if (el) cellRefs.set(key, el)
  else cellRefs.delete(key)
}
const focusCell = (row: number, field: string): void => {
  nextTick(() => cellRefs.get(`${row}-${field}`)?.focus?.())
}
const onDrugPicked = (row: number, r: Prescription): void => {
  doseCalc(r)
  focusCell(row, 'qty')
}
// 用法行回车：聚焦下一行药品；已是最后一行则自动加一行再聚焦
const rxRowDone = (row: number): void => {
  if (row === prescriptions.value.length - 1) addPrescription()
  focusCell(row + 1, 'drug')
}

// 常见配伍禁忌规则（药品名关键字命中即提示，属辅助风险提醒而非硬阻断）
const drugConflicts: Array<[string, string]> = [
  ['阿莫西林', '庆大霉素'], ['恩诺沙星', '甲硝唑'], ['甲硝唑', '头孢'],
  ['多西环素', '青霉素'], ['卡托普利', '布洛芬'], ['布洛芬', '地塞米松'],
  ['阿司匹林', '地塞米松'], ['奥美拉唑', '多潘立酮'],
]
// 检出一张处方内的配伍禁忌组合（药品名含关键字即命中，兼容"阿莫西林片"等带剂型名称）
const findConflicts = (ps: Prescription[]): string[] => {
  const names = ps.filter(p => p.drugId && p.drugId > 0).map(p => p.drugName || '').filter(Boolean)
  const hits: string[] = []
  drugConflicts.forEach(([a, b]) => {
    if (names.some(n => n.includes(a)) && names.some(n => n.includes(b))) hits.push(`${a} 与 ${b}`)
  })
  return hits
}

// 分区保存
const saveRx = (): void => {
  const ps = prescriptions.value.filter(p => p.drugId && p.drugId > 0)
  if (!ps.length) { ElMessage.warning('请先添加药品'); return }
  // 配伍禁忌提示（辅助风险提醒，不阻断，确认后仍可保存）
  const conflicts = findConflicts(ps)
  if (conflicts.length) {
    ElMessageBox.confirm(
        `当前处方存在配伍禁忌：${conflicts.join('、')}。请确认是否仍按此开药？`,
        '处方配伍禁忌提醒', {confirmButtonText: '仍要开药', cancelButtonText: '返回修改', type: 'error'}
    ).then(() => { afterConflictCheck(ps) }).catch(() => ElMessage.info('已返回，请调整药品'))
    return
  }
  afterConflictCheck(ps)
}
const afterConflictCheck = (ps: Prescription[]): void => {
  // 数量与「单次剂量×频次×天数」算量结果一致性校验（防计费数量与医嘱用量脱节）
  const mismatch = ps.filter(p => p.singleDose != null && p.singleDose > 0 &&
      p.quantity !== Math.ceil(p.singleDose * (p.dailyTimes || 1) * (p.durationDays || 1)))
  const doSave = (): void => {
    rxSaving.value = true
    reqAddRecordPrescription({recordId: treatRecordId.value, prescriptions: ps}).then(res => {
      rxSaving.value = false
      if (res.code === 1) { prescriptions.value = []; loadSaved() }
      else ElMessage.error(res.msg || '保存失败')
    }).catch(() => { rxSaving.value = false })
  }
  if (mismatch.length) {
    ElMessageBox.confirm(
        `有 ${mismatch.length} 个药品的「数量」与 单次剂量×频次×天数 的算量结果不一致，可能导致计费数量与医嘱用量不符。确认按当前数量保存吗？`,
        '数量与算量不一致', {confirmButtonText: '仍要保存', cancelButtonText: '返回修改', type: 'warning'}
    ).then(doSave).catch(() => ElMessage.info('已取消，请核对数量'))
    return
  }
  doSave()
}
const savePkg = (): void => {
  const ids = packages.value.filter(p => p.packageId && p.packageId > 0).map(p => p.packageId)
  if (!ids.length) { ElMessage.warning('请先添加套餐'); return }
  pkgSaving.value = true
  reqAddRecordPackage({recordId: treatRecordId.value, packageIds: ids}).then(res => {
    pkgSaving.value = false
    if (res.code === 1) { packages.value = []; loadSaved() }
    else ElMessage.error(res.msg || '保存失败')
  }).catch(() => { pkgSaving.value = false })
}
const createSurgery = (): void => {
  if (!surgeryForm.value.surgeryName.trim()) { ElMessage.warning('请填写手术名称'); return }
  surgerySaving.value = true
  reqCreateSurgery({...surgeryForm.value, recordId: treatRecordId.value}).then(res => {
    surgerySaving.value = false
    if (res.code === 1) {
      needSurgery.value = false
      surgeryForm.value = {recordId: null, surgeryName: '', surgeryType: null, anesthesiaType: null, scheduledTime: null, surgeon: null, assistant: null, fee: 0}
      loadSaved()
    } else ElMessage.error(res.msg || '安排失败')
  }).catch(() => { surgerySaving.value = false })
}

// 费用合计
const feeSurgery = (): string => (needSurgery.value ? (surgeryForm.value.fee || 0).toFixed(2) : '0.00')
const feePackage = (): string => {
  let sum = 0
  for (const p of packages.value) {
    const pk = pkgOf(p.packageId)
    if (pk && pk.price != null) sum += pk.price
  }
  return sum.toFixed(2)
}
const feeDrug = (): string => {
  let sum = 0
  for (const p of prescriptions.value) {
    const d = drugOf(p.drugId)
    if (d && d.drugPrice != null) sum += d.drugPrice * p.quantity
  }
  return sum.toFixed(2)
}
const savedDrugSum = (): number => savedRx.value.reduce((s, p) => s + (p.drugPrice ?? 0) * (p.quantity ?? 0), 0)
const savedPkgSum = (): number => savedPkg.value.reduce((s, p) => s + (p.price ?? 0), 0)
const savedSurgerySum = (): number => savedSurgery.value.reduce((s, o) => s + (o.fee ?? 0), 0)
const treatTotal = (): string => {
  const sum = (recordForm.value.feeRegister || 0) + (recordForm.value.feeTreat || 0)
      + savedDrugSum() + savedPkgSum() + savedSurgerySum()
      + Number(feeDrug()) + Number(feePackage()) + Number(feeSurgery())
      + savedLabSum() + savedImageSum() + Number(labTotalFee()) + Number(imageFee())
  return sum.toFixed(2)
}

// ───────── 检验/影像向导内嵌开单 ─────────
const labGroups = computed(() => {
  const set: string[] = []
  tests.value.forEach(t => {
    const g = t.testGroup || '其他'
    if (!set.includes(g)) set.push(g)
  })
  return set
})
const labTestsOf = (g: string): LabTest[] => tests.value.filter(t => (t.testGroup || '其他') === g)
const labTotalFee = (): string => {
  const sum = labForm.testIds.reduce((s, id) => {
    const t = tests.value.find(x => x.testId === id)
    return s + (t?.testPrice ?? 0)
  }, 0)
  return sum.toFixed(2)
}
const imageFee = (): string => {
  const it = imageList.value.find(x => x.itemId === imageForm.itemId)
  return (it?.price ?? 0).toFixed(2)
}
const savedLabSum = (): number => savedLab.value.reduce((s, o) => s + (o.fee ?? 0), 0)
const savedImageSum = (): number => savedImage.value.reduce((s, o) => s + (o.fee ?? 0), 0)

const loadLabTests = (): void => { reqLabTestList().then(r => { tests.value = r || [] }) }
const loadImageTests = (): void => { reqImgGroup().then(r => { imageList.value = r || [] }) }
const loadOrders = (): void => {
  if (!treatRecordId.value) return
  reqLabByRecord(treatRecordId.value).then(r => {
    savedLab.value = r || []
    labCount.value = (r || []).length
  })
  reqImageByRecord(treatRecordId.value).then(r => {
    savedImage.value = r || []
    imageCount.value = (r || []).length
  })
}
const submitLabOrder = (): void => {
  if (labForm.testIds.length === 0) { ElMessage.warning('请至少勾选一项检验'); return }
  labSaving.value = true
  reqCreateLabOrder({ recordId: treatRecordId.value, orderNote: labForm.orderNote, testIds: [...labForm.testIds] })
      .then(res => {
        labSaving.value = false
        if (res.code === 1) {
          ElMessage.success('已开检验单并计入账单')
          labForm.testIds = []
          labForm.orderNote = ''
          loadOrders()
        } else ElMessage.error(res.msg || '开单失败')
      })
      .catch(() => { labSaving.value = false; ElMessage.error('开单失败') })
}
const submitImageOrder = (): void => {
  if (!imageForm.itemId) { ElMessage.warning('请选择影像项目'); return }
  imageSaving.value = true
  reqCreateImageOrder({ recordId: treatRecordId.value, itemId: imageForm.itemId, examPart: imageForm.examPart, note: imageForm.note })
      .then(res => {
        imageSaving.value = false
        if (res.code === 1) {
          ElMessage.success('已开影像单并计入账单')
          imageForm.itemId = null
          imageForm.examPart = ''
          imageForm.note = ''
          loadOrders()
        } else ElMessage.error(res.msg || '开单失败')
      })
      .catch(() => { imageSaving.value = false; ElMessage.error('开单失败') })
}

// 完结就诊：有未完成事项时确认后强制完结，完结后进入结算步骤
const finishTreat = (): void => {
  reqFinishRecord(treatRecordId.value, false).then(res => {
    if (res.code === 1) { showSummary(); return }
    // 有未完成事项（待出结果/待手术/待随访）：确认后强制完结
    ElMessageBox.confirm(res.msg + '。确认仍要完结吗？', '存在未完成事项', {
      confirmButtonText: '仍要完结', cancelButtonText: '返回继续处理', type: 'warning'
    }).then(() => {
      reqFinishRecord(treatRecordId.value, true).then(r2 => {
        if (r2.code === 1) showSummary()
        else ElMessage.error(r2.msg || '完结失败')
      })
    }).catch(() => ElMessage.info('已取消，可继续处理'))
  })
}
const showSummary = (): void => {
  treatStatus.value = 1
  clearDraft()
  if (treatAppoint.value) recordStatusMap.value[treatAppoint.value.appointmentId] = 1
  // 完结后直达结算步骤（汇总预览+收尾操作）
  activeStep.value = 3
  queryAppoint()
}
// 作废就诊（仅接诊中，需填原因）
const cancelTreat = (): void => {
  ElMessageBox.prompt('填写作废原因（必填）：', '作废就诊', {
    confirmButtonText: '确认作废', cancelButtonText: '取消', inputType: 'textarea',
    inputPattern: /\S+/, inputErrorMessage: '请填写作废原因'
  }).then(({value}) => {
    reqCancelRecord(treatRecordId.value, value).then(res => {
      if (res.code === 1) {
        ElMessage.success('已作废')
        clearDraft()
        treatStatus.value = 2
        if (treatAppoint.value) recordStatusMap.value[treatAppoint.value.appointmentId] = 2
        treatView.value = false
        queryAppoint()
      } else ElMessage.error(res.msg || '作废失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}

// 打印预览：模板宫格（国标病历 / 彩色详情版 / 处方笺），iframe 实时预览
// 打印选择弹窗（电子病历 / 兽用处方笺）
const printChoice = ref(false)
// 处方笺快捷打印弹窗（第3步处方区进入，直接进处方笺模式）
const rxQuickView = ref(false)
const goOrder = (path: string): void => {
  router.push('/doctorPage/' + path + '?recordId=' + treatRecordId.value)
}
// 临床动作统一走医生端（组件与后台共用，不再切到管理后台语境）
const goDoctor = (path: string): void => {
  router.push('/doctorPage/' + path + '?recordId=' + treatRecordId.value)
}
// 去收银台：直达待收费列表（完结生成的账单在那里收款/发药）
const goCashier = (): void => {
  // 收费/发药已收进医生工作台，不再跳出到 /backstage 的另一套信息架构
  router.push('/doctorPage/y-billList?payStatus=0')
}

const recordRules = reactive<FormRules>({
  "chiefComplaint": [{required: true, message: '请填写主诉', trigger: 'blur'}],
  "diagnosis": [{required: true, message: '请填写诊断', trigger: 'blur'}],
})

/**
 ┌───────────────────────────────────┐
 │=============新增挂号==============│
 └───────────────────────────────────┘
 */
const modalView = ref(false)
const petRegRef = ref<InstanceType<typeof PetRegPicker>>()
const ticketRef = ref<InstanceType<typeof AppointTicket>>()
const form = ref<Appoint>(exampleAppoint())
const myFormRef = ref<FormInstance>()
const addRoleB = (): void => {
  form.value = exampleAppoint()
  myFormRef.value?.resetFields()
  modalView.value = true
}
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      // 成功后才关窗：失败时保留已填的宠物/时间/描述，并给出原因
      reqDoctorAddAppoint(form.value).then(res => {
        if (res.code === 1) {
          modalView.value = false
          queryAppoint()
          // 新增成功（后端随 data 返回单号）弹凭证卡；医生端即接诊医生本人，不再展示医生行
          if (res.data) {
            const arc = petRegRef.value?.archive
            ticketRef.value?.open({
              appointId: res.data,
              date: <string>form.value.appointmentDate,
              petName: arc?.pet.petName || '',
              clientName: arc?.client?.clientName || '',
              departmentName: '',
              doctorName: ''
            })
          }
        } else ElMessage.error(res.msg || '挂号保存失败，请核对号源后重试')
      }).catch(() => ElMessage.error('挂号保存失败，请检查网络后重试'))
    }
  })
}
const rules = reactive<FormRules>({
  "appointmentInfo": [{required: true, message: '请输入描述', trigger: 'blur'}],
  "appointmentDate": [{required: true, message: '请输入日期', trigger: 'blur'}],
  "clientId": [{required: true, message: '请检索并选择宠物', trigger: 'change'}],
  "petId": [{required: true, message: '请检索并选择宠物', trigger: 'change'}],
})

/* ===== 诊断编码字典（SOAP 的 A：结构化编码 + 体系） ===== */
const diagnosisOptions = ref<DiagnosisDict[]>([])
const loadDiagnosis = (): void => {
  reqDiagnosisList().then(list => { diagnosisOptions.value = list || [] })
      .catch(() => { /* 字典不可用时仍可手填诊断，不阻塞接诊 */ })
}
const onDiagnosisPick = (code: string | null): void => {
  const hit = diagnosisOptions.value.find(d => d.code === code)
  recordForm.value.diagnosisCodeSystem = hit?.codeSystem ?? null
}
onMounted(loadDiagnosis)

/* ===== 病历版本留痕与审核 ===== */
const revView = ref(false)
const revisions = ref<RecordRevision[]>([])
const snapshotView = ref(false)
const snapshotText = ref('')
const openRevisions = (): void => {
  if (!treatRecordId.value) { ElMessage.warning('该挂号单尚未建立病历'); return }
  reqRecordRevisions(treatRecordId.value).then(list => {
    revisions.value = list || []
    revView.value = true
  }).catch(() => ElMessage.error('版本历史加载失败'))
}
const showSnapshot = (row: RecordRevision): void => {
  try { snapshotText.value = JSON.stringify(JSON.parse(row.snapshotJson || '{}'), null, 2) }
  catch { snapshotText.value = row.snapshotJson || '{}' }
  snapshotView.value = true
}
const reviewCurrent = (): void => {
  reqReviewRecord(treatRecordId.value).then(res => {
    if (res.code === 1) { ElMessage.success(res.msg || '审核完成'); openRevisions() }
    else ElMessage.error(res.msg || '审核失败')
  }).catch(() => ElMessage.error('审核失败'))
}
/** 审核退回：附理由，后端把病历状态改回接诊中，医生才能按意见修订 */
const rejectCurrent = (): void => {
  ElMessageBox.prompt('请填写退回理由（医生修订后需重新完结并签名）', '审核退回', {
    confirmButtonText: '确认退回', cancelButtonText: '取消', inputType: 'textarea',
    inputValidator: (v: string) => (v && v.trim().length >= 2) || '请填写至少 2 个字'
  }).then(({value}) => {
    reqRejectRecord(treatRecordId.value, value).then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '已退回')
        revView.value = false
        treatStatus.value = 0
        activeStep.value = 0
      } else ElMessage.error(res.msg || '退回失败')
    }).catch(() => ElMessage.error('退回失败'))
  }).catch(() => { /* 取消 */ })
}

/* ===== 接诊向导草稿：自动暂存到 sessionStorage，误关/刷新后可恢复 ===== */
type TreatDraft = {
  savedAt: number
  recordId: number
  step: number
  form: MedicalRecord
  tplId: number | null
  prescriptions: Prescription[]
  packages: ServicePackage[]
  needSurgery: boolean
  surgeryForm: SurgeryVo
  lab: { testIds: number[]; orderNote: string }
  image: { itemId: number | null; examPart: string; note: string }
}
const DRAFT_PREFIX = 'mixpet_treat_draft_'
const DRAFT_TTL = 24 * 60 * 60 * 1000
const hasDraft = ref(false)
const draftPaused = ref(false)
const lastSavedAt = ref(0)
const savingPending = ref(false)
let draftTimer: number | undefined
const draftKey = (aid: number): string => DRAFT_PREFIX + aid
/** 只序列化医生录入内容（不含步骤/时间戳），用于判断是否真有未保存修改 */
const draftSnapshot = (): string => JSON.stringify({
  f: recordForm.value,
  ps: prescriptions.value,
  pk: packages.value,
  su: needSurgery.value ? surgeryForm.value : null,
  lab: labForm,
  img: imageForm
})
const baseline = ref('')
const draftPayload = (): TreatDraft => ({
  savedAt: Date.now(),
  recordId: treatRecordId.value,
  step: activeStep.value,
  form: recordForm.value,
  tplId: tplId.value,
  prescriptions: prescriptions.value,
  packages: packages.value,
  needSurgery: needSurgery.value,
  surgeryForm: surgeryForm.value,
  lab: {testIds: [...labForm.testIds], orderNote: labForm.orderNote},
  image: {itemId: imageForm.itemId, examPart: imageForm.examPart, note: imageForm.note}
})
const clearDraft = (): void => {
  window.clearTimeout(draftTimer)
  const aid = treatAppoint.value?.appointmentId
  if (aid) {
    try { localStorage.removeItem(draftKey(aid)) } catch { /* 隐私模式下 storage 不可用，忽略 */ }
  }
  hasDraft.value = false
  lastSavedAt.value = 0
}
/** 内容已落库：清掉草稿并把当前状态记为新基线 */
const markClean = (): void => {
  clearDraft()
  baseline.value = draftSnapshot()
}
const persistDraft = (): void => {
  if (draftPaused.value || !treatView.value || orderLocked.value || !treatAppoint.value) { savingPending.value = false; return }
  if (draftSnapshot() === baseline.value) { savingPending.value = false; return }
  try {
    localStorage.setItem(draftKey(treatAppoint.value.appointmentId), JSON.stringify(draftPayload()))
    hasDraft.value = true
    lastSavedAt.value = Date.now()
  } catch { /* 容量/隐私模式：草稿不可用，仍由离开确认兜底 */ }
  savingPending.value = false
}
// 深度监听全部录入项，400ms 节流，避免每敲一个字就写一次 storage
watch([recordForm, prescriptions, packages, needSurgery, surgeryForm, labForm, imageForm], () => {
  savingPending.value = true
  window.clearTimeout(draftTimer)
  draftTimer = window.setTimeout(persistDraft, 400)
}, {deep: true})
const restoreDraft = (): void => {
  const aid = treatAppoint.value?.appointmentId
  if (!aid) { baseline.value = draftSnapshot(); draftPaused.value = false; return }
  let raw: string | null = null
  try { raw = localStorage.getItem(draftKey(aid)) } catch { raw = null }
  if (orderLocked.value) {
    clearDraft()
  } else if (raw) {
    let d: TreatDraft | null = null
    try { d = JSON.parse(raw) as TreatDraft } catch { d = null }
    if (!d || !d.savedAt || Date.now() - d.savedAt > DRAFT_TTL || d.recordId !== treatRecordId.value) {
      clearDraft()
      ElMessage.info('本地草稿已过期或与当前病历不一致，已清除')
    } else {
      Object.assign(recordForm.value, d.form || {})
      tplId.value = d.tplId ?? null
      prescriptions.value = d.prescriptions || []
      packages.value = d.packages || []
      needSurgery.value = !!d.needSurgery
      if (d.surgeryForm) surgeryForm.value = d.surgeryForm
      labForm.testIds = d.lab?.testIds || []
      labForm.orderNote = d.lab?.orderNote || ''
      imageForm.itemId = d.image?.itemId ?? null
      imageForm.examPart = d.image?.examPart || ''
      imageForm.note = d.image?.note || ''
      activeStep.value = Math.min(3, Math.max(0, d.step ?? 0))
      hasDraft.value = true
      lastSavedAt.value = d.savedAt
      ElMessage.success('已恢复上次未保存的接诊草稿')
    }
  }
  baseline.value = draftSnapshot()
  draftPaused.value = false
}
/** 主动丢弃草稿：重新从服务端载入该挂号单 */
const discardDraft = (): void => {
  const a = treatAppoint.value
  if (!a) return
  ElMessageBox.confirm('清空本地草稿并重新载入服务端病历？未保存的修改将丢失。', '清空草稿',
      {confirmButtonText: '清空并重载', cancelButtonText: '取消', type: 'warning'})
      .then(() => { clearDraft(); openTreat(a) }).catch(() => { /* 继续编辑 */ })
}

/* ===== 接诊向导防误关：有草稿或相对基线有改动时离开前确认 ===== */
const treatDirty = computed<boolean>(() => treatView.value && !orderLocked.value &&
    (hasDraft.value || draftSnapshot() !== baseline.value))
/** 实时自动保存状态：让医生清楚"内容已安全暂存"，消除"怕丢"的焦虑 */
const autosaveText = computed<string>(() => {
  if (!treatView.value || orderLocked.value) return ''
  if (savingPending.value) return '正在自动保存…'
  if (treatDirty.value && !lastSavedAt.value) return '有未保存的修改'
  if (lastSavedAt.value) return '已自动保存 ' + new Date(lastSavedAt.value).toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit'})
  return ''
})
const flushDraft = (): void => {
  window.clearTimeout(draftTimer)
  persistDraft()
}
const beforeCloseTreat = (done: () => void): void => {
  if (!treatDirty.value) { done(); return }
  ElMessageBox.confirm('接诊内容已自动暂存，下次打开该挂号单可继续。确定关闭向导吗？', '提示', {type: 'warning'})
      .then(() => { flushDraft(); done() }).catch(() => { /* 继续编辑 */ })
}
const guardUnload = (e: BeforeUnloadEvent): void => {
  if (!treatDirty.value) return
  e.preventDefault()
  e.returnValue = ''
}
onMounted(() => window.addEventListener('beforeunload', guardUnload))
onBeforeUnmount(() => {
  window.clearTimeout(draftTimer)
  window.removeEventListener('beforeunload', guardUnload)
})
onBeforeRouteLeave(async () => {
  if (!treatDirty.value) return true
  try {
    await ElMessageBox.confirm('接诊内容已自动暂存，可稍后继续。确定离开吗？', '提示', {type: 'warning'})
    flushDraft()
    return true
  } catch { return false }
})
</script>

<style lang="scss" scoped>
.appoint-tabbar { margin: 0 80px 12px; }
.appoint-tab {
  margin-right: 10px;
  padding: 8px 22px;
  border: 1px solid #dcdfe6;
  border-radius: 18px;
  background-color: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all .2s;
  &:hover { border-color: #409eff; color: #409eff; }
  &.active { background-color: #409eff; border-color: #409eff; color: #fff; }
  /* 有未来预约时给页签一个柔和提示，未选中也能一眼看到「明天有号」 */
  &.has-future:not(.active) { border-color: #20b2aa; color: #159b93; background-color: #f0fbfa; }
  .tab-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #20b2aa; margin-left: 6px; vertical-align: middle; }
  &.active .tab-dot { background: #fff; }
}
.fee-sep { margin: 0 12px 0 24px; color: #909399; }
.auto-save-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #67c23a; margin-right: 5px; vertical-align: middle; }
.auto-save-dot.saving { background: #e6a23c; animation: asPulse 1s infinite; }
@keyframes asPulse { 0%,100% { opacity: 1; } 50% { opacity: .3; } }
.fee-tip { font-size: 12px; color: #E6A23C; line-height: 1.5; margin-top: 2px; }
.sc-sub {
  font-size: 11px;
  color: #909399;
  margin: 6px 0 2px;
  font-weight: 600;
}
.sc-fee.draft { color: #a8abb2; b { color: #a8abb2; } }
.phase-tip { margin: 4px 0 0 90px; font-size: 12px; color: #909399; }
/* 第三步：左病历摘要 · 右开单区 单屏布局 */
.order-layout {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.order-side {
  flex: 0 0 250px;
  padding: 12px 14px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  position: sticky;
  top: 0;

  .side-tit {
    font-size: 13px;
    font-weight: 700;
    color: #303133;
    margin-bottom: 8px;
  }
  .side-field {
    display: flex;
    gap: 8px;
    font-size: 13px;
    margin-bottom: 6px;

    .k { color: #909399; flex-shrink: 0; }
    .v { color: #303133; font-weight: 600; word-break: break-all; }
  }
  .side-fee {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin: 10px 0 6px;
    padding-top: 8px;
    border-top: 1px dashed #dcdfe6;
    font-size: 13px;
    color: #606266;

    b { color: #f56c6c; font-size: 16px; }
  }
  .side-orders {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;

    span {
      padding: 2px 8px;
      border-radius: 10px;
      background: #eceff4;
      color: #909399;
      font-size: 12px;

      &.on { background: #e1f0e1; color: #67c23a; font-weight: 600; }
    }
  }
  .side-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #c0c4cc;
    line-height: 1.5;
  }
}
.order-main { flex: 1; min-width: 0; }
.treat-total { float: left; font-size: 15px; }

/* 检验/影像向导内嵌开单 */
.lab-group-title { margin: 6px 0; font-size: 12px; font-weight: 700; color: #1E6FA8; }
.price { margin-left: 4px; font-size: 11px; color: #f56c6c; }

/* 步骤标题行：分区标题 + 右侧 AI 辅助入口 */
.step-head {
  display: flex;
  align-items: center;
  gap: 10px;

  .el-divider {
    flex: 1;
    margin: 12px 0 8px;
  }
}

/* 处方体重提示条 */
.rx-weight-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  margin-bottom: 8px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 8px;
  font-size: 12px;
  color: #8a6d3b;

  b { color: #E6A23C; }
}

/* 算量提示：默认黄(算量依据)；医生改过数量=红(与算量不符) */
.dose-tip {
  margin-top: 2px;
  font-size: 11px;
  line-height: 1.35;
  color: #67C23A;

  &.changed { color: #F56C6C; font-weight: 600; }
}

/* 查体快捷标签 */
.exam-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}
.exam-tag {
  padding: 2px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all .2s;
  &:hover { background: #409eff; color: #fff; }
}

/* AI 辅助抽屉 */
.ai-tit {
  margin: 14px 0 8px;
  font-size: 14px;
  font-weight: 700;
  color: #1F2D3D;
}
.ai-tags { line-height: 2; }
.ai-line {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}

/* tab 数字角标 */
.tab-num {
  display: inline-block;
  min-width: 16px;
  margin-left: 4px;
  padding: 0 4px;
  background: #f56c6c;
  color: #fff;
  border-radius: 8px;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  font-weight: 600;
}

/* 向导步骤容器：固定最小高度，切换步骤不跳动 */
.treat-step { min-height: 340px; }

/* 完结结算卡片 */
.settle-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}
.settle-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 14px;
  background: #fafbfc;
  .sc-tit { font-size: 13px; font-weight: 700; color: #1F2D3D; margin-bottom: 8px; }
  p { margin: 4px 0; font-size: 12px; color: #606266; line-height: 1.6; }
  .sc-tip { color: #c0c4cc; font-size: 11px; }
}
.sc-fee {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #606266;
  padding: 3px 0;
  b { color: #1F2D3D; }
  &.sc-total {
    border-top: 1px dashed #dcdfe6;
    margin-top: 4px;
    padding-top: 6px;
    b { color: #f56c6c; font-size: 14px; }
  }
}
.settle-ops {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  background: #f8f9fb;
  border-radius: 10px;
}

/* 接诊弹窗：左表单 + 右病史侧栏 */
.treat-body { display: flex; gap: 14px; align-items: flex-start; }
.treat-main { flex: 1; min-width: 0; }
.pet-history {
  width: 260px;
  flex-shrink: 0;
  max-height: 66vh;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fafbfc;

  .ph-head { font-weight: 700; color: #1F2D3D; margin-bottom: 8px; font-size: 14px; }
  .ph-sec { margin-bottom: 10px; p { margin: 2px 0; font-size: 12px; color: #606266; line-height: 1.5; } }
  .ph-tit { font-size: 12px; font-weight: 600; color: #909399; margin-bottom: 3px; }
  .ph-rec { display: flex; justify-content: space-between; gap: 8px; font-size: 12px; padding: 2px 0; }
  .ph-date { color: #909399; flex-shrink: 0; }
  .ph-diag { color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .ph-empty { color: #c0c4cc; }
  .ph-danger { background: #fef0f0; border-radius: 6px; padding: 6px; .ph-tit { color: #f56c6c; } p { color: #dd6161; } }
  .ph-warn { background: #fdf6ec; border-radius: 6px; padding: 6px; .ph-tit { color: #e6a23c; } }
}
@media (max-width: 900px) {
  .treat-body { flex-direction: column; }
  /* 病史侧栏窄屏不丢：置顶通栏 + 限高滚动（过敏史/既往史移动端也要可见） */
  .pet-history {
    display: block;
    order: -1;
    width: 100%;
    max-height: 200px;
    overflow-y: auto;
    margin-bottom: 10px;
  }
}
/* 移动端接诊向导简化：隐藏复杂表单，只保留查看和关键操作 */
@media (max-width: 768px) {
  .treat-step { min-height: auto; }
  .order-layout { flex-direction: column; }
  .order-side { flex: none; width: 100%; position: static; }
  .exam-tags { display: none; }
  .rx-weight-bar { font-size: 11px; padding: 4px 8px; }
  .settle-cards { grid-template-columns: 1fr; }
  .settle-ops { flex-direction: column; }
  .settle-ops .el-button { width: 100%; margin: 0; }
}
.summary-btns { margin-top: 16px; text-align: right; }
.rev-tip { font-size: 12px; color: #64748b; margin-bottom: 10px; }
.rev-json { max-height: 420px; overflow: auto; padding: 12px; border-radius: 8px; background: #f6f8fa; font-size: 12px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; }

/* ===== 电子病历录入（问诊与检查）现代样式 2026-10-24 ===== */

/* 病历模板一键填充条 */
.rec-tpl-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  margin-bottom: 14px;
  background: linear-gradient(135deg, #f0f7fc 0%, #f8fbff 100%);
  border: 1px solid #c6e2ff;
  border-radius: 12px;

  .rec-tpl-ico {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--brand) 0%, #4ba3e0 100%);
    border-radius: 10px;
    color: #fff;
    font-size: 20px;
  }

  .rec-tpl-main {
    flex: 1;
    min-width: 0;

    .rec-tpl-tit {
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 1px;
      color: var(--brand);
      margin-bottom: 4px;
    }
  }
}

/* 步骤标题（替换 el-divider） */
.step-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 16px 0 10px;

  .step-tit {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 700;
    color: #1F2D3D;

    .step-no {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 22px;
      height: 22px;
      background: linear-gradient(135deg, var(--brand) 0%, #4ba3e0 100%);
      color: #fff;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 700;
    }
  }
}

/* 问诊卡片组 */
.rec-card {
  background: #fff;
  border: 1px solid #EBEEF5;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 10px;
  transition: border-color .15s;

  &:focus-within { border-color: #c6e2ff; }

  /* 主诉：重点卡（品牌浅底+左侧色条） */
  &.key {
    background: #F8FBFF;
    border-left: 3px solid var(--brand);
  }

  .rec-card-tit {
    display: flex;
    align-items: baseline;
    gap: 8px;
    font-size: 13px;
    font-weight: 600;
    color: #1F2D3D;
    margin-bottom: 6px;

    em {
      font-style: normal;
      font-size: 11px;
      font-weight: 400;
      color: #a8abb2;
    }
  }

  /* 卡片内表单去掉 label 占位（label-width:0 后无残留） */
  .el-form-item { margin-bottom: 0; }
}

/* 体征网格：3列（窄屏2列） */
.rec-vitals {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px 14px;
  padding: 12px 14px;
  background: #F8FAFC;
  border: 1px solid #EBEEF5;
  border-radius: 12px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  /* 网格内表单项：标签顶部显示 */
  .vital-item {
    margin-bottom: 0;

    :deep(.el-form-item__label) {
      position: static;
      display: block;
      text-align: left;
      padding: 0 0 4px;
      font-size: 11px;
      color: #909399;
      line-height: 1.4;
    }

    :deep(.el-form-item__content) {
      margin-left: 0 !important;
    }
  }
}

</style>
