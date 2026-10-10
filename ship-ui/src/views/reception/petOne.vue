<template>
  <div>
    <div class="base-page">
      <!-- 导航栏-->
      <PageHead>宠物详情</PageHead>
      <MyRow2>
        <!-- 左边div：宠物头像-->
        <div class="left-div">
          <img :src="'/api/common/download?name='+pet.petPhoto" alt="头像" class="my-avatar">
          <h3>{{ pet.petName }}</h3>
          <span>{{ getAge(pet.petBirthday, pet.petBirthdayEstimated) }}</span>
        </div>
        <template #right>
          <!-- 右边div，宠物信息 分组卡片 -->
          <div class="info-card">
            <div class="card-title">基本信息</div>
            <table class="myTable">
              <tr>
                <td class="bold">姓名</td>
                <td>{{ pet.petName || '--' }}</td>
              </tr>
              <tr>
                <td class="bold">品种</td>
                <td>{{ pet.petVariety || '--' }}</td>
              </tr>
              <tr>
                <td class="bold">性别</td>
                <td>{{ pet.petSex ? "公" : "母" }}</td>
              </tr>
              <tr>
                <td class="bold">出生日期</td>
                <td>{{ pet.petBirthdayEstimated ? '约 ' : '' }}{{ pet.petBirthday || '--' }}</td>
              </tr>
              <tr>
                <td class="bold">年龄</td>
                <td>{{ getAge(pet.petBirthday, pet.petBirthdayEstimated) }}</td>
              </tr>
              <tr>
                <td class="bold">毛色</td>
                <td>{{ pet.petColor || '--' }}</td>
              </tr>
              <tr>
                <td class="bold">体重</td>
                <td>{{ pet.petWeight != null ? pet.petWeight + ' kg' : '--' }}</td>
              </tr>
            </table>
          </div>

          <div class="info-card">
            <div class="card-title">健康信息</div>
            <table class="myTable">
              <tr>
                <td class="bold">健康状况</td>
                <td>
                  <el-tag v-if="pet.petStatus">{{ pet.petStatus }}</el-tag>
                  <span v-else>--</span>
                </td>
              </tr>
              <tr>
                <td class="bold">绝育情况</td>
                <td>
                  <el-tag :type="pet.petSterilization ? 'success' : 'info'">
                    {{ pet.petSterilization ? '已绝育' : '未绝育' }}
                  </el-tag>
                </td>
              </tr>
              <tr>
                <td class="bold">芯片号</td>
                <td>{{ pet.petChip || '--' }}</td>
              </tr>
              <tr>
                <td class="bold">过敏史</td>
                <td>{{ pet.petAllergy || '无' }}</td>
              </tr>
              <tr>
                <td class="bold">既往病史</td>
                <td>{{ pet.petHistory || '无' }}</td>
              </tr>
              <tr>
                <td class="bold">是否在世</td>
                <td>
                  <el-tag :type="pet.petAlive ? 'success' : 'danger'">
                    {{ pet.petAlive ? '在世' : '离世' }}
                  </el-tag>
                </td>
              </tr>
            </table>
          </div>

          <div class="info-card">
            <div class="card-title">归属与备注</div>
            <table class="myTable">
              <tr>
                <td class="bold">最近状况</td>
                <td>{{ pet.petInfo || '--' }}</td>
              </tr>
              <tr>
                <td class="bold">备注</td>
                <td>{{ pet.petNote || '--' }}</td>
              </tr>
            </table>
          </div>
          <el-button v-if="inAdopt===0 && pet.clientId==null" style="width: 50%" type="primary"
                     @click="dialogVisible = true">
            申请领养
          </el-button>
          <el-button v-else-if="pet.clientId==null" disabled style="width: 50%" type="info">
            审核中
          </el-button>
        </template>
      </MyRow2>
    </div>
    <!--弹出框-->
    <el-dialog v-model="dialogVisible" title="申请领养">
      <el-form :model="dialogAsk">
        <el-form-item label="申请理由" label-width="140px">
          <el-input v-model="dialogAsk.adoptInfo" autocomplete="off"/>
        </el-form-item>
        <el-form-item label="预付押金" label-width="140px">
          <el-input-number
              v-model="dialogAsk.adoptMoney"
              :max="5000"
              :min="100"
              :step="100"
              controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="adoptPet">
          确认
        </el-button>
      </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import PageHead from "@/components/row/PageHead.vue";
import {examplePet, Pet} from "@/model/entiy/Pet";
import {getAge} from "@/utils/TimeUtil";
import {onMounted, reactive, ref} from "vue";
import {PetAskOne} from "@/model/VO/PetAskOne";
import {reqPetOne} from "@/request/PetApi";
import {reqAdoptPet, reqInAdopt} from "@/request/AdoptApi";
import MyRow2 from "@/components/row/MyRow2.vue";

// 如此获取传参
const props = defineProps<{
  petId: string
}>()
// 样例数据
const pet = ref<Pet>(examplePet())
// 是否领养
const inAdopt = ref<number>(0)

// 弹出框设置
const dialogVisible = ref(false)
const dialogAsk: PetAskOne = reactive({
  petId: parseInt(props.petId),
  adoptInfo: "",
  adoptMoney: 300
})
const adoptPet = () => {
  dialogVisible.value = false
  inAdopt.value = 1
  reqAdoptPet(dialogAsk)
}

onMounted(() => {
  reqPetOne(parseInt(props.petId)).then(res => {
    pet.value = res
  })
  reqInAdopt(parseInt(props.petId)).then(res => {
    inAdopt.value = res
  })
})
</script>

<style lang="scss" scoped>
/*设置大致布局*/
.base-page {
  background-color: whitesmoke;
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.15);
  width: calc(100% - 32px);
  max-width: 1100px;
  margin: 16px auto 40px;
  padding: 20px 24px;
  border-radius: 12px;

  img.my-avatar {
    border-radius: 50%;
    margin-top: 10px;
    width: 128px;
    height: 128px;
  }
}

/* 覆盖 MyRow2 内部右侧的滚动限制，让整页自然滚动 */
:deep(.my-row-2 .right) {
  max-height: none;
  overflow-y: visible;
  text-align: left;
  padding-left: 16px;
}

:deep(.my-row-2) {
  /* 屏幕较窄时左侧头像占一行，右栏占满 */
  @media (max-width: 768px) {
    gap: 16px;
  }
}

/* 窄屏下左侧头像居中显示 */
@media (max-width: 768px) {
  .left-div {
    margin-bottom: 16px;
  }
}

/* 分组卡片 */
.info-card {
  margin-bottom: 16px;

  .card-title {
    font-weight: bold;
    font-size: 15px;
    color: #409eff;
    border-left: 4px solid #409eff;
    padding-left: 8px;
    margin-bottom: 8px;
  }
}

/*设置信息展示表格*/
.myTable {
  border-collapse: collapse;
  /*设置定位*/
  width: 100%;
  margin-bottom: 4px;

  td {
    border: 1px solid black;
    border-left: none;
    border-right: none;
    padding: 2px;
  }

  tr:first-child td {
    border-top: none;
  }

  tr:last-child td {
    border-bottom: none;
  }

  .bold {
    font-weight: bold;
  }
}
</style>