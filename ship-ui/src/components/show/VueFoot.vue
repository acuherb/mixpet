<template>
  <footer class="vue-foot">
    <!-- 页脚内容 -->
    <div class="foot-main">
      <div class="foot-cols">
        <!-- 品牌 -->
        <div class="foot-col foot-brand">
          <div class="brand-line">
            <span class="brand-mark">
              <img v-if="hosp.logo" :src="'/api/common/download?name=' + hosp.logo" alt="logo"/>
              <template v-else>PET</template>
            </span>
            <span class="brand-name">{{ hosp.name || 'mixpet' }}</span>
          </div>
          <p class="brand-desc">{{ hosp.intro || '宠物医院管理系统' }}</p>
        </div>

        <!-- 快捷入口 -->
        <div class="foot-col">
          <h3 class="col-title">快捷入口</h3>
          <ul class="col-list">
            <li><router-link to="/reception/doctorSee">医生团队</router-link></li>
            <li><router-link to="/reception/petSee">待领养宠物</router-link></li>
            <li><router-link to="/reception/chat">在线咨询</router-link></li>
          </ul>
        </div>

        <!-- 关于 -->
        <div class="foot-col">
          <h3 class="col-title">关于我们</h3>
          <ul class="col-list">
            <li>营业时间：{{ hosp.bizHours || '09:00 - 20:00' }}</li>
            <li>咨询电话：{{ hosp.phone || '400-000-0000' }}</li>
            <li>地址：{{ hosp.address || '请在后台设置' }}</li>
          </ul>
        </div>
      </div>

      <div class="foot-bottom">
        <span>© 2026 mixpet</span>
        <span class="dot">·</span>
        <span>基于 <a href="https://github.com/MixJade/MixPet" target="_blank" rel="noopener">MixPet</a> 二次开发</span>
      </div>
    </div>
  </footer>
</template>

<script lang="ts" setup>
import {onMounted} from 'vue'
import {hospital, loadHospital} from "@/utils/hospital";

// 医院信息（全局单例，App 已加载；此处兜底再触发一次）
const hosp = hospital
onMounted(() => loadHospital())
</script>

<style lang="scss" scoped>
.vue-foot {
  width: 100%;
  margin-top: 2.5rem;
  /* 页脚背景随主题色联动（极淡品牌色） */
  background: linear-gradient(180deg, var(--brand-soft), #ffffff);
}

/* 主体 */
.foot-main {
  border-top: 1px solid #E4E7ED;
  padding: 36px 24px 24px;
  max-width: 1200px;
  margin: 0 auto;
  box-sizing: border-box;
}

.foot-cols {
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr;
  gap: 32px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

.foot-col {
  text-align: left;
}

.foot-brand .brand-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;

  .brand-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 6px;
    background: var(--brand);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
    overflow: hidden;

    img { width: 100%; height: 100%; object-fit: cover; }
  }

  .brand-name {
    font-size: 16px;
    font-weight: 700;
    color: var(--brand);
    letter-spacing: 0.3px;
  }
}

.brand-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.col-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1F2D3D;
}

.col-list {
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;

    a {
      color: #606266;
      transition: color 0.15s;

      &:hover {
        color: var(--brand);
      }
    }
  }
}

/* 底栏 */
.foot-bottom {
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px solid #E4E7ED;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;

  a {
    color: var(--brand);

    &:hover {
      text-decoration: underline;
    }
  }

  .dot {
    color: #C0C4CC;
  }
}
</style>