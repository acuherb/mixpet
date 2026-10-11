<template>
  <div ref="innerRef" class="msg-scroll" @click="rollBottom">
    <!--信息主体-->
    <div v-for="m in msgList" :class="{'right-align':m.isMine}" class="msg-card">
      <MyAvatar :name="m.rolePhoto"/>
      <div class="msg">
        <span class="msg-meta">{{ m.roleName }}&nbsp;{{ getDisplayTime(m.createTime) }}</span>
        <div :class="m.isMine?'bg-mine':'bg-other'" class="msgText">
          <img v-if="m.isImg" :src="'/api/common/downChat?name='+m.msgContent" alt="聊天图片"/>
          <div v-else class="msg-txt">{{ m.msgContent }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
/*=====前台的聊天记录展示，会自动将滚动条划到底=====*/
import {MsgDo} from "@/model/DO/MsgDo";
import {onBeforeUpdate, onMounted, ref} from "vue";
import {getDisplayTime} from "@/utils/TimeUtil";
import MyAvatar from "@/components/show/MyAvatar.vue";

defineProps<{
  msgList: MsgDo[]
}>()

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

      /*聊天图片：限制尺寸，不撑爆气泡*/
      img {
        display: block;
        max-width: 200px;
        max-height: 240px;
        border-radius: 8px;
        object-fit: contain;
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
</style>
