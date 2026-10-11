<template>
  <div class="avatar-box">
    <el-upload
        :before-upload="beforeAvatarUpload"
        :http-request="doUpload"
        :show-file-list="false"
        accept="image/jpeg,image/png"
        class="avatar-uploader"
        name="myFile"
    >
      <img v-if="imageUrl" :src="'/api/common/download?name='+imageUrl" alt="头像" class="avatar"/>
      <el-icon v-else>
        <Plus/>
      </el-icon>
    </el-upload>
    <!-- 随机头像候选：点选即用，不用上传真实照片 -->
    <div v-if="showRandom" class="random-row">
      <img
          v-for="(c, i) in candidates"
          :key="i"
          :src="c"
          alt="随机头像"
          class="random-item"
          title="使用这个头像"
          @click="pickRandom(c)"
      />
      <span class="random-refresh" title="换一批" @click="genCandidates">换</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
/*=====图片上传框（上传前自动压缩，可选随机头像）=====*/
import {Plus} from "@element-plus/icons-vue"
import {onMounted, ref, watch} from "vue";
import {ElMessage, UploadRequestOptions} from "element-plus";
import axios from "axios";
import {compressImage} from "@/utils/ImageUtil";

const props = withDefaults(defineProps<{
  photoNm: string
  // 用于生成「首字头像」的名字（如用户名/宠物名）；不传则用随机动物
  name?: string
  // 是否显示随机头像候选行
  showRandom?: boolean
}>(), {
  name: "",
  showRandom: false
})
const emit = defineEmits<{
  (e: "upPhoto", val: string): void
}>()
watch(() => props.photoNm, (newValue) => {
  imageUrl.value = newValue
})
// 头像上传框
const imageUrl = ref<string>('')
imageUrl.value = props.photoNm

/**
 * 自定义上传：压缩后 POST 到 /api/common/upload（返回新文件名）
 */
const doUpload = async (options: UploadRequestOptions): Promise<void> => {
  try {
    const file = await compressImage(options.file as File)
    const fd = new FormData()
    fd.append('myFile', file)
    const resp = await axios.post<string>('/api/common/upload', fd)
    if (typeof resp.data === 'string' && resp.data && !resp.data.includes(' ')) {
      imageUrl.value = resp.data
      emit('upPhoto', resp.data)
    } else ElMessage.error('上传失败，请重试')
  } catch (e) {
    ElMessage.error('上传失败，请重试')
  }
}

/**
 * 上传前的校验：只收 JPG/PNG（大小不限制，反正会压缩）
 */
const beforeAvatarUpload = (rawFile: File) => {
  if (rawFile.type !== 'image/png' && rawFile.type !== 'image/jpeg') {
    ElMessage.error('请上传JPG、PNG图片')
    return false
  }
  return true
}

/*============ 随机头像：canvas 现场生成，无外部资源 ============*/
const EMOJIS = ['🐱', '🐶', '🐰', '🦊', '🐻', '🐹', '🐼', '🐨']
const candidates = ref<string[]>([])
// 单个候选：柔和渐变底 + 名字首字（没名字就用随机动物）
const makeAvatar = (): string => {
  const size = 256
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = size
  const ctx = canvas.getContext('2d')
  if (!ctx) return ''
  const hue = Math.floor(Math.random() * 360)
  const grad = ctx.createLinearGradient(0, 0, size, size)
  grad.addColorStop(0, `hsl(${hue}, 62%, 74%)`)
  grad.addColorStop(1, `hsl(${(hue + 40) % 360}, 62%, 56%)`)
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, size, size)
  const ch = props.name?.trim()?.charAt(0) || EMOJIS[Math.floor(Math.random() * EMOJIS.length)]
  ctx.fillStyle = '#ffffff'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.font = /[a-zA-Z0-9]/.test(ch) ? 'bold 128px sans-serif' : 'bold 118px sans-serif'
  ctx.fillText(ch, size / 2, size / 2 + size * 0.06)
  return canvas.toDataURL('image/png')
}
const genCandidates = (): void => {
  candidates.value = Array.from({length: 6}, () => makeAvatar())
}
onMounted(() => {
  if (props.showRandom) genCandidates()
})
// 选中一个随机头像：转成文件走正常上传流程
const pickRandom = async (dataURL: string): Promise<void> => {
  try {
    const blob = await (await fetch(dataURL)).blob()
    const file = new File([blob], 'avatar.png', {type: 'image/png'})
    await doUpload({file} as UploadRequestOptions)
  } catch (e) {
    ElMessage.error('生成头像失败，请重试')
  }
}
</script>

<style lang="scss" scoped>
/*整个上传框*/
$primary: #0d6efd;
$info: #6c757d;
.avatar-box {
  display: inline-block;
}

.avatar-uploader {
  border: 2px dashed $info;
  width: 128px;
  height: 128px;
  /*响应式定位*/
  @media (min-width: 768px) {
    position: absolute;
    right: 15%;
    top: 15%;
  }
  @media (max-width: 768px) {
    position: relative;
    left: 50%;
    margin-left: -64px;
    margin-bottom: 10px;
  }
  /*悬停改变边框及加号颜色*/
  &:hover {
    border-color: $primary;
    /*悬停加号颜色*/
    .el-icon {
      color: $primary;
    }
  }

  /*上传的组件*/
  .el-upload {
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: 0.2s;
  }

  /*回显的头像*/
  .avatar {
    width: 128px;
    height: 128px;
    display: block;
  }

  /*加号*/
  .el-icon {
    font-size: 28px;
    color: $info;
    width: 128px;
    height: 128px;
    text-align: center;
  }
}

/*随机头像候选行*/
.random-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;

  .random-item {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    cursor: pointer;
    border: 2px solid transparent;
    transition: 0.15s;
    object-fit: cover;

    &:hover {
      border-color: $primary;
      transform: scale(1.12);
    }
  }

  .random-refresh {
    width: 40px;
    height: 40px;
    line-height: 36px;
    text-align: center;
    border-radius: 50%;
    border: 2px dashed #c0c4cc;
    color: $info;
    font-size: 14px;
    cursor: pointer;
    transition: 0.15s;
    box-sizing: border-box;

    &:hover {
      border-color: $primary;
      color: $primary;
    }
  }
}
</style>
