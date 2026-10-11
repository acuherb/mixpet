<template>
  <div>
    <!--顶部操作-->
    <div class="pet-head">
      <el-button :icon="Plus" round type="primary" @click="openAddDialog">添加宠物</el-button>
    </div>
    <div v-for="pet in petList" :key="pet.petId" class="pa-card">
      <div class="my-row-2">
        <div class="left">
          <img :src="'/api/common/download?name='+pet.petPhoto" alt="宠物" class="my-avatar"/>
        </div>
        <div class="right">
          <el-descriptions :column="descColumn" border>
            <template #title>
              <span>{{ pet.petName }}</span>
              <el-tag v-if="pet.active === false" size="small" style="margin-left: 8px" type="warning">审核中</el-tag>
            </template>
            <el-descriptions-item
                align="center"
                label="品种"
                label-align="right">
              <el-tag>{{ pet.petVariety }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item align="center" label="健康状况" label-align="right">
              <el-tag>{{ pet.petStatus }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item align="center" label="年龄" label-align="right">
              {{ getAge(pet.petBirthday)}}
            </el-descriptions-item>
            <el-descriptions-item align="center" label="性别" label-align="right">
              <TagSex :sex="pet.petSex"/>
            </el-descriptions-item>
            <el-descriptions-item align="center" label="操作" label-align="right">
              <el-button type="success" @click="$router.push('/reception/petOne/'+pet.petId)">详情
              </el-button>
              <el-button type="danger" @click="returnPet(pet)">退养</el-button>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </div>
    <el-empty v-if="petList.length===0" description="暂无宠物，点上方「添加宠物」登记您的宠物吧"/>

    <!--添加宠物弹窗-->
    <el-dialog v-model="addVisible" title="添加宠物" top="6vh" width="90%" style="max-width: 480px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <!--头像（居中显示）-->
        <div class="photo-wrap">
          <UpImg :photoNm="addForm.petPhoto" @upPhoto="changePhoto"/>
          <div class="photo-tip">点击虚线框上传头像（JPG/PNG，≤3MB，可选）</div>
        </div>

        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="姓名" prop="petName">
          <el-input v-model="addForm.petName" clearable maxlength="16" placeholder="宠物姓名"/>
        </el-form-item>
        <el-form-item label="品种" prop="petVariety">
          <el-input v-model="addForm.petVariety" clearable maxlength="16" placeholder="如：英国短毛猫"/>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="addForm.petSex">
            <el-radio :label="true" border>公</el-radio>
            <el-radio :label="false" border>母</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
              v-model="addForm.petBirthday"
              format="YYYY-MM-DD"
              placeholder="选择出生日期（可选）"
              style="width: 100%"
              type="date"
              value-format="YYYY-MM-DD"
              @change="addForm.petBirthdayEstimated = false"
          />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input
              v-model="ageInput"
              clearable
              placeholder="不知道生日可填年龄，如：2岁3个月 / 约2岁"
              @change="onAgeInput"
          />
        </el-form-item>

        <el-divider content-position="left">详细信息（可选）</el-divider>
        <el-form-item label="毛色">
          <el-input v-model="addForm.petColor" clearable maxlength="16" placeholder="如：三花"/>
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number
              v-model="addForm.petWeight"
              :max="200"
              :min="0"
              :precision="1"
              :step="0.5"
              placeholder="如：4.5"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="绝育">
          <el-radio-group v-model="addForm.petSterilization">
            <el-radio :label="true" border>已绝育</el-radio>
            <el-radio :label="false" border>未绝育</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="健康状况">
          <el-input v-model="addForm.petStatus" clearable maxlength="16" placeholder="默认为「健康」"/>
        </el-form-item>
        <el-form-item label="芯片号">
          <el-input v-model="addForm.petChip" clearable maxlength="32" placeholder="宠物芯片编号"/>
        </el-form-item>

        <el-divider content-position="left">健康档案（可选）</el-divider>
        <el-form-item label="过敏史">
          <el-input v-model="addForm.petAllergy" :rows="2" maxlength="128" placeholder="如：对青霉素过敏" type="textarea"/>
        </el-form-item>
        <el-form-item label="既往病史">
          <el-input v-model="addForm.petHistory" :rows="2" maxlength="128" placeholder="曾患疾病" type="textarea"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.petNote" :rows="2" maxlength="128" placeholder="其他想告诉医生的信息" type="textarea"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {Pet, examplePet} from "@/model/entiy/Pet";
import {getAge, parseAgeToBirthday} from "@/utils/TimeUtil";
import TagSex from "@/components/show/TagSex.vue";
import {onMounted, reactive, ref} from "vue";
import {reqAddSelfPet, reqClientPet} from "@/request/PetApi";
import {reqReturnPet} from "@/request/AdoptApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import UpImg from "@/components/UpImg.vue";
import {Plus} from "@element-plus/icons-vue";

const petList = ref<Pet[]>([]);

// 退养：把自己名下的宠物退回，宠物变回无主待领养
const returnPet = (pet: Pet): void => {
  ElMessageBox.confirm(
      `确定退养【${pet.petName}】吗？退养后它将回到待领养列表，可以被其他人领养。`,
      '退养确认',
      {confirmButtonText: '确定退养', cancelButtonText: '再想想', type: 'warning'}
  ).then(() => {
    reqReturnPet(pet.petId).then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '退养成功')
        reqClientPet().then(r => {
          petList.value = r
        })
      } else {
        ElMessage.error(res.msg || '退养失败')
      }
    })
  }).catch(() => ElMessage.info('已取消'))
}

