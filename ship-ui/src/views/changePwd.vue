<template>
  <div class="change-page">
    <div class="change-card">
      <div class="header">
        <div class="icon">🔐</div>
        <h2>请先修改初始密码</h2>
        <p>为了账号安全，首次登录需修改初始密码后才能使用系统</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="原密码" prop="oldPwd">
          <el-input v-model="form.oldPwd" type="password" show-password placeholder="初始密码通常是 123456"/>
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="form.newPwd" type="password" show-password placeholder="至少 6 位"/>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPwd">
          <el-input v-model="form.confirmPwd" type="password" show-password placeholder="再次输入新密码"/>
        </el-form-item>
      </el-form>
      <el-button :loading="loading" style="width:100%" type="primary" @click="submit">
        确认修改
      </el-button>
      <div class="footer-tip">修改成功后即可正常使用系统</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {reqUpdatePwd} from "@/request/ClientApi";

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  oldPwd: "",
  newPwd: "",
  confirmPwd: ""
})

const rules: FormRules = {
  oldPwd: [{required: true, message: "请输入原密码", trigger: "blur"}],
  newPwd: [
    {required: true, message: "请输入新密码", trigger: "blur"},
    {min: 6, message: "新密码至少6位", trigger: "blur"}
  ],
  confirmPwd: [
    {required: true, message: "请再次输入新密码", trigger: "blur"},
    {
      validator: (_r: unknown, v: string, cb: (e?: Error) => void) => {
        if (v !== form.newPwd) cb(new Error("两次输入的密码不一致"))
        else cb()
      },
      trigger: "blur"
    }
  ]
}

const submit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  reqUpdatePwd({oldPwd: form.oldPwd, newPwd: form.newPwd}).then(res => {
    loading.value = false
    if (res.code === 1) {
      ElMessage.success("密码修改成功")
      setTimeout(() => router.push("/reception"), 600)
    } else {
      ElMessage.error(res.msg || "修改失败")
    }
  }).catch(() => {
    loading.value = false
  })
}
</script>

<style lang="scss" scoped>
.change-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #E8F5EE 0%, #F5F7FA 55%, #EAF3FA 100%);
  padding: 20px;
  box-sizing: border-box;
}

.change-card {
  width: 420px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 36px 32px 28px;
  box-shadow: 0 10px 40px rgba(31, 45, 61, 0.10);
  box-sizing: border-box;
}

.header {
  text-align: center;
  margin-bottom: 24px;

  .icon {
    font-size: 36px;
    margin-bottom: 12px;
  }

  h2 {
    margin: 0 0 8px;
    font-size: 20px;
    color: #1F2D3D;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #909399;
    line-height: 1.5;
  }
}

.footer-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

@media (max-width: 480px) {
  .change-card {
    padding: 28px 20px 22px;
  }
}
</style>
