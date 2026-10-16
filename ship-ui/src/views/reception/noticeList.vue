<template>
  <div class="nl-page">
    <header class="nl-top">
      <a class="nl-back" @click="router.push('/')"><el-icon><ArrowLeft/></el-icon> 返回首页</a>
      <h2>网站公告</h2>
    </header>

    <div class="nl-body" v-loading="loading">
      <el-input v-model="kw" clearable placeholder="搜索公告标题" style="max-width:320px" @input="debSearch"/>
      <div class="nl-list">
        <div v-for="n in list.records" :key="n.noticeId" class="nl-item" @click="openDetail(n)">
          <span class="nl-title">{{ n.noticeTitle }}</span>
          <span class="nl-time">{{ noticeTime(n.createTime) }}</span>
          <el-icon class="nl-arrow"><ArrowRight/></el-icon>
        </div>
        <el-empty v-if="!loading && !list.records.length" description="暂无公告"/>
      </div>
      <div v-if="list.total > qp.pageSize" class="nl-pager">
        <el-pagination background layout="prev, pager, next" :total="list.total"
                       :page-size="qp.pageSize" :current-page="qp.numPage" @current-change="onPage"/>
      </div>
    </div>

    <el-dialog v-model="detailView" :title="cur?.noticeTitle || '公告详情'" width="640px">
      <div class="nl-content">{{ detailText || "加载中..." }}</div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {ArrowLeft, ArrowRight} from "@element-plus/icons-vue";
import {reqNoticeDetail, reqNoticePublic} from "@/request/NoticeApi";
import {NoticeDto} from "@/model/DO/NoticeDto";
import {Page} from "@/model/DO/Page";
import {getDisplayTime} from "@/utils/TimeUtil";
import {debounce} from "@/utils/DebounceUtil";

const router = useRouter()
const qp = reactive({numPage: 1, pageSize: 10, noticeName: ""})
const list = ref<Page<NoticeDto>>({records: [], total: 0})
const loading = ref(false)
const kw = ref("")
const noticeTime = (t: string | null | undefined): string => (t ? getDisplayTime(t) : "")

const load = (): void => {
  loading.value = true
  qp.noticeName = kw.value
  reqNoticePublic(qp).then(res => { list.value = res }).finally(() => { loading.value = false })
}
onMounted(load)
const onPage = (p: number): void => { qp.numPage = p; load() }
const debSearch = debounce(() => { qp.numPage = 1; load() }, 400)

const detailView = ref(false)
const cur = ref<NoticeDto | null>(null)
const detailText = ref("")
const openDetail = (n: NoticeDto): void => {
  cur.value = n
  detailView.value = true
  detailText.value = ""
  reqNoticeDetail(n.noticeId).then(t => { detailText.value = t || "暂无内容" })
}
</script>

<style lang="scss" scoped>
.nl-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 16px 60px;
  min-height: 100vh;
  box-sizing: border-box;
}

.nl-top {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;

  h2 { margin: 0; font-size: 22px; color: #1F2D3D; }

  .nl-back {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #1E6FA8;
    cursor: pointer;

    &:hover { text-decoration: underline; }
  }
}

.nl-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nl-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #E4E7ED;
  border-radius: 10px;
  padding: 14px 16px;
  cursor: pointer;
  transition: box-shadow .2s, transform .2s;

  &:hover {
    box-shadow: 0 6px 16px rgba(31, 45, 61, .10);
    transform: translateY(-1px);
  }

  .nl-title {
    flex: 1;
    min-width: 0;
    font-size: 15px;
    color: #1F2D3D;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .nl-time { font-size: 12px; color: #909399; }
  .nl-arrow { color: #C0C4CC; }
}

.nl-pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.nl-content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #303133;
}
</style>
