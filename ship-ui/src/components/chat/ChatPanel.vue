<template>
  <div class="chat-panel">
    <!--顶部：返回栏 + 手机端联系人开关-->
    <div class="chat-top">
      <PageHead>与{{ roleName }}{{ opType }}的聊天</PageHead>
      <el-button v-if="groupList.length>1" :icon="User" class="contact-toggle" round size="small"
                 type="success" plain @click="showContacts=!showContacts">联系人
      </el-button>
    </div>
    <section>
      <!--联系人侧栏：只有一个联系人时自动隐藏；手机端折叠为抽屉，选完自动收起-->
      <aside v-if="groupList.length>1" :class="{open: showContacts}">
        <h4>当前聊天</h4>
        <ul>
          <li class="active">
            <MyAvatar :name="groupList[0]?.rolePhoto"/>
            <span>{{ roleName }}</span>
          </li>
        </ul>
        <h4>其它{{ opType }}</h4>
        <ul>
          <li v-for="gItem in groupList.slice(1)" :key="gItem.roleId"
              @click="cutOtherB(gItem.roleId)">
            <MyAvatar :name="gItem.rolePhoto"/>
            <span>{{ gItem.roleName }}</span>
          </li>
        </ul>
      </aside>
      <!--手机端抽屉打开时的遮罩，点击收起-->
      <div v-if="groupList.length>1 && showContacts" class="contact-mask" @click="showContacts=false"/>
      <div class="chat-right">
        <main>
          <ChatMain :msg-list="msgList"/>
        </main>
        <footer>
          <el-upload
              :before-upload="beforeImgUpload"
              :on-success="handleAvatarSuccess"
              :show-file-list="false"
              action="/api/common/upChat"
              name="myFile"
          >
            <el-icon size="26">
              <PictureRounded/>
            </el-icon>
          </el-upload>
          <el-input
              v-model="textarea1"
              autosize
              placeholder="请输入消息，Enter 发送"
              type="textarea"
              @keydown.enter="onEnterKey"
          />
          <el-button :icon="Position" type="success" @click="sendMsgB">发送</el-button>
        </footer>
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>
/*=====封装的聊天页面，医生与用户只是传递的参数不同=====*/
import {PictureRounded, Position, User} from "@element-plus/icons-vue"
import PageHead from "@/components/row/PageHead.vue";
import {ref} from "vue";
import {ChatGroup} from "@/model/DO/ChatGroup";
import {MsgDo} from "@/model/DO/MsgDo";
import ChatMain from "@/components/chat/ChatMain.vue";
import MyAvatar from "@/components/show/MyAvatar.vue";
import {ElMessage, UploadRawFile} from "element-plus";

defineProps<{
  opType: "用户" | "医生",
  roleName: string,
  groupList: ChatGroup[],
  msgList: MsgDo[]
}>()

// 待发送的消息
const textarea1 = ref("")
// 手机端联系人抽屉的展开状态
const showContacts = ref(false)
const emit = defineEmits<{
  (e: "sendTxtMsg", value: string): void,
  (e: "sendImgMsg", value: string): void,
  (e: "cutOther", value: number): void
}>()
const sendMsgB = (): void => {
  emit("sendTxtMsg", textarea1.value)
  textarea1.value = ""
}
const cutOtherB = (roleId: number): void => {
  showContacts.value = false// 选完联系人自动收起抽屉
  emit("cutOther", roleId)
}
// Enter 直接发送（中文输入法选词、Shift+Enter 换行时不发送）
const onEnterKey = (e: KeyboardEvent): void => {
  if (e.isComposing || e.shiftKey) return
  e.preventDefault()
  sendMsgB()
}

/**
 * 图片上传框：上传成功之后的事件
 * @param response 图片名称
 */
const handleAvatarSuccess = (response: string): void => {
  emit("sendImgMsg", response)
}
/**
 * 图片上传框：上传前的校验
 * @param rawFile 上传的聊天图片
 */
const beforeImgUpload = (rawFile: UploadRawFile) => {
  if (rawFile.type !== 'image/png' && rawFile.type !== 'image/jpeg') {
    ElMessage.error('请上传JPG、PNG图片')
    return false
  } else if (rawFile.size > 3145000) {
    ElMessage.error('图片大小不能大于3MB')
    return false
  }
  return true
}
</script>

<style lang="scss" scoped>
/*聊天主体页面：占满整个视口，方向为上导航下内容*/
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /*手机端浏览器地址栏收起/展开时依然占满*/
  height: 100dvh;

  .chat-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-right: 10px;

    /*桌面端不需要联系人开关，手机端才显示*/
    .contact-toggle {
      display: none;
    }
  }

  section {
    position: relative;
    display: flex;
    flex: 1;
    /*否则 main 的滚动条不会生效*/
    min-height: 0;

    /*联系人侧栏*/
    aside {
      box-sizing: border-box;
      width: 220px;
      flex-shrink: 0;
      overflow-y: auto;
      padding: 10px;
      background-color: #fff;
      border-right: 1px solid #e4e7ed;

      h4 {
        margin: 8px 0 6px;
        padding-left: 8px;
        color: #909399;
        font-size: 12px;
        font-weight: normal;
      }

      ul {
        list-style: none;
        margin: 0;
        padding: 0;
        cursor: pointer;

        li {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 4px;
          padding: 6px 8px;
          border-radius: 8px;
          color: #303133;

          :deep(.my-avatar) {
            width: 36px;
            height: 36px;
            flex-shrink: 0;
          }

          &:hover {
            background-color: #f5f7fa;
          }

          /*当前聊天的联系人*/
          &.active {
            background-color: #e1f3d8;
            color: #67c23a;
            font-weight: bold;
          }
        }
      }
    }

    /*手机端抽屉打开时的遮罩*/
    .contact-mask {
      position: absolute;
      inset: 0;
      z-index: 5;
      background-color: rgba(0, 0, 0, 0.35);
    }

    /*侧边栏右边*/
    .chat-right {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-width: 0;

      /*信息展示区域*/
      main {
        box-sizing: border-box;
        flex: 1;
        min-height: 0;
        padding: 8px;
        background-color: #f5f7fa;
      }

      /*输入区域：随内容自动增高*/
      footer {
        box-sizing: border-box;
        display: flex;
        gap: 8px;
        align-items: flex-end;
        padding: 8px 10px;
        background-color: #fff;
        border-top: 1px solid #e4e7ed;

        .el-upload {
          flex-shrink: 0;
          padding: 6px;
          line-height: 1;
          cursor: pointer;
          color: #67c23a;
        }

        .el-textarea {
          flex: 1;
        }

        .el-button {
          flex-shrink: 0;
        }
      }
    }
  }
}

/*手机端：联系人自动折叠为左侧抽屉*/
@media (max-width: 768px) {
  .chat-panel {
    .contact-toggle {
      display: inline-flex;
    }

    section {
      aside {
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        z-index: 6;
        width: 72vw;
        max-width: 280px;
        transform: translateX(-100%);
        transition: transform 0.25s ease;

        /*展开时*/
        &.open {
          transform: translateX(0);
          box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
        }
      }
    }
  }
}
</style>
