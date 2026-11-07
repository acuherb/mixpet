<template>
  <ChatPanel :groupList="groupList" :msg-list="msgList" :role-name="doctor.doctorName" opType="医生"
             @cutOther="cutDoctor"
             @sendImgMsg="sendImgMsg"
             @sendFileMsg="sendFileMsg"
             @sendTxtMsg="sendTextMsg"
  />
</template>

<script lang="ts" setup>
import {ChatGroup} from "@/model/DO/ChatGroup";
import {MsgDo} from "@/model/DO/MsgDo";
import ChatPanel from "@/components/chat/ChatPanel.vue";
import {onBeforeUnmount, ref} from "vue";
import {reqClientGetNearDoctor, reqClientMsgGroup, reqClientMsgList, reqSendClientMsg} from "@/request/MsgApi";
import {Msg} from "@/model/entiy/Msg";
import {getNowISO} from "@/utils/TimeUtil";
import {send, subscribe} from "@/utils/chatSocket";

/**
 ┌───────────────────────────────────┐
 │=============准备好的函数============│
 └───────────────────────────────────┘
 */
// 分组与聊天记录
const groupList = ref<ChatGroup[]>([])
const msgList = ref<MsgDo[]>([])
// 当前医生
const doctor = {
  doctorId: 1,
  doctorName: "无",
  doctorPhoto: "defaultDoctor.jpg"
}
// 切换医生
const cutDoctor = (doctorID: number): void => {
  doctor.doctorId = doctorID
  reqClientMsgList(doctorID).then(res1 => {
    msgList.value = res1
  })
  reqClientMsgGroup(doctorID).then(res2 => {
    doctor.doctorName = res2[0].roleName
    doctor.doctorPhoto = res2[0].rolePhoto
    groupList.value = res2
  })
}

/**
 ┌───────────────────────────────────┐
 │=============初始化的逻辑============│
 └───────────────────────────────────┘
 */
const props = defineProps<{
  doctorId?: string
}>()
if (props.doctorId == null || props.doctorId == '') {
  // 没有收到ID，执行ID的初始化逻辑
  reqClientGetNearDoctor().then(res => {
    doctor.doctorId = res.roleId;
    doctor.doctorName = res.roleName;
    cutDoctor(res.roleId)
  })
} else {
  // 收到了ID，切换过去
  doctor.doctorId = parseInt(props.doctorId)
  cutDoctor(doctor.doctorId)
}

// 共享单例连接：断线自动指数退避重连，JSON 解析与非消息帧由 chatSocket 统一处理
const unsubscribeChat = subscribe((dataMsg: Msg) => {
  if (dataMsg.doctorId === doctor.doctorId) {
    const msgDo: MsgDo = {
      createTime: getNowISO(),
      isImg: dataMsg.isImg,
      msgType: dataMsg.msgType,
      msgName: dataMsg.msgName,
      isMine: false,
      isRead: false,
      msgContent: dataMsg.msgContent,
      roleName: doctor.doctorName,
      rolePhoto: doctor.doctorPhoto
    }
    msgList.value.push(msgDo)
  }
})

/**
 ┌───────────────────────────────────┐
 │=============聊天时的过程============│
 └───────────────────────────────────┘
 */
// 发送文字消息
const sendTextMsg = (val: string): void => {
  if (val === "") return;
  const msg: Msg = {
    doctorId: doctor.doctorId,
    isImg: false,
    msgContent: val
  }
  reqSendClientMsg(msg).then(res => {
    if (res.code === 1) {
      reqClientMsgList(doctor.doctorId).then(res1 => {
        msgList.value = res1
      })
    }
  })
  send(msg)
}
// 发送图片消息
const sendImgMsg = (imgName: string): void => {
  const msg: Msg = {
    doctorId: doctor.doctorId,
    isImg: true,
    msgType: 'IMAGE',
    msgContent: imgName
  }
  reqSendClientMsg(msg).then(res => {
    if (res.code === 1) {
      reqClientMsgList(doctor.doctorId).then(res1 => {
        msgList.value = res1
      })
    }
  })
  send(msg)
}
// 发送文件消息
const sendFileMsg = (fileMeta: {name: string, origin: string}): void => {
  const msg: Msg = {
    doctorId: doctor.doctorId,
    isImg: false,
    msgType: 'FILE',
    msgName: fileMeta.origin,
    msgContent: fileMeta.name
  }
  reqSendClientMsg(msg).then(res => {
    if (res.code === 1) {
      reqClientMsgList(doctor.doctorId).then(res1 => {
        msgList.value = res1
      })
    }
  })
  send(msg)
}
/**
 ┌───────────────────────────────────┐
 │=============退出时的操作============│
 └───────────────────────────────────┘
 */
onBeforeUnmount(() => {
  unsubscribeChat()
})
</script>

<style scoped>
</style>