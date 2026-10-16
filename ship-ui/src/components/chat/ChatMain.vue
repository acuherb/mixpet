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
  <!--图片全屏预览：滚轮/双指缩放 + 鼠标/单指拖动 + 预览框贴合图片-->
  <teleport to="body">
    <transition name="pv-fade">
      <div v-if="previewVisible" class="pv-mask" @click.self="closePreview">
        <div class="pv-wrap" :style="wrapStyle"
             @mousedown.prevent="onDragStart" @wheel.prevent="onWheel"
             @touchstart.prevent="onTouchStart" @touchmove.prevent="onTouchMove" @touchend="onTouchEnd">
          <img ref="imgEl" :src="previewSrc" alt="预览图片" class="pv-img" :style="imgStyle"
               draggable="false" @load="onImgLoad"/>
        </div>
        <button class="pv-close" title="关闭" @click="closePreview"><el-icon><Close/></el-icon></button>
        <div class="pv-bar">
          <button class="pv-btn" title="缩小" @click="scaleDown"><el-icon><ZoomOut/></el-icon></button>
          <span class="pv-zoom">{{ Math.round(previewScale * 100) }}%</span>
          <button class="pv-btn" title="放大" @click="scaleUp"><el-icon><ZoomIn/></el-icon></button>
          <button class="pv-btn" title="重置" @click="resetScale"><el-icon><Refresh/></el-icon></button>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script lang="ts" setup>
/*=====前台的聊天记录展示，会自动将滚动条划到底=====*/
import {MsgDo} from "@/model/DO/MsgDo";
import {computed, onBeforeUpdate, onMounted, ref} from "vue";
import {getDisplayTime} from "@/utils/TimeUtil";
import MyAvatar from "@/components/show/MyAvatar.vue";
import {Close, Document, Refresh, ZoomIn, ZoomOut} from "@element-plus/icons-vue";

defineProps<{
  msgList: MsgDo[]
}>()

// 图片全屏预览：滚轮/双指缩放 + 鼠标/单指拖动 + 预览框贴合图片
const previewVisible = ref(false)
const previewSrc = ref("")
const previewScale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const baseW = ref(0)
const baseH = ref(0)
const imgEl = ref<HTMLImageElement>()
const wrapStyle = computed(() => ({ width: baseW.value + 'px', height: baseH.value + 'px' }))
const imgStyle = computed(() => ({
  transform: `translate(${offsetX.value}px, ${offsetY.value}px) scale(${previewScale.value})`
}))
const openPreview = (src: string): void => {
  previewSrc.value = src
  previewScale.value = 1
  offsetX.value = 0
  offsetY.value = 0
  baseW.value = 0
  baseH.value = 0
  previewVisible.value = true
}
const closePreview = (): void => {
  previewVisible.value = false
  previewSrc.value = ""
  previewScale.value = 1
  offsetX.value = 0
  offsetY.value = 0
}
// 图片加载后按视口适配尺寸，使预览框贴合图片、无上下空白
const onImgLoad = (): void => {
  const img = imgEl.value
  if (!img || !img.naturalWidth) return
  const fit = Math.min(1, (window.innerWidth * 0.9) / img.naturalWidth, (window.innerHeight * 0.82) / img.naturalHeight)
  baseW.value = Math.round(img.naturalWidth * fit)
  baseH.value = Math.round(img.naturalHeight * fit)
}
const clampScale = (s: number): number => Math.min(5, Math.max(0.2, +s.toFixed(2)))
const scaleUp = (): void => { previewScale.value = clampScale(previewScale.value + 0.25) }
const scaleDown = (): void => { previewScale.value = clampScale(previewScale.value - 0.25) }
const resetScale = (): void => {
  previewScale.value = 1
  offsetX.value = 0
  offsetY.value = 0
}
// 滚轮缩放
const onWheel = (e: WheelEvent): void => {
  if (e.deltaY < 0) scaleUp()
  else scaleDown()
}
// 鼠标拖动移动图片
let dragging = false
let lastX = 0
let lastY = 0
const onDragStart = (e: MouseEvent): void => {
  dragging = true
  lastX = e.clientX
  lastY = e.clientY
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}
const onDragMove = (e: MouseEvent): void => {
  if (!dragging) return
  offsetX.value += e.clientX - lastX
  offsetY.value += e.clientY - lastY
  lastX = e.clientX
  lastY = e.clientY
}
const onDragEnd = (): void => {
  dragging = false
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}
// 触摸：单指拖动 + 双指缩放
let lastTouch: { x: number, y: number } | null = null
let pinchStart = 0
let pinchScaleStart = 1
const touchDist = (t: TouchList): number =>
    Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY)
