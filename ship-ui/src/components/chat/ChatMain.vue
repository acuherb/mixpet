<template>
  <div ref="innerRef" class="msg-scroll" @click="rollBottom">
    <!--信息主体-->
    <div v-for="m in msgList" :class="{'right-align':m.isMine}" class="msg-card">
      <MyAvatar :name="m.rolePhoto"/>
      <div class="msg">
        <span class="msg-meta">{{ m.roleName }}&nbsp;{{ getDisplayTime(m.createTime) }}</span>
        <div :class="m.isMine?'bg-mine':'bg-other'" class="msgText">
          <template v-if="m.msgType === 'FILE'">
            <div class="file-msg">
              <el-icon size="26"><Document/></el-icon>
              <div class="file-info">
                <span class="file-name" :title="m.msgName || m.msgContent">{{ m.msgName || m.msgContent }}</span>
                <a class="file-link"
                   :href="'/api/common/downChat?type=file&name='+m.msgContent"
                   :download="m.msgName || m.msgContent">下载</a>
              </div>
            </div>
          </template>
          <img v-else-if="m.isImg" :src="'/api/common/downChat?type=image&name='+m.msgContent" alt="聊天图片"
               @click="openPreview('/api/common/downChat?type=image&name='+m.msgContent)"/>
          <div v-else class="msg-txt">{{ m.msgContent }}</div>
        </div>
      </div>
    </div>
  </div>
  <!--图片点击放大预览-->
  <el-dialog v-model="previewVisible" title="图片预览" width="80%" top="5vh" @close="closePreview">
    <div class="preview-toolbar">
      <el-button-group>
        <el-button @click="scaleDown">缩小</el-button>
        <el-button @click="scaleUp">放大</el-button>
        <el-button @click="resetScale">重置(100%)</el-button>
      </el-button-group>
      <span class="preview-scale">{{ Math.round(previewScale * 100) }}%</span>
      <span class="preview-tip">滚轮可缩放，拖动画布查看大图</span>
    </div>
    <div class="preview-body" @wheel.prevent="onWheel">
      <img v-if="previewSrc" :src="previewSrc" alt="预览图片" class="preview-img"
           :style="{ transform: `scale(${previewScale})` }"/>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
/*=====前台的聊天记录展示，会自动将滚动条划到底=====*/
import {MsgDo} from "@/model/DO/MsgDo";
import {onBeforeUpdate, onMounted, ref} from "vue";
import {getDisplayTime} from "@/utils/TimeUtil";
import MyAvatar from "@/components/show/MyAvatar.vue";
import {Document} from "@element-plus/icons-vue";

defineProps<{
  msgList: MsgDo[]
}>()

// 图片点击放大预览
const previewVisible = ref(false)
const previewSrc = ref("")
const previewScale = ref(1)
const openPreview = (src: string): void => {
  previewSrc.value = src
  previewScale.value = 1
  previewVisible.value = true
}
const closePreview = (): void => {
  previewVisible.value = false
  previewSrc.value = ""
  previewScale.value = 1
}
const scaleUp = (): void => {
  previewScale.value = Math.min(5, +(previewScale.value + 0.25).toFixed(2))
}
const scaleDown = (): void => {
  previewScale.value = Math.max(0.25, +(previewScale.value - 0.25).toFixed(2))
}
const resetScale = (): void => { previewScale.value = 1 }
const onWheel = (e: WheelEvent): void => {
  if (e.deltaY < 0) scaleUp()
  else scaleDown()
}

// 滚动条到底
const innerRef = ref<HTMLDivElement>()
const rollBottom = () => {
  setTimeout(() => {
    innerRef.value!.scrollTop = innerRef.value!.scrollHeight
  }, 500)
}
onBeforeUpdate(() => {
  rollBottom()
})
onMounted(() => {
  rollBottom()
})
</script>

<style lang="scss" scoped>
/*滚动外壳*/
.msg-scroll {
  overflow: auto;
  height: 100%;
}

/*信息外壳：头像 + 消息列*/
.msg-card {
  display: flex;
  gap: 8px;
  padding: 4px 0;

  :deep(.my-avatar) {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
  }

  /*消息列（名字时间 + 气泡）*/
  .msg {
    display: flex;
    flex-direction: column;
    max-width: 55%;

    /*用户名与时间*/
    .msg-meta {
      margin: 0 4px 2px;
      font-size: 12px;
      color: #909399;
    }

    /*消息气泡*/
    .msgText {
      padding: 8px 12px;
      border-radius: 12px;
      line-height: 1.5;
      word-break: break-word;
      white-space: pre-wrap;

      /*聊天图片：限制尺寸，不撑爆气泡；可点击放大*/
      img {
        display: block;
        max-width: 200px;
        max-height: 240px;
        border-radius: 8px;
        object-fit: contain;
        cursor: pointer;
      }

      /*文件消息卡片*/
      .file-msg {
        display: flex;
        gap: 8px;
        align-items: center;
        min-width: 140px;
        max-width: 240px;

        .file-info {
          display: flex;
          flex-direction: column;
          gap: 4px;
          min-width: 0;

          .file-name {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font-size: 13px;
          }

          .file-link {
            font-size: 12px;
            color: #409eff;
            text-decoration: none;
            margin-right: 8px;
            &:hover { text-decoration: underline; }
          }
        }
      }
    }

    /*我的消息：微信绿气泡*/
    .bg-mine {
      background-color: #95ec69;
      color: #1a1a1a;
      border-bottom-right-radius: 4px;
    }

    /*对方的消息：白色气泡*/
    .bg-other {
      background-color: #fff;
      color: #303133;
      border: 1px solid #ececec;
      border-bottom-left-radius: 4px;
    }
  }
}

/*我的消息：靠右显示*/
.right-align {
  flex-direction: row-reverse;

  .msg {
    align-items: flex-end;

    .msg-meta {
      text-align: right;
    }
  }
}

/*手机端：气泡放宽一点，方便阅读*/
@media (max-width: 768px) {
  .msg-card .msg {
    max-width: 75%;
  }
}

/*=====图片预览弹窗=====*/
.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;

  .preview-scale {
    font-size: 13px;
    color: #606266;
  }

  .preview-tip {
    font-size: 12px;
    color: #909399;
  }
}

.preview-body {
  overflow: auto;
  max-height: 70vh;
  text-align: center;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 8px;

  .preview-img {
    transform-origin: center center;
    transition: transform 0.1s ease;
    max-width: none;
    display: inline-block;
  }
}
</style>
