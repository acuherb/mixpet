<template>
  <div>
    <PageHead>医院资料</PageHead>
    <el-card shadow="never" style="margin: 0 80px 40px">
      <el-alert :closable="false" show-icon type="info" style="margin-bottom: 16px"
                title="此处填写的信息将一键生效于：前台首页页脚、浏览器标题、病历打印抬头、收费小票与 A4 发票"/>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 640px">
        <el-form-item label="医院名称" prop="name">
          <el-input v-model="form.name" clearable maxlength="64" placeholder="如：爱康宠物医院"/>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" clearable maxlength="32" placeholder="如：0755-1234567"/>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" clearable maxlength="160" placeholder="医院地址（单据抬头展示）"/>
        </el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="form.bizHours" clearable maxlength="64" placeholder="如：09:00 - 20:00"/>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.intro" :rows="2" clearable maxlength="255" placeholder="一句话介绍/宣传文案（页脚展示）" type="textarea"/>
        </el-form-item>
        <el-form-item label="LOGO">
          <!-- 上传框在表单流内占位（UpImg 默认 absolute 是为弹窗头像设计的，本页需常规流） -->
          <div class="logo-uploader-wrap">
            <UpImg :photo-nm="form.logo || ''" @upPhoto="onLogo"/>
          </div>
          <div class="form-tip">上传后保存即生效（复用公共下载接口）；未上传时各品牌位显示默认标识</div>
        </el-form-item>
        <el-form-item label="品牌主题色">
          <div class="brand-color-row">
            <el-color-picker v-model="form.brandColor" :predefine="brandPresets" size="small"/>
            <span v-for="c in brandPresets" :key="c" class="bc-dot" :style="{background: c}" title="点击选用" @click="form.brandColor = c"/>
            <span class="color-tip">按钮/渐变/图标主色调</span>
          </div>
          <div class="form-tip">保存后即时生效；页面背景与页脚会随主题色联动调整</div>
        </el-form-item>
        <el-form-item>
          <el-button :loading="saving" type="primary" @click="submit">保存设置</el-button>
          <el-button style="margin-left:8px" @click="reset">还原</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import PageHead from "@/components/row/PageHead.vue";
import UpImg from "@/components/UpImg.vue";
import {emptyHospital, HospitalSetting} from "@/model/entiy/HospitalSetting";
import {reqHospitalSetting, reqSaveHospitalSetting} from "@/request/SettingApi";
import {loadHospital} from "@/utils/hospital";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

const form = ref<HospitalSetting>(emptyHospital())
// 品牌色预设（多医院交付可快速切换）
const brandPresets = ['#1E6FA8', '#2BB6A8', '#7C5CBF', '#E6A23C', '#F56C6C', '#3A7D44', '#0F4C81', '#5B8C5A']
const formRef = ref<FormInstance>()
const saving = ref(false)
const rules = reactive<FormRules>({
  name: [{required: true, message: '请输入医院名称', trigger: 'blur'}],
  phone: [{required: true, message: '请输入联系电话', trigger: 'blur'}]
})

const onLogo = (f: string): void => { form.value.logo = f }

onMounted(() => {
  reqHospitalSetting().then(res => { form.value = res || emptyHospital() })
})

const reset = (): void => {
  reqHospitalSetting().then(res => { form.value = res || emptyHospital() })
}

const submit = (): void => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    saving.value = true
    reqSaveHospitalSetting(form.value).then((res: Res) => {
      saving.value = false
      if (res.code === 1) {
        // 刷新全局单例，让页脚/title/品牌色立即更新
        loadHospital(true)
      } else ElMessage.error(res.msg || '保存失败')
    }).catch(() => { saving.value = false })
  })
}
</script>

<style lang="scss" scoped>
/* 品牌色选择行 */
.brand-color-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  .bc-dot {
    width: 18px;
    height: 18px;
    border-radius: 50%;
    cursor: pointer;
    border: 2px solid #fff;
    box-shadow: 0 0 0 1px #dcdfe6;
    transition: transform .15s;

    &:hover { transform: scale(1.15); }
  }

  .color-tip { font-size: 12px; color: #909399; }
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 2px;
}
/* LOGO 上传框：覆盖 UpImg 默认的绝对定位（本页需常规文档流，避免与下方主题色行重叠） */
.logo-uploader-wrap {
  :deep(.avatar-uploader) {
    position: relative !important;
    right: auto !important;
    top: auto !important;
    left: auto !important;
    margin: 0 !important;
  }
}
</style>