const onTouchStart = (e: TouchEvent): void => {
  if (e.touches.length === 1) {
    lastTouch = { x: e.touches[0].clientX, y: e.touches[0].clientY }
  } else if (e.touches.length === 2) {
    lastTouch = null
    pinchStart = touchDist(e.touches)
    pinchScaleStart = previewScale.value
  }
}
const onTouchMove = (e: TouchEvent): void => {
  if (e.touches.length === 1 && lastTouch) {
    const t = e.touches[0]
    offsetX.value += t.clientX - lastTouch.x
    offsetY.value += t.clientY - lastTouch.y
    lastTouch = { x: t.clientX, y: t.clientY }
  } else if (e.touches.length === 2 && pinchStart > 0) {
    previewScale.value = clampScale(pinchScaleStart * (touchDist(e.touches) / pinchStart))
  }
}
const onTouchEnd = (): void => {
  lastTouch = null
  pinchStart = 0
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

/*=====图片全屏预览（缩放/拖动/双指，贴合图片）=====*/
.pv-mask {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(8, 10, 13, .9);
  backdrop-filter: blur(4px);
  overflow: hidden;

  /* 图片容器：尺寸=适配后的图片尺寸，紧贴图片无空白 */
  .pv-wrap {
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: grab;
    touch-action: none;

    &:active { cursor: grabbing; }

    .pv-img {
      max-width: none;
      display: block;
      border-radius: 6px;
      box-shadow: 0 16px 50px rgba(0, 0, 0, .6);
      transform-origin: center center;
      user-select: none;
      -webkit-user-drag: none;
      will-change: transform;
    }
  }

  /* 右上角关闭 */
  .pv-close {
    position: fixed;
    top: 18px;
    right: 22px;
    width: 42px;
    height: 42px;
    border: none;
    border-radius: 50%;
    background: rgba(255, 255, 255, .12);
    color: #fff;
    font-size: 22px;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: background .15s;

    &:hover { background: rgba(255, 255, 255, .26); }
  }

  /* 底部玻璃工具栏 */
  .pv-bar {
    position: fixed;
    bottom: 28px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 14px;
    border-radius: 999px;
    background: rgba(28, 32, 38, .85);
    box-shadow: 0 10px 30px rgba(0, 0, 0, .45);
    backdrop-filter: blur(6px);

    .pv-zoom {
      min-width: 56px;
      text-align: center;
      font-size: 13px;
      font-variant-numeric: tabular-nums;
      color: #e8eaed;
    }

    .pv-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 38px;
      height: 38px;
      border: none;
      border-radius: 50%;
      background: rgba(255, 255, 255, .1);
      color: #fff;
      cursor: pointer;
      font-size: 18px;
      transition: background .15s, transform .1s;

      &:hover { background: #1e6fa8; }
      &:active { transform: scale(.92); }
    }
  }
}

/* 预览淡入淡出 */
.pv-fade-enter-active,
.pv-fade-leave-active { transition: opacity .18s ease; }
.pv-fade-enter-from,
.pv-fade-leave-to { opacity: 0; }
</style>
