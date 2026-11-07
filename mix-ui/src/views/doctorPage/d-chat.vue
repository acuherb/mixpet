<template>
  <ChatPanel :groupList="groupList" :msg-list="msgList" :role-name="client.clientName" opType="用户"
             @cutOther="cutClient "
             @sendImgMsg="sendImgMsg"
             @sendFileMsg="sendFileMsg"
             @sendTxtMsg="sendTextMsg"
  />
</template>

<script lang="ts" setup>
import {ChatGroup} from "@/model/DO/ChatGroup";
import {MsgDo} from "@/model/DO/MsgDo";
import ChatPanel from "@/components/chat/ChatPanel.vue";
import {onBeforeUnmount, onMounted, ref} from "vue";
import {useRoute} from "vue-router";
import {reqDoctorGetNearClient, reqDoctorMsgGroup, reqDoctorMsgList, reqSendDoctorMsg} from "@/request/MsgApi";
import {Msg} from "@/model/entiy/Msg";
import {getNowISO} from "@/utils/TimeUtil";
import {send, subscribe} from "@/utils/chatSocket";

/**
 ┌───────────────────────────────────┐
 │=============初始化的准备============│
 └───────────────────────────────────┘
 */
const route = useRoute()
onMounted(() => {
  // 支持从工作台带参进入，直接选中指定客户会话
  const cid = Number(route.query.clientId)
  if (cid) {
    cutClient(cid)
  } else {
    reqDoctorGetNearClient().then(res => {
      client.clientId = res.roleId
      client.clientName = res.roleName
      cutClient(res.roleId)
    })
  }
})
// 当前用户的ID
const client = {
  clientId: 1,
  clientName: "无",
  clientPhoto: "defaultDoctor.jpg",
}
// 得到分组与聊天记录
const groupList = ref<ChatGroup[]>([]);
const msgList = ref<MsgDo[]>([]);
// 共享单例连接：断线自动指数退避重连，JSON 解析与非消息帧由 chatSocket 统一处理
const unsubscribeChat = subscribe((dataMsg: Msg) => {
  if (dataMsg.clientId === client.clientId) {
    const msgDo: MsgDo = {
      createTime: getNowISO(),
      isImg: dataMsg.isImg,
      msgType: dataMsg.msgType,
      msgName: dataMsg.msgName,
      isMine: false,
      isRead: false,
      msgContent: dataMsg.msgContent,
      roleName: client.clientName,
      rolePhoto: client.clientPhoto
    }
    msgList.value.push(msgDo)
  }
})
/**
 ┌───────────────────────────────────┐
 │=============聊天时的过程============│
 └───────────────────────────────────┘
 */
// 切换用户
const cutClient = (clientId: number): void => {
  client.clientId = clientId
  reqDoctorMsgList(clientId).then(res1 => {
    msgList.value = res1
  })
  reqDoctorMsgGroup(clientId).then(res2 => {
    client.clientName = res2[0].roleName
    client.clientPhoto = res2[0].rolePhoto
    groupList.value = res2
  })
}

// 发送文字消息
const sendTextMsg = (val: string): void => {
  if (val === "") return;
  const msg: Msg = {
    clientId: client.clientId,
    isImg: false,
    msgContent: val
  }
  reqSendDoctorMsg(msg).then(res => {
    if (res.code === 1) {
      reqDoctorMsgList(client.clientId).then(res1 => {
        msgList.value = res1
      })
    }
  })
  send(msg)
}
// 发送图片消息
const sendImgMsg = (imgName: string): void => {
  const msg: Msg = {
    clientId: client.clientId,
    isImg: true,
    msgType: 'IMAGE',
    msgContent: imgName
  }
  reqSendDoctorMsg(msg).then(res => {
    if (res.code === 1) {
      reqDoctorMsgList(client.clientId).then(res1 => {
        msgList.value = res1
      })
    }
  })
  send(msg)
}
// 发送文件消息
const sendFileMsg = (fileMeta: {name: string, origin: string}): void => {
  const msg: Msg = {
    clientId: client.clientId,
    isImg: false,
    msgType: 'FILE',
    msgName: fileMeta.origin,
    msgContent: fileMeta.name
  }
  reqSendDoctorMsg(msg).then(res => {
    if (res.code === 1) {
      reqDoctorMsgList(client.clientId).then(res1 => {
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