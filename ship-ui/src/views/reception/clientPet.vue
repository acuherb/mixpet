<template>
  <div v-for="pet in petList" :key="pet.petId" class="pa-card">
    <div class="my-row-2">
      <div class="left">
        <img :src="'/api/common/download?name='+pet.petPhoto" alt="宠物" class="my-avatar"/>
      </div>
      <div class="right">
        <el-descriptions :column="descColumn" :title="pet.petName" border>
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
    <el-empty v-if="petList.length===0" description="暂无宠物"/>
  </div>
</template>

<script lang="ts" setup>
import {Pet} from "@/model/entiy/Pet";
import {getAge} from "@/utils/TimeUtil";
import TagSex from "@/components/show/TagSex.vue";
import {onMounted, ref} from "vue";
import {reqClientPet} from "@/request/PetApi";
import {reqReturnPet} from "@/request/AdoptApi";
import {ElMessage, ElMessageBox} from "element-plus";

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

.my-row-2 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-areas: "left right right right";
  /* 在屏幕宽度小于992px时改变布局 */
  @media screen and (max-width: 992px) {
    grid-template-columns: 1fr;
    grid-template-areas:"left" "right";
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