/*=====添加宠物=====*/
const addVisible = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = ref<Pet>(examplePet())
const ageInput = ref("")
const addRules = reactive<FormRules>({
  petName: [{required: true, message: '请给宠物起一个名字', trigger: 'blur'}],
  petVariety: [{required: true, message: '请填写宠物品种', trigger: 'blur'}],
})
const openAddDialog = (): void => {
  addForm.value = examplePet()
  ageInput.value = ""
  addFormRef.value?.clearValidate()
  addVisible.value = true
}
// 头像上传回调
const changePhoto = (val: string): void => {
  addForm.value.petPhoto = val
}
// 输入年龄推算生日（带「约」为估算）
const onAgeInput = (): void => {
  const r = parseAgeToBirthday(ageInput.value)
  if (r) {
    addForm.value.petBirthday = r.birthday
    addForm.value.petBirthdayEstimated = r.estimated
  }
}
const submitAdd = (): void => {
  addFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    reqAddSelfPet(addForm.value).then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '添加成功')
        addVisible.value = false
        reqClientPet().then(r => {
          petList.value = r
        })
      } else {
        ElMessage.error(res.msg || '添加失败')
      }
    })
  })
}

// 响应式列数：手机 1 列，平板 2 列，桌面 3 列
const descColumn = ref(3)
const updateColumn = () => {
  const w = window.innerWidth
  if (w < 576) descColumn.value = 1
  else if (w < 992) descColumn.value = 2
  else descColumn.value = 3
}

onMounted(() => {
  updateColumn()
  window.addEventListener('resize', updateColumn)
  reqClientPet().then(res => {
    petList.value = res
  })
})
</script>

<style lang="scss" scoped>
@import url("/css/pa-card.css");

.pet-head {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

/* 添加宠物弹窗：头像区 */
.photo-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 4px 0 8px;

  .photo-tip {
    font-size: 12px;
    color: #909399;
  }
}

/* 覆盖 UpImg 组件默认的绝对定位（那是给老后台弹窗右上角设计的，会把头像浮到表单上面） */
:deep(.avatar-uploader) {
  position: static;
  left: auto;
  margin: 0;
  border-radius: 50%;

  .avatar {
    border-radius: 50%;
    object-fit: cover;
  }

  .el-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
  }
}

.my-row-2 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-areas: "left right right right";
  /* 在屏幕宽度小于992px时改变布局 */
  @media screen and (max-width: 992px) {
    grid-template-columns: 1fr;
    grid-template-areas: "left" "right";
  }

  .left {
    grid-area: left;
    text-align: center;

    img.my-avatar {
      border-radius: 50%;
      width: 128px;
      height: 128px;
    }
  }

  .right {
    grid-area: right;
    min-width: 0;
    overflow: hidden;
  }

  @media (max-width: 576px) {
    .left img.my-avatar {
      width: 96px;
      height: 96px;
    }
  }
}
</style>
